package com.hiresphere.ai.usage;

import com.hiresphere.ai.FakeUsageStore;
import com.hiresphere.ai.config.AiLimitsProperties;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class AiQuotaServiceTest {

    private final FakeUsageStore store = new FakeUsageStore();
    private final AiQuotaService service =
            new AiQuotaService(new AiLimitsProperties(10, 5_000, 100_000, null, 0, 0), store);

    @Test
    void shouldShowUsedLeftAndWhenItResets() {
        store.spend(7L, 1_200);

        AiQuotaService.Quota quota = service.quota(7L);

        assertThat(quota.dailyTokenLimit()).isEqualTo(5_000);
        assertThat(quota.usedToday()).isEqualTo(1_200);
        assertThat(quota.remaining()).isEqualTo(3_800);
        assertThat(quota.resetsAt()).isEqualTo(Instant.parse("2026-11-10T00:00:00Z"));
        assertThat(quota.requestsPerMinute()).isEqualTo(10);
    }

    @Test
    void whatIsLeftShouldNeverGoBelowZeroEvenAfterTheSoftLimitWasCrossed() {
        store.spend(7L, 5_400);

        assertThat(service.quota(7L).remaining()).isZero();
    }

    @Test
    void onlyThatUsersTokensShouldCount() {
        store.spend(8L, 4_000);

        assertThat(service.quota(7L).usedToday()).isZero();
    }
}