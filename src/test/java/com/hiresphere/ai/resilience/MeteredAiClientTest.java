package com.hiresphere.ai.resilience;

import com.hiresphere.ai.AiCallContext;
import com.hiresphere.ai.AiClient;
import com.hiresphere.ai.AiException;
import com.hiresphere.ai.AiException.Kind;
import com.hiresphere.ai.AiRequest;
import com.hiresphere.ai.AiResponse;
import com.hiresphere.ai.FakeUsageStore;
import com.hiresphere.ai.config.AiLimitsProperties;
import com.hiresphere.ai.metrics.AiMetrics;
import com.hiresphere.ai.usage.UsageEntry;
import com.hiresphere.ai.usage.UsageStatus;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MeteredAiClientTest {

    private static final AiResponse OK = new AiResponse("ok", "STOP", false, 10, 5, 15, "model-x", 20);
    private static final AiRequest REQUEST = AiRequest.text(null, "hi");

    private final FakeUsageStore store = new FakeUsageStore();
    private final AtomicLong nanos = new AtomicLong(1_000);
    private int calls;
    private RuntimeException failure;

    private final AiClient delegate = request -> {
        calls++;
        if (failure != null) {
            throw failure;
        }
        return OK;
    };

    private MeteredAiClient client(int perMinute, long perUser, long global) {
        AiLimitsProperties limits = new AiLimitsProperties(perMinute, perUser, global, null, 0, 0);
        return new MeteredAiClient(delegate, new AiUserLimiter(perMinute, nanos::get), limits, store);
    }

    private AiResponse as(Long userId, MeteredAiClient client) {
        return AiCallContext.with("SKILLS_EXTRACT", userId, () -> client.generate(REQUEST));
    }

    private void assertKind(Runnable call, Kind kind) {
        assertThatThrownBy(call::run)
                .isInstanceOfSatisfying(AiException.class, e -> assertThat(e.getKind()).isEqualTo(kind));
    }

    @Test
    void aSuccessfulCallShouldBeLoggedWithTheUserFeatureModelAndTokens() {
        as(7L, client(10, 1_000, 10_000));

        assertThat(store.entries).hasSize(1);
        UsageEntry e = store.entries.get(0);
        assertThat(e.userId()).isEqualTo(7L);
        assertThat(e.feature()).isEqualTo("SKILLS_EXTRACT");
        assertThat(e.model()).isEqualTo("model-x");
        assertThat(e.status()).isEqualTo(UsageStatus.OK);
        assertThat(e.promptTokens()).isEqualTo(10);
        assertThat(e.outputTokens()).isEqualTo(5);
        assertThat(e.totalTokens()).isEqualTo(15);
        assertThat(e.latencyMs()).isEqualTo(20);
    }

    @Test
    void aCallWithoutAContextShouldBeLoggedAsUnspecifiedAndHaveNoUser() {
        client(10, 1_000, 10_000).generate(REQUEST);

        assertThat(store.entries.get(0).feature()).isEqualTo("UNSPECIFIED");
        assertThat(store.entries.get(0).userId()).isNull();
    }

    @Test
    void aFailedCallShouldBeLoggedWithItsKindAndNoTokensAndStillThrow() {
        failure = new AiException(Kind.TIMEOUT, "slow");

        assertKind(() -> as(7L, client(10, 1_000, 10_000)), Kind.TIMEOUT);

        UsageEntry e = store.entries.get(0);
        assertThat(e.status()).isEqualTo(UsageStatus.FAILED);
        assertThat(e.errorKind()).isEqualTo("TIMEOUT");
        assertThat(e.totalTokens()).isZero();
    }

    @Test
    void aUserOverTheirPerMinuteLimitShouldBeRefusedBeforeTheProviderIsCalled() {
        MeteredAiClient client = client(2, 1_000_000, 1_000_000);
        as(7L, client);
        as(7L, client);

        assertKind(() -> as(7L, client), Kind.USER_RATE_LIMITED);

        assertThat(calls).isEqualTo(2);
        assertThat(store.entries).hasSize(2);   // mana ki gayi request ka koi row nahi
    }

    @Test
    void theRateLimitShouldBePerUserAndShouldResetAfterAMinute() {
        MeteredAiClient client = client(1, 1_000_000, 1_000_000);
        as(7L, client);

        assertKind(() -> as(7L, client), Kind.USER_RATE_LIMITED);
        assertThat(as(8L, client)).isSameAs(OK);

        nanos.addAndGet(Duration.ofSeconds(61).toNanos());
        assertThat(as(7L, client)).isSameAs(OK);
    }

    @Test
    void systemCallsWithoutAUserShouldSkipThePerUserChecks() {
        MeteredAiClient client = client(1, 1_000, 1_000_000);

        for (int i = 0; i < 5; i++) {
            client.generate(REQUEST);
        }

        assertThat(calls).isEqualTo(5);
    }

    @Test
    void aUserWhoUsedTheirDailyBudgetShouldBeRefusedAndNothingIsCalled() {
        store.spend(7L, 1_000);

        assertKind(() -> as(7L, client(10, 1_000, 1_000_000)), Kind.BUDGET_EXCEEDED);

        assertThat(calls).isZero();
        assertThat(store.entries).hasSize(1);   // sirf purana seed row
    }

    @Test
    void justBelowTheBudgetStillWorksAndOtherPeoplesUsageDoesNotCount() {
        store.spend(7L, 999);
        store.spend(8L, 5_000);

        assertThat(as(7L, client(10, 1_000, 1_000_000))).isSameAs(OK);
        assertThat(as(9L, client(10, 1_000, 1_000_000))).isSameAs(OK);
    }

    @Test
    void theBudgetIsSoftSoTheCallThatCrossesItIsAllowedButTheNextIsNot() {
        MeteredAiClient client = client(10, 20, 1_000_000);

        as(7L, client);   // 0 < 20: theek, 15 tokens kharch
        as(7L, client);   // 15 < 20: theek, ab 30 kharch
        assertKind(() -> as(7L, client), Kind.BUDGET_EXCEEDED);

        assertThat(calls).isEqualTo(2);
    }

    @Test
    void theGlobalBudgetShouldStopEveryoneIncludingSystemCalls() {
        store.spend(99L, 5_000);
        MeteredAiClient client = client(10, 1_000, 5_000);

        assertKind(() -> as(7L, client), Kind.GLOBAL_BUDGET_EXCEEDED);
        assertKind(() -> client.generate(REQUEST), Kind.GLOBAL_BUDGET_EXCEEDED);
        assertThat(calls).isZero();
    }

    @Test
    void failuresInTheUsageLogMustNeverBreakTheUsersCall() {
        store.failOnRecord = true;

        assertThat(as(7L, client(10, 1_000, 10_000))).isSameAs(OK);
        assertThat(calls).isEqualTo(1);
    }

    @Test
    void theOriginalExceptionShouldSurviveAFailingUsageLog() {
        store.failOnRecord = true;
        failure = new AiException(Kind.UPSTREAM, "down");

        assertKind(() -> as(7L, client(10, 1_000, 10_000)), Kind.UPSTREAM);
    }

    @Test
    void metricsShouldCountOkFailedAndRefusedCallsSeparately() {
        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        AiMetrics metrics = new AiMetrics(registry);
        AiLimitsProperties limits = new AiLimitsProperties(1, 1_000, 10_000, null, 0, 0);
        MeteredAiClient client = new MeteredAiClient(delegate, new AiUserLimiter(1, nanos::get), limits, store, metrics);

        as(7L, client);                                           // ok
        assertKind(() -> as(7L, client), Kind.USER_RATE_LIMITED); // refused
        failure = new AiException(Kind.TIMEOUT, "slow");
        assertKind(() -> as(8L, client), Kind.TIMEOUT);           // failed

        AiMetrics.Snapshot s = metrics.snapshot();
        assertThat(s.ok()).isEqualTo(1);
        assertThat(s.refused()).isEqualTo(1);
        assertThat(s.failed()).isEqualTo(1);
        assertThat(s.promptTokens()).isEqualTo(10);
        assertThat(s.outputTokens()).isEqualTo(5);
        assertThat(registry.get(AiMetrics.CALLS).tag("outcome", "refused").tag("kind", "USER_RATE_LIMITED")
                .counter().count()).isEqualTo(1.0);
    }
}