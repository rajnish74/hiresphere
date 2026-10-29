package com.hiresphere.auth.service;

import com.hiresphere.auth.repository.AuthTokenRepository;
import com.hiresphere.auth.repository.RefreshTokenRepository;
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
public class TokenCleanupJob {

    private final RefreshTokenRepository refreshTokenRepository;
    private final AuthTokenRepository authTokenRepository;

    @Scheduled(cron = "0 0 3 * * *")
    @Transactional
    public void purgeExpired() {
        Instant cutoff = Instant.now().minus(1, ChronoUnit.DAYS);
        log.info("Purged {} expired refresh tokens and {} expired auth tokens",
                refreshTokenRepository.deleteExpired(cutoff), authTokenRepository.deleteExpired(cutoff));
    }
}