package com.hiresphere.admin.service;

import com.hiresphere.admin.dto.PlatformStats;
import com.hiresphere.application.entity.ApplicationStatus;
import com.hiresphere.application.service.JobApplicationService;
import com.hiresphere.job.service.JobModerationService;
import com.hiresphere.recruiter.service.CompanyService;
import com.hiresphere.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AdminStatsService {

    private final UserService userService;
    private final CompanyService companyService;
    private final JobModerationService jobModerationService;
    private final JobApplicationService applicationService;

    @Transactional(readOnly = true)
    public PlatformStats get() {
        Instant now = Instant.now();
        Instant weekAgo = now.minus(7, ChronoUnit.DAYS);

        Map<ApplicationStatus, Long> applicationsByStatus = applicationService.platformStatusCounts();
        long applicationTotal = applicationsByStatus.values().stream().mapToLong(Long::longValue).sum();

        return new PlatformStats(
                userService.stats(weekAgo),
                companyService.countByStatus(),
                new PlatformStats.JobStats(jobModerationService.countByStatus(), jobModerationService.countHidden()),
                new PlatformStats.ApplicationStats(applicationTotal, applicationsByStatus,
                        applicationService.countSince(weekAgo)),
                now);
    }
}