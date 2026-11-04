package com.hiresphere.notification;

import com.hiresphere.application.entity.ApplicationStatus;
import com.hiresphere.application.event.ApplicationStatusChangedEvent;
import com.hiresphere.application.event.ApplicationSubmittedEvent;
import com.hiresphere.job.entity.JobReportStatus;
import com.hiresphere.job.event.JobHiddenEvent;
import com.hiresphere.job.event.JobReportClosedEvent;
import com.hiresphere.notification.entity.NotificationTarget;
import com.hiresphere.notification.entity.NotificationType;
import com.hiresphere.notification.listener.InAppNotificationListener;
import com.hiresphere.notification.service.InAppNotificationService;
import com.hiresphere.recruiter.entity.CompanyStatus;
import com.hiresphere.recruiter.event.CompanyStatusChangedEvent;
import com.hiresphere.recruiter.service.CompanyService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InAppNotificationListenerTest {

    @Mock
    private InAppNotificationService notifications;
    @Mock
    private CompanyService companyService;

    private InAppNotificationListener listener;

    @BeforeEach
    void setUp() {
        listener = new InAppNotificationListener(notifications, companyService);
    }

    private ApplicationStatusChangedEvent statusEvent(ApplicationStatus to, String note, boolean byCandidate) {
        return new ApplicationStatusChangedEvent(9L, 5L, 1L, "Asha", "Java Dev", "Acme",
                ApplicationStatus.APPLIED, to, note, byCandidate);
    }

    @Test
    void aNewApplicationShouldNotifyTheCompanyOwner() {
        when(companyService.getOwnerId(5L)).thenReturn(Optional.of(2L));

        listener.onApplicationSubmitted(new ApplicationSubmittedEvent(9L, 5L, 1L, "Asha", "Java Dev", "Acme"));

        verify(notifications).create(2L, NotificationType.APPLICATION_SUBMITTED, "Java Dev", "Asha", null,
                NotificationTarget.APPLICATION, 9L);
    }

    @Test
    void aRecruiterStatusChangeShouldNotifyTheCandidateWithTheStatusAndNote() {
        listener.onStatusChanged(statusEvent(ApplicationStatus.SHORTLISTED, "Strong profile", false));

        verify(notifications).create(1L, NotificationType.APPLICATION_STATUS_CHANGED, "Java Dev", "SHORTLISTED",
                "Strong profile", NotificationTarget.APPLICATION, 9L);
        verifyNoInteractions(companyService);
    }

    @Test
    void aWithdrawalShouldNotifyTheOwnerAndNotTheCandidateWhoDidIt() {
        when(companyService.getOwnerId(5L)).thenReturn(Optional.of(2L));

        listener.onStatusChanged(statusEvent(ApplicationStatus.WITHDRAWN, null, true));

        verify(notifications).create(2L, NotificationType.APPLICATION_WITHDRAWN, "Java Dev", "Asha", null,
                NotificationTarget.APPLICATION, 9L);
        verify(notifications, never()).create(eq(1L), any(), any(), any(), any(), any(), any());
    }

    @Test
    void aCompanyDecisionShouldNotifyTheOwnerWithTheReason() {
        listener.onCompanyStatusChanged(
                new CompanyStatusChangedEvent(5L, 2L, "Acme", CompanyStatus.REJECTED, "Website mismatch"));

        verify(notifications).create(2L, NotificationType.COMPANY_STATUS_CHANGED, "Acme", "REJECTED",
                "Website mismatch", NotificationTarget.COMPANY, 5L);
    }

    @Test
    void aHiddenJobShouldNotifyTheCompanyOwner() {
        when(companyService.getOwnerId(5L)).thenReturn(Optional.of(2L));

        listener.onJobHidden(new JobHiddenEvent(20L, 5L, "Java Dev", "Misleading salary"));

        verify(notifications).create(2L, NotificationType.JOB_HIDDEN, "Java Dev", null, "Misleading salary",
                NotificationTarget.JOB, 20L);
    }

    @Test
    void aClosedReportShouldNotifyTheReporterWithOnlyTheOutcome() {
        listener.onReportClosed(new JobReportClosedEvent(1L, 20L, "Java Dev", JobReportStatus.RESOLVED));

        verify(notifications).create(1L, NotificationType.JOB_REPORT_CLOSED, "Java Dev", "RESOLVED", null,
                NotificationTarget.JOB, 20L);
    }

    @Test
    void aCompanyWithoutAnOwnerShouldNotifyNobody() {
        when(companyService.getOwnerId(5L)).thenReturn(Optional.empty());

        listener.onApplicationSubmitted(new ApplicationSubmittedEvent(9L, 5L, 1L, "Asha", "Java Dev", "Acme"));
        listener.onJobHidden(new JobHiddenEvent(20L, 5L, "Java Dev", "x"));

        verifyNoInteractions(notifications);
    }
}