package com.hiresphere.application.dto;

import com.hiresphere.application.entity.ApplicationStatus;
import com.hiresphere.application.entity.JobApplication;

import java.time.Instant;

public record RecruiterApplicationSummary(
        Long id,
        String
        candidateName,
        String candidateHeadline,

        ApplicationStatus status,
        Instant appliedAt) {

    public static RecruiterApplicationSummary from(JobApplication a) {
        return new RecruiterApplicationSummary(
                a.getId(),
                a.getCandidateName(),
                a.getCandidateHeadline(),
                a.getStatus(),
                a.getCreatedAt());
    }
}