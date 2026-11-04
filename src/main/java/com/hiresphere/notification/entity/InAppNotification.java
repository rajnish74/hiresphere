package com.hiresphere.notification.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;

/** Sirf factory se banti hai. read_at sirf bulk UPDATE se badalta hai, isliye koi setter nahi. */
@Entity
@Table(name = "notifications")
@Getter
@NoArgsConstructor
public class InAppNotification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, updatable = false)
    private Long userId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40, updatable = false)
    private NotificationType type;

    @Column(nullable = false, length = 200, updatable = false)
    private String subject;

    @Column(length = 200, updatable = false)
    private String detail;

    @Column(length = 500, updatable = false)
    private String note;

    @Enumerated(EnumType.STRING)
    @Column(length = 20, updatable = false)
    private NotificationTarget targetType;

    @Column(updatable = false)
    private Long targetId;

    private Instant readAt;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    public static InAppNotification create(Long userId, NotificationType type, String subject, String detail,
                                           String note, NotificationTarget targetType, Long targetId) {
        if ((targetType == null) != (targetId == null)) {
            throw new IllegalArgumentException("targetType and targetId must be given together");
        }
        InAppNotification n = new InAppNotification();
        n.userId = userId;
        n.type = type;
        n.subject = clip(subject == null ? "" : subject, 200);
        n.detail = clip(detail, 200);
        n.note = clip(note, 500);
        n.targetType = targetType;
        n.targetId = targetId;
        n.createdAt = Instant.now();
        return n;
    }

    public boolean isRead() {
        return readAt != null;
    }

    private static String clip(String value, int max) {
        if (value == null) {
            return null;
        }
        return value.length() <= max ? value : value.substring(0, max);
    }
}