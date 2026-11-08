package com.hiresphere.ai.resilience;

import java.time.Duration;
import java.util.function.DoubleSupplier;

public final class RetryPolicy {

    private final int maxAttempts;
    private final Duration initialBackoff;
    private final Duration maxBackoff;
    private final DoubleSupplier random;

    public RetryPolicy(int maxAttempts, Duration initialBackoff, Duration maxBackoff, DoubleSupplier random) {
        if (maxAttempts < 1) {
            throw new IllegalArgumentException("maxAttempts must be at least 1");
        }
        this.maxAttempts = maxAttempts;
        this.initialBackoff = initialBackoff;
        this.maxBackoff = maxBackoff;
        this.random = random;
    }

    public boolean hasAttemptsLeft(int attemptsMade) {
        return attemptsMade < maxAttempts;
    }

    public Duration backoffAfter(int attemptsMade) {
        if (attemptsMade < 1) {
            throw new IllegalArgumentException("attemptsMade must be at least 1");
        }
        long exponential = initialBackoff.toMillis() << Math.min(attemptsMade - 1, 20);   // shift limit: overflow nahi
        long capped = Math.min(maxBackoff.toMillis(), exponential);
        double jitter = 0.5 + 0.5 * random.getAsDouble();
        return Duration.ofMillis(Math.max(1, Math.round(capped * jitter)));
    }
}