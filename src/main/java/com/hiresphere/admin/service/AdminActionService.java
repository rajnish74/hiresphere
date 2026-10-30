package com.hiresphere.admin.service;

import com.hiresphere.admin.entity.AuditAction;
import com.hiresphere.admin.entity.AuditTargetType;
import com.hiresphere.auth.service.RefreshTokenService;
import com.hiresphere.job.dto.AdminJobSummary;
import com.hiresphere.job.dto.JobReportOutcome;
import com.hiresphere.job.service.JobModerationService;
import com.hiresphere.job.service.JobReportService;
import com.hiresphere.recruiter.dto.CompanyResponse;
import com.hiresphere.recruiter.service.CompanyService;
import com.hiresphere.user.dto.AdminUserResponse;
import com.hiresphere.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AdminActionService {

    private final CompanyService companyService;
    private final UserService userService;
    private final RefreshTokenService refreshTokenService;
    private final JobModerationService jobModerationService;
    private final AuditService auditService;
    private final JobReportService jobReportService;

    // ---------- companies ----------

    @Transactional
    public CompanyResponse approveCompany(Long adminId, Long companyId) {
        CompanyResponse result = companyService.approve(companyId, adminId);
        auditService.record(adminId, AuditAction.COMPANY_APPROVED, AuditTargetType.COMPANY, companyId, null);
        return result;
    }

    @Transactional
    public CompanyResponse rejectCompany(Long adminId, Long companyId, String reason) {
        CompanyResponse result = companyService.reject(companyId, adminId, reason);
        auditService.record(adminId, AuditAction.COMPANY_REJECTED, AuditTargetType.COMPANY, companyId, reason);
        return result;
    }

    @Transactional
    public CompanyResponse suspendCompany(Long adminId, Long companyId, String reason) {
        CompanyResponse result = companyService.suspend(companyId, adminId, reason);
        auditService.record(adminId, AuditAction.COMPANY_SUSPENDED, AuditTargetType.COMPANY, companyId, reason);
        return result;
    }

    // ---------- users ----------

    @Transactional
    public AdminUserResponse blockUser(Long adminId, Long userId, String reason) {
        AdminUserResponse result = userService.block(adminId, userId, reason);
        refreshTokenService.revokeAllForUser(userId);
        auditService.record(adminId, AuditAction.USER_BLOCKED, AuditTargetType.USER, userId, reason);
        return result;
    }

    @Transactional
    public AdminUserResponse unblockUser(Long adminId, Long userId) {
        AdminUserResponse result = userService.unblock(userId);
        auditService.record(adminId, AuditAction.USER_UNBLOCKED, AuditTargetType.USER, userId, null);
        return result;
    }

    // ---------- jobs ----------

    @Transactional
    public AdminJobSummary hideJob(Long adminId, Long jobId, String reason) {
        AdminJobSummary result = jobModerationService.hide(jobId, reason);
        auditService.record(adminId, AuditAction.JOB_HIDDEN, AuditTargetType.JOB, jobId, reason);
        return result;
    }

    @Transactional
    public AdminJobSummary unhideJob(Long adminId, Long jobId) {
        AdminJobSummary result = jobModerationService.unhide(jobId);
        auditService.record(adminId, AuditAction.JOB_UNHIDDEN, AuditTargetType.JOB, jobId, null);
        return result;
    }

    // ---------- job reports ----------

    @Transactional
    public JobReportOutcome resolveJobReport(Long adminId, Long reportId, String note, boolean hideJob) {
        JobReportOutcome outcome = jobReportService.resolve(reportId, adminId, note);
        String cleanNote = outcome.resolutionNote();

        boolean hidden = false;
        if (hideJob) {
            hidden = jobModerationService.hideIfNotHidden(outcome.jobId(), cleanNote);
            if (hidden) {
                auditService.record(adminId, AuditAction.JOB_HIDDEN, AuditTargetType.JOB, outcome.jobId(),
                        "Via report #" + reportId + ": " + cleanNote);
            }
        }
        auditService.record(adminId, AuditAction.JOB_REPORT_RESOLVED, AuditTargetType.JOB_REPORT, reportId, cleanNote);
        return outcome.withJobHidden(hidden);
    }

    @Transactional
    public JobReportOutcome dismissJobReport(Long adminId, Long reportId, String note) {
        JobReportOutcome outcome = jobReportService.dismiss(reportId, adminId, note);
        auditService.record(adminId, AuditAction.JOB_REPORT_DISMISSED, AuditTargetType.JOB_REPORT, reportId,
                outcome.resolutionNote());
        return outcome;
    }
}