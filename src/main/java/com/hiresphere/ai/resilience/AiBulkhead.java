package com.hiresphere.ai.resilience;

import com.hiresphere.ai.config.ResilienceProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

@Component
@ConditionalOnProperty(name = "app.ai.enabled", havingValue = "true")
public class AiBulkhead {

    private final Semaphore permits;
    private final Duration acquireTimeout;
    private final int max;

    @Autowired
    public AiBulkhead(ResilienceProperties props) {
        this(props.maxConcurrent(), props.acquireTimeout());
    }

    public AiBulkhead(int max, Duration acquireTimeout) {
        if (max < 1) {
            throw new IllegalArgumentException("max must be at least 1");
        }
        this.max = max;
        this.acquireTimeout = acquireTimeout;
        this.permits = new Semaphore(max, true);   // fair: koi request bhookhi nahi rehti
    }

    public boolean tryAcquire() {
        try {
            return permits.tryAcquire(acquireTimeout.toNanos(), TimeUnit.NANOSECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        }
    }

    public void release() {
        permits.release();
    }

    public int available() {
        return permits.availablePermits();
    }

    public int max() {
        return max;
    }
}