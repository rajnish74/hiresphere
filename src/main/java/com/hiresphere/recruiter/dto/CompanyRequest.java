package com.hiresphere.recruiter.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CompanyRequest(
        @NotBlank @Size(max = 150)
        String name,
        @Size(max = 255) @Pattern(regexp = "^https?://\\S+$", message = "Must be a valid http(s) URL")
        String website,
        @Size(max = 100)
        String industry,
        @Size(max = 3000)
        String description,
        @Size(max = 100)
        String location) {
}