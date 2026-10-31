package com.hiresphere.config;

import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiGroupsConfig {

    @Bean
    public GroupedOpenApi publicApi() {
        return GroupedOpenApi.builder().group("1-public")
                .pathsToMatch("/api/v1/auth/**", "/api/v1/jobs/**", "/api/v1/public/**")
                .build();
    }

    @Bean
    public GroupedOpenApi candidateApi() {
        return GroupedOpenApi.builder().group("2-candidate")
                .pathsToMatch("/api/v1/candidates/**", "/api/v1/users/**")
                .build();
    }

    @Bean
    public GroupedOpenApi recruiterApi() {
        return GroupedOpenApi.builder().group("3-recruiter")
                .pathsToMatch("/api/v1/recruiter/**")
                .build();
    }

    @Bean
    public GroupedOpenApi adminApi() {
        return GroupedOpenApi.builder().group("4-admin")
                .pathsToMatch("/api/v1/admin/**")
                .build();
    }
}