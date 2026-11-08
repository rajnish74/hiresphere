package com.hiresphere.ai.resilience;

import com.hiresphere.ai.resilience.AiCircuitBreaker.Permission;
import com.hiresphere.ai.resilience.AiCircuitBreaker.State;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AiCircuitBreakerTest {

    private final AtomicLong clock = new AtomicLong(0);
    private final AiCircuitBreaker breaker = new AiCircuitBreaker(3, Duration.ofSeconds(30), clock::get);

    private void fail(int times) {
        for (int i = 0; i < times; i++) {
            breaker.onFailure(breaker.tryAcquire());
        }
    }

    private void pass(Duration time) {
        clock.addAndGet(time.toNanos());
    }

    @Test
    void shouldStartClosedAndAllowCalls() {
        assertThat(breaker.state()).isEqualTo(State.CLOSED);
        assertThat(breaker.tryAcquire()).isEqualTo(Permission.ALLOWED);
    }

    @Test
    void failuresBelowTheThresholdShouldNotOpenIt() {
        fail(2);

        assertThat(breaker.state()).isEqualTo(State.CLOSED);
        assertThat(breaker.consecutiveFailures()).isEqualTo(2);
    }

    @Test
    void aSuccessShouldResetTheCountOfFailuresInARow() {
        fail(2);
        breaker.onSuccess(breaker.tryAcquire());
        fail(2);

        assertThat(breaker.state()).isEqualTo(State.CLOSED);
    }

    @Test
    void reachingTheThresholdShouldOpenItAndRefuseCalls() {
        fail(3);

        assertThat(breaker.state()).isEqualTo(State.OPEN);
        assertThat(breaker.tryAcquire()).isEqualTo(Permission.REJECTED);
    }

    @Test
    void itShouldStayOpenUntilTheOpenDurationIsOver() {
        fail(3);
        pass(Duration.ofSeconds(29));

        assertThat(breaker.tryAcquire()).isEqualTo(Permission.REJECTED);
        assertThat(breaker.remainingOpen()).isEqualTo(Duration.ofSeconds(1));
    }

    @Test
    void afterwardsExactlyOneProbeShouldGetThrough() {
        fail(3);
        pass(Duration.ofSeconds(30));

        assertThat(breaker.tryAcquire()).isEqualTo(Permission.PROBE);
        assertThat(breaker.state()).isEqualTo(State.HALF_OPEN);
        assertThat(breaker.tryAcquire()).isEqualTo(Permission.REJECTED);
    }

    @Test
    void aSuccessfulProbeShouldCloseIt() {
        fail(3);
        pass(Duration.ofSeconds(30));
        breaker.onSuccess(breaker.tryAcquire());

        assertThat(breaker.state()).isEqualTo(State.CLOSED);
        assertThat(breaker.consecutiveFailures()).isZero();
        assertThat(breaker.tryAcquire()).isEqualTo(Permission.ALLOWED);
    }

    @Test
    void aFailedProbeShouldOpenItAgainForAFullPeriod() {
        fail(3);
        pass(Duration.ofSeconds(30));
        breaker.onFailure(breaker.tryAcquire());

        assertThat(breaker.state()).isEqualTo(State.OPEN);
        pass(Duration.ofSeconds(29));
        assertThat(breaker.tryAcquire()).isEqualTo(Permission.REJECTED);
        pass(Duration.ofSeconds(1));
        assertThat(breaker.tryAcquire()).isEqualTo(Permission.PROBE);
    }

    @Test
    void anAbandonedProbeShouldFreeTheSlotForTheNextCaller() {
        fail(3);
        pass(Duration.ofSeconds(30));
        Permission probe = breaker.tryAcquire();
        assertThat(breaker.tryAcquire()).isEqualTo(Permission.REJECTED);

        breaker.abandon(probe);

        assertThat(breaker.tryAcquire()).isEqualTo(Permission.PROBE);
    }

    @Test
    void aProbeThatNeverReportsShouldTimeOutSoTheBreakerCannotGetStuck() {
        fail(3);
        pass(Duration.ofSeconds(30));
        breaker.tryAcquire();   // probe gaya, result kabhi nahi aaya

        pass(Duration.ofSeconds(31));

        assertThat(breaker.tryAcquire()).isEqualTo(Permission.PROBE);
    }

    @Test
    void lateResultsOfOlderCallsShouldNotChangeAnOpenOrHalfOpenBreaker() {
        Permission stale = breaker.tryAcquire();   // circuit band hone se pehle shuru hui call
        fail(3);

        breaker.onSuccess(stale);
        assertThat(breaker.state()).isEqualTo(State.OPEN);
        breaker.onFailure(stale);
        assertThat(breaker.state()).isEqualTo(State.OPEN);

        pass(Duration.ofSeconds(30));
        breaker.tryAcquire();   // probe
        breaker.onSuccess(stale);
        assertThat(breaker.state()).isEqualTo(State.HALF_OPEN);   // sirf probe band kar sakta hai
    }

    @Test
    void invalidSettingsShouldBeRejected() {
        assertThatThrownBy(() -> new AiCircuitBreaker(0, Duration.ofSeconds(1), clock::get))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new AiCircuitBreaker(3, Duration.ZERO, clock::get))
                .isInstanceOf(IllegalArgumentException.class);
    }
}