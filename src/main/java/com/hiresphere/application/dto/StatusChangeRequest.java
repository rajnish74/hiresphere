package com.hiresphere.application.dto;

import com.hiresphere.application.entity.ApplicationStatus;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record StatusChangeRequest(
        @NotNull ApplicationStatus status,
        @Size(max = 500) String note) {
}