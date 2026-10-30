package com.hiresphere.job.dto;

import com.hiresphere.job.entity.JobReportReason;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ReportJobRequest(@NotNull JobReportReason reason, @Size(max = 1000) String details) {
}