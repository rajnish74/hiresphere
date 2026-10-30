package com.hiresphere.user.event;

public record UserBlockedEvent(Long userId, String reason) {
}