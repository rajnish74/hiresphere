package com.hiresphere.auth.service;

import com.hiresphere.auth.entity.RefreshToken;
import com.hiresphere.auth.repository.RefreshTokenRepository;
import com.hiresphere.common.exception.AppException;
import com.hiresphere.common.util.TokenUtil;
import com.hiresphere.config.JwtProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RefreshTokenService {

    public record Rotation(Long userId, String refreshToken) {
    }

    private final RefreshTokenRepository repository;
    private final JwtProperties props;

    @Transactional
    public String issueNewFamily(Long userId) {
        return issue(userId, UUID.randomUUID().toString());
    }

    // noRollbackFor:
    // Rollback ho gaya to revoke bhi undo ho jata.
    @Transactional(noRollbackFor = AppException.class)
    public Rotation rotate(String rawToken) {
        RefreshToken current = repository.findByTokenHash(TokenUtil.sha256(rawToken))
                .orElseThrow(this::invalid);
        Instant now = Instant.now();

        if (current.getRevokedAt() != null) {
            repository.revokeFamily(current.getFamilyId(), now);   // token reuse
            throw invalid();
        }
        if (current.getExpiresAt().isBefore(now)) {
            throw invalid();
        }

        current.setRevokedAt(now);
        String next = issue(current.getUserId(), current.getFamilyId());
        return new Rotation(current.getUserId(), next);
    }

    @Transactional
    public void revoke(String rawToken) {
        repository.findByTokenHash(TokenUtil.sha256(rawToken))
                .ifPresent(t -> repository.revokeFamily(t.getFamilyId(), Instant.now()));
    }

    @Transactional
    public void revokeAllForUser(Long userId) {
        repository.revokeAllForUser(userId, Instant.now());
    }

    private String issue(Long userId, String familyId) {
        String raw = TokenUtil.generateToken();
        Instant now = Instant.now();

        RefreshToken token = new RefreshToken();
        token.setUserId(userId);
        token.setTokenHash(TokenUtil.sha256(raw));
        token.setFamilyId(familyId);
        token.setCreatedAt(now);
        token.setExpiresAt(now.plus(props.refreshTokenDays(), ChronoUnit.DAYS));
        repository.save(token);
        return raw;
    }

    private AppException invalid() {
        return new AppException(HttpStatus.UNAUTHORIZED, "Invalid or expired refresh token");
    }
}