package com.hiresphere.notification;

import com.hiresphere.notification.entity.InAppNotification;
import com.hiresphere.notification.entity.NotificationTarget;
import com.hiresphere.notification.entity.NotificationType;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class InAppNotificationTest {

    @Test
    void createShouldCarryTheDataAndStartUnread() {
        InAppNotification n = InAppNotification.create(7L, NotificationType.APPLICATION_SUBMITTED, "Java Dev",
                "Asha", null, NotificationTarget.APPLICATION, 9L);

        assertThat(n.getUserId()).isEqualTo(7L);
        assertThat(n.getType()).isEqualTo(NotificationType.APPLICATION_SUBMITTED);
        assertThat(n.getSubject()).isEqualTo("Java Dev");
        assertThat(n.getDetail()).isEqualTo("Asha");
        assertThat(n.getTargetType()).isEqualTo(NotificationTarget.APPLICATION);
        assertThat(n.getTargetId()).isEqualTo(9L);
        assertThat(n.isRead()).isFalse();
        assertThat(n.getCreatedAt()).isNotNull();
    }

    @Test
    void longTextShouldBeClippedToTheColumnSizesSoTheInsertCannotFail() {
        InAppNotification n = InAppNotification.create(7L, NotificationType.JOB_HIDDEN, "s".repeat(500),
                "d".repeat(500), "n".repeat(900), NotificationTarget.JOB, 1L);

        assertThat(n.getSubject()).hasSize(200);
        assertThat(n.getDetail()).hasSize(200);
        assertThat(n.getNote()).hasSize(500);
    }

    @Test
    void aMissingSubjectShouldBecomeEmptyNotNull() {
        assertThat(InAppNotification.create(7L, NotificationType.JOB_HIDDEN, null, null, null, null, null).getSubject())
                .isEmpty();
    }

    @Test
    void aTargetNeedsBothItsTypeAndItsId() {
        assertThatThrownBy(() -> InAppNotification.create(7L, NotificationType.JOB_HIDDEN, "x", null, null,
                NotificationTarget.JOB, null)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> InAppNotification.create(7L, NotificationType.JOB_HIDDEN, "x", null, null,
                null, 5L)).isInstanceOf(IllegalArgumentException.class);

        assertThat(InAppNotification.create(7L, NotificationType.JOB_HIDDEN, "x", null, null, null, null)
                .getTargetType()).isNull();
    }
}