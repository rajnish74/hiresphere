package com.hiresphere.notification.entity;

public enum NotificationType {
    APPLICATION_SUBMITTED(NotificationCategory.NEW_APPLICANTS),
    APPLICATION_WITHDRAWN(NotificationCategory.NEW_APPLICANTS),
    APPLICATION_STATUS_CHANGED(NotificationCategory.APPLICATION_UPDATES),
    COMPANY_STATUS_CHANGED(NotificationCategory.ACCOUNT),
    JOB_HIDDEN(NotificationCategory.ACCOUNT),
    JOB_REPORT_CLOSED(NotificationCategory.REPORT_UPDATES);

    private final NotificationCategory category;

    NotificationType(NotificationCategory category) {
        this.category = category;
    }

    public NotificationCategory category() {
        return category;
    }
}