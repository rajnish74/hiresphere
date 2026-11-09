package com.hiresphere.ai.usage;

import com.hiresphere.ai.config.AiLimitsProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

@Slf4j
@Component
@RequiredArgsConstructor
public class AiUsageCleanupJob {

    private final AiUsageRepository repository;
    private final AiLimitsProperties limits;

    @Scheduled(cron = "${app.ai.usage-cleanup-cron:0 50 3 * * *}")
    @Transactional
    public void purge() {
        int deleted = repository.deleteOlderThan(Instant.now().minus(limits.usageRetentionDays(), ChronoUnit.DAYS));
        log.info("AI usage cleanup: removed {} rows older than {} days", deleted, limits.usageRetentionDays());
    }
}