package com.hiresphere.notification.service;

import com.hiresphere.notification.repository.InAppNotificationRepository;
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
public class NotificationCleanupJob {

    public static final int RETENTION_DAYS = 90;
    public static final int KEEP_PER_USER = 200;

    private final InAppNotificationRepository repository;

    @Scheduled(cron = "${app.notifications.cleanup-cron:0 40 3 * * *}")
    @Transactional
    public void purge() {
        int old = repository.deleteOlderThan(Instant.now().minus(RETENTION_DAYS, ChronoUnit.DAYS));
        int extra = repository.trimPerUser(KEEP_PER_USER);
        log.info("Notification cleanup: {} older than {} days, {} beyond the newest {} per user",
                old, RETENTION_DAYS, extra, KEEP_PER_USER);
    }
}