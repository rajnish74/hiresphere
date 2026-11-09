package com.hiresphere.ai;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AiRequestAndExceptionTest {

    @Test
    void aBlankPromptIsAProgrammingErrorAndIsRejected() {
        assertThatThrownBy(() -> AiRequest.text("sys", " ")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> AiRequest.text("sys", null)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void temperatureAndTokenLimitsShouldBeChecked() {
        assertThatThrownBy(() -> new AiRequest(null, "x", 3.0, null, null)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new AiRequest(null, "x", -0.1, null, null)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new AiRequest(null, "x", null, 0, null)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void plainTextAndJsonRequestsShouldDiffer() {
        AiRequest text = AiRequest.text("sys", "hi");
        AiRequest json = AiRequest.json("sys", "hi", Map.of("type", "object"));

        assertThat(text.wantsJson()).isFalse();
        assertThat(text.temperature()).isNull();
        assertThat(json.wantsJson()).isTrue();
        assertThat(json.temperature()).isEqualTo(0.2);
    }

    @Test
    void everyKindShouldHaveAStatusAMessageAndARetryRule() {
        for (AiException.Kind kind : AiException.Kind.values()) {
            AiException e = new AiException(kind, "internal detail");

            assertThat(e.getStatus()).as(kind.name()).isNotNull();
            assertThat(e.getMessage()).as(kind.name()).isNotBlank().doesNotContain("internal detail");
            assertThat(e.getDetail()).isEqualTo("internal detail");
        }
    }

    @Test
    void onlyTemporaryProblemsShouldBeRetryable() {
        assertThat(new AiException(AiException.Kind.TIMEOUT, "").isRetryable()).isTrue();
        assertThat(new AiException(AiException.Kind.RATE_LIMITED, "").isRetryable()).isTrue();
        assertThat(new AiException(AiException.Kind.UPSTREAM, "").isRetryable()).isTrue();
        assertThat(new AiException(AiException.Kind.MISCONFIGURED, "").isRetryable()).isFalse();
        assertThat(new AiException(AiException.Kind.BLOCKED, "").isRetryable()).isFalse();
        assertThat(new AiException(AiException.Kind.EMPTY, "").isRetryable()).isFalse();
        assertThat(new AiException(AiException.Kind.DISABLED, "").isRetryable()).isFalse();
        assertThat(new AiException(AiException.Kind.INVALID_OUTPUT, "").isRetryable()).isTrue();
        assertThat(new AiException(AiException.Kind.CIRCUIT_OPEN, "").isRetryable()).isFalse();
        assertThat(new AiException(AiException.Kind.BUSY, "").isRetryable()).isFalse();
    }

    @Test
    void theStatusesShouldTellTheCallerWhoseFaultItIs() {
        assertThat(new AiException(AiException.Kind.TIMEOUT, "").getStatus()).isEqualTo(HttpStatus.GATEWAY_TIMEOUT);
        assertThat(new AiException(AiException.Kind.DISABLED, "").getStatus()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
        assertThat(new AiException(AiException.Kind.BLOCKED, "").getStatus()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
        assertThat(new AiException(AiException.Kind.UPSTREAM, "").getStatus()).isEqualTo(HttpStatus.BAD_GATEWAY);
    }

    @Test
    void theDisabledClientShouldAlwaysSayAiIsOff() {
        assertThatThrownBy(() -> new DisabledAiClient().generate(AiRequest.text(null, "hi")))
                .isInstanceOfSatisfying(AiException.class, e -> assertThat(e.getKind()).isEqualTo(AiException.Kind.DISABLED));
    }

    @Test
    void ourOwnLimitsShouldNotBeRetryableAndShouldTellWhoseLimitItIs() {
        assertThat(new AiException(AiException.Kind.USER_RATE_LIMITED, "").isRetryable()).isFalse();
        assertThat(new AiException(AiException.Kind.USER_RATE_LIMITED, "").getStatus()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS);
        assertThat(new AiException(AiException.Kind.BUDGET_EXCEEDED, "").getStatus()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS);
        assertThat(new AiException(AiException.Kind.BUDGET_EXCEEDED, "").getMessage()).contains("midnight UTC");
        assertThat(new AiException(AiException.Kind.GLOBAL_BUDGET_EXCEEDED, "").getStatus()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
        assertThat(new AiException(AiException.Kind.GLOBAL_BUDGET_EXCEEDED, "").isRetryable()).isFalse();
    }
}