package com.hiresphere.job;

import com.hiresphere.common.dto.PageResponse;
import com.hiresphere.common.exception.AppException;
import com.hiresphere.common.exception.BadRequestException;
import com.hiresphere.common.exception.DuplicateResourceException;
import com.hiresphere.common.exception.ResourceNotFoundException;
import com.hiresphere.job.dto.*;
import com.hiresphere.job.entity.JobReport;
import com.hiresphere.job.entity.JobReportReason;
import com.hiresphere.job.entity.JobReportStatus;
import com.hiresphere.job.event.JobReportClosedEvent;
import com.hiresphere.job.repository.JobReportRepository;
import com.hiresphere.job.service.JobQueryService;
import com.hiresphere.job.service.JobReportService;
import com.hiresphere.user.dto.UserResponse;
import com.hiresphere.user.entity.Role;
import com.hiresphere.user.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JobReportServiceTest {

    @Mock
    private JobReportRepository repository;
    @Mock
    private JobQueryService jobQueryService;
    @Mock
    private UserService userService;

    private JobReportService service;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @BeforeEach
    void setUp() {
        service = new JobReportService(repository, jobQueryService, userService, eventPublisher);
    }

    private UserResponse reporter(boolean verified) {
        return new UserResponse(1L, "Asha", "asha@test.com", Role.CANDIDATE, verified);
    }

    private void stubAllowedReport() {
        when(userService.getById(1L)).thenReturn(reporter(true));
        when(jobQueryService.getPublicJob(10L)).thenReturn(new JobRef(10L, "Java Dev", 5L));
        when(repository.existsByReporterUserIdAndJobId(1L, 10L)).thenReturn(false);
    }

    // ---------- candidate side ----------

    @Test
    void reportShouldBeStoredWithATrimmedDetailAndAJobTitleSnapshot() {
        stubAllowedReport();
        when(repository.countByReporterUserIdAndCreatedAtAfter(eq(1L), any(Instant.class))).thenReturn(0L);
        when(repository.saveAndFlush(any(JobReport.class))).thenAnswer(i -> i.getArgument(0));

        JobReportResponse response = service.report(1L, 10L,
                new ReportJobRequest(JobReportReason.SCAM, "  asked for money upfront "));

        ArgumentCaptor<JobReport> saved = ArgumentCaptor.forClass(JobReport.class);
        verify(repository).saveAndFlush(saved.capture());
        assertThat(saved.getValue().getDetails()).isEqualTo("asked for money upfront");
        assertThat(saved.getValue().getJobTitle()).isEqualTo("Java Dev");
        assertThat(response.status()).isEqualTo(JobReportStatus.OPEN);
    }

    @Test
    void unverifiedReporterShouldBeForbidden() {
        when(userService.getById(1L)).thenReturn(reporter(false));

        assertThatThrownBy(() -> service.report(1L, 10L, new ReportJobRequest(JobReportReason.SCAM, null)))
                .isInstanceOfSatisfying(AppException.class,
                        e -> assertThat(e.getStatus()).isEqualTo(HttpStatus.FORBIDDEN));

        verifyNoInteractions(jobQueryService, repository);
    }

    @Test
    void reasonOtherNeedsADescription() {
        when(userService.getById(1L)).thenReturn(reporter(true));

        assertThatThrownBy(() -> service.report(1L, 10L, new ReportJobRequest(JobReportReason.OTHER, " short ")))
                .isInstanceOf(BadRequestException.class);

        verifyNoInteractions(jobQueryService, repository);
    }

    @Test
    void aJobThatIsNotPublicCannotBeReported() {
        when(userService.getById(1L)).thenReturn(reporter(true));
        when(jobQueryService.getPublicJob(10L)).thenThrow(new ResourceNotFoundException("Job not found"));

        assertThatThrownBy(() -> service.report(1L, 10L, new ReportJobRequest(JobReportReason.SCAM, null)))
                .isInstanceOf(ResourceNotFoundException.class);

        verifyNoInteractions(repository);
    }

    @Test
    void reportingTheSameJobTwiceShouldFail() {
        when(userService.getById(1L)).thenReturn(reporter(true));
        when(jobQueryService.getPublicJob(10L)).thenReturn(new JobRef(10L, "Java Dev", 5L));
        when(repository.existsByReporterUserIdAndJobId(1L, 10L)).thenReturn(true);

        assertThatThrownBy(() -> service.report(1L, 10L, new ReportJobRequest(JobReportReason.SCAM, null)))
                .isInstanceOf(DuplicateResourceException.class);

        verify(repository, never()).saveAndFlush(any());
    }

    @Test
    void dailyLimitShouldGiveTooManyRequests() {
        stubAllowedReport();
        when(repository.countByReporterUserIdAndCreatedAtAfter(eq(1L), any(Instant.class))).thenReturn(10L);

        assertThatThrownBy(() -> service.report(1L, 10L, new ReportJobRequest(JobReportReason.SCAM, null)))
                .isInstanceOfSatisfying(AppException.class,
                        e -> assertThat(e.getStatus()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS));

        verify(repository, never()).saveAndFlush(any());
    }

    // ---------- admin side ----------

    @Test
    void resolveShouldCloseTheReportWithATrimmedNote() {
        JobReport report = JobReport.create(20L, 1L, JobReportReason.SCAM, null, "Java Dev");
        when(repository.findById(7L)).thenReturn(Optional.of(report));

        JobReportOutcome outcome = service.resolve(7L, 99L, "  Confirmed ");

        assertThat(report.getStatus()).isEqualTo(JobReportStatus.RESOLVED);
        assertThat(outcome.resolutionNote()).isEqualTo("Confirmed");
        assertThat(outcome.jobId()).isEqualTo(20L);
        assertThat(outcome.jobHidden()).isFalse();
    }

    @Test
    void dismissShouldCloseTheReport() {
        JobReport report = JobReport.create(20L, 1L, JobReportReason.SCAM, null, "Java Dev");
        when(repository.findById(7L)).thenReturn(Optional.of(report));

        service.dismiss(7L, 99L, "Genuine job");

        assertThat(report.getStatus()).isEqualTo(JobReportStatus.DISMISSED);
    }

    @Test
    void unknownReportShouldBeNotFound() {
        when(repository.findById(7L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.resolve(7L, 99L, "x")).isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void listShouldAttachReporterEmailAndTheNumberOfOpenReportsPerJob() {
        JobReport report = JobReport.create(20L, 1L, JobReportReason.SCAM, null, "Java Dev");
        when(repository.findByStatus(eq(JobReportStatus.OPEN), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(report)));
        when(userService.getEmails(Set.of(1L))).thenReturn(Map.of(1L, "asha@test.com"));
        when(repository.countByJobIdsAndStatus(Set.of(20L), JobReportStatus.OPEN))
                .thenReturn(List.of(new JobReportCount(20L, 3L)));

        PageResponse<AdminJobReportResponse> page = service.list(JobReportStatus.OPEN, 0, 20);

        assertThat(page.content()).hasSize(1);
        assertThat(page.content().get(0).reporterEmail()).isEqualTo("asha@test.com");
        assertThat(page.content().get(0).openReportsForJob()).isEqualTo(3L);
    }

    @Test
    void resolveShouldAnnounceTheOutcomeToTheReporterWithoutTheAdminNote() {
        JobReport report = JobReport.create(20L, 1L, JobReportReason.SCAM, null, "Java Dev");
        when(repository.findById(7L)).thenReturn(Optional.of(report));

        service.resolve(7L, 99L, "Internal: recruiter has 3 other fake posts");

        verify(eventPublisher).publishEvent(
                new JobReportClosedEvent(1L, 20L, "Java Dev", JobReportStatus.RESOLVED));
    }

    @Test
    void dismissShouldAnnounceDismissed() {
        JobReport report = JobReport.create(20L, 1L, JobReportReason.SCAM, null, "Java Dev");
        when(repository.findById(7L)).thenReturn(Optional.of(report));

        service.dismiss(7L, 99L, "Genuine job");

        verify(eventPublisher).publishEvent(
                new JobReportClosedEvent(1L, 20L, "Java Dev", JobReportStatus.DISMISSED));
    }

    @Test
    void anAlreadyClosedReportShouldNotAnnounceAnything() {
        JobReport report = JobReport.create(20L, 1L, JobReportReason.SCAM, null, "Java Dev");
        report.resolve(99L, "x");
        when(repository.findById(7L)).thenReturn(Optional.of(report));

        assertThatThrownBy(() -> service.dismiss(7L, 99L, "y")).isInstanceOf(AppException.class);

        verify(eventPublisher, never()).publishEvent(any(Object.class));
    }

    @Test
    void listMineShouldReturnOnlyTheCallersReportsWithoutAdminNotes() {
        JobReport report = JobReport.create(20L, 1L, JobReportReason.SCAM, "details", "Java Dev");
        report.resolve(99L, "secret admin note");
        when(repository.findByReporterUserId(eq(1L), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(report)));

        PageResponse<MyJobReportResponse> page = service.listMine(1L, 0, 20);

        assertThat(page.content()).hasSize(1);
        assertThat(page.content().get(0).status()).isEqualTo(JobReportStatus.RESOLVED);
        assertThat(page.content().get(0).toString()).doesNotContain("secret admin note");
    }
}