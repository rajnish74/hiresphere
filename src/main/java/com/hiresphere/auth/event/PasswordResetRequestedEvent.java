package com.hiresphere.auth.event;

public record PasswordResetRequestedEvent(String fullName, String email, String token, long validMinutes) {

    @Override
    public String toString() {
        return "PasswordResetRequestedEvent[token hidden]";
    }
}