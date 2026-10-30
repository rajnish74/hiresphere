package com.hiresphere.admin;

import com.hiresphere.admin.entity.AuditAction;
import com.hiresphere.admin.entity.AuditTargetType;
import com.hiresphere.admin.service.AdminActionService;
import com.hiresphere.admin.service.AuditService;
import com.hiresphere.auth.service.RefreshTokenService;
import com.hiresphere.common.exception.BadRequestException;
import com.hiresphere.job.dto.JobReportOutcome;
import com.hiresphere.job.entity.JobReportStatus;
import com.hiresphere.job.service.JobModerationService;
import com.hiresphere.job.service.JobReportService;
import com.hiresphere.recruiter.service.CompanyService;
import com.hiresphere.user.dto.AdminUserResponse;
import com.hiresphere.user.entity.Role;
import com.hiresphere.user.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminActionServiceTest {

    @Mock
    private CompanyService companyService;
    @Mock
    private UserService userService;
    @Mock
    private RefreshTokenService refreshTokenService;
    @Mock
    private JobModerationService jobModerationService;
    @Mock
    private AuditService auditService;
    @Mock
    private JobReportService jobReportService;

    private AdminActionService service;

    @BeforeEach
    void setUp() {
        service = new AdminActionService(companyService, userService, refreshTokenService,
                jobModerationService, auditService, jobReportService);
    }

    private JobReportOutcome outcome(JobReportStatus status, String note) {
        return new JobReportOutcome(7L, 20L, status, note, false);
    }

    @Test
    void blockingAUserShouldRevokeRefreshTokensAndRecordAudit() {
        when(userService.block(1L, 5L, "Spam applications"))
                .thenReturn(new AdminUserResponse(5L, "V", "v@test.com", Role.CANDIDATE, false, true, null));

        service.blockUser(1L, 5L, "Spam applications");

        verify(refreshTokenService).revokeAllForUser(5L);
        verify(auditService).record(1L, AuditAction.USER_BLOCKED, AuditTargetType.USER, 5L, "Spam applications");
    }

    @Test
    void aRefusedBlockShouldRevokeAndRecordNothing() {
        when(userService.block(1L, 1L, "x")).thenThrow(new BadRequestException("You cannot block your own account"));

        assertThatThrownBy(() -> service.blockUser(1L, 1L, "x")).isInstanceOf(BadRequestException.class);

        verifyNoInteractions(refreshTokenService, auditService);
    }

    @Test
    void unblockingShouldRecordAudit() {
        service.unblockUser(1L, 5L);

        verify(userService).unblock(5L);
        verify(auditService).record(1L, AuditAction.USER_UNBLOCKED, AuditTargetType.USER, 5L, null);
    }

    @Test
    void approvingACompanyShouldRecordAudit() {
        service.approveCompany(1L, 9L);

        verify(companyService).approve(9L, 1L);
        verify(auditService).record(1L, AuditAction.COMPANY_APPROVED, AuditTargetType.COMPANY, 9L, null);
    }

    @Test
    void rejectingACompanyShouldStoreTheReasonInTheAudit() {
        service.rejectCompany(1L, 9L, "Fake website");

        verify(companyService).reject(9L, 1L, "Fake website");
        verify(auditService).record(1L, AuditAction.COMPANY_REJECTED, AuditTargetType.COMPANY, 9L, "Fake website");
    }

    @Test
    void hidingAJobShouldRecordTheReason() {
        service.hideJob(1L, 20L, "Misleading salary");

        verify(jobModerationService).hide(20L, "Misleading salary");
        verify(auditService).record(1L, AuditAction.JOB_HIDDEN, AuditTargetType.JOB, 20L, "Misleading salary");
    }

    // ---------- job reports ----------

    @Test
    void resolvingWithHideJobShouldHideTheJobAndAuditBothActions() {
        when(jobReportService.resolve(7L, 1L, "Confirmed")).thenReturn(outcome(JobReportStatus.RESOLVED, "Confirmed"));
        when(jobModerationService.hideIfNotHidden(20L, "Confirmed")).thenReturn(true);

        JobReportOutcome result = service.resolveJobReport(1L, 7L, "Confirmed", true);

        assertThat(result.jobHidden()).isTrue();
        verify(auditService).record(1L, AuditAction.JOB_HIDDEN, AuditTargetType.JOB, 20L,
                "Via report #7: Confirmed");
        verify(auditService).record(1L, AuditAction.JOB_REPORT_RESOLVED, AuditTargetType.JOB_REPORT, 7L, "Confirmed");
    }

    @Test
    void resolvingWhenTheJobIsAlreadyHiddenShouldNotAuditASecondHide() {
        when(jobReportService.resolve(7L, 1L, "Confirmed")).thenReturn(outcome(JobReportStatus.RESOLVED, "Confirmed"));
        when(jobModerationService.hideIfNotHidden(20L, "Confirmed")).thenReturn(false);

        JobReportOutcome result = service.resolveJobReport(1L, 7L, "Confirmed", true);

        assertThat(result.jobHidden()).isFalse();
        verify(auditService, never()).record(eq(1L), eq(AuditAction.JOB_HIDDEN), any(), any(), any());
        verify(auditService).record(1L, AuditAction.JOB_REPORT_RESOLVED, AuditTargetType.JOB_REPORT, 7L, "Confirmed");
    }

    @Test
    void resolvingWithoutHideJobShouldNotTouchTheJob() {
        when(jobReportService.resolve(7L, 1L, "Checked")).thenReturn(outcome(JobReportStatus.RESOLVED, "Checked"));

        service.resolveJobReport(1L, 7L, "Checked", false);

        verifyNoInteractions(jobModerationService);
    }

    @Test
    void dismissingShouldAuditTheDismissal() {
        when(jobReportService.dismiss(7L, 1L, "Looks fine")).thenReturn(outcome(JobReportStatus.DISMISSED, "Looks fine"));

        service.dismissJobReport(1L, 7L, "Looks fine");

        verify(auditService).record(1L, AuditAction.JOB_REPORT_DISMISSED, AuditTargetType.JOB_REPORT, 7L, "Looks fine");
        verifyNoInteractions(jobModerationService);
    }
}