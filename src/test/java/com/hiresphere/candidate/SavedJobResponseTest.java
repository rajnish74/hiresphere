package com.hiresphere.candidate;

import com.hiresphere.candidate.dto.SavedJobResponse;
import com.hiresphere.candidate.entity.SavedJob;
import com.hiresphere.job.dto.PublicJobSummary;
import com.hiresphere.job.entity.JobType;
import com.hiresphere.job.entity.WorkMode;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class SavedJobResponseTest {

    private SavedJob saved() {
        SavedJob s = new SavedJob();
        ReflectionTestUtils.setField(s, "jobId", 10L);
        ReflectionTestUtils.setField(s, "jobTitle", "Old title");
        ReflectionTestUtils.setField(s, "companyName", "Old Co");
        ReflectionTestUtils.setField(s, "createdAt", Instant.parse("2026-10-30T10:00:00Z"));
        return s;
    }

    @Test
    void availableJobShouldShowLiveSalaryAndExperience() {
        PublicJobSummary live = new PublicJobSummary(10L, "Java Dev", 5L, "Acme", "Pune", JobType.FULL_TIME,
                WorkMode.HYBRID, 0, 2, new BigDecimal("600000"), new BigDecimal("1200000"), "INR", null, null);

        SavedJobResponse r = SavedJobResponse.of(saved(), live);

        assertThat(r.available()).isTrue();
        assertThat(r.title()).isEqualTo("Java Dev");
        assertThat(r.salaryMax()).isEqualByComparingTo("1200000");
        assertThat(r.minExperienceYears()).isZero();
        assertThat(r.currency()).isEqualTo("INR");
    }

    @Test
    void unavailableJobShouldFallBackToTheSnapshotWithoutDetails() {
        SavedJobResponse r = SavedJobResponse.of(saved(), null);

        assertThat(r.available()).isFalse();
        assertThat(r.title()).isEqualTo("Old title");
        assertThat(r.companyName()).isEqualTo("Old Co");
        assertThat(r.salaryMin()).isNull();
        assertThat(r.minExperienceYears()).isNull();
    }
}