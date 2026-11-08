package com.hiresphere.ai.config;

import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ResiliencePropertiesTest {

    @Test
    void shouldFallBackToSensibleDefaults() {
        ResilienceProperties p = new ResilienceProperties(0, null, null, null, 0, null, 0, null);

        assertThat(p.maxAttempts()).isEqualTo(3);
        assertThat(p.initialBackoff()).isEqualTo(Duration.ofMillis(500));
        assertThat(p.maxBackoff()).isEqualTo(Duration.ofSeconds(4));
        assertThat(p.totalBudget()).isEqualTo(Duration.ofSeconds(45));
        assertThat(p.failureThreshold()).isEqualTo(5);
        assertThat(p.openDuration()).isEqualTo(Duration.ofSeconds(30));
        assertThat(p.maxConcurrent()).isEqualTo(4);
        assertThat(p.acquireTimeout()).isEqualTo(Duration.ofSeconds(2));
    }

    @Test
    void explicitValuesShouldBeKept() {
        ResilienceProperties p = new ResilienceProperties(2, Duration.ofMillis(100), Duration.ofSeconds(1),
                Duration.ofSeconds(10), 3, Duration.ofSeconds(5), 2, Duration.ofMillis(300));

        assertThat(p.maxAttempts()).isEqualTo(2);
        assertThat(p.totalBudget()).isEqualTo(Duration.ofSeconds(10));
        assertThat(p.maxConcurrent()).isEqualTo(2);
    }

    @Test
    void tooManyAttemptsShouldBeRejected() {
        assertThatThrownBy(() -> new ResilienceProperties(6, null, null, null, 0, null, 0, null))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("max-attempts");
    }

    @Test
    void aMaximumBackoffShorterThanTheInitialOneShouldBeRejected() {
        assertThatThrownBy(() -> new ResilienceProperties(3, Duration.ofSeconds(2), Duration.ofSeconds(1),
                null, 0, null, 0, null)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void zeroOrNegativeDurationsShouldBeRejectedNotSilentlyReplaced() {
        assertThatThrownBy(() -> new ResilienceProperties(3, Duration.ZERO, null, null, 0, null, 0, null))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("initial-backoff");
        assertThatThrownBy(() -> new ResilienceProperties(3, null, null, null, 0, Duration.ofMillis(-1), 0, null))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("open-duration");
    }
}