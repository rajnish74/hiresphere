package com.hiresphere.admin.dto;

import com.hiresphere.application.entity.ApplicationStatus;
import com.hiresphere.job.entity.JobStatus;
import com.hiresphere.recruiter.entity.CompanyStatus;
import com.hiresphere.user.dto.UserStats;

import java.time.Instant;
import java.util.Map;

public record PlatformStats(UserStats users, Map<CompanyStatus, Long> companies, JobStats jobs,
                            ApplicationStats applications, Instant generatedAt) {

    public record JobStats(Map<JobStatus, Long> byStatus, long hidden) {
    }

    public record ApplicationStats(long total, Map<ApplicationStatus, Long> byStatus, long newLast7Days) {
    }
}