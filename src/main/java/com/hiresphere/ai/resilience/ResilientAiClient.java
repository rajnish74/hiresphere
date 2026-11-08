package com.hiresphere.ai.resilience;

import com.hiresphere.ai.AiClient;
import com.hiresphere.ai.AiException;
import com.hiresphere.ai.AiException.Kind;
import com.hiresphere.ai.AiRequest;
import com.hiresphere.ai.AiResponse;
import com.hiresphere.ai.config.ResilienceProperties;
import com.hiresphere.ai.gemini.GeminiClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.EnumSet;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.DoubleSupplier;
import java.util.function.LongSupplier;

@Slf4j
@Primary
@Component
@ConditionalOnProperty(name = "app.ai.enabled", havingValue = "true")
public class ResilientAiClient implements AiClient {

    private static final Set<Kind> HEALTH_FAILURES =
            EnumSet.of(Kind.TIMEOUT, Kind.RATE_LIMITED, Kind.UPSTREAM, Kind.MISCONFIGURED);

    private final AiClient delegate;
    private final ResilienceProperties props;
    private final AiCircuitBreaker breaker;
    private final AiBulkhead bulkhead;
    private final Sleeper sleeper;
    private final RetryPolicy policy;
    private final LongSupplier nanos;

    @Autowired
    public ResilientAiClient(GeminiClient delegate, ResilienceProperties props, AiCircuitBreaker breaker,
                             AiBulkhead bulkhead) {
        this(delegate, props, breaker, bulkhead, Sleeper.THREAD,
                () -> ThreadLocalRandom.current().nextDouble(), System::nanoTime);
    }

    public ResilientAiClient(AiClient delegate, ResilienceProperties props, AiCircuitBreaker breaker,
                             AiBulkhead bulkhead, Sleeper sleeper, DoubleSupplier random, LongSupplier nanos) {
        this.delegate = delegate;
        this.props = props;
        this.breaker = breaker;
        this.bulkhead = bulkhead;
        this.sleeper = sleeper;
        this.nanos = nanos;
        this.policy = new RetryPolicy(props.maxAttempts(), props.initialBackoff(), props.maxBackoff(), random);
    }

    @Override
    public AiResponse generate(AiRequest request) {
        long started = nanos.getAsLong();
        AiException last = null;

        for (int attempt = 1; ; attempt++) {
            AiCircuitBreaker.Permission permission = breaker.tryAcquire();
            if (permission == AiCircuitBreaker.Permission.REJECTED) {
                throw last != null ? last : new AiException(Kind.CIRCUIT_OPEN,
                        "Circuit open, retry in about " + breaker.remainingOpen().toSeconds() + " s");
            }
            if (!bulkhead.tryAcquire()) {
                breaker.abandon(permission);
                throw last != null ? last : new AiException(Kind.BUSY,
                        "No free AI slot within " + props.acquireTimeout());
            }

            AiException failure;
            try {
                AiResponse response = delegate.generate(request);
                breaker.onSuccess(permission);
                return response;
            } catch (AiException e) {
                report(permission, e);
                failure = e;
            } catch (RuntimeException e) {
                breaker.abandon(permission);
                throw e;
            } finally {
                bulkhead.release();
            }

            last = failure;
            if (!failure.isRetryable() || !policy.hasAttemptsLeft(attempt)) {
                throw failure;
            }
            if (breaker.state() == AiCircuitBreaker.State.OPEN) {
                throw failure;
            }
            Duration wait = policy.backoffAfter(attempt);
            long elapsed = nanos.getAsLong() - started;
            if (elapsed + wait.toNanos() >= props.totalBudget().toNanos()) {
                log.warn("AI retry budget of {} used up after {} attempt(s)", props.totalBudget(), attempt);
                throw failure;
            }
            log.warn("AI attempt {}/{} failed ({}); retrying in {} ms", attempt, props.maxAttempts(),
                    failure.getKind(), wait.toMillis());
            try {
                sleeper.sleep(wait);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw failure;
            }
        }
    }

    private void report(AiCircuitBreaker.Permission permission, AiException e) {
        if (HEALTH_FAILURES.contains(e.getKind())) {
            breaker.onFailure(permission);
        } else {
            breaker.onSuccess(permission);
        }
    }
}