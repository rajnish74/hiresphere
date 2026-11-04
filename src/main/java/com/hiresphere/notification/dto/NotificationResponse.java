package com.hiresphere.notification.dto;

import com.hiresphere.notification.entity.InAppNotification;
import com.hiresphere.notification.entity.NotificationTarget;
import com.hiresphere.notification.entity.NotificationType;

import java.time.Instant;

public record NotificationResponse(Long id, NotificationType type, String subject, String detail, String note,
                                   NotificationTarget targetType, Long targetId, boolean read, Instant createdAt) {

    public static NotificationResponse from(InAppNotification n) {
        return new NotificationResponse(n.getId(), n.getType(), n.getSubject(), n.getDetail(), n.getNote(),
                n.getTargetType(), n.getTargetId(), n.isRead(), n.getCreatedAt());
    }
}