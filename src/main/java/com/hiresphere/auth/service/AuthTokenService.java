package com.hiresphere.auth.service;

import com.hiresphere.auth.entity.AuthToken;
import com.hiresphere.auth.entity.AuthTokenType;
import com.hiresphere.auth.repository.AuthTokenRepository;
import com.hiresphere.common.exception.AppException;
import com.hiresphere.common.util.TokenUtil;
import com.hiresphere.config.AuthTokenProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;

@Service
@RequiredArgsConstructor
public class AuthTokenService {

    private final AuthTokenRepository repository;
    private final AuthTokenProperties props;

    @Transactional
    public String issue(Long userId, AuthTokenType type) {
        Instant now = Instant.now();
        repository.invalidateAllForUser(userId, type, now);

        String raw = TokenUtil.generateToken();
        repository.save(AuthToken.create(userId, type, TokenUtil.sha256(raw), now.plus(ttl(type))));
        return raw;
    }

    @Transactional
    public Long consume(String rawToken, AuthTokenType type) {
        AuthToken token = repository.findByTokenHashAndType(TokenUtil.sha256(rawToken), type)
                .orElseThrow(this::invalid);
        if (repository.markUsedIfValid(token.getId(), Instant.now()) != 1) {
            throw invalid();
        }
        return token.getUserId();
    }

    @Transactional
    public void invalidateAll(Long userId, AuthTokenType type) {
        repository.invalidateAllForUser(userId, type, Instant.now());
    }

    private Duration ttl(AuthTokenType type) {
        return switch (type) {
            case EMAIL_VERIFICATION -> Duration.ofHours(props.verificationTtlHours());
            case PASSWORD_RESET -> Duration.ofMinutes(props.resetTtlMinutes());
        };
    }

    private AppException invalid() {
        return new AppException(HttpStatus.BAD_REQUEST, "Invalid or expired token");
    }
}