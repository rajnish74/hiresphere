package com.hiresphere.job.dto;

import com.hiresphere.job.entity.Job;
import com.hiresphere.job.entity.JobStatus;

import java.time.Instant;

public record AdminJobSummary(Long id, String title, Long companyId, String companyName, JobStatus status,
                              boolean hidden, String moderationReason, Instant createdAt) {

    public static AdminJobSummary from(Job j, String companyName) {
        return new AdminJobSummary(j.getId(), j.getTitle(), j.getCompanyId(), companyName, j.getStatus(),
                j.isHidden(), j.getModerationReason(), j.getCreatedAt());
    }
}