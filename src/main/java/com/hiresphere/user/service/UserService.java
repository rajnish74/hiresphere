package com.hiresphere.user.service;

import com.hiresphere.common.dto.PageResponse;
import com.hiresphere.common.exception.AppException;
import com.hiresphere.common.exception.BadRequestException;
import com.hiresphere.common.exception.ResourceNotFoundException;
import com.hiresphere.common.util.CountUtil;
import com.hiresphere.common.util.PageUtil;
import com.hiresphere.user.dto.AdminUserResponse;
import com.hiresphere.user.dto.UserResponse;
import com.hiresphere.user.dto.UserStats;
import com.hiresphere.user.entity.Role;
import com.hiresphere.user.entity.User;
import com.hiresphere.user.event.SessionsInvalidatedEvent;
import com.hiresphere.user.event.UserBlockedEvent;
import com.hiresphere.user.repository.UserRepository;
import com.hiresphere.user.repository.UserSpecifications;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final ApplicationEventPublisher events;

    @Transactional(readOnly = true)
    public UserResponse getById(Long id) {
        return userRepository.findById(id)
                .map(UserResponse::from)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }

    @Transactional(readOnly = true)
    public Map<Long, String> getEmails(Collection<Long> ids) {
        if (ids.isEmpty()) {
            return Map.of();
        }
        return userRepository.findAllById(ids).stream()
                .collect(Collectors.toMap(User::getId, User::getEmail));
    }

    // ---------- admin side ----------

    @Transactional(readOnly = true)
    public PageResponse<AdminUserResponse> search(String q, Role role, Boolean enabled, int page, int size) {
        if (q != null && q.length() > 100) {
            throw new BadRequestException("Search text is too long (max 100 characters)");
        }
        Page<User> result = userRepository.findAll(
                UserSpecifications.search(q, role, enabled), PageUtil.newestFirst(page, size));
        return PageResponse.of(result, AdminUserResponse::from);
    }

    @Transactional(readOnly = true)
    public AdminUserResponse getForAdmin(Long id) {
        return AdminUserResponse.from(findUser(id));
    }

    @Transactional
    public AdminUserResponse block(Long adminId, Long targetId, String reason) {
        if (reason == null || reason.isBlank()) {
            throw new BadRequestException("A reason is required to block a user");
        }
        if (adminId.equals(targetId)) {
            throw new BadRequestException("You cannot block your own account");
        }
        User target = findUser(targetId);
        if (target.getRole() == Role.ADMIN) {
            throw new AppException(HttpStatus.FORBIDDEN, "Admin accounts cannot be blocked from the API");
        }
        if (!target.isEnabled()) {
            throw new AppException(HttpStatus.CONFLICT, "User is already blocked");
        }
        target.setEnabled(false);
        target.invalidateSessions();
        events.publishEvent(new SessionsInvalidatedEvent(targetId));
        events.publishEvent(new UserBlockedEvent(targetId, reason.trim()));
        return AdminUserResponse.from(target);
    }
    @Transactional
    public AdminUserResponse unblock(Long targetId) {
        User target = findUser(targetId);
        if (target.isEnabled()) {
            throw new AppException(HttpStatus.CONFLICT, "User is not blocked");
        }
        target.setEnabled(true);
        return AdminUserResponse.from(target);
    }

    @Transactional(readOnly = true)
    public UserStats stats(Instant since) {
        Map<Role, Long> byRole = CountUtil.toEnumMap(Role.class, userRepository.countGroupedByRole());
        long total = byRole.values().stream().mapToLong(Long::longValue).sum();
        return new UserStats(total, byRole, userRepository.countByEnabledFalse(),
                userRepository.countByEmailVerifiedFalse(), userRepository.countByCreatedAtAfter(since));
    }

    private User findUser(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }

    @Transactional(readOnly = true)
    public Optional<String> findActiveCandidateName(Long userId) {
        return userRepository.findById(userId)
                .filter(u -> u.getRole() == Role.CANDIDATE && u.isEnabled())
                .map(User::getFullName);
    }
}