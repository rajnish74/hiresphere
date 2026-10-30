package com.hiresphere.user.dto;

import com.hiresphere.user.entity.Role;
import com.hiresphere.user.entity.User;

import java.time.Instant;

public record AdminUserResponse(Long id,
                                String fullName,
                                String email,
                                Role role,
                                boolean enabled,
                                boolean emailVerified,
                                Instant createdAt) {

    public static AdminUserResponse from(User u) {
        return new AdminUserResponse(u.getId(),
                u.getFullName(),
                u.getEmail(),
                u.getRole(),
                u.isEnabled(),
                u.isEmailVerified(),
                u.getCreatedAt());
    }
}