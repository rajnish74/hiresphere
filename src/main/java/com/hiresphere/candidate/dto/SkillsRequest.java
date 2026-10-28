package com.hiresphere.candidate.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

public record SkillsRequest(
        @NotNull @Size(max = 30, message = "At most 30 skills allowed")
        List<@NotBlank @Size(max = 60) String> skills) {
}