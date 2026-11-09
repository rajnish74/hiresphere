package com.hiresphere.ai.resilience;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.assertThat;

class AiUserLimiterTest {

    private final AtomicLong nanos = new AtomicLong(5_000);
    private final AiUserLimiter limiter = new AiUserLimiter(3, nanos::get);

    private void pass(Duration time) {
        nanos.addAndGet(time.toNanos());
    }

    @Test
    void shouldAllowTheLimitAndRefuseTheNextOne() {
        assertThat(limiter.tryAcquire(1L)).isTrue();
        assertThat(limiter.tryAcquire(1L)).isTrue();
        assertThat(limiter.tryAcquire(1L)).isTrue();
        assertThat(limiter.tryAcquire(1L)).isFalse();
    }

    @Test
    void eachUserShouldHaveTheirOwnAllowance() {
        for (int i = 0; i < 3; i++) {
            limiter.tryAcquire(1L);
        }

        assertThat(limiter.tryAcquire(1L)).isFalse();
        assertThat(limiter.tryAcquire(2L)).isTrue();
    }

    @Test
    void theAllowanceShouldComeBackAfterAMinute() {
        for (int i = 0; i < 4; i++) {
            limiter.tryAcquire(1L);
        }

        pass(Duration.ofSeconds(59));
        assertThat(limiter.tryAcquire(1L)).isFalse();
        pass(Duration.ofSeconds(1));
        assertThat(limiter.tryAcquire(1L)).isTrue();
    }

    @Test
    void aRefusedRequestShouldNotPushTheWindowFurtherAway() {
        for (int i = 0; i < 3; i++) {
            limiter.tryAcquire(1L);
        }
        pass(Duration.ofSeconds(30));
        assertThat(limiter.tryAcquire(1L)).isFalse();   // 30s par mana
        pass(Duration.ofSeconds(30));

        assertThat(limiter.tryAcquire(1L)).isTrue();    // pehli request se pura 60s: window khatam
    }

    @Test
    void aNewWindowShouldStartFromZeroNotFromTheOldCount() {
        for (int i = 0; i < 3; i++) {
            limiter.tryAcquire(1L);
        }
        pass(Duration.ofMinutes(1));

        assertThat(limiter.tryAcquire(1L)).isTrue();
        assertThat(limiter.tryAcquire(1L)).isTrue();
        assertThat(limiter.tryAcquire(1L)).isTrue();
        assertThat(limiter.tryAcquire(1L)).isFalse();
    }
}