package com.hiresphere.auth.service;

import com.hiresphere.common.exception.BadRequestException;

import java.util.Locale;
import java.util.Optional;
import java.util.Set;

public final class PasswordPolicy {

    private static final Set<String> COMMON = Set.of(
            "password", "password1", "passw0rd", "12345678", "123456789", "1234567890", "12341234",
            "qwerty123", "qwertyui", "qwertyuiop", "iloveyou", "admin123", "letmein123",
            "welcome123", "abc12345", "11111111", "00000000", "football1");

    private static final int MIN_DISTINCT_CHARS = 4;
    private static final int MIN_EMAIL_PART_LENGTH = 4;

    private PasswordPolicy() {
    }

    public static Optional<String> violation(String password, String email) {
        if (password == null || password.isBlank()) {
            return Optional.of("Password is required");
        }
        String lower = password.toLowerCase(Locale.ROOT);

        if (COMMON.contains(lower)) {
            return Optional.of("This password is too common. Please choose a less predictable one.");
        }
        if (lower.chars().distinct().count() < MIN_DISTINCT_CHARS) {
            return Optional.of("Password is too repetitive. Use a wider mix of characters.");
        }
        if (email != null && !email.isBlank()) {
            String fullEmail = email.trim().toLowerCase(Locale.ROOT);
            int at = fullEmail.indexOf('@');
            String localPart = at > 0 ? fullEmail.substring(0, at) : fullEmail;

            if (lower.contains(fullEmail)
                    || (localPart.length() >= MIN_EMAIL_PART_LENGTH && lower.contains(localPart))) {
                return Optional.of("Password must not contain your email address or name.");
            }
        }
        return Optional.empty();
    }

    public static void require(String password, String email) {
        violation(password, email).ifPresent(message -> {
            throw new BadRequestException(message);
        });
    }
}