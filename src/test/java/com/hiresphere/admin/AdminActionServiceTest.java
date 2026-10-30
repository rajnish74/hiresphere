package com.hiresphere.admin;

import com.hiresphere.admin.entity.AuditAction;
import com.hiresphere.admin.entity.AuditTargetType;
import com.hiresphere.admin.service.AdminActionService;
import com.hiresphere.admin.service.AuditService;
import com.hiresphere.auth.service.RefreshTokenService;
import com.hiresphere.common.exception.BadRequestException;
import com.hiresphere.job.service.JobModerationService;
import com.hiresphere.recruiter.service.CompanyService;
import com.hiresphere.user.dto.AdminUserResponse;
import com.hiresphere.user.entity.Role;
import com.hiresphere.user.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
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

    private AdminActionService service;

    @BeforeEach
    void setUp() {
        service = new AdminActionService(companyService, userService, refreshTokenService,
                jobModerationService, auditService);
    }

    @Test
    void blockingAUserShouldRevokeRefreshTokensAndRecordAudit() {
        when(userService.block(1L, 5L))
                .thenReturn(new AdminUserResponse(5L, "V", "v@test.com", Role.CANDIDATE, false, true, null));

        service.blockUser(1L, 5L, "Spam applications");

        verify(refreshTokenService).revokeAllForUser(5L);
        verify(auditService).record(1L, AuditAction.USER_BLOCKED, AuditTargetType.USER, 5L, "Spam applications");
    }

    @Test
    void aRefusedBlockShouldRevokeAndRecordNothing() {
        when(userService.block(1L, 1L)).thenThrow(new BadRequestException("You cannot block your own account"));

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
}