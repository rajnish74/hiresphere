package com.hiresphere.job.dto;

import com.hiresphere.job.entity.JobType;
import com.hiresphere.job.entity.WorkMode;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record JobRequest(
        @NotBlank @Size(max = 150)
        String title,
        @NotBlank @Size(min = 20, max = 10000)
        String description,
        @Size(max = 100)
        String location,
        @NotNull
        JobType jobType,
        @NotNull
        WorkMode workMode,
        @NotNull @Min(0) @Max(50)
        Integer minExperienceYears,
        @Min(0) @Max(50)
        Integer maxExperienceYears,
        @DecimalMin("0.0")
        BigDecimal salaryMin,
        @DecimalMin("0.0")
        BigDecimal salaryMax,
        @NotNull @Min(1) @Max(1000)
        Integer openings,
        @Future
        Instant expiresAt,
        @Size(max = 20, message = "At most 20 skills allowed")
        List<@NotBlank @Size(max = 60) String> skills) {
}