package com.hiresphere.admin.dto;

import com.hiresphere.admin.entity.AuditAction;
import com.hiresphere.admin.entity.AuditLog;
import com.hiresphere.admin.entity.AuditTargetType;

import java.time.Instant;

public record AuditLogResponse(Long id, Long actorId, String actorEmail, AuditAction action,
                               AuditTargetType targetType, Long targetId, String details, Instant createdAt) {

    public static AuditLogResponse from(AuditLog a, String actorEmail) {
        return new AuditLogResponse(a.getId(), a.getActorId(), actorEmail, a.getAction(), a.getTargetType(),
                a.getTargetId(), a.getDetails(), a.getCreatedAt());
    }
}