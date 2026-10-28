package com.hiresphere.auth.service;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.hiresphere.common.exception.AppException;
import com.hiresphere.config.LoginProperties;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;

@Service
public class LoginAttemptService {

    private final int maxAttempts;
    private final long blockMinutes;
    private final Cache<String, AtomicInteger> failures;

    public LoginAttemptService(LoginProperties props) {
        this.maxAttempts = props.maxAttempts();
        this.blockMinutes = props.blockMinutes();
        this.failures = Caffeine.newBuilder()
                .expireAfterWrite(Duration.ofMinutes(props.blockMinutes()))
                .maximumSize(10_000)
                .build();
    }

    public void checkNotBlocked(String key) {
        AtomicInteger count = failures.getIfPresent(key);
        if (count != null && count.get() >= maxAttempts) {
            throw new AppException(HttpStatus.TOO_MANY_REQUESTS,
                    "Too many failed login attempts. Try again in " + blockMinutes + " minutes.");
        }
    }

    public void recordFailure(String key) {
        failures.get(key, k -> new AtomicInteger()).incrementAndGet();
    }

    public void reset(String key) {
        failures.invalidate(key);
    }
}