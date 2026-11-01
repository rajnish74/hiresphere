package com.hiresphere.application.service;

import com.hiresphere.application.dto.ApplicationSummary;
import com.hiresphere.application.dto.CandidateDashboard;
import com.hiresphere.application.entity.ApplicationStatus;
import com.hiresphere.application.repository.ApplicationStatusHistoryRepository;
import com.hiresphere.application.repository.JobApplicationRepository;
import com.hiresphere.candidate.service.CandidateProfileService;
import com.hiresphere.candidate.service.SavedJobService;
import com.hiresphere.common.util.CountUtil;
import com.hiresphere.common.util.PageUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class CandidateDashboardService {

    private final CandidateProfileService profileService;
    private final JobApplicationRepository applicationRepository;
    private final ApplicationStatusHistoryRepository historyRepository;
    private final SavedJobService savedJobService;
    private final ProfileViewService profileViewService;

    @Transactional
    public CandidateDashboard get(Long userId) {
        Instant weekAgo = Instant.now().minus(7, ChronoUnit.DAYS);

        Map<ApplicationStatus, Long> byStatus = CountUtil.toEnumMap(ApplicationStatus.class,
                applicationRepository.countGroupedByStatusForCandidate(userId));
        long total = byStatus.values().stream().mapToLong(Long::longValue).sum();

        CandidateDashboard.WeeklyChange week = new CandidateDashboard.WeeklyChange(
                applicationRepository.countByCandidateUserIdAndCreatedAtAfter(userId, weekAgo),
                historyRepository.countTransitionsForCandidate(userId, ApplicationStatus.SHORTLISTED, weekAgo),
                historyRepository.countTransitionsForCandidate(userId, ApplicationStatus.INTERVIEW, weekAgo));

        List<ApplicationSummary> recent = applicationRepository
                .findByCandidateUserId(userId, PageUtil.newestFirst(0, 5))
                .map(ApplicationSummary::from).getContent();

        return new CandidateDashboard(profileService.getCompleteness(userId), total, byStatus, week,
                savedJobService.count(userId), profileViewService.summary(userId).companiesCount(), recent);
    }
}