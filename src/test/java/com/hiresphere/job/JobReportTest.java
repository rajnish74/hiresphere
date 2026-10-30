package com.hiresphere.job;

import com.hiresphere.common.exception.AppException;
import com.hiresphere.job.entity.JobReport;
import com.hiresphere.job.entity.JobReportReason;
import com.hiresphere.job.entity.JobReportStatus;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JobReportTest {

    private JobReport report() {
        return JobReport.create(20L, 1L, JobReportReason.SCAM, "Asked for money", "Java Dev");
    }

    @Test
    void newReportShouldBeOpen() {
        JobReport r = report();

        assertThat(r.getStatus()).isEqualTo(JobReportStatus.OPEN);
        assertThat(r.getCreatedAt()).isNotNull();
        assertThat(r.getResolvedBy()).isNull();
    }

    @Test
    void resolveShouldRecordWhoWhenAndWhy() {
        JobReport r = report();

        r.resolve(99L, "Confirmed scam");

        assertThat(r.getStatus()).isEqualTo(JobReportStatus.RESOLVED);
        assertThat(r.getResolvedBy()).isEqualTo(99L);
        assertThat(r.getResolutionNote()).isEqualTo("Confirmed scam");
        assertThat(r.getResolvedAt()).isNotNull();
    }

    @Test
    void dismissShouldCloseTheReport() {
        JobReport r = report();

        r.dismiss(99L, "Job is genuine");

        assertThat(r.getStatus()).isEqualTo(JobReportStatus.DISMISSED);
    }

    @Test
    void aClosedReportCannotBeClosedAgain() {
        JobReport r = report();
        r.resolve(99L, "x");

        assertThatThrownBy(() -> r.dismiss(99L, "y")).isInstanceOf(AppException.class);
        assertThatThrownBy(() -> r.resolve(99L, "y")).isInstanceOf(AppException.class);
        assertThat(r.getStatus()).isEqualTo(JobReportStatus.RESOLVED);
    }
}