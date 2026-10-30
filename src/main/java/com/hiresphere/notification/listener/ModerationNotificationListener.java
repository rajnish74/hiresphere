package com.hiresphere.notification.listener;

import com.hiresphere.job.event.JobHiddenEvent;
import com.hiresphere.notification.service.NotificationService;
import com.hiresphere.user.event.UserBlockedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
@RequiredArgsConstructor
public class ModerationNotificationListener {

    private final NotificationService notificationService;

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onUserBlocked(UserBlockedEvent event) {
        try {
            notificationService.onUserBlocked(event);
        } catch (Exception e) {
            log.error("Block notification failed for user {}", event.userId(), e);
        }
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onJobHidden(JobHiddenEvent event) {
        try {
            notificationService.onJobHidden(event);
        } catch (Exception e) {
            log.error("Job hidden notification failed for job {}", event.jobId(), e);
        }
    }
}