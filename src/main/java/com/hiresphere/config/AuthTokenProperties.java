package com.hiresphere.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.auth-tokens")
public record AuthTokenProperties(long resetTtlMinutes, long verificationTtlHours,
                                  int requestLimit, long requestWindowMinutes) {
}