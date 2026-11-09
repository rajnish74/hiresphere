package com.hiresphere.ai.resilience;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.Ticker;
import com.hiresphere.ai.config.AiLimitsProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
@ConditionalOnProperty(name = "app.ai.enabled", havingValue = "true")
public class AiUserLimiter {

    private static final long WINDOW_NANOS = Duration.ofMinutes(1).toNanos();

    private static final class Window {
        long start;
        int count;
    }

    private final int limit;
    private final Ticker ticker;
    private final Cache<Long, Window> windows;

    @Autowired
    public AiUserLimiter(AiLimitsProperties limits) {
        this(limits.perUserPerMinute(), Ticker.systemTicker());
    }

    public AiUserLimiter(int limit, Ticker ticker) {
        this.limit = limit;
        this.ticker = ticker;
        this.windows = Caffeine.newBuilder().ticker(ticker).expireAfterAccess(Duration.ofMinutes(2))
                .maximumSize(100_000).build();
    }

    public boolean tryAcquire(Long userId) {
        Window window = windows.get(userId, key -> new Window());
        long now = ticker.read();
        synchronized (window) {
            if (window.count == 0 || now - window.start >= WINDOW_NANOS) {
                window.start = now;
                window.count = 0;
            }
            if (window.count >= limit) {
                return false;
            }
            window.count++;
            return true;
        }
    }
}