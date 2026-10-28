package com.hiresphere.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.security.login")
public record LoginProperties(
        int maxAttempts,
        long blockMinutes) {
}