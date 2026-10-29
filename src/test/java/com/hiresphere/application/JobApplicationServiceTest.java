package com.hiresphere.application;

import com.hiresphere.application.dto.*;
import com.hiresphere.application.entity.ApplicationStatus;
import com.hiresphere.application.entity.JobApplication;
import com.hiresphere.application.event.ApplicationSubmittedEvent;
import com.hiresphere.application.repository.JobApplicationRepository;
import com.hiresphere.application.service.JobApplicationService;
import com.hiresphere.candidate.dto.ApplicantSnapshot;
import com.hiresphere.candidate.service.CandidateProfileService;
import com.hiresphere.common.exception.AppException;
import com.hiresphere.common.exception.BadRequestException;
import com.hiresphere.common.exception.DuplicateResourceException;
import com.hiresphere.common.exception.ResourceNotFoundException;
import com.hiresphere.common.storage.FileStorageService;
import com.hiresphere.job.dto.JobRef;
import com.hiresphere.job.event.JobFilledEvent;
import com.hiresphere.job.service.JobQueryService;
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
import org.springframework.context.ApplicationEventPublisher;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JobApplicationServiceTest {

    @Mock
    private JobApplicationRepository applicationRepository;
    @Mock
    private JobQueryService jobQueryService;
    @Mock
    private CompanyService companyService;
    @Mock
    private CandidateProfileService candidateProfileService;
    @Mock
    private UserService userService;
    @Mock
    private FileStorageService storage;

    private JobApplicationService service;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @BeforeEach
    void setUp() {
        service = new JobApplicationService(applicationRepository, jobQueryService, companyService,
                candidateProfileService, userService, storage, eventPublisher);
    }

    private final JobRef job = new JobRef(10L, "Java Dev", 5L);

    @Test
    void applyShouldFailWhenAlreadyApplied() {
        when(jobQueryService.getPublicJob(10L)).thenReturn(job);
        when(applicationRepository.existsByJobIdAndCandidateUserId(10L, 1L)).thenReturn(true);

        assertThatThrownBy(() -> service.apply(1L, 10L, null))
                .isInstanceOf(DuplicateResourceException.class);

        verify(storage, never()).copy(any());
    }

    @Test
    void applyShouldFailWhenCandidateHasNoResume() {
        when(jobQueryService.getPublicJob(10L)).thenReturn(job);
        when(applicationRepository.existsByJobIdAndCandidateUserId(10L, 1L)).thenReturn(false);
        when(candidateProfileService.getSnapshot(1L)).thenReturn(new ApplicantSnapshot("Dev", null, null));

        assertThatThrownBy(() -> service.apply(1L, 10L, null)).isInstanceOf(BadRequestException.class);

        verify(applicationRepository, never()).saveAndFlush(any());
    }

    @Test
    void applyShouldSnapshotDataCopyResumeAndStartTimeline() {
        when(jobQueryService.getPublicJob(10L)).thenReturn(job);
        when(applicationRepository.existsByJobIdAndCandidateUserId(10L, 1L)).thenReturn(false);
        when(candidateProfileService.getSnapshot(1L))
                .thenReturn(new ApplicantSnapshot("Backend dev", "orig.pdf", "cv.pdf"));
        when(userService.getById(1L)).thenReturn(new UserResponse(1L, "Asha", "asha@test.com", Role.CANDIDATE, true));
        when(companyService.getNames(Set.of(5L))).thenReturn(Map.of(5L, "Acme"));
        when(storage.copy("orig.pdf")).thenReturn("copy.pdf");
        when(applicationRepository.saveAndFlush(any(JobApplication.class))).thenAnswer(i -> i.getArgument(0));


        ApplicationDetailResponse response = service.apply(1L, 10L, new ApplyRequest("Hello team"));

        assertThat(response.status()).isEqualTo(ApplicationStatus.APPLIED);
        assertThat(response.jobTitle()).isEqualTo("Java Dev");
        assertThat(response.companyName()).isEqualTo("Acme");
        assertThat(response.coverLetter()).isEqualTo("Hello team");
        assertThat(response.timeline()).hasSize(1);
        verify(storage).copy("orig.pdf");
        verify(eventPublisher).publishEvent(any(ApplicationSubmittedEvent.class));
    }

    @Test
    void recruiterOfUnapprovedCompanyCannotChangeStatus() {
        when(companyService.getOwnCompanySummary(2L))
                .thenReturn(new CompanySummary(5L, "Acme", CompanyStatus.PENDING));

        assertThatThrownBy(() -> service.changeStatus(2L, 7L,
                new StatusChangeRequest(ApplicationStatus.SHORTLISTED, null)))
                .isInstanceOf(AppException.class);

        verify(applicationRepository, never()).findByIdAndCompanyId(any(), any());
    }

    @Test
    void recruiterCannotSetAppliedOrWithdrawn() {
        when(companyService.getOwnCompanySummary(2L))
                .thenReturn(new CompanySummary(5L, "Acme", CompanyStatus.APPROVED));

        assertThatThrownBy(() -> service.changeStatus(2L, 7L,
                new StatusChangeRequest(ApplicationStatus.WITHDRAWN, null)))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    void recruiterShouldMoveApplicationAndRecordNote() {
        JobApplication application = new JobApplication();
        application.recordSubmitted(1L);
        when(companyService.getOwnCompanySummary(2L))
                .thenReturn(new CompanySummary(5L, "Acme", CompanyStatus.APPROVED));
        when(applicationRepository.findByIdAndCompanyId(7L, 5L)).thenReturn(Optional.of(application));

        RecruiterApplicationResponse response = service.changeStatus(2L, 7L,
                new StatusChangeRequest(ApplicationStatus.SHORTLISTED, "Good profile"));

        assertThat(response.status()).isEqualTo(ApplicationStatus.SHORTLISTED);
        assertThat(response.timeline()).hasSize(2);
        assertThat(response.timeline().get(1).note()).isEqualTo("Good profile");
        assertThat(response.allowedNextStatuses())
                .containsExactly(ApplicationStatus.INTERVIEW, ApplicationStatus.REJECTED);
    }


    private JobApplication applicationAtOffer() {
        JobApplication a = new JobApplication();
        a.setJobId(10L);
        a.recordSubmitted(1L);
        a.transitionTo(ApplicationStatus.SHORTLISTED, 2L, null);
        a.transitionTo(ApplicationStatus.INTERVIEW, 2L, null);
        a.transitionTo(ApplicationStatus.OFFER, 2L, null);
        return a;
    }

    private void stubApprovedRecruiterWith(JobApplication application, JobRef job, long alreadyHired) {
        when(companyService.getOwnCompanySummary(2L))
                .thenReturn(new CompanySummary(5L, "Acme", CompanyStatus.APPROVED));
        when(applicationRepository.findByIdAndCompanyId(7L, 5L)).thenReturn(Optional.of(application));
        when(jobQueryService.getCompanyJob(5L, 10L)).thenReturn(job);
        when(applicationRepository.countByJobIdAndStatus(10L, ApplicationStatus.HIRED)).thenReturn(alreadyHired);
    }

    @Test
    void hiringTheLastOpeningShouldPublishJobFilledEvent() {
        stubApprovedRecruiterWith(applicationAtOffer(), new JobRef(10L, "Java Dev", 5L, 2), 1L);

        service.changeStatus(2L, 7L, new StatusChangeRequest(ApplicationStatus.HIRED, null));

        verify(eventPublisher).publishEvent(any(JobFilledEvent.class));
    }

    @Test
    void hiringWhenOpeningsRemainShouldNotPublishEvent() {
        stubApprovedRecruiterWith(applicationAtOffer(), new JobRef(10L, "Java Dev", 5L, 3), 0L);

        service.changeStatus(2L, 7L, new StatusChangeRequest(ApplicationStatus.HIRED, null));

        verify(eventPublisher, never()).publishEvent(any(JobFilledEvent.class));
    }

    @Test
    void hiringBeyondOpeningsShouldBeRejected() {
        JobApplication application = applicationAtOffer();
        stubApprovedRecruiterWith(application, new JobRef(10L, "Java Dev", 5L, 1), 1L);

        assertThatThrownBy(() -> service.changeStatus(2L, 7L,
                new StatusChangeRequest(ApplicationStatus.HIRED, null)))
                .isInstanceOf(AppException.class)
                .hasMessageContaining("openings");

        assertThat(application.getStatus()).isEqualTo(ApplicationStatus.OFFER);   // status nahi badla
        verify(eventPublisher, never()).publishEvent(any(Object.class));
    }

    @Test
    void statusCountsShouldIncludeZeroForMissingStatuses() {
        when(companyService.getOwnCompanySummary(2L))
                .thenReturn(new CompanySummary(5L, "Acme", CompanyStatus.APPROVED));
        when(jobQueryService.getCompanyJob(5L, 10L)).thenReturn(job);
        when(applicationRepository.countByStatus(10L)).thenReturn(List.of(
                new StatusCount(ApplicationStatus.APPLIED, 4L),
                new StatusCount(ApplicationStatus.HIRED, 1L)));

        Map<ApplicationStatus, Long> counts = service.statusCounts(2L, 10L);

        assertThat(counts).hasSize(ApplicationStatus.values().length);
        assertThat(counts.get(ApplicationStatus.APPLIED)).isEqualTo(4L);
        assertThat(counts.get(ApplicationStatus.HIRED)).isEqualTo(1L);
        assertThat(counts.get(ApplicationStatus.OFFER)).isZero();
    }


    @Test
    void withdrawShouldReturnNotFoundForAnotherCandidatesApplication() {
        when(applicationRepository.findByIdAndCandidateUserId(7L, 2L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.withdraw(2L, 7L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void withdrawShouldMoveOwnApplicationToWithdrawn() {
        JobApplication application = new JobApplication();
        application.recordSubmitted(1L);
        when(applicationRepository.findByIdAndCandidateUserId(7L, 1L)).thenReturn(Optional.of(application));

        ApplicationDetailResponse response = service.withdraw(1L, 7L);

        assertThat(response.status()).isEqualTo(ApplicationStatus.WITHDRAWN);
        assertThat(response.timeline()).hasSize(2);
    }


    @Test
    void applyShouldBeBlockedUntilEmailIsVerified() {
        when(jobQueryService.getPublicJob(10L)).thenReturn(job);
        when(applicationRepository.existsByJobIdAndCandidateUserId(10L, 1L)).thenReturn(false);
        when(candidateProfileService.getSnapshot(1L))
                .thenReturn(new ApplicantSnapshot("Dev", "orig.pdf", "cv.pdf"));
        when(userService.getById(1L)).thenReturn(new UserResponse(1L, "Asha", "asha@test.com", Role.CANDIDATE, false));

        assertThatThrownBy(() -> service.apply(1L, 10L, null))
                .isInstanceOf(AppException.class)
                .hasMessageContaining("verify your email");

        verify(storage, never()).copy(any());
        verify(applicationRepository, never()).saveAndFlush(any());
    }
}