package com.hiresphere.auth.event;

public record EmailVerificationRequestedEvent(String fullName, String email, String token, long validHours) {

    @Override
    public String toString() {
        return "EmailVerificationRequestedEvent[token hidden]";
    }
}