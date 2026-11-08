package com.hiresphere.ai.resilience;

import com.hiresphere.ai.AiClient;
import com.hiresphere.ai.AiException;
import com.hiresphere.ai.AiException.Kind;
import com.hiresphere.ai.AiRequest;
import com.hiresphere.ai.AiResponse;
import com.hiresphere.ai.config.ResilienceProperties;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.time.Duration;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ResilientAiClientTest {

    private static final AiResponse OK = new AiResponse("ok", "STOP", false, 1, 1, 2, "m", 5);
    private static final AiRequest REQUEST = AiRequest.text(null, "hi");

    private final Queue<Object> script = new ArrayDeque<>();
    private final List<Duration> sleeps = new ArrayList<>();
    private final AtomicLong clock = new AtomicLong(1_000);
    private int calls;

    private AiCircuitBreaker breaker;
    private AiBulkhead bulkhead;

    /** Script khali ho to jawab OK. Warna agla item: AiException (ya koi aur exception) phenko, ya AiResponse do. */
    private final AiClient delegate = request -> {
        calls++;
        Object next = script.poll();
        if (next instanceof RuntimeException e) {
            throw e;
        }
        return next == null ? OK : (AiResponse) next;
    };

    private AiException ex(Kind kind) {
        return new AiException(kind, "test");
    }

    private ResilientAiClient build(int attempts, Duration budget, int threshold, int concurrent, Sleeper sleeper) {
        ResilienceProperties props = new ResilienceProperties(attempts, Duration.ofMillis(500), Duration.ofSeconds(4),
                budget, threshold, Duration.ofSeconds(30), concurrent, Duration.ofMillis(30));
        breaker = new AiCircuitBreaker(props.failureThreshold(), props.openDuration(), clock::get);
        bulkhead = new AiBulkhead(props.maxConcurrent(), props.acquireTimeout());
        return new ResilientAiClient(delegate, props, breaker, bulkhead, sleeper, () -> 1.0, clock::get);
    }

    private ResilientAiClient build(int attempts, Duration budget, int threshold, int concurrent) {
        return build(attempts, budget, threshold, concurrent, d -> {
            sleeps.add(d);
            clock.addAndGet(d.toNanos());
        });
    }

    private ResilientAiClient standard() {
        return build(3, Duration.ofSeconds(45), 5, 4);
    }

    private void assertKind(Runnable call, Kind kind) {
        assertThatThrownBy(call::run).isInstanceOfSatisfying(AiException.class, e -> assertThat(e.getKind()).isEqualTo(kind));
    }

    @Test
    void aFirstSuccessShouldCallOnceNeverSleepAndLeaveEverythingFree() {
        AiResponse result = standard().generate(REQUEST);

        assertThat(result).isSameAs(OK);
        assertThat(calls).isEqualTo(1);
        assertThat(sleeps).isEmpty();
        assertThat(breaker.state()).isEqualTo(AiCircuitBreaker.State.CLOSED);
        assertThat(bulkhead.available()).isEqualTo(4);
    }

    @ParameterizedTest
    @EnumSource(value = Kind.class, names = {"TIMEOUT", "RATE_LIMITED", "UPSTREAM"})
    void temporaryProblemsShouldBeRetriedAndCanSucceed(Kind kind) {
        script.add(ex(kind));

        AiResponse result = standard().generate(REQUEST);

        assertThat(result).isSameAs(OK);
        assertThat(calls).isEqualTo(2);
        assertThat(sleeps).containsExactly(Duration.ofMillis(500));
    }

    @Test
    void theWaitShouldGrowBetweenRetries() {
        script.add(ex(Kind.TIMEOUT));
        script.add(ex(Kind.UPSTREAM));

        standard().generate(REQUEST);

        assertThat(calls).isEqualTo(3);
        assertThat(sleeps).containsExactly(Duration.ofMillis(500), Duration.ofMillis(1000));
    }

    @Test
    void shouldGiveUpAfterTheMaximumAttemptsAndThrowTheRealError() {
        script.add(ex(Kind.TIMEOUT));
        script.add(ex(Kind.TIMEOUT));
        script.add(ex(Kind.TIMEOUT));

        assertKind(() -> standard().generate(REQUEST), Kind.TIMEOUT);

        assertThat(calls).isEqualTo(3);
        assertThat(sleeps).hasSize(2);   // aakhri failure ke baad intezaar nahi
    }

    @ParameterizedTest
    @EnumSource(value = Kind.class, names = {"MISCONFIGURED", "BLOCKED", "EMPTY", "DISABLED", "CIRCUIT_OPEN", "BUSY"})
    void problemsThatTimeCannotFixShouldNotBeRetried(Kind kind) {
        script.add(ex(kind));

        assertKind(() -> standard().generate(REQUEST), kind);

        assertThat(calls).isEqualTo(1);
        assertThat(sleeps).isEmpty();
    }

    @Test
    void shouldNotStartAnotherWaitThatWouldCrossTheTimeBudget() {
        script.add(ex(Kind.TIMEOUT));
        script.add(ex(Kind.TIMEOUT));
        script.add(ex(Kind.TIMEOUT));

        assertKind(() -> build(3, Duration.ofSeconds(1), 5, 4).generate(REQUEST), Kind.TIMEOUT);

        // pehli wait 500ms theek thi, doosri 1000ms ke saath 1s se paar ho jati
        assertThat(calls).isEqualTo(2);
        assertThat(sleeps).containsExactly(Duration.ofMillis(500));
    }

    @Test
    void repeatedFailuresShouldOpenTheCircuitAndLaterCallsShouldNotReachTheProvider() {
        ResilientAiClient client = build(1, Duration.ofSeconds(45), 2, 4);
        script.add(ex(Kind.UPSTREAM));
        script.add(ex(Kind.UPSTREAM));

        assertKind(() -> client.generate(REQUEST), Kind.UPSTREAM);
        assertKind(() -> client.generate(REQUEST), Kind.UPSTREAM);
        assertKind(() -> client.generate(REQUEST), Kind.CIRCUIT_OPEN);

        assertThat(calls).isEqualTo(2);
        assertThat(breaker.state()).isEqualTo(AiCircuitBreaker.State.OPEN);
    }

    @Test
    void ifTheCircuitOpensInTheMiddleOfARetryLoopTheRealErrorShouldComeBackWithoutPointlessWaiting() {
        script.add(ex(Kind.TIMEOUT));
        script.add(ex(Kind.TIMEOUT));

        assertKind(() -> build(3, Duration.ofSeconds(45), 2, 4).generate(REQUEST), Kind.TIMEOUT);

        assertThat(calls).isEqualTo(2);
        assertThat(sleeps).hasSize(1);   // doosre failure par circuit khula, to dobara ruke nahi
        assertThat(breaker.state()).isEqualTo(AiCircuitBreaker.State.OPEN);
    }

    @Test
    void aSuccessfulProbeAfterTheOpenPeriodShouldCloseTheCircuit() {
        ResilientAiClient client = build(1, Duration.ofSeconds(45), 2, 4);
        script.add(ex(Kind.UPSTREAM));
        script.add(ex(Kind.UPSTREAM));
        assertKind(() -> client.generate(REQUEST), Kind.UPSTREAM);
        assertKind(() -> client.generate(REQUEST), Kind.UPSTREAM);

        clock.addAndGet(Duration.ofSeconds(31).toNanos());

        assertThat(client.generate(REQUEST)).isSameAs(OK);
        assertThat(breaker.state()).isEqualTo(AiCircuitBreaker.State.CLOSED);
    }

    @Test
    void aFailedProbeShouldKeepTheCircuitOpenAndRefuseTheNextCallInstantly() {
        ResilientAiClient client = build(1, Duration.ofSeconds(45), 2, 4);
        script.add(ex(Kind.UPSTREAM));
        script.add(ex(Kind.UPSTREAM));
        script.add(ex(Kind.UPSTREAM));   // probe bhi fail
        assertKind(() -> client.generate(REQUEST), Kind.UPSTREAM);
        assertKind(() -> client.generate(REQUEST), Kind.UPSTREAM);
        clock.addAndGet(Duration.ofSeconds(31).toNanos());

        assertKind(() -> client.generate(REQUEST), Kind.UPSTREAM);
        int callsAfterProbe = calls;
        assertKind(() -> client.generate(REQUEST), Kind.CIRCUIT_OPEN);

        assertThat(calls).isEqualTo(callsAfterProbe);
    }

    @Test
    void answersLikeBlockedMeanTheProviderIsAliveAndMustNotOpenTheCircuit() {
        ResilientAiClient client = build(3, Duration.ofSeconds(45), 2, 4);
        for (int i = 0; i < 5; i++) {
            script.add(ex(Kind.BLOCKED));
        }

        for (int i = 0; i < 5; i++) {
            assertKind(() -> client.generate(REQUEST), Kind.BLOCKED);
        }

        assertThat(calls).isEqualTo(5);
        assertThat(breaker.state()).isEqualTo(AiCircuitBreaker.State.CLOSED);
    }

    @Test
    void aFullBulkheadShouldSayBusyWithoutCallingTheProvider() {
        ResilientAiClient client = build(3, Duration.ofSeconds(45), 5, 1);
        assertThat(bulkhead.tryAcquire()).isTrue();   // koi aur call chal rahi hai

        assertKind(() -> client.generate(REQUEST), Kind.BUSY);
        assertThat(calls).isZero();
        assertThat(breaker.state()).isEqualTo(AiCircuitBreaker.State.CLOSED);

        bulkhead.release();
        assertThat(client.generate(REQUEST)).isSameAs(OK);
    }

    @Test
    void aProbeThatCouldNotRunBecauseTheBulkheadWasFullShouldNotBlockTheNextProbe() {
        ResilientAiClient client = build(1, Duration.ofSeconds(45), 2, 1);
        script.add(ex(Kind.UPSTREAM));
        script.add(ex(Kind.UPSTREAM));
        assertKind(() -> client.generate(REQUEST), Kind.UPSTREAM);
        assertKind(() -> client.generate(REQUEST), Kind.UPSTREAM);
        clock.addAndGet(Duration.ofSeconds(31).toNanos());
        bulkhead.tryAcquire();

        assertKind(() -> client.generate(REQUEST), Kind.BUSY);

        bulkhead.release();
        assertThat(breaker.tryAcquire()).isEqualTo(AiCircuitBreaker.Permission.PROBE);
    }

    @Test
    void slotsShouldBeFreedAfterFailuresToo() {
        script.add(ex(Kind.MISCONFIGURED));

        assertKind(() -> standard().generate(REQUEST), Kind.MISCONFIGURED);

        assertThat(bulkhead.available()).isEqualTo(4);
    }

    @Test
    void anUnexpectedBugShouldPassThroughWithoutCountingAgainstTheProvider() {
        script.add(new IllegalStateException("bug"));

        assertThatThrownBy(() -> standard().generate(REQUEST)).isInstanceOf(IllegalStateException.class);

        assertThat(breaker.consecutiveFailures()).isZero();
        assertThat(bulkhead.available()).isEqualTo(4);
        assertThat(calls).isEqualTo(1);
    }

    @Test
    void anInterruptWhileWaitingShouldStopRetryingAndKeepTheInterruptFlag() {
        script.add(ex(Kind.TIMEOUT));
        ResilientAiClient client = build(3, Duration.ofSeconds(45), 5, 4, d -> {
            throw new InterruptedException();
        });

        assertKind(() -> client.generate(REQUEST), Kind.TIMEOUT);

        assertThat(Thread.interrupted()).isTrue();
        assertThat(calls).isEqualTo(1);
    }
}