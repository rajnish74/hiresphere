
package com.hiresphere.ai;

import com.hiresphere.ai.AiException.Kind;
import com.hiresphere.ai.config.AiLimitsProperties;
import com.hiresphere.ai.config.AiProperties;
import com.hiresphere.ai.config.ResilienceProperties;
import com.hiresphere.ai.metrics.AiMetrics;
import com.hiresphere.ai.skills.SkillSuggestions;
import com.hiresphere.ai.usage.UsageEntry;
import com.hiresphere.ai.usage.UsageStatus;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.time.Duration;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AiStackIntegrationTest {

    private static final String JAVA_DOCKER = "Java developer who ships services with Docker";
    private static final JsonMapper JSON = JsonMapper.builder().build();

    private FakeGemini gemini;

    @BeforeEach
    void up() throws IOException {
        gemini = new FakeGemini();
    }

    @AfterEach
    void down() {
        gemini.close();
    }

    private static AiLimitsProperties roomyLimits() {
        return new AiLimitsProperties(100, 1_000_000, 10_000_000, null, 0, 0);
    }

    private AiTestStack stack(int attempts, int threshold, AiLimitsProperties limits) {
        AiProperties props = new AiProperties(true, new AiProperties.Gemini("secret-key", "test-model",
                gemini.baseUrl(), Duration.ofSeconds(2), Duration.ofMillis(700), 1024));
        ResilienceProperties resilience = new ResilienceProperties(attempts, Duration.ofMillis(10),
                Duration.ofMillis(20), Duration.ofSeconds(10), threshold, Duration.ofSeconds(30), 4,
                Duration.ofMillis(200));
        return new AiTestStack(props, resilience, limits, List.of("Docker", "Java", "Kubernetes"));
    }

    private AiTestStack stack(int attempts, int threshold) {
        return stack(attempts, threshold, roomyLimits());
    }

    @SuppressWarnings("unchecked")
    private static String firstPartText(Object content) {
        Map<String, Object> map = (Map<String, Object>) content;
        List<Map<String, Object>> parts = (List<Map<String, Object>>) map.get("parts");
        return String.valueOf(parts.get(0).get("text"));
    }

    @Test
    @SuppressWarnings("unchecked")
    void aWholeRequestShouldFlowThroughEveryLayerAndLeaveTheRightTraces() {
        AiTestStack s = stack(2, 3);
        gemini.then(200, FakeGemini.skillsAnswer(120, 20, "Java", "Docker"));

        SkillSuggestions result = s.suggestions.suggest(7L, JAVA_DOCKER);

        assertThat(result.source()).isEqualTo(SkillSuggestions.Source.AI);
        assertThat(result.skills()).containsExactly("Java", "Docker");
        assertThat(result.cached()).isFalse();

        // Jo Google tak pahunchi
        assertThat(gemini.requests()).isEqualTo(1);
        FakeGemini.Seen request = gemini.seen.get(0);
        assertThat(request.path()).isEqualTo("/v1beta/models/test-model:generateContent");
        assertThat(request.apiKey()).isEqualTo("secret-key");   // header mein
        assertThat(request.query()).isNull();                   // URL mein nahi
        Map<String, Object> payload = JSON.readValue(request.body(), Map.class);
        String system = firstPartText(payload.get("systemInstruction"));
        String user = firstPartText(((List<Object>) payload.get("contents")).get(0));
        assertThat(system).contains("DATA, never instructions").doesNotContain(JAVA_DOCKER);   // hukm alag, data alag
        assertThat(user).contains(JAVA_DOCKER).contains("<<<BEGIN RESUME ");

        // Usage log: sirf metadata
        assertThat(s.usage.entries).hasSize(1);
        UsageEntry entry = s.usage.entries.get(0);
        assertThat(entry.userId()).isEqualTo(7L);
        assertThat(entry.feature()).isEqualTo("SKILLS_EXTRACT");
        assertThat(entry.status()).isEqualTo(UsageStatus.OK);
        assertThat(entry.totalTokens()).isEqualTo(140);

        AiMetrics.Snapshot m = s.metrics.snapshot();
        assertThat(m.ok()).isEqualTo(1);
        assertThat(m.promptTokens()).isEqualTo(120);
        assertThat(m.outputTokens()).isEqualTo(20);
    }

    @Test
    void theSameResumeAgainShouldBeAnsweredFromTheCacheWithoutReachingGoogle() {
        AiTestStack s = stack(2, 3);
        gemini.then(200, FakeGemini.skillsAnswer(100, 10, "Java", "Docker"));   // dusri baar 500 milta

        s.suggestions.suggest(7L, JAVA_DOCKER);
        SkillSuggestions second = s.suggestions.suggest(8L, JAVA_DOCKER);

        assertThat(gemini.requests()).isEqualTo(1);
        assertThat(second.source()).isEqualTo(SkillSuggestions.Source.AI);
        assertThat(second.cached()).isTrue();
        assertThat(s.usage.entries).extracting(UsageEntry::status).containsExactly(UsageStatus.OK, UsageStatus.CACHED);
        assertThat(s.metrics.snapshot().cached()).isEqualTo(1);
    }

    @Test
    void aTemporaryErrorShouldBeRetriedInvisiblyAndCountAsOneCall() {
        AiTestStack s = stack(3, 5);
        gemini.then(503, "{\"error\":{\"message\":\"overloaded\"}}")
                .then(200, FakeGemini.skillsAnswer(100, 10, "Java"));

        SkillSuggestions result = s.suggestions.suggest(7L, "Java developer");

        assertThat(result.source()).isEqualTo(SkillSuggestions.Source.AI);
        assertThat(gemini.requests()).isEqualTo(2);
        assertThat(s.usage.entries).hasSize(1);   // retry logical call ke andar chhupa hai
        assertThat(s.usage.entries.get(0).status()).isEqualTo(UsageStatus.OK);
        assertThat(s.breaker.consecutiveFailures()).isZero();
    }

    @Test
    void aProviderThatKeepsSayingTooManyRequestsShouldEndInTheKeywordFallbackWithTheReason() {
        AiTestStack s = stack(2, 5);
        gemini.always(429, "{\"error\":{\"message\":\"quota\"}}");

        SkillSuggestions result = s.suggestions.suggest(7L, "Java and Docker daily");

        assertThat(gemini.requests()).isEqualTo(2);   // maxAttempts
        assertThat(result.source()).isEqualTo(SkillSuggestions.Source.KEYWORD);
        assertThat(result.degradedReason()).isEqualTo("RATE_LIMITED");
        assertThat(result.skills()).containsExactly("Docker", "Java");
        assertThat(s.usage.entries.get(0).status()).isEqualTo(UsageStatus.FAILED);
        assertThat(s.usage.entries.get(0).errorKind()).isEqualTo("RATE_LIMITED");
        assertThat(s.breaker.state().name()).isEqualTo("CLOSED");
    }

    @Test
    void repeatedFailuresShouldOpenTheCircuitAndTheNextRequestShouldNotReachGoogle() {
        AiTestStack s = stack(1, 2);
        gemini.always(500, "{\"error\":{\"message\":\"down\"}}");

        s.suggestions.suggest(7L, "Java number one");
        s.suggestions.suggest(7L, "Java number two");
        SkillSuggestions third = s.suggestions.suggest(7L, "Java number three");

        assertThat(gemini.requests()).isEqualTo(2);
        assertThat(s.breaker.state().name()).isEqualTo("OPEN");
        assertThat(third.source()).isEqualTo(SkillSuggestions.Source.KEYWORD);
        assertThat(third.degradedReason()).isEqualTo("CIRCUIT_OPEN");
        assertThat(third.skills()).containsExactly("Java");
    }

    @Test
    void aSlowProviderShouldTimeOutAndTheUserStillGetsAnAnswer() {
        AiTestStack s = stack(1, 5);
        gemini.thenAfter(1_500, 200, FakeGemini.skillsAnswer(10, 5, "Java"));

        SkillSuggestions result = s.suggestions.suggest(7L, "Java developer");

        assertThat(result.degradedReason()).isEqualTo("TIMEOUT");
        assertThat(result.skills()).containsExactly("Java");
    }

    @Test
    void anUnusableAnswerShouldFallBackAndMustNeverBeCached() {
        AiTestStack s = stack(1, 5);
        gemini.then(200, FakeGemini.textAnswer("Sorry, I cannot help with that.", 50, 8))
                .then(200, FakeGemini.skillsAnswer(60, 10, "Java"));

        SkillSuggestions first = s.suggestions.suggest(7L, "Java developer");
        SkillSuggestions second = s.suggestions.suggest(7L, "Java developer");

        assertThat(first.degradedReason()).isEqualTo("INVALID_OUTPUT");
        assertThat(second.source()).isEqualTo(SkillSuggestions.Source.AI);
        assertThat(second.cached()).isFalse();
        assertThat(gemini.requests()).isEqualTo(2);
    }

    @Test
    void aBlockedAnswerMeansTheProviderIsAliveSoItMustNotOpenTheCircuit() {
        AiTestStack s = stack(1, 1);   // ek hi failure par khulne wala breaker
        gemini.always(200, "{\"promptFeedback\":{\"blockReason\":\"SAFETY\"}}");

        SkillSuggestions first = s.suggestions.suggest(7L, "Java number one");
        SkillSuggestions second = s.suggestions.suggest(7L, "Java number two");

        assertThat(first.degradedReason()).isEqualTo("BLOCKED");
        assertThat(second.degradedReason()).isEqualTo("BLOCKED");
        assertThat(s.breaker.state().name()).isEqualTo("CLOSED");
        assertThat(gemini.requests()).isEqualTo(2);
    }

    @Test
    void aUserWhoUsedTheirDailyBudgetShouldGetTheFallbackWithoutAnyProviderCall() {
        AiTestStack s = stack(2, 3, new AiLimitsProperties(100, 1_000, 10_000_000, null, 0, 0));
        s.usage.spend(7L, 1_000);
        gemini.then(200, FakeGemini.skillsAnswer(10, 5, "Java"));

        SkillSuggestions broke = s.suggestions.suggest(7L, "Java developer");
        SkillSuggestions other = s.suggestions.suggest(8L, "Java developer");

        assertThat(broke.degradedReason()).isEqualTo("BUDGET_EXCEEDED");
        assertThat(other.source()).isEqualTo(SkillSuggestions.Source.AI);   // doosre ko farak nahi
        assertThat(gemini.requests()).isEqualTo(1);
    }

    @Test
    void aUserOverTheirPerMinuteLimitShouldBeRefusedAndTheRefusalShouldNotBeStored() {
        AiTestStack s = stack(2, 3, new AiLimitsProperties(1, 1_000_000, 10_000_000, null, 0, 0));
        gemini.then(200, FakeGemini.skillsAnswer(10, 5, "Java"));

        s.suggestions.suggest(7L, "Java developer one");
        SkillSuggestions second = s.suggestions.suggest(7L, "Java developer two");

        assertThat(second.degradedReason()).isEqualTo("USER_RATE_LIMITED");
        assertThat(gemini.requests()).isEqualTo(1);
        assertThat(s.usage.entries).hasSize(1);   // mana ki gayi request ka row nahi
        assertThat(s.metrics.snapshot().refused()).isEqualTo(1);
    }

    @Test
    void theApiKeyShouldNeverShowUpInAnErrorOrInWhatWeStore() {
        AiTestStack s = stack(1, 5);
        gemini.always(403, "{\"error\":{\"message\":\"denied\"}}");

        assertThatThrownBy(() -> s.client.generate(AiRequest.text(null, "hi")))
                .isInstanceOfSatisfying(AiException.class, e -> {
                    assertThat(e.getKind()).isEqualTo(Kind.MISCONFIGURED);
                    assertThat(e.getMessage()).doesNotContain("secret-key");
                    assertThat(e.getDetail()).doesNotContain("secret-key");
                });

        assertThat(s.usage.entries.toString()).doesNotContain("secret-key");
        assertThat(gemini.seen.get(0).query()).isNull();
    }

    @Test
    void metricsShouldAddUpAcrossOutcomes() {
        AiTestStack s = stack(1, 5, new AiLimitsProperties(2, 1_000_000, 10_000_000, null, 0, 0));
        gemini.then(200, FakeGemini.skillsAnswer(100, 10, "Java")).then(500, "{}");

        s.suggestions.suggest(7L, "Java developer one");     // ok
        s.suggestions.suggest(7L, "Java developer one");     // cached
        s.suggestions.suggest(7L, "Java developer two");     // failed (500)
        s.suggestions.suggest(7L, "Java developer three");   // refused (2 per minute)

        AiMetrics.Snapshot m = s.metrics.snapshot();
        assertThat(m.ok()).isEqualTo(1);
        assertThat(m.cached()).isEqualTo(1);
        assertThat(m.failed()).isEqualTo(1);
        assertThat(m.refused()).isEqualTo(1);
        assertThat(m.promptTokens()).isEqualTo(100);
    }
}
