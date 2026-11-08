package com.hiresphere.ai.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties(prefix = "app.ai.resilience")
public record ResilienceProperties(int maxAttempts, Duration initialBackoff, Duration maxBackoff,
                                   Duration totalBudget, int failureThreshold, Duration openDuration,
                                   int maxConcurrent, Duration acquireTimeout) {

    public ResilienceProperties {
        maxAttempts = maxAttempts <= 0 ? 3 : maxAttempts;
        if (maxAttempts > 5) {
            throw new IllegalArgumentException("max-attempts must be 5 or less");
        }
        initialBackoff = positive("initial-backoff", initialBackoff, Duration.ofMillis(500));
        maxBackoff = positive("max-backoff", maxBackoff, Duration.ofSeconds(4));
        if (maxBackoff.compareTo(initialBackoff) < 0) {
            throw new IllegalArgumentException("max-backoff must not be shorter than initial-backoff");
        }
        totalBudget = positive("total-budget", totalBudget, Duration.ofSeconds(45));
        failureThreshold = failureThreshold <= 0 ? 5 : failureThreshold;
        openDuration = positive("open-duration", openDuration, Duration.ofSeconds(30));
        maxConcurrent = maxConcurrent <= 0 ? 4 : maxConcurrent;
        acquireTimeout = positive("acquire-timeout", acquireTimeout, Duration.ofSeconds(2));
    }

    private static Duration positive(String name, Duration value, Duration fallback) {
        if (value == null) {
            return fallback;
        }
        if (value.isZero() || value.isNegative()) {
            throw new IllegalArgumentException(name + " must be greater than zero");
        }
        return value;
    }
}