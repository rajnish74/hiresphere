package com.hiresphere.job.dto;

import com.hiresphere.job.entity.JobReport;
import com.hiresphere.job.entity.JobReportReason;
import com.hiresphere.job.entity.JobReportStatus;

import java.time.Instant;

public record MyJobReportResponse(Long id, Long jobId, String jobTitle, JobReportReason reason,
                                  JobReportStatus status, Instant createdAt, Instant resolvedAt) {

    public static MyJobReportResponse from(JobReport r) {
        return new MyJobReportResponse(r.getId(), r.getJobId(), r.getJobTitle(), r.getReason(),
                r.getStatus(), r.getCreatedAt(), r.getResolvedAt());
    }
}