package com.hiresphere.candidate.dto;

import com.hiresphere.candidate.entity.SavedJob;
import com.hiresphere.job.dto.PublicJobSummary;
import com.hiresphere.job.entity.JobType;
import com.hiresphere.job.entity.WorkMode;

import java.time.Instant;

public record SavedJobResponse(Long jobId, String title, String companyName, String location,
                               JobType jobType, WorkMode workMode, boolean available, Instant savedAt) {

    public static SavedJobResponse of(SavedJob saved, PublicJobSummary live) {
        if (live == null) {
            return new SavedJobResponse(saved.getJobId(), saved.getJobTitle(), saved.getCompanyName(),
                    null, null, null, false, saved.getCreatedAt());
        }
        return new SavedJobResponse(live.id(), live.title(), live.companyName(), live.location(),
                live.jobType(), live.workMode(), true, saved.getCreatedAt());
    }
}