package com.hiresphere.notification;

import com.hiresphere.common.exception.BadRequestException;
import com.hiresphere.notification.dto.ChannelChoice;
import com.hiresphere.notification.dto.PreferenceItem;
import com.hiresphere.notification.entity.NotificationCategory;
import com.hiresphere.notification.entity.NotificationChannel;
import com.hiresphere.notification.entity.NotificationOptOut;
import com.hiresphere.notification.repository.NotificationOptOutRepository;
import com.hiresphere.notification.service.NotificationPreferenceService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NotificationPreferenceServiceTest {

    @Mock
    private NotificationOptOutRepository repository;

    private NotificationPreferenceService service;

    @BeforeEach
    void setUp() {
        service = new NotificationPreferenceService(repository);
    }

    private NotificationOptOut optOut(NotificationCategory category, NotificationChannel channel) {
        NotificationOptOut o = new NotificationOptOut();
        ReflectionTestUtils.setField(o, "category", category);
        ReflectionTestUtils.setField(o, "channel", channel);
        return o;
    }

    private PreferenceItem item(List<PreferenceItem> items, NotificationCategory category) {
        return items.stream().filter(i -> i.category() == category).findFirst().orElseThrow();
    }

    @Test
    void mandatoryCategoriesShouldAlwaysBeOnForBothChannelsWithoutTouchingTheDatabase() {
        assertThat(service.shouldSend(1L, "password-reset")).isTrue();
        assertThat(service.shouldSend(1L, "brand-new-unmapped-template")).isTrue();
        assertThat(service.isEnabled(1L, NotificationCategory.ACCOUNT, NotificationChannel.IN_APP)).isTrue();
        assertThat(service.isEnabled(1L, NotificationCategory.SECURITY, NotificationChannel.EMAIL)).isTrue();

        verifyNoInteractions(repository);
    }

    @Test
    void switchingOffEmailShouldNotSwitchOffTheBell() {
        when(repository.findOptedOutCategories(1L, NotificationChannel.EMAIL))
                .thenReturn(List.of(NotificationCategory.APPLICATION_UPDATES));
        when(repository.findOptedOutCategories(1L, NotificationChannel.IN_APP)).thenReturn(List.of());

        assertThat(service.shouldSend(1L, "application-status-candidate")).isFalse();
        assertThat(service.isEnabled(1L, NotificationCategory.APPLICATION_UPDATES, NotificationChannel.IN_APP)).isTrue();
    }

    @Test
    void getShouldShowEachChannelSeparatelyAndMandatoryOnesAsAlwaysOn() {
        when(repository.findByUserId(1L)).thenReturn(List.of(
                optOut(NotificationCategory.APPLICATION_UPDATES, NotificationChannel.EMAIL),
                optOut(NotificationCategory.NEW_APPLICANTS, NotificationChannel.IN_APP)));

        List<PreferenceItem> items = service.get(1L);

        assertThat(items).hasSize(NotificationCategory.values().length);
        PreferenceItem updates = item(items, NotificationCategory.APPLICATION_UPDATES);
        assertThat(updates.email()).isFalse();
        assertThat(updates.inApp()).isTrue();
        PreferenceItem applicants = item(items, NotificationCategory.NEW_APPLICANTS);
        assertThat(applicants.email()).isTrue();
        assertThat(applicants.inApp()).isFalse();
        PreferenceItem security = item(items, NotificationCategory.SECURITY);
        assertThat(security.optional()).isFalse();
        assertThat(security.email()).isTrue();
        assertThat(security.inApp()).isTrue();
    }

    @Test
    void aMandatoryCategoryCannotBeChangedAndNothingIsWritten() {
        Map<NotificationCategory, ChannelChoice> changes = new HashMap<>();
        changes.put(NotificationCategory.PRODUCT_NEWS, new ChannelChoice(false, null));
        changes.put(NotificationCategory.SECURITY, new ChannelChoice(null, false));

        assertThatThrownBy(() -> service.update(1L, changes)).isInstanceOf(BadRequestException.class);

        verifyNoInteractions(repository);   // PRODUCT_NEWS ka bhi koi write nahi
    }

    @Test
    void aChoiceThatChangesNothingShouldBeRejected() {
        Map<NotificationCategory, ChannelChoice> changes = new HashMap<>();
        changes.put(NotificationCategory.PRODUCT_NEWS, new ChannelChoice(null, null));

        assertThatThrownBy(() -> service.update(1L, changes)).isInstanceOf(BadRequestException.class);

        changes.put(NotificationCategory.PRODUCT_NEWS, null);
        assertThatThrownBy(() -> service.update(1L, changes)).isInstanceOf(BadRequestException.class);
        verifyNoInteractions(repository);
    }

    @Test
    void updateShouldOnlyTouchTheChannelsThatWereMentioned() {
        Map<NotificationCategory, ChannelChoice> changes = new HashMap<>();
        changes.put(NotificationCategory.APPLICATION_UPDATES, new ChannelChoice(false, null));
        changes.put(NotificationCategory.NEW_APPLICANTS, new ChannelChoice(null, true));

        service.update(1L, changes);

        verify(repository).optOut(1L, "APPLICATION_UPDATES", "EMAIL");
        verify(repository).optIn(eq(1L), eq(Set.of(NotificationCategory.NEW_APPLICANTS)), eq(NotificationChannel.IN_APP));
        verify(repository, never()).optOut(eq(1L), eq("APPLICATION_UPDATES"), eq("IN_APP"));
        verify(repository, never()).optOut(eq(1L), eq("NEW_APPLICANTS"), any());
        verify(repository, never()).optIn(eq(1L), any(), eq(NotificationChannel.EMAIL));
    }
}