package com.hiresphere.auth.service;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.hiresphere.common.exception.AppException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;

@Service
public class LoginAttemptService {

    private static final int MAX_ATTEMPTS = 5;

    private final Cache<String, AtomicInteger> failures = Caffeine.newBuilder()
            .expireAfterWrite(Duration.ofMinutes(15))
            .maximumSize(10_000)
            .build();

    public void checkNotBlocked(String key) {
        AtomicInteger count = failures.getIfPresent(key);
        if (count != null && count.get() >= MAX_ATTEMPTS) {
            throw new AppException(HttpStatus.TOO_MANY_REQUESTS,
                    "Too many failed login attempts. Try again in 15 minutes.");
        }
    }

    public void recordFailure(String key) {
        failures.get(key, k -> new AtomicInteger()).incrementAndGet();
    }

    public void reset(String key) {
        failures.invalidate(key);
    }
}