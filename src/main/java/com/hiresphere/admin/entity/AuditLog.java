package com.hiresphere.admin.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Entity
@Table(name = "audit_logs")
@Getter
@NoArgsConstructor
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, updatable = false)
    private Long actorId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50, updatable = false)
    private AuditAction action;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30, updatable = false)
    private AuditTargetType targetType;

    @Column(nullable = false, updatable = false)
    private Long targetId;

    @Column(length = 1000, updatable = false)
    private String details;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    public static AuditLog of(Long actorId, AuditAction action, AuditTargetType targetType,
                              Long targetId, String details) {
        AuditLog log = new AuditLog();
        log.actorId = actorId;
        log.action = action;
        log.targetType = targetType;
        log.targetId = targetId;
        log.details = (details != null && details.length() > 1000) ? details.substring(0, 1000) : details;
        log.createdAt = Instant.now();
        return log;
    }
}