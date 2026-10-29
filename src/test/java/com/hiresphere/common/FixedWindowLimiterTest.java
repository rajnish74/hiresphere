package com.hiresphere.common;

import com.hiresphere.common.util.FixedWindowLimiter;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

class FixedWindowLimiterTest {

    @Test
    void shouldAllowUpToTheLimitThenBlock() {
        FixedWindowLimiter limiter = new FixedWindowLimiter(3, Duration.ofMinutes(15));

        assertThat(limiter.tryAcquire("a")).isTrue();
        assertThat(limiter.tryAcquire("a")).isTrue();
        assertThat(limiter.tryAcquire("a")).isTrue();
        assertThat(limiter.tryAcquire("a")).isFalse();
    }

    @Test
    void keysShouldBeIndependent() {
        FixedWindowLimiter limiter = new FixedWindowLimiter(1, Duration.ofMinutes(15));

        assertThat(limiter.tryAcquire("a")).isTrue();
        assertThat(limiter.tryAcquire("a")).isFalse();
        assertThat(limiter.tryAcquire("b")).isTrue();
    }
}