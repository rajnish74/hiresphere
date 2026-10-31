package com.hiresphere.job.event;

import com.hiresphere.job.entity.JobReportStatus;

public record JobReportClosedEvent(Long reporterUserId, Long jobId, String jobTitle, JobReportStatus outcome) {
}