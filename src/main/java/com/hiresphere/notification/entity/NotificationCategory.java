package com.hiresphere.notification.entity;

public enum NotificationCategory {
    APPLICATION_UPDATES(true),
    NEW_APPLICANTS(true),
    JOB_ALERTS(true),
    PRODUCT_NEWS(true),          // future
    SECURITY(false),
    ACCOUNT(false);

    private final boolean optional;

    NotificationCategory(boolean optional) {
        this.optional = optional;
    }

    public boolean isOptional() {
        return optional;
    }
}