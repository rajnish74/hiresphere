package com.hiresphere.job.dto;

import com.hiresphere.job.entity.Job;
import com.hiresphere.job.entity.JobType;
import com.hiresphere.job.entity.SalaryPeriod;
import com.hiresphere.job.entity.WorkMode;
import com.hiresphere.skill.entity.Skill;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record PublicJobSummary(Long id, String title, Long companyId, String companyName, String location,
                               JobType jobType, WorkMode workMode, Integer minExperienceYears,
                               Integer maxExperienceYears, BigDecimal salaryMin, BigDecimal salaryMax,
                               String currency, Instant publishedAt, Instant expiresAt, List<String> skills,
                               SalaryPeriod salaryPeriod) {

    public PublicJobSummary(Long id, String title, Long companyId, String companyName, String location,
                            JobType jobType, WorkMode workMode, Integer minExperienceYears,
                            Integer maxExperienceYears, BigDecimal salaryMin, BigDecimal salaryMax,
                            String currency, Instant publishedAt, Instant expiresAt, List<String> skills) {
        this(id, title, companyId, companyName, location, jobType, workMode, minExperienceYears,
                maxExperienceYears, salaryMin, salaryMax, currency, publishedAt, expiresAt, skills,
                SalaryPeriod.YEAR);
    }

    public PublicJobSummary(Long id, String title, Long companyId, String companyName, String location,
                            JobType jobType, WorkMode workMode, Integer minExperienceYears,
                            Integer maxExperienceYears, BigDecimal salaryMin, BigDecimal salaryMax,
                            String currency, Instant publishedAt, Instant expiresAt) {
        this(id, title, companyId, companyName, location, jobType, workMode, minExperienceYears,
                maxExperienceYears, salaryMin, salaryMax, currency, publishedAt, expiresAt, List.of(),
                SalaryPeriod.YEAR);
    }

    public static PublicJobSummary from(Job j, String companyName) {
        return new PublicJobSummary(j.getId(), j.getTitle(), j.getCompanyId(), companyName, j.getLocation(),
                j.getJobType(), j.getWorkMode(), j.getMinExperienceYears(), j.getMaxExperienceYears(),
                j.getSalaryMin(), j.getSalaryMax(), j.getCurrency(), j.getPublishedAt(), j.getExpiresAt(),
                j.getSkills().stream().map(Skill::getName).sorted(String.CASE_INSENSITIVE_ORDER).toList(),
                j.getSalaryPeriod());
    }
}