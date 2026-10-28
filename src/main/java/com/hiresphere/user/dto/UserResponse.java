package com.hiresphere.user.dto;

import com.hiresphere.user.entity.Role;
import com.hiresphere.user.entity.User;

public record UserResponse(
        Long id,
        String fullName,
        String email,
        Role role) {

    public static UserResponse from(User u) {
        return new UserResponse(u.getId(), u.getFullName(), u.getEmail(), u.getRole());
    }
}