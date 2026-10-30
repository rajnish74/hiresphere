package com.hiresphere.user.dto;

import com.hiresphere.user.entity.Role;
import com.hiresphere.user.entity.User;

import java.time.Instant;

public record AdminUserResponse(Long id, String fullName, String email, Role role,
                                boolean enabled, boolean emailVerified, Instant createdAt,
                                Instant lastLoginAt) {

    public AdminUserResponse(Long id, String fullName, String email, Role role,
                             boolean enabled, boolean emailVerified, Instant createdAt) {
        this(id, fullName, email, role, enabled, emailVerified, createdAt, null);
    }

    public static AdminUserResponse from(User u) {
        return new AdminUserResponse(u.getId(), u.getFullName(), u.getEmail(), u.getRole(),
                u.isEnabled(), u.isEmailVerified(), u.getCreatedAt(), u.getLastLoginAt());
    }
}