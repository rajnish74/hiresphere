package com.hiresphere.notification.service;

import com.hiresphere.common.dto.PageResponse;
import com.hiresphere.common.exception.ResourceNotFoundException;
import com.hiresphere.common.util.PageUtil;
import com.hiresphere.notification.dto.DeleteScope;
import com.hiresphere.notification.dto.NotificationResponse;
import com.hiresphere.notification.entity.InAppNotification;
import com.hiresphere.notification.entity.NotificationChannel;
import com.hiresphere.notification.entity.NotificationTarget;
import com.hiresphere.notification.entity.NotificationType;
import com.hiresphere.notification.event.InAppNotificationCreatedEvent;
import com.hiresphere.notification.repository.InAppNotificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
@RequiredArgsConstructor
public class InAppNotificationService {

    private final InAppNotificationRepository repository;
    private final NotificationPreferenceService preferences;
    private final ApplicationEventPublisher events;

    @Transactional(propagation = Propagation.MANDATORY)
    public void create(Long userId, NotificationType type, String subject, String detail, String note,
                       NotificationTarget targetType, Long targetId) {
        if (!preferences.isEnabled(userId, type.category(), NotificationChannel.IN_APP)) {
            return;
        }
        repository.save(InAppNotification.create(userId, type, subject, detail, note, targetType, targetId));
        events.publishEvent(new InAppNotificationCreatedEvent(userId));
    }

    @Transactional(readOnly = true)
    public PageResponse<NotificationResponse> list(Long userId, boolean unreadOnly, int page, int size) {
        Pageable pageable = PageUtil.newestFirst(page, size);
        Page<InAppNotification> result = unreadOnly
                ? repository.findByUserIdAndReadAtIsNull(userId, pageable)
                : repository.findByUserId(userId, pageable);
        return PageResponse.of(result, NotificationResponse::from);
    }

    @Transactional(readOnly = true)
    public long unreadCount(Long userId) {
        return repository.countByUserIdAndReadAtIsNull(userId);
    }

    @Transactional
    public void markRead(Long userId, Long id) {
        int updated = repository.markRead(id, userId, Instant.now());
        if (updated == 0 && !repository.existsByIdAndUserId(id, userId)) {
            throw new ResourceNotFoundException("Notification not found");
        }
    }

    @Transactional
    public int markAllRead(Long userId) {
        return repository.markAllRead(userId, Instant.now());
    }


    @Transactional
    public void delete(Long userId, Long id) {
        if (repository.deleteOwned(id, userId) == 0) {
            throw new ResourceNotFoundException("Notification not found");
        }
    }

    @Transactional
    public int deleteAll(Long userId, DeleteScope scope) {
        return scope == DeleteScope.READ ? repository.deleteAllRead(userId) : repository.deleteAllFor(userId);
    }



}