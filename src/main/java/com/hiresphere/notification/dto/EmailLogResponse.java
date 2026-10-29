package com.hiresphere.notification.dto;

import com.hiresphere.notification.entity.EmailLog;
import com.hiresphere.notification.entity.EmailStatus;

import java.time.Instant;

public record EmailLogResponse(Long id, String recipient, String subject, String template,
                               EmailStatus status, int attempts, String error, Instant createdAt) {

    public static EmailLogResponse from(EmailLog l) {
        return new EmailLogResponse(l.getId(), l.getRecipient(), l.getSubject(), l.getTemplate(),
                l.getStatus(), l.getAttempts(), l.getError(), l.getCreatedAt());
    }
}