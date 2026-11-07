package com.hiresphere.notification.service;

import com.hiresphere.common.exception.BadRequestException;
import com.hiresphere.notification.dto.ChannelChoice;
import com.hiresphere.notification.dto.PreferenceItem;
import com.hiresphere.notification.entity.NotificationCategory;
import com.hiresphere.notification.entity.NotificationChannel;
import com.hiresphere.notification.repository.NotificationOptOutRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class NotificationPreferenceService {

    private final NotificationOptOutRepository repository;

    /** Email path (NotificationService ke purane callers ke liye). */
    @Transactional(readOnly = true)
    public boolean shouldSend(Long userId, String template) {
        return isEnabled(userId, NotificationTemplates.categoryOf(template), NotificationChannel.EMAIL);
    }

    /** Mandatory category ke liye DB ko chhua bhi nahi jata. */
    @Transactional(readOnly = true)
    public boolean isEnabled(Long userId, NotificationCategory category, NotificationChannel channel) {
        if (!category.isOptional()) {
            return true;
        }
        return !repository.findOptedOutCategories(userId, channel).contains(category);
    }

    @Transactional(readOnly = true)
    public List<PreferenceItem> get(Long userId) {
        Map<NotificationChannel, Set<NotificationCategory>> off = new EnumMap<>(NotificationChannel.class);
        for (NotificationChannel channel : NotificationChannel.values()) {
            off.put(channel, EnumSet.noneOf(NotificationCategory.class));
        }
        repository.findByUserId(userId).forEach(o -> off.get(o.getChannel()).add(o.getCategory()));

        return Arrays.stream(NotificationCategory.values())
                .map(c -> new PreferenceItem(c, c.isOptional(),
                        !c.isOptional() || !off.get(NotificationChannel.EMAIL).contains(c),
                        !c.isOptional() || !off.get(NotificationChannel.IN_APP).contains(c)))
                .toList();
    }

    @Transactional
    public List<PreferenceItem> update(Long userId, Map<NotificationCategory, ChannelChoice> changes) {
        // Pehle poori validation, phir koi bhi write: aadha update nahi hona chahiye
        changes.forEach((category, choice) -> {
            if (choice == null || (choice.email() == null && choice.inApp() == null)) {
                throw new BadRequestException("Choose email, inApp or both for " + category);
            }
            if (!category.isOptional()) {
                throw new BadRequestException(category + " notifications cannot be turned off");
            }
        });

        for (NotificationChannel channel : NotificationChannel.values()) {
            Set<NotificationCategory> turnOn = EnumSet.noneOf(NotificationCategory.class);
            changes.forEach((category, choice) -> {
                Boolean wanted = choice.valueFor(channel);
                if (wanted == null) {
                    return;
                }
                if (wanted) {
                    turnOn.add(category);
                } else {
                    repository.optOut(userId, category.name(), channel.name());
                }
            });
            if (!turnOn.isEmpty()) {
                repository.optIn(userId, turnOn, channel);
            }
        }
        return get(userId);
    }
}