
package com.hiresphere.notification;

import com.hiresphere.common.dto.PageResponse;
import com.hiresphere.common.exception.ResourceNotFoundException;
import com.hiresphere.notification.dto.NotificationResponse;
import com.hiresphere.notification.entity.*;
import com.hiresphere.notification.event.InAppNotificationCreatedEvent;
import com.hiresphere.notification.repository.InAppNotificationRepository;
import com.hiresphere.notification.service.InAppNotificationService;
import com.hiresphere.notification.service.NotificationPreferenceService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InAppNotificationServiceTest {

    @Mock
    private InAppNotificationRepository repository;

    @Mock
    private NotificationPreferenceService preferences;

    @Mock
    private ApplicationEventPublisher events;

    private InAppNotificationService service;

    @BeforeEach
    void setUp() {
        service = new InAppNotificationService(
                repository,
                preferences,
                events
        );
    }

    private InAppNotification sample(String subject) {
        return InAppNotification.create(
                1L,
                NotificationType.APPLICATION_STATUS_CHANGED,
                subject,
                "SHORTLISTED",
                "Good",
                NotificationTarget.APPLICATION,
                9L
        );
    }

    @Test
    void createShouldSaveARowForTheRecipient() {
        when(preferences.isEnabled(
                2L,
                NotificationCategory.NEW_APPLICANTS,
                NotificationChannel.IN_APP
        )).thenReturn(true);

        service.create(
                2L,
                NotificationType.APPLICATION_SUBMITTED,
                "Java Dev",
                "Asha",
                null,
                NotificationTarget.APPLICATION,
                9L
        );

        ArgumentCaptor<InAppNotification> saved =
                ArgumentCaptor.forClass(InAppNotification.class);

        verify(repository).save(saved.capture());

        assertThat(saved.getValue().getUserId()).isEqualTo(2L);
        assertThat(saved.getValue().getType())
                .isEqualTo(NotificationType.APPLICATION_SUBMITTED);
        assertThat(saved.getValue().getDetail()).isEqualTo("Asha");

        // Verify event is published for the notification recipient.
        verify(events).publishEvent(
                new InAppNotificationCreatedEvent(2L)
        );
    }

    @Test
    void createShouldSaveNothingWhenTheUserSwitchedOffTheBellForThatCategory() {
        when(preferences.isEnabled(
                2L,
                NotificationCategory.APPLICATION_UPDATES,
                NotificationChannel.IN_APP
        )).thenReturn(false);

        service.create(
                2L,
                NotificationType.APPLICATION_STATUS_CHANGED,
                "Java Dev",
                "SHORTLISTED",
                null,
                NotificationTarget.APPLICATION,
                9L
        );

        verify(repository, never()).save(any());

        // No event should be published when the user disabled in-app notifications.
        verify(events, never()).publishEvent(any(Object.class));
    }

    @Test
    void listShouldUseTheUnreadQueryOnlyWhenAsked() {
        when(repository.findByUserId(
                eq(1L),
                any(Pageable.class)
        )).thenReturn(
                new PageImpl<>(List.of(sample("A"), sample("B")))
        );

        PageResponse<NotificationResponse> all =
                service.list(1L, false, 0, 20);

        assertThat(all.content()).hasSize(2);
        assertThat(all.content().get(0).read()).isFalse();

        verify(repository, never())
                .findByUserIdAndReadAtIsNull(any(), any());
    }

    @Test
    void listOfUnreadOnlyShouldUseTheUnreadQuery() {
        when(repository.findByUserIdAndReadAtIsNull(
                eq(1L),
                any(Pageable.class)
        )).thenReturn(
                new PageImpl<>(List.of(sample("A")))
        );

        assertThat(service.list(1L, true, 0, 20).content())
                .hasSize(1);

        verify(repository, never())
                .findByUserId(any(), any());
    }

    @Test
    void unreadCountShouldAskTheRepository() {
        when(repository.countByUserIdAndReadAtIsNull(1L))
                .thenReturn(4L);

        assertThat(service.unreadCount(1L)).isEqualTo(4L);
    }

    @Test
    void markingAnUnreadOwnNotificationShouldSucceed() {
        when(repository.markRead(
                eq(5L),
                eq(1L),
                any(Instant.class)
        )).thenReturn(1);

        assertThatCode(() -> service.markRead(1L, 5L))
                .doesNotThrowAnyException();

        verify(repository, never()).existsByIdAndUserId(any(), any());
    }

    @Test
    void markingAnAlreadyReadNotificationShouldStillSucceed() {
        when(repository.markRead(
                eq(5L),
                eq(1L),
                any(Instant.class)
        )).thenReturn(0);

        when(repository.existsByIdAndUserId(5L, 1L))
                .thenReturn(true);

        assertThatCode(() -> service.markRead(1L, 5L))
                .doesNotThrowAnyException();
    }

    @Test
    void markingSomeoneElsesOrAMissingNotificationShouldLookLikeNotFound() {
        when(repository.markRead(
                eq(5L),
                eq(1L),
                any(Instant.class)
        )).thenReturn(0);

        when(repository.existsByIdAndUserId(5L, 1L))
                .thenReturn(false);

        assertThatThrownBy(() -> service.markRead(1L, 5L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void markAllShouldReturnHowManyChanged() {
        when(repository.markAllRead(eq(1L), any(Instant.class)))
                .thenReturn(7);

        assertThat(service.markAllRead(1L)).isEqualTo(7);
    }
}
