package com.hiresphere.user.service;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.hiresphere.user.event.SessionsInvalidatedEvent;
import com.hiresphere.user.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.time.Duration;
import java.util.Optional;

@Service
public class TokenVersionService {

    private final UserRepository userRepository;
    private final Cache<Long, Long> versions = Caffeine.newBuilder()
            .expireAfterWrite(Duration.ofSeconds(30))
            .maximumSize(50_000)
            .build();

    public TokenVersionService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public boolean isCurrent(long userId, long tokenVersion) {
        Long cached = versions.getIfPresent(userId);
        if (cached != null && cached == tokenVersion) {
            return true;
        }
        Optional<Integer> current = userRepository.findTokenVersionById(userId);
        if (current.isEmpty()) {
            versions.invalidate(userId);
            return false;
        }
        versions.put(userId, current.get().longValue());
        return current.get() == tokenVersion;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void onSessionsInvalidated(SessionsInvalidatedEvent event) {
        versions.invalidate(event.userId());
    }
}