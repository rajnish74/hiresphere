package com.hiresphere.notification.service;

import com.hiresphere.notification.entity.NotificationCategory;

import java.util.Map;

public final class NotificationTemplates {

    private static final Map<String, NotificationCategory> CATEGORIES = Map.ofEntries(
            Map.entry("application-submitted-candidate", NotificationCategory.APPLICATION_UPDATES),
            Map.entry("application-status-candidate", NotificationCategory.APPLICATION_UPDATES),
            Map.entry("application-received-recruiter", NotificationCategory.NEW_APPLICANTS),
            Map.entry("application-withdrawn-recruiter", NotificationCategory.NEW_APPLICANTS),
            Map.entry("company-status-recruiter", NotificationCategory.ACCOUNT),
            Map.entry("job-hidden-recruiter", NotificationCategory.ACCOUNT),
            Map.entry("account-blocked", NotificationCategory.ACCOUNT),
            Map.entry("email-verification", NotificationCategory.SECURITY),
            Map.entry("password-reset", NotificationCategory.SECURITY),
            Map.entry("password-changed", NotificationCategory.SECURITY));

    private NotificationTemplates() {
    }

    public static NotificationCategory categoryOf(String template) {
        return CATEGORIES.getOrDefault(template, NotificationCategory.ACCOUNT);
    }

    public static boolean isMapped(String template) {
        return CATEGORIES.containsKey(template);
    }
}