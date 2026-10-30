package com.hiresphere.job.dto;

import com.hiresphere.job.entity.JobReport;
import com.hiresphere.job.entity.JobReportReason;
import com.hiresphere.job.entity.JobReportStatus;

import java.time.Instant;

public record AdminJobReportResponse(Long id, Long jobId, String jobTitle, JobReportReason reason, String details,
                                     JobReportStatus status, String reporterEmail, long openReportsForJob,
                                     String resolutionNote, Instant resolvedAt, Instant createdAt) {

    public static AdminJobReportResponse from(JobReport r, String reporterEmail, long openReportsForJob) {
        return new AdminJobReportResponse(r.getId(), r.getJobId(), r.getJobTitle(), r.getReason(), r.getDetails(),
                r.getStatus(), reporterEmail, openReportsForJob, r.getResolutionNote(), r.getResolvedAt(),
                r.getCreatedAt());
    }
}