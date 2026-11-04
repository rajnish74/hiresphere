package com.hiresphere.config;

import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.ArrayList;

@Configuration
public class OpenApiTypesConfig {


    @Bean
    public OpenApiCustomizer responsesAreFullyPopulated() {
        return openApi -> {
            if (openApi.getComponents() == null || openApi.getComponents().getSchemas() == null) {
                return;
            }
            openApi.getComponents().getSchemas().forEach((name, schema) -> {
                if (name.endsWith("Request") || schema.getProperties() == null || schema.getProperties().isEmpty()) {
                    return;
                }
                schema.setRequired(new ArrayList<>(schema.getProperties().keySet()));
            });
        };
    }
}