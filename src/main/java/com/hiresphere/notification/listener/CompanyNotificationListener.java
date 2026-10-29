package com.hiresphere.notification.listener;

import com.hiresphere.notification.service.NotificationService;
import com.hiresphere.recruiter.event.CompanyStatusChangedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
@RequiredArgsConstructor
public class CompanyNotificationListener {

    private final NotificationService notificationService;

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onCompanyStatusChanged(CompanyStatusChangedEvent event) {
        try {
            notificationService.onCompanyStatusChanged(event);
        } catch (Exception e) {
            log.error("Company notification failed for company {}", event.companyId(), e);
        }
    }
}