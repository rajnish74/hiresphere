package com.hiresphere.ai.resilience;

import com.hiresphere.ai.AiCallContext;
import com.hiresphere.ai.AiClient;
import com.hiresphere.ai.AiException;
import com.hiresphere.ai.AiException.Kind;
import com.hiresphere.ai.AiRequest;
import com.hiresphere.ai.AiResponse;
import com.hiresphere.ai.config.AiLimitsProperties;
import com.hiresphere.ai.usage.AiUsageStore;
import com.hiresphere.ai.usage.UsageEntry;
import com.hiresphere.ai.usage.UsageLog;
import com.hiresphere.ai.usage.UsageStatus;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

@Slf4j
@Primary
@Component
@ConditionalOnProperty(name = "app.ai.enabled", havingValue = "true")
public class MeteredAiClient implements AiClient {

    private final AiClient delegate;
    private final AiUserLimiter limiter;
    private final AiLimitsProperties limits;
    private final AiUsageStore usage;

    public MeteredAiClient(@Qualifier("resilientAiClient") AiClient delegate, AiUserLimiter limiter,
                           AiLimitsProperties limits, AiUsageStore usage) {
        this.delegate = delegate;
        this.limiter = limiter;
        this.limits = limits;
        this.usage = usage;
    }

    @Override
    public AiResponse generate(AiRequest request) {
        AiCallContext.Context context = AiCallContext.current();
        enforce(context);
        try {
            AiResponse response = delegate.generate(request);
            UsageLog.quietly(usage, new UsageEntry(context.userId(), context.feature(), response.model(),
                    UsageStatus.OK, null, response.promptTokens(), response.outputTokens(), response.totalTokens(),
                    response.latencyMs()));
            return response;
        } catch (AiException e) {
            UsageLog.quietly(usage, new UsageEntry(context.userId(), context.feature(), null, UsageStatus.FAILED,
                    e.getKind().name(), 0, 0, 0, 0));
            throw e;
        }
    }

    private void enforce(AiCallContext.Context context) {
        Long userId = context.userId();
        if (userId != null && !limiter.tryAcquire(userId)) {
            log.warn("AI request refused: user {} is over {} requests per minute (feature {})", userId,
                    limits.perUserPerMinute(), context.feature());
            throw new AiException(Kind.USER_RATE_LIMITED, "user " + userId + " over " + limits.perUserPerMinute() + "/min");
        }
        if (usage.tokensUsedTodayByEveryone() >= limits.globalDailyTokens()) {
            log.warn("AI request refused: the global daily budget of {} tokens is used up", limits.globalDailyTokens());
            throw new AiException(Kind.GLOBAL_BUDGET_EXCEEDED, "global budget " + limits.globalDailyTokens());
        }
        if (userId != null && usage.tokensUsedToday(userId) >= limits.perUserDailyTokens()) {
            log.warn("AI request refused: user {} used the daily budget of {} tokens (feature {})", userId,
                    limits.perUserDailyTokens(), context.feature());
            throw new AiException(Kind.BUDGET_EXCEEDED, "user " + userId + " budget " + limits.perUserDailyTokens());
        }
    }
}