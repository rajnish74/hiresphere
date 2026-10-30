package com.hiresphere.job.dto;

import com.hiresphere.job.entity.JobReport;
import com.hiresphere.job.entity.JobReportStatus;

public record JobReportOutcome(Long id, Long jobId, JobReportStatus status, String resolutionNote,
                               boolean jobHidden) {

    public static JobReportOutcome of(JobReport r) {
        return new JobReportOutcome(r.getId(), r.getJobId(), r.getStatus(), r.getResolutionNote(), false);
    }

    public JobReportOutcome withJobHidden(boolean hidden) {
        return new JobReportOutcome(id, jobId, status, resolutionNote, hidden);
    }
}