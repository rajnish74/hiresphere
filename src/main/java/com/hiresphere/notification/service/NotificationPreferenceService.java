package com.hiresphere.notification.service;

import com.hiresphere.common.exception.BadRequestException;
import com.hiresphere.notification.dto.PreferenceItem;
import com.hiresphere.notification.entity.NotificationCategory;
import com.hiresphere.notification.repository.NotificationOptOutRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class NotificationPreferenceService {

    private final NotificationOptOutRepository repository;

    /** Mandatory category ke liye DB ko chhua bhi nahi jata. */
    @Transactional(readOnly = true)
    public boolean shouldSend(Long userId, String template) {
        NotificationCategory category = NotificationTemplates.categoryOf(template);
        if (!category.isOptional()) {
            return true;
        }
        return !repository.findOptedOutCategories(userId).contains(category);
    }

    @Transactional(readOnly = true)
    public List<PreferenceItem> get(Long userId) {
        Set<NotificationCategory> optedOut = new HashSet<>(repository.findOptedOutCategories(userId));
        return Arrays.stream(NotificationCategory.values())
                .map(c -> new PreferenceItem(c, c.isOptional(), !c.isOptional() || !optedOut.contains(c)))
                .toList();
    }

    @Transactional
    public List<PreferenceItem> update(Long userId, Map<NotificationCategory, Boolean> changes) {
        // Pehle poori validation, phir koi bhi write: aadha update nahi hona chahiye
        changes.forEach((category, enabled) -> {
            if (enabled == null) {
                throw new BadRequestException("A true or false value is required for " + category);
            }
            if (!category.isOptional()) {
                throw new BadRequestException(category + " notifications cannot be turned off");
            }
        });

        Set<NotificationCategory> turnOn = EnumSet.noneOf(NotificationCategory.class);
        changes.forEach((category, enabled) -> {
            if (enabled) {
                turnOn.add(category);
            } else {
                repository.optOut(userId, category.name());
            }
        });
        if (!turnOn.isEmpty()) {
            repository.optIn(userId, turnOn);
        }
        return get(userId);
    }
}