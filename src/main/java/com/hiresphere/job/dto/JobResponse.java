package com.hiresphere.job.dto;

import com.hiresphere.job.entity.Job;
import com.hiresphere.job.entity.JobStatus;
import com.hiresphere.job.entity.JobType;
import com.hiresphere.job.entity.WorkMode;
import com.hiresphere.skill.entity.Skill;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record JobResponse(
        Long id, Long companyId, String title, String description, String location,
        JobType jobType, WorkMode workMode, Integer minExperienceYears, Integer maxExperienceYears,
        BigDecimal salaryMin, BigDecimal salaryMax, String currency, Integer openings,
        JobStatus status, List<String> skills,
        Instant publishedAt, Instant expiresAt, Instant closedAt, Instant createdAt) {

    public static JobResponse from(Job j) {
        return new JobResponse(
                j.getId(), j.getCompanyId(), j.getTitle(), j.getDescription(), j.getLocation(),
                j.getJobType(), j.getWorkMode(), j.getMinExperienceYears(), j.getMaxExperienceYears(),
                j.getSalaryMin(), j.getSalaryMax(), j.getCurrency(), j.getOpenings(),
                j.getStatus(),
                j.getSkills().stream().map(Skill::getName).sorted(String.CASE_INSENSITIVE_ORDER).toList(),
                j.getPublishedAt(), j.getExpiresAt(), j.getClosedAt(), j.getCreatedAt());
    }
}