package com.hiresphere.job.dto;

import com.hiresphere.job.entity.JobReport;
import com.hiresphere.job.entity.JobReportReason;
import com.hiresphere.job.entity.JobReportStatus;

import java.time.Instant;

public record JobReportResponse(Long id, Long jobId, JobReportReason reason, JobReportStatus status,
                                Instant createdAt) {

    public static JobReportResponse from(JobReport r) {
        return new JobReportResponse(r.getId(), r.getJobId(), r.getReason(), r.getStatus(), r.getCreatedAt());
    }
}