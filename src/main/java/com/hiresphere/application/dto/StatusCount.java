package com.hiresphere.application.dto;

import com.hiresphere.application.entity.ApplicationStatus;

public record StatusCount(ApplicationStatus status, long total) {
}