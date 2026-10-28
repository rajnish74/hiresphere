package com.hiresphere.auth.dto;

import com.hiresphere.user.dto.UserResponse;

public record AuthResponse(
        String accessToken,
        String tokenType,
        long expiresInSeconds,
        UserResponse user) {
}