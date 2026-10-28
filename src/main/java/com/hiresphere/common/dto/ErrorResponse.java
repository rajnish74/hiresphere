package com.hiresphere.common.dto;

import java.time.Instant;
import java.util.Map;

public record ErrorResponse(
        int status,
        String error,
        String message,
        String path,
        Map<String, String> fieldErrors,
        Instant timestamp) {
}