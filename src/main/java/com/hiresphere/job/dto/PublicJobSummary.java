package com.hiresphere.job.dto;

import com.hiresphere.job.entity.Job;
import com.hiresphere.job.entity.JobType;
import com.hiresphere.job.entity.WorkMode;

import java.math.BigDecimal;
import java.time.Instant;

public record PublicJobSummary(Long id, String title, Long companyId, String companyName, String location,
                               JobType jobType, WorkMode workMode, Integer minExperienceYears,
                               Integer maxExperienceYears, BigDecimal salaryMin, BigDecimal salaryMax,
                               String currency, Instant publishedAt, Instant expiresAt) {

    public static PublicJobSummary from(Job j, String companyName) {
        return new PublicJobSummary(j.getId(), j.getTitle(), j.getCompanyId(), companyName, j.getLocation(),
                j.getJobType(), j.getWorkMode(), j.getMinExperienceYears(), j.getMaxExperienceYears(),
                j.getSalaryMin(), j.getSalaryMax(), j.getCurrency(), j.getPublishedAt(), j.getExpiresAt());
    }
}