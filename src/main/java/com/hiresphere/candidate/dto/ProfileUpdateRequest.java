package com.hiresphere.candidate.dto;

import com.hiresphere.candidate.entity.ProfileVisibility;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record ProfileUpdateRequest(
        @Size(max = 150)
        String headline,
        @Size(max = 3000)
        String summary,
        @Size(max = 100)
        String location,
        @Size(max = 255)
        @Pattern(regexp = "^https?://\\S+$", message = "Must be a valid http(s) URL")
        String linkedinUrl,
        @Size(max = 255) @Pattern(regexp = "^https?://\\S+$", message = "Must be a valid http(s) URL")
        String githubUrl,
        @Size(max = 255) @Pattern(regexp = "^https?://\\S+$", message = "Must be a valid http(s) URL")
        String portfolioUrl,
        @NotNull
        ProfileVisibility visibility,
        @NotNull
        Boolean openToWork) {
}