package com.hiresphere.notification.listener;

import com.hiresphere.application.event.ApplicationStatusChangedEvent;
import com.hiresphere.application.event.ApplicationSubmittedEvent;
import com.hiresphere.job.event.JobHiddenEvent;
import com.hiresphere.job.event.JobReportClosedEvent;
import com.hiresphere.notification.entity.NotificationTarget;
import com.hiresphere.notification.entity.NotificationType;
import com.hiresphere.notification.service.InAppNotificationService;
import com.hiresphere.recruiter.event.CompanyStatusChangedEvent;
import com.hiresphere.recruiter.service.CompanyService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class InAppNotificationListener {

    private final InAppNotificationService notifications;
    private final CompanyService companyService;

    @EventListener
    public void onApplicationSubmitted(ApplicationSubmittedEvent e) {
        notifyOwner(e.companyId(), NotificationType.APPLICATION_SUBMITTED, e.jobTitle(), e.candidateName(), null,
                NotificationTarget.APPLICATION, e.applicationId());
    }

    @EventListener
    public void onStatusChanged(ApplicationStatusChangedEvent e) {
        if (e.byCandidate()) {
            notifyOwner(e.companyId(), NotificationType.APPLICATION_WITHDRAWN, e.jobTitle(), e.candidateName(), null,
                    NotificationTarget.APPLICATION, e.applicationId());
            return;
        }
        notifications.create(e.candidateUserId(), NotificationType.APPLICATION_STATUS_CHANGED, e.jobTitle(),
                e.to().name(), e.note(), NotificationTarget.APPLICATION, e.applicationId());
    }

    @EventListener
    public void onCompanyStatusChanged(CompanyStatusChangedEvent e) {
        notifications.create(e.ownerId(), NotificationType.COMPANY_STATUS_CHANGED, e.companyName(),
                e.status().name(), e.reason(), NotificationTarget.COMPANY, e.companyId());
    }

    @EventListener
    public void onJobHidden(JobHiddenEvent e) {
        notifyOwner(e.companyId(), NotificationType.JOB_HIDDEN, e.jobTitle(), null, e.reason(),
                NotificationTarget.JOB, e.jobId());
    }

    @EventListener
    public void onReportClosed(JobReportClosedEvent e) {
        notifications.create(e.reporterUserId(), NotificationType.JOB_REPORT_CLOSED, e.jobTitle(),
                e.outcome().name(), null, NotificationTarget.JOB, e.jobId());
    }

    private void notifyOwner(Long companyId, NotificationType type, String subject, String detail, String note,
                             NotificationTarget target, Long targetId) {
        companyService.getOwnerId(companyId)
                .ifPresent(owner -> notifications.create(owner, type, subject, detail, note, target, targetId));
    }
}