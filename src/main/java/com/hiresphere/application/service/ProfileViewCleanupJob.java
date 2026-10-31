package com.hiresphere.application.service;

import com.hiresphere.application.repository.ProfileViewRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.ZoneOffset;

@Slf4j
@Component
@RequiredArgsConstructor
public class ProfileViewCleanupJob {

    private static final int RETENTION_DAYS = 90;

    private final ProfileViewRepository repository;

    @Scheduled(cron = "0 30 3 * * *")
    @Transactional
    public void purgeOld() {
        int deleted = repository.deleteOlderThan(LocalDate.now(ZoneOffset.UTC).minusDays(RETENTION_DAYS));
        log.info("Purged {} old profile views", deleted);
    }
}