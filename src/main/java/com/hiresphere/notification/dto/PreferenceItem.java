package com.hiresphere.notification.dto;

import com.hiresphere.notification.entity.NotificationCategory;

public record PreferenceItem(NotificationCategory category, boolean optional, boolean enabled) {
}