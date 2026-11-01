package com.hiresphere.application.dto;

import com.hiresphere.application.entity.ApplicationStatus;

public record JobStatusRow(Long jobId, ApplicationStatus status) {
}