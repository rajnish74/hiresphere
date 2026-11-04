package com.hiresphere.notification;

import com.hiresphere.notification.repository.InAppNotificationRepository;
import com.hiresphere.notification.service.NotificationCleanupJob;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class NotificationCleanupJobTest {

    @Mock
    private InAppNotificationRepository repository;

    @Test
    void shouldDeleteOldRowsAndTrimEachUserToTheirNewestOnes() {
        new NotificationCleanupJob(repository).purge();

        ArgumentCaptor<Instant> cutoff = ArgumentCaptor.forClass(Instant.class);
        verify(repository).deleteOlderThan(cutoff.capture());
        Duration age = Duration.between(cutoff.getValue(), Instant.now());
        assertThat(age.toDays()).isEqualTo(NotificationCleanupJob.RETENTION_DAYS);
        verify(repository).trimPerUser(NotificationCleanupJob.KEEP_PER_USER);
    }
}