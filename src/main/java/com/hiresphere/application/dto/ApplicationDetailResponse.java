package com.hiresphere.application.dto;

import com.hiresphere.application.entity.ApplicationStatus;
import com.hiresphere.application.entity.JobApplication;

import java.time.Instant;
import java.util.List;

public record ApplicationDetailResponse(
        Long id,
        Long jobId,
        String jobTitle,
        String companyName,

        ApplicationStatus status,
        String coverLetter,
        String resumeName,

        Instant appliedAt,
        Instant updatedAt,
        List<TimelineEntry> timeline) {

    public static ApplicationDetailResponse from(JobApplication a) {
        return new ApplicationDetailResponse(
                a.getId(),
                a.getJobId(),
                a.getJobTitle(),
                a.getCompanyName(),
                a.getStatus(),
                a.getCoverLetter(),
                a.getResumeOriginalName(),
                a.getCreatedAt(),
                a.getUpdatedAt(),
                a.getHistory().stream().map(TimelineEntry::from).toList());
    }
}