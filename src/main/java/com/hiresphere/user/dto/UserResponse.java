package com.hiresphere.user.dto;

import com.hiresphere.user.entity.Role;
import com.hiresphere.user.entity.User;

public record UserResponse(Long id, String fullName, String email, Role role, boolean emailVerified, String phone) {

    public UserResponse(Long id, String fullName, String email, Role role, boolean emailVerified) {
        this(id, fullName, email, role, emailVerified, null);
    }

    public UserResponse(Long id, String fullName, String email, Role role) {
        this(id, fullName, email, role, false, null);
    }

    public static UserResponse from(User u) {
        return new UserResponse(u.getId(), u.getFullName(), u.getEmail(), u.getRole(),
                u.isEmailVerified(), u.getPhone());
    }
}