package com.hiresphere.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.mail")
public record EmailProperties(boolean enabled, String from, String frontendUrl,
                              int maxAttempts, long retryDelayMs) {
}