package com.hiresphere.auth.dto;

import com.hiresphere.user.entity.Role;
import jakarta.validation.constraints.*;

public record RegisterRequest(
        @NotBlank @Size(max = 100)
        String fullName,
        @NotBlank @Email @Size(max = 150)
        String email,
        @NotBlank @Size(min = 8, max = 72, message = "Password must be 8-72 characters")
        String password,
        @NotNull
        Role role) {
}