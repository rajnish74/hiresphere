package com.hiresphere.candidate.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record ProjectRequest(
        @NotBlank @Size(max = 120)
        String title,
        @Size(max = 2000)
        String description,
        @Size(max = 300)
        String techStack,
        @Size(max = 255) @Pattern(regexp = "^https?://\\S+$", message = "Must be a valid http(s) URL")
        String projectUrl,
        @Size(max = 255) @Pattern(regexp = "^https?://\\S+$", message = "Must be a valid http(s) URL")
        String repoUrl) {
}