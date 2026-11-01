package com.hiresphere.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record UpdateMeRequest(
        @NotBlank @Size(min = 2, max = 100) String fullName,
        @Pattern(regexp = "^\\+?[0-9]{8,14}$", message = "Phone must be 8-14 digits, optionally starting with +")
        String phone) {
}