package com.hiresphere.application.dto;

import com.hiresphere.application.entity.ApplicationStatus;
import com.hiresphere.application.entity.JobApplication;

import java.time.Instant;

public record ApplicationSummary(Long id, Long jobId, String jobTitle, String companyName,
                                 ApplicationStatus status, Instant appliedAt, Instant updatedAt) {

    public static ApplicationSummary from(JobApplication a) {
        return new ApplicationSummary(a.getId(), a.getJobId(), a.getJobTitle(), a.getCompanyName(),
                a.getStatus(), a.getCreatedAt(), a.getUpdatedAt());
    }
}