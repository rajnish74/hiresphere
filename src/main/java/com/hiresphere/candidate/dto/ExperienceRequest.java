package com.hiresphere.candidate.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record ExperienceRequest(
        @NotBlank @Size(max = 150)
        String company,
        @NotBlank @Size(max = 100)
        String title,
        @Size(max = 100)
        String location,
        @NotNull
        LocalDate startDate,
        LocalDate endDate,
        @Size(max = 2000)
        String description) {
}