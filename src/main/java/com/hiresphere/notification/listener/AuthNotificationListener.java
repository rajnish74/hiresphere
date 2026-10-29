package com.hiresphere.notification.listener;

import com.hiresphere.auth.event.EmailVerificationRequestedEvent;
import com.hiresphere.auth.event.PasswordChangedEvent;
import com.hiresphere.auth.event.PasswordResetRequestedEvent;
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
public class AuthNotificationListener {

    private final NotificationService notificationService;

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onVerification(EmailVerificationRequestedEvent event) {
        try {
            notificationService.onEmailVerification(event);
        } catch (Exception e) {
            log.error("Verification email failed", e);
        }
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onResetRequested(PasswordResetRequestedEvent event) {
        try {
            notificationService.onPasswordResetRequested(event);
        } catch (Exception e) {
            log.error("Password reset email failed", e);
        }
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onPasswordChanged(PasswordChangedEvent event) {
        try {
            notificationService.onPasswordChanged(event);
        } catch (Exception e) {
            log.error("Password changed email failed", e);
        }
    }
}