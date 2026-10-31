package com.hiresphere.application;

import com.hiresphere.application.entity.ApplicationStatus;
import com.hiresphere.application.repository.JobApplicationRepository;
import com.hiresphere.application.service.CandidateAccessService;
import com.hiresphere.application.service.ProfileViewService;
import com.hiresphere.candidate.dto.CandidateProfileView;
import com.hiresphere.candidate.dto.PublicCandidateView;
import com.hiresphere.candidate.entity.ProfileVisibility;
import com.hiresphere.candidate.service.CandidateProfileService;
import com.hiresphere.common.exception.AppException;
import com.hiresphere.common.exception.ResourceNotFoundException;
import com.hiresphere.recruiter.dto.CompanySummary;
import com.hiresphere.recruiter.entity.CompanyStatus;
import com.hiresphere.recruiter.service.CompanyService;
import com.hiresphere.user.dto.UserResponse;
import com.hiresphere.user.entity.Role;
import com.hiresphere.user.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CandidateAccessServiceTest {

    @Mock
    private CompanyService companyService;
    @Mock
    private UserService userService;
    @Mock
    private CandidateProfileService candidateProfileService;
    @Mock
    private JobApplicationRepository applicationRepository;
    @Mock
    private ProfileViewService profileViewService;

    private CandidateAccessService service;

    @BeforeEach
    void setUp() {
        service = new CandidateAccessService(companyService, userService, candidateProfileService,
                applicationRepository, profileViewService);
    }

    private void approvedAndVerifiedRecruiter() {
        when(companyService.getOwnCompanySummary(2L))
                .thenReturn(new CompanySummary(5L, "Acme", CompanyStatus.APPROVED));
        when(userService.getById(2L)).thenReturn(new UserResponse(2L, "Maya", "m@test.com", Role.RECRUITER, true));
    }

    private void candidateExistsWith(ProfileVisibility visibility, boolean applied) {
        when(userService.findActiveCandidateName(9L)).thenReturn(Optional.of("Asha"));
        when(candidateProfileService.findVisibility(9L)).thenReturn(Optional.of(visibility));
        when(applicationRepository.existsByCompanyIdAndCandidateUserIdAndStatusNot(
                5L, 9L, ApplicationStatus.WITHDRAWN)).thenReturn(applied);
    }

    private CandidateProfileView view() {
        return new CandidateProfileView(9L, "Asha", "Java Dev", null, null, null, null, null, true,
                List.of(), List.of(), List.of(), List.of());
    }

    private PublicCandidateView publicView() {
        return new PublicCandidateView("Asha", "Java Dev", null, "Patna", null, null, true, List.of(), List.of());
    }

    // ---------- recruiter view ----------

    @Test
    void recruiterOfAnUnapprovedCompanyShouldBeForbidden() {
        when(companyService.getOwnCompanySummary(2L))
                .thenReturn(new CompanySummary(5L, "Acme", CompanyStatus.PENDING));

        assertThatThrownBy(() -> service.viewProfile(2L, 9L))
                .isInstanceOfSatisfying(AppException.class,
                        e -> assertThat(e.getStatus()).isEqualTo(HttpStatus.FORBIDDEN));

        verifyNoInteractions(candidateProfileService, profileViewService);
    }

    @Test
    void unverifiedRecruiterShouldBeForbidden() {
        when(companyService.getOwnCompanySummary(2L))
                .thenReturn(new CompanySummary(5L, "Acme", CompanyStatus.APPROVED));
        when(userService.getById(2L)).thenReturn(new UserResponse(2L, "Maya", "m@test.com", Role.RECRUITER, false));

        assertThatThrownBy(() -> service.viewProfile(2L, 9L))
                .isInstanceOfSatisfying(AppException.class,
                        e -> assertThat(e.getStatus()).isEqualTo(HttpStatus.FORBIDDEN));

        verifyNoInteractions(candidateProfileService, profileViewService);
    }

    @Test
    void privateProfileWithoutAnApplicationShouldLookLikeNotFoundAndBeNeitherReadNorLogged() {
        approvedAndVerifiedRecruiter();
        candidateExistsWith(ProfileVisibility.PRIVATE, false);

        assertThatThrownBy(() -> service.viewProfile(2L, 9L)).isInstanceOf(ResourceNotFoundException.class);

        verify(candidateProfileService, never()).findView(any(), any());
        verifyNoInteractions(profileViewService);
    }

    @Test
    void privateProfileShouldBeVisibleToACompanyTheCandidateAppliedToAndTheViewIsLogged() {
        approvedAndVerifiedRecruiter();
        candidateExistsWith(ProfileVisibility.PRIVATE, true);
        when(candidateProfileService.findView(9L, "Asha")).thenReturn(Optional.of(view()));

        assertThat(service.viewProfile(2L, 9L).fullName()).isEqualTo("Asha");

        verify(profileViewService).record(5L, 9L);
    }

    @Test
    void recruitersOnlyProfileShouldBeVisibleWithoutAnApplicationAndLogged() {
        approvedAndVerifiedRecruiter();
        candidateExistsWith(ProfileVisibility.RECRUITERS_ONLY, false);
        when(candidateProfileService.findView(9L, "Asha")).thenReturn(Optional.of(view()));

        assertThat(service.viewProfile(2L, 9L).userId()).isEqualTo(9L);

        verify(profileViewService).record(5L, 9L);
    }

    @Test
    void blockedOrUnknownCandidateShouldBeNotFoundWithoutLookingAtTheProfile() {
        approvedAndVerifiedRecruiter();
        when(userService.findActiveCandidateName(9L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.viewProfile(2L, 9L)).isInstanceOf(ResourceNotFoundException.class);

        verifyNoInteractions(candidateProfileService, applicationRepository, profileViewService);
    }

    // ---------- public page ----------

    @Test
    void publicProfileShouldBeVisibleWithoutLoginAndNotLoggedAsAView() {
        when(userService.findActiveCandidateName(9L)).thenReturn(Optional.of("Asha"));
        when(candidateProfileService.findPublicView(9L, "Asha")).thenReturn(Optional.of(publicView()));

        assertThat(service.viewPublic(9L, "1.1.1.1").fullName()).isEqualTo("Asha");

        verifyNoInteractions(profileViewService);   // anonymous view kisi company ka nahi
    }

    @Test
    void aProfileThatIsNotPublicShouldLookLikeNotFound() {
        when(userService.findActiveCandidateName(9L)).thenReturn(Optional.of("Asha"));
        when(candidateProfileService.findPublicView(9L, "Asha")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.viewPublic(9L, "1.1.1.1")).isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void aBlockedOrUnknownCandidateShouldBeNotFoundOnThePublicPageToo() {
        when(userService.findActiveCandidateName(9L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.viewPublic(9L, "1.1.1.1")).isInstanceOf(ResourceNotFoundException.class);

        verifyNoInteractions(candidateProfileService);
    }

    @Test
    void thePublicPageShouldBeRateLimitedPerIp() {
        when(userService.findActiveCandidateName(9L)).thenReturn(Optional.of("Asha"));
        when(candidateProfileService.findPublicView(9L, "Asha")).thenReturn(Optional.of(publicView()));

        for (int i = 0; i < 30; i++) {
            service.viewPublic(9L, "1.1.1.1");
        }

        assertThatThrownBy(() -> service.viewPublic(9L, "1.1.1.1"))
                .isInstanceOfSatisfying(AppException.class,
                        e -> assertThat(e.getStatus()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS));
        assertThat(service.viewPublic(9L, "2.2.2.2").fullName()).isEqualTo("Asha");   // doosra IP theek
    }
}