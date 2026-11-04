package com.hiresphere.candidate.dto;

import com.hiresphere.candidate.entity.SavedJob;
import com.hiresphere.job.dto.PublicJobSummary;
import com.hiresphere.job.entity.JobType;
import com.hiresphere.job.entity.SalaryPeriod;
import com.hiresphere.job.entity.WorkMode;

import java.math.BigDecimal;
import java.time.Instant;

public record SavedJobResponse(Long jobId, String title, String companyName, String location,
                               JobType jobType, WorkMode workMode, Integer minExperienceYears,
                               Integer maxExperienceYears, BigDecimal salaryMin, BigDecimal salaryMax,
                               String currency, boolean available, Instant savedAt, SalaryPeriod salaryPeriod) {

    public static SavedJobResponse of(SavedJob saved, PublicJobSummary live) {
        if (live == null) {
            return new SavedJobResponse(saved.getJobId(), saved.getJobTitle(), saved.getCompanyName(),
                    null, null, null, null, null, null, null, null, false, saved.getCreatedAt(), null);
        }
        return new SavedJobResponse(live.id(), live.title(), live.companyName(), live.location(),
                live.jobType(), live.workMode(), live.minExperienceYears(), live.maxExperienceYears(),
                live.salaryMin(), live.salaryMax(), live.currency(), true, saved.getCreatedAt(),
                live.salaryPeriod());
    }
}