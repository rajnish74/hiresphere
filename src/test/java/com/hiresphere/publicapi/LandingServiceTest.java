package com.hiresphere.publicapi;

import com.hiresphere.application.entity.ApplicationStatus;
import com.hiresphere.application.service.JobApplicationService;
import com.hiresphere.job.service.PublicJobService;
import com.hiresphere.publicapi.dto.PublicStats;
import com.hiresphere.publicapi.service.LandingService;
import com.hiresphere.recruiter.entity.CompanyStatus;
import com.hiresphere.recruiter.service.CompanyService;
import com.hiresphere.user.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LandingServiceTest {

    @Mock
    private PublicJobService publicJobService;
    @Mock
    private CompanyService companyService;
    @Mock
    private UserService userService;
    @Mock
    private JobApplicationService applicationService;

    private LandingService service;

    @BeforeEach
    void setUp() {
        service = new LandingService(publicJobService, companyService, userService, applicationService);
    }

    private void stubCounts() {
        when(publicJobService.countVisibleJobs()).thenReturn(12L);
        when(companyService.countByStatus())
                .thenReturn(Map.of(CompanyStatus.APPROVED, 5L, CompanyStatus.PENDING, 2L));
        when(userService.countActiveCandidates()).thenReturn(40L);
        when(applicationService.platformStatusCounts()).thenReturn(Map.of(ApplicationStatus.HIRED, 3L));
    }

    @Test
    void statsShouldCombineTheFourRealCounts() {
        stubCounts();

        PublicStats stats = service.stats();

        assertThat(stats).isEqualTo(new PublicStats(12L, 5L, 40L, 3L));
    }

    @Test
    void statsShouldBeCachedSoPublicTrafficDoesNotReachTheDatabase() {
        stubCounts();

        service.stats();
        service.stats();
        service.stats();

        verify(publicJobService, times(1)).countVisibleJobs();
    }

    @Test
    void limitsShouldBeClampedBeforeReachingTheService() {
        when(publicJobService.topCompanies(20)).thenReturn(List.of());
        when(publicJobService.topCompanies(1)).thenReturn(List.of());
        when(publicJobService.popularSkills(10)).thenReturn(List.of());

        service.topCompanies(1000);
        service.topCompanies(0);
        service.popularSkills(10);

        verify(publicJobService).topCompanies(20);
        verify(publicJobService).topCompanies(1);
    }
}