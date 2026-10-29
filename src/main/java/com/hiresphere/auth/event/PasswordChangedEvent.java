package com.hiresphere.auth.event;

public record PasswordChangedEvent(String fullName, String email) {
}