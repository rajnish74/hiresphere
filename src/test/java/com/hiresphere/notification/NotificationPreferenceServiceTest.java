package com.hiresphere.notification;

import com.hiresphere.common.exception.BadRequestException;
import com.hiresphere.notification.dto.PreferenceItem;
import com.hiresphere.notification.entity.NotificationCategory;
import com.hiresphere.notification.repository.NotificationOptOutRepository;
import com.hiresphere.notification.service.NotificationPreferenceService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.eq;
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

    private PreferenceItem item(List<PreferenceItem> items, NotificationCategory category) {
        return items.stream().filter(i -> i.category() == category).findFirst().orElseThrow();
    }

    @Test
    void mandatoryTemplatesShouldAlwaysBeSentWithoutTouchingTheDatabase() {
        assertThat(service.shouldSend(1L, "password-reset")).isTrue();
        assertThat(service.shouldSend(1L, "account-blocked")).isTrue();
        assertThat(service.shouldSend(1L, "brand-new-unmapped-template")).isTrue();   // unknown = mandatory

        verifyNoInteractions(repository);
    }

    @Test
    void optionalTemplateShouldBeBlockedWhenTheUserOptedOut() {
        when(repository.findOptedOutCategories(1L)).thenReturn(List.of(NotificationCategory.APPLICATION_UPDATES));

        assertThat(service.shouldSend(1L, "application-status-candidate")).isFalse();
    }

    @Test
    void optingOutOfOneCategoryShouldNotBlockAnother() {
        when(repository.findOptedOutCategories(1L)).thenReturn(List.of(NotificationCategory.PRODUCT_NEWS));

        assertThat(service.shouldSend(1L, "application-status-candidate")).isTrue();
    }

    @Test
    void getShouldShowMandatoryCategoriesAsNonOptionalAndAlwaysEnabled() {
        when(repository.findOptedOutCategories(1L)).thenReturn(List.of(NotificationCategory.APPLICATION_UPDATES));

        List<PreferenceItem> items = service.get(1L);

        assertThat(items).hasSize(NotificationCategory.values().length);
        assertThat(item(items, NotificationCategory.APPLICATION_UPDATES).enabled()).isFalse();
        assertThat(item(items, NotificationCategory.NEW_APPLICANTS).enabled()).isTrue();
        PreferenceItem security = item(items, NotificationCategory.SECURITY);
        assertThat(security.optional()).isFalse();
        assertThat(security.enabled()).isTrue();
    }

    @Test
    void mandatoryCategoriesCannotBeTurnedOffAndNothingIsWritten() {
        Map<NotificationCategory, Boolean> changes = new HashMap<>();
        changes.put(NotificationCategory.PRODUCT_NEWS, false);
        changes.put(NotificationCategory.SECURITY, false);

        assertThatThrownBy(() -> service.update(1L, changes)).isInstanceOf(BadRequestException.class);

        verifyNoInteractions(repository);   // PRODUCT_NEWS ka bhi koi write nahi: aadha update nahi
    }

    @Test
    void updateShouldOptOutDisabledCategoriesAndOptInEnabledOnes() {
        Map<NotificationCategory, Boolean> changes = new HashMap<>();
        changes.put(NotificationCategory.APPLICATION_UPDATES, false);
        changes.put(NotificationCategory.NEW_APPLICANTS, true);

        service.update(1L, changes);

        verify(repository).optOut(1L, "APPLICATION_UPDATES");
        verify(repository).optIn(eq(1L), eq(Set.of(NotificationCategory.NEW_APPLICANTS)));
    }
}