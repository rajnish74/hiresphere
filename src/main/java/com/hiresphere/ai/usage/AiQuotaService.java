package com.hiresphere.ai.usage;

import com.hiresphere.ai.config.AiLimitsProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

@Service
@RequiredArgsConstructor
public class AiQuotaService {

    public record Quota(long dailyTokenLimit, long usedToday, long remaining, Instant resetsAt, int requestsPerMinute) {
    }

    private final AiLimitsProperties limits;
    private final AiUsageStore usage;

    public Quota quota(Long userId) {
        long used = usage.tokensUsedToday(userId);
        return new Quota(limits.perUserDailyTokens(), used, Math.max(0, limits.perUserDailyTokens() - used),
                usage.startOfToday().plus(1, ChronoUnit.DAYS), limits.perUserPerMinute());
    }
}