package com.hiresphere.common.util;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;

public class FixedWindowLimiter {

    private final int maxRequests;
    private final Cache<String, AtomicInteger> counters;

    public FixedWindowLimiter(int maxRequests, Duration window) {
        this.maxRequests = maxRequests;
        this.counters = Caffeine.newBuilder()
                .expireAfterWrite(window)
                .maximumSize(50_000)
                .build();
    }

    public boolean tryAcquire(String key) {
        return counters.get(key, k -> new AtomicInteger()).incrementAndGet() <= maxRequests;
    }
}