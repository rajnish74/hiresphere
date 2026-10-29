package com.hiresphere.auth;

import com.hiresphere.auth.entity.AuthToken;
import com.hiresphere.auth.entity.AuthTokenType;
import com.hiresphere.auth.repository.AuthTokenRepository;
import com.hiresphere.auth.service.AuthTokenService;
import com.hiresphere.common.exception.AppException;
import com.hiresphere.common.util.TokenUtil;
import com.hiresphere.config.AuthTokenProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthTokenServiceTest {

    @Mock
    private AuthTokenRepository repository;

    private AuthTokenService service;

    @BeforeEach
    void setUp() {
        service = new AuthTokenService(repository, new AuthTokenProperties(30, 24, 3, 15));
    }

    private AuthToken stored(String raw, long id) {
        AuthToken t = AuthToken.create(7L, AuthTokenType.PASSWORD_RESET, TokenUtil.sha256(raw),
                Instant.now().plusSeconds(600));
        ReflectionTestUtils.setField(t, "id", id);
        return t;
    }

    @Test
    void issueShouldStoreOnlyTheHashAndInvalidateOlderTokens() {
        String raw = service.issue(7L, AuthTokenType.PASSWORD_RESET);

        ArgumentCaptor<AuthToken> saved = ArgumentCaptor.forClass(AuthToken.class);
        verify(repository).save(saved.capture());
        verify(repository).invalidateAllForUser(eq(7L), eq(AuthTokenType.PASSWORD_RESET), any(Instant.class));
        assertThat(saved.getValue().getTokenHash()).isEqualTo(TokenUtil.sha256(raw)).isNotEqualTo(raw);
        assertThat(saved.getValue().getExpiresAt())
                .isBetween(Instant.now().plusSeconds(29 * 60), Instant.now().plusSeconds(31 * 60));
    }

    @Test
    void verificationTokensShouldLiveForHoursNotMinutes() {
        service.issue(7L, AuthTokenType.EMAIL_VERIFICATION);

        ArgumentCaptor<AuthToken> saved = ArgumentCaptor.forClass(AuthToken.class);
        verify(repository).save(saved.capture());
        assertThat(saved.getValue().getExpiresAt()).isAfter(Instant.now().plusSeconds(23 * 3600));
    }

    @Test
    void consumeShouldReturnUserIdForAValidToken() {
        when(repository.findByTokenHashAndType(TokenUtil.sha256("raw"), AuthTokenType.PASSWORD_RESET))
                .thenReturn(Optional.of(stored("raw", 11L)));
        when(repository.markUsedIfValid(eq(11L), any(Instant.class))).thenReturn(1);

        assertThat(service.consume("raw", AuthTokenType.PASSWORD_RESET)).isEqualTo(7L);
    }

    @Test
    void consumeShouldFailWhenTokenWasAlreadyUsedExpiredOrRaced() {
        when(repository.findByTokenHashAndType(TokenUtil.sha256("raw"), AuthTokenType.PASSWORD_RESET))
                .thenReturn(Optional.of(stored("raw", 11L)));
        when(repository.markUsedIfValid(eq(11L), any(Instant.class))).thenReturn(0);

        assertThatThrownBy(() -> service.consume("raw", AuthTokenType.PASSWORD_RESET))
                .isInstanceOf(AppException.class);
    }

    @Test
    void consumeShouldFailForUnknownToken() {
        when(repository.findByTokenHashAndType(any(), any())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.consume("garbage", AuthTokenType.PASSWORD_RESET))
                .isInstanceOf(AppException.class)
                .hasMessage("Invalid or expired token");
    }
}