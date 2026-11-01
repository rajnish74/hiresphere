package com.hiresphere.publicapi.service;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.hiresphere.application.entity.ApplicationStatus;
import com.hiresphere.application.service.JobApplicationService;
import com.hiresphere.job.dto.SkillCount;
import com.hiresphere.job.dto.TopCompany;
import com.hiresphere.job.service.PublicJobService;
import com.hiresphere.publicapi.dto.PublicStats;
import com.hiresphere.recruiter.entity.CompanyStatus;
import com.hiresphere.recruiter.service.CompanyService;
import com.hiresphere.user.service.UserService;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.List;

@Service
public class LandingService {

    private final PublicJobService publicJobService;
    private final CompanyService companyService;
    private final UserService userService;
    private final JobApplicationService applicationService;

    private final Cache<String, PublicStats> stats = newCache();
    private final Cache<Integer, List<TopCompany>> topCompanies = newCache();
    private final Cache<Integer, List<SkillCount>> popularSkills = newCache();

    public LandingService(PublicJobService publicJobService, CompanyService companyService,
                          UserService userService, JobApplicationService applicationService) {
        this.publicJobService = publicJobService;
        this.companyService = companyService;
        this.userService = userService;
        this.applicationService = applicationService;
    }

    public PublicStats stats() {
        return stats.get("stats", key -> new PublicStats(
                publicJobService.countVisibleJobs(),
                companyService.countByStatus().getOrDefault(CompanyStatus.APPROVED, 0L),
                userService.countActiveCandidates(),
                applicationService.platformStatusCounts().getOrDefault(ApplicationStatus.HIRED, 0L)));
    }

    public List<TopCompany> topCompanies(int limit) {
        return topCompanies.get(clamp(limit), publicJobService::topCompanies);
    }

    public List<SkillCount> popularSkills(int limit) {
        return popularSkills.get(clamp(limit), publicJobService::popularSkills);
    }

    private int clamp(int limit) {
        return Math.min(Math.max(limit, 1), 20);
    }

    private static <K, V> Cache<K, V> newCache() {
        return Caffeine.newBuilder().expireAfterWrite(Duration.ofSeconds(60)).maximumSize(50).build();
    }
}