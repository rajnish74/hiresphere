package com.hiresphere.notification.listener;

import com.hiresphere.job.event.JobReportClosedEvent;
import com.hiresphere.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
@RequiredArgsConstructor
public class ReportNotificationListener {

    private final NotificationService notificationService;

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onReportClosed(JobReportClosedEvent event) {
        try {
            notificationService.onJobReportClosed(event);
        } catch (Exception e) {
            log.error("Report notification failed for job {}", event.jobId(), e);
        }
    }
}