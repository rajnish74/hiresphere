package com.hiresphere.ai.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties(prefix = "app.ai.limits")
public record AiLimitsProperties(int perUserPerMinute, long perUserDailyTokens, long globalDailyTokens,
                                 Duration cacheTtl, int cacheMaxEntries, int usageRetentionDays) {

    public AiLimitsProperties {
        perUserPerMinute = perUserPerMinute <= 0 ? 10 : perUserPerMinute;
        perUserDailyTokens = perUserDailyTokens <= 0 ? 50_000 : perUserDailyTokens;
        globalDailyTokens = globalDailyTokens <= 0 ? 1_000_000 : globalDailyTokens;
        if (globalDailyTokens < perUserDailyTokens) {
            throw new IllegalArgumentException("global-daily-tokens must not be smaller than per-user-daily-tokens");
        }
        if (cacheTtl == null) {
            cacheTtl = Duration.ofHours(24);
        } else if (cacheTtl.isZero() || cacheTtl.isNegative()) {
            throw new IllegalArgumentException("cache-ttl must be greater than zero");
        }
        cacheMaxEntries = cacheMaxEntries <= 0 ? 500 : cacheMaxEntries;
        usageRetentionDays = usageRetentionDays <= 0 ? 90 : usageRetentionDays;
    }
}