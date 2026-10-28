package com.hiresphere.candidate.dto;

import jakarta.validation.constraints.*;

public record EducationRequest(
        @NotBlank @Size(max = 150)
        String institution,
        @NotBlank @Size(max = 100)
        String degree,
        @Size(max = 100)
        String fieldOfStudy,
        @NotNull @Min(1950) @Max(2100)
        Integer startYear,
        @Min(1950) @Max(2100)
        Integer endYear,
        @Size(max = 30)
        String grade) {
}