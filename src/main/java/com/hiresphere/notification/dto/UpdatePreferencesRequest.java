package com.hiresphere.notification.dto;

import com.hiresphere.notification.entity.NotificationCategory;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.Map;

public record UpdatePreferencesRequest(@NotEmpty Map<NotificationCategory, @NotNull Boolean> categories) {
}