package com.hiresphere.job.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ResolveReportRequest(@NotBlank @Size(max = 500) String note, boolean hideJob) {
}