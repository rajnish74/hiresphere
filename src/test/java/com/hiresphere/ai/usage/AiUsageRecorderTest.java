package com.hiresphere.ai.usage;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AiUsageRecorderTest {

    private static final Instant NOW = Instant.parse("2026-11-09T23:59:30Z");
    private static final Instant TODAY = Instant.parse("2026-11-09T00:00:00Z");

    @Mock
    private AiUsageRepository repository;

    private AiUsageRecorder recorder;

    @BeforeEach
    void setUp() {
        recorder = new AiUsageRecorder(repository, Clock.fixed(NOW, ZoneOffset.UTC));
    }

    private AiUsage saved() {
        ArgumentCaptor<AiUsage> captor = ArgumentCaptor.forClass(AiUsage.class);
        verify(repository).save(captor.capture());
        return captor.getValue();
    }

    @Test
    void recordShouldStoreTheMetadataWithTheClocksTime() {
        recorder.record(new UsageEntry(7L, "SKILLS_EXTRACT", "model-x", UsageStatus.OK, null, 10, 5, 15, 40));

        AiUsage row = saved();
        assertThat(row.getUserId()).isEqualTo(7L);
        assertThat(row.getFeature()).isEqualTo("SKILLS_EXTRACT");
        assertThat(row.getModel()).isEqualTo("model-x");
        assertThat(row.getStatus()).isEqualTo(UsageStatus.OK);
        assertThat(row.getTotalTokens()).isEqualTo(15);
        assertThat(row.getLatencyMs()).isEqualTo(40);
        assertThat(row.getCreatedAt()).isEqualTo(NOW);
    }

    @Test
    void oddValuesShouldBeMadeSafeSoTheInsertCannotFail() {
        recorder.record(new UsageEntry(null, " ", "m".repeat(300), UsageStatus.FAILED, "e".repeat(80), -5, -1, -6, -2));

        AiUsage row = saved();
        assertThat(row.getUserId()).isNull();
        assertThat(row.getFeature()).isEqualTo("UNSPECIFIED");
        assertThat(row.getModel()).hasSize(100);
        assertThat(row.getErrorKind()).hasSize(30);
        assertThat(row.getPromptTokens()).isZero();
        assertThat(row.getOutputTokens()).isZero();
        assertThat(row.getTotalTokens()).isZero();
        assertThat(row.getLatencyMs()).isZero();
    }

    @Test
    void todayShouldStartAtMidnightUtcEvenJustBeforeTheDayEnds() {
        assertThat(recorder.startOfToday()).isEqualTo(TODAY);
    }

    @Test
    void thebudgetSumsShouldCountFromTheStartOfTheUtcDay() {
        when(repository.sumTokensForUser(7L, TODAY)).thenReturn(1_234L);
        when(repository.sumTokensAll(TODAY)).thenReturn(9_999L);

        assertThat(recorder.tokensUsedToday(7L)).isEqualTo(1_234L);
        assertThat(recorder.tokensUsedTodayByEveryone()).isEqualTo(9_999L);
    }

    @Test
    void theSummaryRowsShouldBecomeTypedFigures() {
        Instant since = TODAY.minusSeconds(86_400);
        when(repository.summarySince(since)).thenReturn(
                List.<Object[]>of(new Object[]{"SKILLS_EXTRACT", 4L, 2L, 1L, 1, 120L}));

        List<FeatureUsage> summary = recorder.summarySince(since);

        assertThat(summary).containsExactly(new FeatureUsage("SKILLS_EXTRACT", 4, 2, 1, 1, 120));
    }
}