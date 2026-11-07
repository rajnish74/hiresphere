package com.hiresphere.notification.stream;

import com.hiresphere.notification.event.InAppNotificationCreatedEvent;
import com.hiresphere.user.event.SessionsInvalidatedEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
public class NotificationStreamListener {

    private final NotificationStreamHub hub;

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onCreated(InAppNotificationCreatedEvent event) {
        hub.nudge(event.userId());
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void onSessionsInvalidated(SessionsInvalidatedEvent event) {
        hub.closeAll(event.userId());
    }
}