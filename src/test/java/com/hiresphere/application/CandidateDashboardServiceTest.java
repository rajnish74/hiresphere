package com.hiresphere.application;

import com.hiresphere.application.dto.CandidateDashboard;
import com.hiresphere.application.dto.ProfileViewSummary;
import com.hiresphere.application.entity.ApplicationStatus;
import com.hiresphere.application.entity.JobApplication;
import com.hiresphere.application.repository.ApplicationStatusHistoryRepository;
import com.hiresphere.application.repository.JobApplicationRepository;
import com.hiresphere.application.service.CandidateDashboardService;
import com.hiresphere.application.service.ProfileViewService;
import com.hiresphere.candidate.service.CandidateProfileService;
import com.hiresphere.candidate.service.ProfileCompleteness;
import com.hiresphere.candidate.service.SavedJobService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CandidateDashboardServiceTest {

    @Mock
    private CandidateProfileService profileService;
    @Mock
    private JobApplicationRepository applicationRepository;
    @Mock
    private ApplicationStatusHistoryRepository historyRepository;
    @Mock
    private SavedJobService savedJobService;
    @Mock
    private ProfileViewService profileViewService;

    private CandidateDashboardService service;

    @BeforeEach
    void setUp() {
        service = new CandidateDashboardService(profileService, applicationRepository, historyRepository,
                savedJobService, profileViewService);
    }

    @Test
    void shouldAssembleEverythingTheDashboardNeeds() {
        when(profileService.getCompleteness(1L)).thenReturn(new ProfileCompleteness.Result(70, List.of("resume")));
        when(applicationRepository.countGroupedByStatusForCandidate(1L)).thenReturn(List.<Object[]>of(
                new Object[]{ApplicationStatus.APPLIED, 5L}, new Object[]{ApplicationStatus.SHORTLISTED, 2L}));
        when(applicationRepository.countByCandidateUserIdAndCreatedAtAfter(eq(1L), any(Instant.class)))
                .thenReturn(3L);
        when(historyRepository.countTransitionsForCandidate(eq(1L), eq(ApplicationStatus.SHORTLISTED),
                any(Instant.class))).thenReturn(1L);
        when(historyRepository.countTransitionsForCandidate(eq(1L), eq(ApplicationStatus.INTERVIEW),
                any(Instant.class))).thenReturn(2L);
        when(applicationRepository.findByCandidateUserId(eq(1L), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(new JobApplication())));
        when(savedJobService.count(1L)).thenReturn(4L);
        when(profileViewService.summary(1L)).thenReturn(new ProfileViewSummary(30, 2, List.of()));

        CandidateDashboard dashboard = service.get(1L);

        assertThat(dashboard.profile().percentage()).isEqualTo(70);
        assertThat(dashboard.totalApplications()).isEqualTo(7L);
        assertThat(dashboard.applicationsByStatus()).containsEntry(ApplicationStatus.APPLIED, 5L)
                .containsEntry(ApplicationStatus.OFFER, 0L);
        assertThat(dashboard.thisWeek()).isEqualTo(new CandidateDashboard.WeeklyChange(3L, 1L, 2L));
        assertThat(dashboard.savedJobs()).isEqualTo(4L);
        assertThat(dashboard.profileViewers30d()).isEqualTo(2);
        assertThat(dashboard.recentApplications()).hasSize(1);
    }
}