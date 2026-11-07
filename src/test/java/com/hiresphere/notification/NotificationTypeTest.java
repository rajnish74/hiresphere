package com.hiresphere.notification;

import com.hiresphere.notification.entity.NotificationCategory;
import com.hiresphere.notification.entity.NotificationType;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class NotificationTypeTest {

    @Test
    void everyTypeShouldBelongToACategory() {
        for (NotificationType type : NotificationType.values()) {
            assertThat(type.category()).as(type.name()).isNotNull();
        }
    }

    @Test
    void accountDecisionsShouldNeverBeSwitchableOff() {
        assertThat(NotificationType.COMPANY_STATUS_CHANGED.category().isOptional()).isFalse();
        assertThat(NotificationType.JOB_HIDDEN.category().isOptional()).isFalse();
    }

    @Test
    void applicationActivityShouldFollowTheSameCategoriesAsItsEmails() {
        assertThat(NotificationType.APPLICATION_STATUS_CHANGED.category()).isEqualTo(NotificationCategory.APPLICATION_UPDATES);
        assertThat(NotificationType.APPLICATION_SUBMITTED.category()).isEqualTo(NotificationCategory.NEW_APPLICANTS);
        assertThat(NotificationType.APPLICATION_WITHDRAWN.category()).isEqualTo(NotificationCategory.NEW_APPLICANTS);
        assertThat(NotificationType.JOB_REPORT_CLOSED.category()).isEqualTo(NotificationCategory.REPORT_UPDATES);
    }
}