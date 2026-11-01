package com.hiresphere.application.dto;

import com.hiresphere.application.entity.ApplicationStatus;

import java.util.Map;

public record MyApplicationCounts(long total, Map<ApplicationStatus, Long> byStatus) {
}