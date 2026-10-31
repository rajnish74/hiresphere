package com.hiresphere.config;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;

@Component
@Profile("prod")
@RequiredArgsConstructor
public class ProductionSafetyCheck {

    static final String DEV_SECRET = "dev-only-secret-change-me-in-production-min-32-bytes";

    private final JwtProperties jwtProperties;

    @PostConstruct
    void check() {
        validateJwtSecret(jwtProperties.secret());
    }

    static void validateJwtSecret(String secret) {
        if (secret == null || secret.getBytes(StandardCharsets.UTF_8).length < 32) {
            throw new IllegalStateException(
                    "JWT_SECRET must be at least 32 bytes long in the prod profile. Generate one with: openssl rand -base64 48");
        }
        if (DEV_SECRET.equals(secret)) {
            throw new IllegalStateException(
                    "The development JWT secret is being used in the prod profile. Set a real JWT_SECRET.");
        }
    }
}