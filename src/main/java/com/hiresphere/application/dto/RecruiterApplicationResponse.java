package com.hiresphere.application.dto;

import com.hiresphere.application.entity.ApplicationStatus;
import com.hiresphere.application.entity.JobApplication;

import java.time.Instant;
import java.util.List;

public record RecruiterApplicationResponse(Long id, Long jobId, String jobTitle, Long candidateUserId,
                                           String candidateName, String candidateHeadline,
                                           ApplicationStatus status,
                                           List<ApplicationStatus> allowedNextStatuses, String coverLetter,
                                           String resumeName, Instant appliedAt, List<TimelineEntry> timeline) {

    public static RecruiterApplicationResponse from(JobApplication a) {
        return new RecruiterApplicationResponse(a.getId(), a.getJobId(), a.getJobTitle(), a.getCandidateUserId(),
                a.getCandidateName(), a.getCandidateHeadline(), a.getStatus(),
                a.getStatus().nextForRecruiter().stream().sorted().toList(),
                a.getCoverLetter(), a.getResumeOriginalName(), a.getCreatedAt(),
                a.getHistory().stream().map(TimelineEntry::from).toList());
    }
}