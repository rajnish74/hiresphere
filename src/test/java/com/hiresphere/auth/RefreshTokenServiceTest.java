package com.hiresphere.auth;

import com.hiresphere.auth.entity.RefreshToken;
import com.hiresphere.auth.repository.RefreshTokenRepository;
import com.hiresphere.auth.service.RefreshTokenService;
import com.hiresphere.common.exception.AppException;
import com.hiresphere.common.util.TokenUtil;
import com.hiresphere.config.JwtProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RefreshTokenServiceTest {

    @Mock
    private RefreshTokenRepository repository;

    private RefreshTokenService service;

    @BeforeEach
    void setUp() {
        service = new RefreshTokenService(repository,
                new JwtProperties("x".repeat(32), 15, 7, "hiresphere"));
    }

    private RefreshToken storedToken(String raw, Instant revokedAt) {
        RefreshToken t = new RefreshToken();
        t.setUserId(1L);
        t.setFamilyId("family-1");
        t.setTokenHash(TokenUtil.sha256(raw));
        t.setExpiresAt(Instant.now().plusSeconds(3600));
        t.setRevokedAt(revokedAt);
        return t;
    }

    @Test
    void reusedTokenShouldRevokeWholeFamilyAndFail() {
        String raw = "already-used-token";
        when(repository.findByTokenHash(TokenUtil.sha256(raw)))
                .thenReturn(Optional.of(storedToken(raw, Instant.now().minusSeconds(10))));

        assertThatThrownBy(() -> service.rotate(raw)).isInstanceOf(AppException.class);

        verify(repository).revokeFamily(eq("family-1"), any(Instant.class));
        verify(repository, never()).save(any());
    }

    @Test
    void validTokenShouldRotateAndIssueNewOne() {
        String raw = "valid-token";
        RefreshToken stored = storedToken(raw, null);
        when(repository.findByTokenHash(TokenUtil.sha256(raw))).thenReturn(Optional.of(stored));
        when(repository.save(any(RefreshToken.class))).thenAnswer(inv -> inv.getArgument(0));

        RefreshTokenService.Rotation rotation = service.rotate(raw);

        assertThat(stored.getRevokedAt()).isNotNull();
        assertThat(rotation.userId()).isEqualTo(1L);
        assertThat(rotation.refreshToken()).isNotBlank().isNotEqualTo(raw);
        verify(repository).save(any(RefreshToken.class));
    }

    @Test
    void unknownTokenShouldFailWithoutRevokingAnything() {
        when(repository.findByTokenHash(any())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.rotate("random-garbage")).isInstanceOf(AppException.class);

        verify(repository, never()).revokeFamily(any(), any());
    }
}