package com.hiresphere.ai;

import com.hiresphere.ai.AiException.Kind;
import com.hiresphere.ai.config.AiProperties;
import com.hiresphere.ai.gemini.GeminiClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.net.ConnectException;
import java.net.SocketTimeoutException;
import java.time.Duration;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class GeminiClientTest {

    private static final String BASE = "https://generativelanguage.googleapis.com/v1beta";
    private static final String URL = BASE + "/models/test-model:generateContent";

    private MockRestServiceServer server;
    private GeminiClient client;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder().baseUrl(BASE);
        server = MockRestServiceServer.bindTo(builder).build();
        AiProperties props = new AiProperties(true, new AiProperties.Gemini(
                "secret-key", "test-model", BASE, Duration.ofSeconds(1), Duration.ofSeconds(1), 1024));
        client = new GeminiClient(builder.build(), props);
    }

    private static final String PONG = """
            {"candidates":[{"content":{"role":"model","parts":[{"text":"pong"}]},"finishReason":"STOP"}],
             "usageMetadata":{"promptTokenCount":11,"candidatesTokenCount":3,"totalTokenCount":14}}""";

    @Test
    void shouldSendTheKeyInAHeaderNeverInTheUrlAndShapeTheBodyTheWayGeminiExpects() {
        server.expect(requestTo(URL))   // exact URL: query mein key nahi
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("x-goog-api-key", "secret-key"))
                .andExpect(jsonPath("$.systemInstruction.parts[0].text").value("Be brief"))
                .andExpect(jsonPath("$.contents[0].role").value("user"))
                .andExpect(jsonPath("$.contents[0].parts[0].text").value("Say pong"))
                .andExpect(jsonPath("$.generationConfig.maxOutputTokens").value(1024))
                .andExpect(jsonPath("$.generationConfig.responseMimeType").doesNotExist())
                .andExpect(jsonPath("$.generationConfig.temperature").doesNotExist())
                .andRespond(withSuccess(PONG, MediaType.APPLICATION_JSON));

        AiResponse response = client.generate(AiRequest.text("Be brief", "Say pong"));

        assertThat(response.text()).isEqualTo("pong");
        assertThat(response.finishReason()).isEqualTo("STOP");
        assertThat(response.truncated()).isFalse();
        assertThat(response.promptTokens()).isEqualTo(11);
        assertThat(response.outputTokens()).isEqualTo(3);
        assertThat(response.totalTokens()).isEqualTo(14);
        assertThat(response.model()).isEqualTo("test-model");
        assertThat(response.latencyMs()).isGreaterThanOrEqualTo(0);
        server.verify();
    }

    @Test
    void noSystemInstructionShouldMeanNoSystemInstructionField() {
        server.expect(requestTo(URL))
                .andExpect(jsonPath("$.systemInstruction").doesNotExist())
                .andRespond(withSuccess(PONG, MediaType.APPLICATION_JSON));

        client.generate(AiRequest.text(null, "Say pong"));

        server.verify();
    }

    @Test
    void aJsonRequestShouldAskForJsonWithTheSchemaAndTheGivenTemperature() {
        server.expect(requestTo(URL))
                .andExpect(jsonPath("$.generationConfig.responseMimeType").value("application/json"))
                .andExpect(jsonPath("$.generationConfig.responseJsonSchema.type").value("object"))
                .andExpect(jsonPath("$.generationConfig.temperature").value(0.2))
                .andRespond(withSuccess(PONG, MediaType.APPLICATION_JSON));

        client.generate(AiRequest.json("sys", "extract", Map.of("type", "object")));

        server.verify();
    }

    @Test
    void aPerRequestTokenLimitShouldWinOverTheDefault() {
        server.expect(requestTo(URL))
                .andExpect(jsonPath("$.generationConfig.maxOutputTokens").value(77))
                .andRespond(withSuccess(PONG, MediaType.APPLICATION_JSON));

        client.generate(new AiRequest(null, "hi", null, 77, null));

        server.verify();
    }

    @Test
    void shouldJoinTheAnswerPartsAndSkipTheThinkingParts() {
        server.expect(requestTo(URL)).andRespond(withSuccess("""
                {"candidates":[{"content":{"parts":[
                   {"text":"private reasoning","thought":true},
                   {"text":"Hello "},{"text":"world"}]},"finishReason":"STOP"}]}""", MediaType.APPLICATION_JSON));

        AiResponse response = client.generate(AiRequest.text(null, "hi"));

        assertThat(response.text()).isEqualTo("Hello world");
        assertThat(response.totalTokens()).isZero();   // usage na ho to 0, crash nahi
    }

    @Test
    void anAnswerCutOffByTheTokenLimitShouldBeFlaggedAsTruncated() {
        server.expect(requestTo(URL)).andRespond(withSuccess("""
                {"candidates":[{"content":{"parts":[{"text":"{\\"a\\": 1, \\"b\\""}]},"finishReason":"MAX_TOKENS"}]}""",
                MediaType.APPLICATION_JSON));

        AiResponse response = client.generate(AiRequest.text(null, "hi"));

        assertThat(response.truncated()).isTrue();
        assertThat(response.text()).startsWith("{");
    }

    @Test
    void aBlankAnswerBecauseTheThinkingUsedTheWholeBudgetShouldBeAnEmptyError() {
        server.expect(requestTo(URL)).andRespond(withSuccess("""
                {"candidates":[{"content":{"role":"model"},"finishReason":"MAX_TOKENS"}]}""", MediaType.APPLICATION_JSON));

        assertThatThrownBy(() -> client.generate(AiRequest.text(null, "hi")))
                .isInstanceOfSatisfying(AiException.class, e -> {
                    assertThat(e.getKind()).isEqualTo(Kind.EMPTY);
                    assertThat(e.getDetail()).contains("MAX_TOKENS");
                });
    }

    @Test
    void aBlockedPromptShouldBeABlockedError() {
        server.expect(requestTo(URL)).andRespond(withSuccess("""
                {"promptFeedback":{"blockReason":"SAFETY"}}""", MediaType.APPLICATION_JSON));

        assertThatThrownBy(() -> client.generate(AiRequest.text(null, "hi")))
                .isInstanceOfSatisfying(AiException.class, e -> assertThat(e.getKind()).isEqualTo(Kind.BLOCKED));
    }

    @Test
    void aBlockedAnswerShouldBeABlockedErrorToo() {
        server.expect(requestTo(URL)).andRespond(withSuccess("""
                {"candidates":[{"finishReason":"SAFETY"}]}""", MediaType.APPLICATION_JSON));

        assertThatThrownBy(() -> client.generate(AiRequest.text(null, "hi")))
                .isInstanceOfSatisfying(AiException.class, e -> assertThat(e.getKind()).isEqualTo(Kind.BLOCKED));
    }

    @Test
    void noCandidatesShouldBeAnEmptyError() {
        server.expect(requestTo(URL)).andRespond(withSuccess("{}", MediaType.APPLICATION_JSON));

        assertThatThrownBy(() -> client.generate(AiRequest.text(null, "hi")))
                .isInstanceOfSatisfying(AiException.class, e -> assertThat(e.getKind()).isEqualTo(Kind.EMPTY));
    }

    @ParameterizedTest
    @CsvSource({"400,MISCONFIGURED", "401,MISCONFIGURED", "403,MISCONFIGURED", "404,MISCONFIGURED",
            "429,RATE_LIMITED", "500,UPSTREAM", "503,UPSTREAM"})
    void httpErrorsShouldMapToTheRightKind(int status, Kind expected) {
        server.expect(requestTo(URL)).andRespond(withStatus(HttpStatusCode.valueOf(status))
                .contentType(MediaType.APPLICATION_JSON)
                .body("{\"error\":{\"message\":\"API key not valid. Please pass a valid API key.\"}}"));

        assertThatThrownBy(() -> client.generate(AiRequest.text(null, "hi")))
                .isInstanceOfSatisfying(AiException.class, e -> {
                    assertThat(e.getKind()).isEqualTo(expected);
                    assertThat(e.getMessage()).doesNotContain("API key");   // user ko provider ka text nahi
                    assertThat(e.getDetail()).contains("HTTP " + status).contains("API key not valid");
                });
    }

    @Test
    void aSlowServerShouldBeATimeout() {
        server.expect(requestTo(URL)).andRespond(request -> {
            throw new SocketTimeoutException("Read timed out");
        });

        assertThatThrownBy(() -> client.generate(AiRequest.text(null, "hi")))
                .isInstanceOfSatisfying(AiException.class, e -> {
                    assertThat(e.getKind()).isEqualTo(Kind.TIMEOUT);
                    assertThat(e.isRetryable()).isTrue();
                });
    }

    @Test
    void aConnectionFailureShouldBeUpstreamNotATimeout() {
        server.expect(requestTo(URL)).andRespond(request -> {
            throw new ConnectException("Connection refused");
        });

        assertThatThrownBy(() -> client.generate(AiRequest.text(null, "hi")))
                .isInstanceOfSatisfying(AiException.class, e -> assertThat(e.getKind()).isEqualTo(Kind.UPSTREAM));
    }

    @Test
    void anHtmlPageInsteadOfJsonShouldBeUpstreamNotACrash() {
        server.expect(requestTo(URL)).andRespond(withSuccess("<html>Gateway page</html>", MediaType.TEXT_HTML));

        assertThatThrownBy(() -> client.generate(AiRequest.text(null, "hi")))
                .isInstanceOfSatisfying(AiException.class, e -> assertThat(e.getKind()).isEqualTo(Kind.UPSTREAM));
    }

    @Test
    void theKeyShouldNeverBeInTheExceptionOrItsDetail() {
        server.expect(requestTo(URL)).andRespond(withStatus(HttpStatusCode.valueOf(403)).body("denied"));

        assertThatThrownBy(() -> client.generate(AiRequest.text(null, "hi")))
                .isInstanceOfSatisfying(AiException.class, e -> {
                    assertThat(e.getMessage()).doesNotContain("secret-key");
                    assertThat(e.getDetail()).doesNotContain("secret-key");
                });
    }
}