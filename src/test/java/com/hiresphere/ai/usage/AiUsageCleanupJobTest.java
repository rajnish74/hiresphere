package com.hiresphere.ai.usage;

import com.hiresphere.ai.config.AiLimitsProperties;
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
class AiUsageCleanupJobTest {

    @Mock
    private AiUsageRepository repository;

    @Test
    void shouldDeleteRowsOlderThanTheConfiguredRetention() {
        new AiUsageCleanupJob(repository, new AiLimitsProperties(0, 0, 0, null, 0, 30)).purge();

        ArgumentCaptor<Instant> cutoff = ArgumentCaptor.forClass(Instant.class);
        verify(repository).deleteOlderThan(cutoff.capture());
        assertThat(Duration.between(cutoff.getValue(), Instant.now()).toDays()).isEqualTo(30);
    }
}