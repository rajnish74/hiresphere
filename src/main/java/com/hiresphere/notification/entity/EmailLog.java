package com.hiresphere.notification.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Entity
@Table(name = "email_log")
@Getter
@NoArgsConstructor
public class EmailLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 150)
    private String recipient;

    @Column(nullable = false, length = 200)
    private String subject;

    @Column(nullable = false, length = 60)
    private String template;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private EmailStatus status;

    @Column(nullable = false)
    private int attempts;

    @Column(length = 500)
    private String error;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    public static EmailLog of(String recipient, String subject, String template,
                              EmailStatus status, int attempts, String error) {
        EmailLog log = new EmailLog();
        log.recipient = truncate(recipient, 150);
        log.subject = truncate(subject, 200);
        log.template = truncate(template, 60);
        log.status = status;
        log.attempts = attempts;
        log.error = error == null ? null : truncate(error, 500);
        log.createdAt = Instant.now();
        return log;
    }

    private static String truncate(String value, int max) {
        return value.length() <= max ? value : value.substring(0, max);
    }
}