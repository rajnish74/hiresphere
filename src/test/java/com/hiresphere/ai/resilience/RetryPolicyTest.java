package com.hiresphere.ai.resilience;

import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RetryPolicyTest {

    private RetryPolicy policy(double random) {
        return new RetryPolicy(3, Duration.ofMillis(500), Duration.ofSeconds(4), () -> random);
    }

    @Test
    void theDelayShouldDoubleEachTimeAndStopAtTheMaximum() {
        RetryPolicy full = policy(1.0);   // jitter ka sabse bada hissa

        assertThat(full.backoffAfter(1)).isEqualTo(Duration.ofMillis(500));
        assertThat(full.backoffAfter(2)).isEqualTo(Duration.ofMillis(1000));
        assertThat(full.backoffAfter(3)).isEqualTo(Duration.ofMillis(2000));
        assertThat(full.backoffAfter(4)).isEqualTo(Duration.ofMillis(4000));
        assertThat(full.backoffAfter(5)).isEqualTo(Duration.ofMillis(4000));
    }

    @Test
    void jitterShouldTakeBetweenHalfAndAllOfTheDelay() {
        assertThat(policy(0.0).backoffAfter(1)).isEqualTo(Duration.ofMillis(250));
        assertThat(policy(0.0).backoffAfter(2)).isEqualTo(Duration.ofMillis(500));
        assertThat(policy(0.5).backoffAfter(1)).isEqualTo(Duration.ofMillis(375));
    }

    @Test
    void aHugeAttemptCountShouldNotOverflow() {
        assertThat(policy(1.0).backoffAfter(500)).isEqualTo(Duration.ofSeconds(4));
    }

    @Test
    void shouldKnowWhenNoAttemptsAreLeft() {
        RetryPolicy policy = policy(1.0);

        assertThat(policy.hasAttemptsLeft(1)).isTrue();
        assertThat(policy.hasAttemptsLeft(2)).isTrue();
        assertThat(policy.hasAttemptsLeft(3)).isFalse();
    }

    @Test
    void invalidInputShouldBeRejected() {
        assertThatThrownBy(() -> policy(1.0).backoffAfter(0)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new RetryPolicy(0, Duration.ofMillis(1), Duration.ofMillis(2), () -> 1.0))
                .isInstanceOf(IllegalArgumentException.class);
    }
}