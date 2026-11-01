package com.hiresphere.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

@ConfigurationProperties(prefix = "app.cors")
public record CorsProperties(List<String> allowedOrigins) {

    public CorsProperties {
        List<String> origins = (allowedOrigins == null) ? List.of()
                : allowedOrigins.stream().map(String::trim).filter(s -> !s.isEmpty()).toList();
        if (origins.contains("*")) {
            throw new IllegalArgumentException("Wildcard origins are not allowed. List explicit origins.");
        }
        allowedOrigins = origins;
    }
}