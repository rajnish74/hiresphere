package com.hiresphere.job.dto;

import com.hiresphere.job.entity.Job;
import com.hiresphere.job.entity.JobStatus;
import com.hiresphere.job.entity.JobType;
import com.hiresphere.job.entity.WorkMode;

import java.time.Instant;

public record JobSummaryResponse(Long id, String title, String location, JobType jobType, WorkMode workMode,
                                 JobStatus status, Instant publishedAt, Instant expiresAt, Instant createdAt) {

    public static JobSummaryResponse from(Job j) {
        return new JobSummaryResponse(j.getId(), j.getTitle(), j.getLocation(), j.getJobType(), j.getWorkMode(),
                j.getStatus(), j.getPublishedAt(), j.getExpiresAt(), j.getCreatedAt());
    }
}