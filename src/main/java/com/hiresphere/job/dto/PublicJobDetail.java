package com.hiresphere.job.dto;

import com.hiresphere.job.entity.Job;
import com.hiresphere.job.entity.JobType;
import com.hiresphere.job.entity.WorkMode;
import com.hiresphere.skill.entity.Skill;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record PublicJobDetail(Long id, String title, String description, Long companyId, String companyName,
                              String location, JobType jobType, WorkMode workMode,
                              Integer minExperienceYears, Integer maxExperienceYears,
                              BigDecimal salaryMin, BigDecimal salaryMax, String currency, Integer openings,
                              List<String> skills, Instant publishedAt, Instant expiresAt) {

    public static PublicJobDetail from(Job j, String companyName) {
        return new PublicJobDetail(j.getId(), j.getTitle(), j.getDescription(), j.getCompanyId(), companyName,
                j.getLocation(), j.getJobType(), j.getWorkMode(), j.getMinExperienceYears(),
                j.getMaxExperienceYears(), j.getSalaryMin(), j.getSalaryMax(), j.getCurrency(), j.getOpenings(),
                j.getSkills().stream().map(Skill::getName).sorted(String.CASE_INSENSITIVE_ORDER).toList(),
                j.getPublishedAt(), j.getExpiresAt());
    }
}