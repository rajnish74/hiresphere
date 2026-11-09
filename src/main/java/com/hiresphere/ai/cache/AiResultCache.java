package com.hiresphere.ai.cache;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.Ticker;
import com.hiresphere.ai.config.AiLimitsProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Objects;
import java.util.function.Supplier;


@Component
public class AiResultCache {

    public record Lookup<T>(T value, boolean hit) {
    }

    private final Cache<String, Object> cache;

    @Autowired
    public AiResultCache(AiLimitsProperties limits) {
        this(limits, Ticker.systemTicker());
    }

    public AiResultCache(AiLimitsProperties limits, Ticker ticker) {
        this.cache = Caffeine.newBuilder().ticker(ticker).expireAfterWrite(limits.cacheTtl())
                .maximumSize(limits.cacheMaxEntries()).build();
    }

    @SuppressWarnings("unchecked")
    public <T> Lookup<T> get(String key, Supplier<T> loader) {
        Object cached = cache.getIfPresent(key);
        if (cached != null) {
            return new Lookup<>((T) cached, true);
        }
        T value = Objects.requireNonNull(loader.get(), "loader returned null");
        cache.put(key, value);
        return new Lookup<>(value, false);
    }

    public long size() {
        return cache.estimatedSize();
    }

    public static String keyOf(String... parts) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            for (String part : parts) {
                if (part == null) {
                    throw new IllegalArgumentException("cache key parts must not be null");
                }
                digest.update(part.getBytes(StandardCharsets.UTF_8));
                digest.update((byte) 0);
            }
            return HexFormat.of().formatHex(digest.digest());
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}