package com.hiresphere.ai.config;

import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AiLimitsPropertiesTest {

    @Test
    void shouldFallBackToSensibleDefaults() {
        AiLimitsProperties p = new AiLimitsProperties(0, 0, 0, null, 0, 0);

        assertThat(p.perUserPerMinute()).isEqualTo(10);
        assertThat(p.perUserDailyTokens()).isEqualTo(50_000);
        assertThat(p.globalDailyTokens()).isEqualTo(1_000_000);
        assertThat(p.cacheTtl()).isEqualTo(Duration.ofHours(24));
        assertThat(p.cacheMaxEntries()).isEqualTo(500);
        assertThat(p.usageRetentionDays()).isEqualTo(90);
    }

    @Test
    void explicitValuesShouldBeKept() {
        AiLimitsProperties p = new AiLimitsProperties(3, 500, 9_000, Duration.ofHours(1), 10, 7);

        assertThat(p.perUserPerMinute()).isEqualTo(3);
        assertThat(p.perUserDailyTokens()).isEqualTo(500);
        assertThat(p.globalDailyTokens()).isEqualTo(9_000);
        assertThat(p.usageRetentionDays()).isEqualTo(7);
    }

    @Test
    void theGlobalBudgetCannotBeSmallerThanOneUsersBudget() {
        assertThatThrownBy(() -> new AiLimitsProperties(3, 5_000, 1_000, null, 0, 0))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("global-daily-tokens");
    }

    @Test
    void aZeroOrNegativeCacheTtlShouldBeRejected() {
        assertThatThrownBy(() -> new AiLimitsProperties(0, 0, 0, Duration.ZERO, 0, 0))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new AiLimitsProperties(0, 0, 0, Duration.ofMillis(-1), 0, 0))
                .isInstanceOf(IllegalArgumentException.class);
    }
}