package com.hiresphere.auth;

import com.hiresphere.auth.entity.AuthTokenType;
import com.hiresphere.auth.event.PasswordChangedEvent;
import com.hiresphere.auth.event.PasswordResetRequestedEvent;
import com.hiresphere.auth.service.AuthTokenService;
import com.hiresphere.auth.service.PasswordService;
import com.hiresphere.auth.service.RefreshTokenService;
import com.hiresphere.common.exception.AppException;
import com.hiresphere.common.exception.BadRequestException;
import com.hiresphere.config.AuthTokenProperties;
import com.hiresphere.user.entity.Role;
import com.hiresphere.user.entity.User;
import com.hiresphere.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PasswordServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private AuthTokenService authTokenService;
    @Mock
    private RefreshTokenService refreshTokenService;
    @Mock
    private ApplicationEventPublisher events;

    private PasswordService service;

    @BeforeEach
    void setUp() {
        service = new PasswordService(userRepository, passwordEncoder, authTokenService,
                refreshTokenService, events, new AuthTokenProperties(30, 24, 3, 15));
    }

    private User user() {
        User u = new User();
        u.setId(7L);
        u.setFullName("Asha");
        u.setEmail("asha@test.com");
        u.setPasswordHash("OLD_HASH");
        u.setRole(Role.CANDIDATE);
        return u;
    }

    // ----- forgot password -----

    @Test
    void unknownEmailShouldDoNothingAndNotFail() {
        when(userRepository.findByEmail("nobody@test.com")).thenReturn(Optional.empty());

        service.requestReset("  Nobody@Test.com ", "1.1.1.1");   // normalize bhi hota hai

        verify(authTokenService, never()).issue(any(), any());
        verify(events, never()).publishEvent(any(Object.class));
    }

    @Test
    void existingUserShouldGetATokenAndAnEvent() {
        when(userRepository.findByEmail("asha@test.com")).thenReturn(Optional.of(user()));
        when(authTokenService.issue(7L, AuthTokenType.PASSWORD_RESET)).thenReturn("raw-token");

        service.requestReset("asha@test.com", "1.1.1.1");

        ArgumentCaptor<PasswordResetRequestedEvent> event = ArgumentCaptor.forClass(PasswordResetRequestedEvent.class);
        verify(events).publishEvent(event.capture());
        assertThat(event.getValue().token()).isEqualTo("raw-token");
        assertThat(event.getValue().email()).isEqualTo("asha@test.com");
    }

    @Test
    void disabledUserShouldNotGetAResetToken() {
        User disabled = user();
        disabled.setEnabled(false);
        when(userRepository.findByEmail("asha@test.com")).thenReturn(Optional.of(disabled));

        service.requestReset("asha@test.com", "1.1.1.1");

        verify(authTokenService, never()).issue(any(), any());
    }

    @Test
    void fourthRequestForTheSameEmailShouldBeRateLimited() {
        when(userRepository.findByEmail("asha@test.com")).thenReturn(Optional.empty());
        for (int i = 0; i < 3; i++) {
            service.requestReset("asha@test.com", "10.0.0." + i);
        }

        assertThatThrownBy(() -> service.requestReset("asha@test.com", "10.0.0.9"))
                .isInstanceOfSatisfying(AppException.class,
                        e -> assertThat(e.getStatus()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS));
    }

    // ----- reset password -----

    @Test
    void resetShouldChangePasswordVerifyEmailRevokeSessionsAndNotify() {
        User u = user();
        when(authTokenService.consume("raw", AuthTokenType.PASSWORD_RESET)).thenReturn(7L);
        when(userRepository.findById(7L)).thenReturn(Optional.of(u));
        when(passwordEncoder.encode("NewPass12345")).thenReturn("NEW_HASH");

        service.reset("raw", "NewPass12345");

        assertThat(u.getPasswordHash()).isEqualTo("NEW_HASH");
        assertThat(u.isEmailVerified()).isTrue();
        verify(refreshTokenService).revokeAllForUser(7L);
        verify(authTokenService).invalidateAll(7L, AuthTokenType.PASSWORD_RESET);
        verify(events).publishEvent(any(PasswordChangedEvent.class));
    }

    @Test
    void resetWithInvalidTokenShouldChangeNothing() {
        when(authTokenService.consume("bad", AuthTokenType.PASSWORD_RESET))
                .thenThrow(new AppException(HttpStatus.BAD_REQUEST, "Invalid or expired token"));

        assertThatThrownBy(() -> service.reset("bad", "NewPass12345")).isInstanceOf(AppException.class);

        verify(userRepository, never()).findById(any());
        verify(refreshTokenService, never()).revokeAllForUser(any());
        verify(events, never()).publishEvent(any(Object.class));
    }

    // ----- change password -----

    @Test
    void changeWithWrongCurrentPasswordShouldFail() {
        when(userRepository.findById(7L)).thenReturn(Optional.of(user()));
        when(passwordEncoder.matches("wrong", "OLD_HASH")).thenReturn(false);

        assertThatThrownBy(() -> service.change(7L, "wrong", "NewPass12345"))
                .isInstanceOf(BadRequestException.class);

        verify(refreshTokenService, never()).revokeAllForUser(any());
    }

    @Test
    void changeToTheSamePasswordShouldFail() {
        when(userRepository.findById(7L)).thenReturn(Optional.of(user()));
        when(passwordEncoder.matches("SamePass123", "OLD_HASH")).thenReturn(true);

        assertThatThrownBy(() -> service.change(7L, "SamePass123", "SamePass123"))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    void successfulChangeShouldRevokeAllSessionsAndNotify() {
        User u = user();
        when(userRepository.findById(7L)).thenReturn(Optional.of(u));
        when(passwordEncoder.matches("OldPass123", "OLD_HASH")).thenReturn(true);
        when(passwordEncoder.encode("NewPass12345")).thenReturn("NEW_HASH");

        service.change(7L, "OldPass123", "NewPass12345");

        assertThat(u.getPasswordHash()).isEqualTo("NEW_HASH");
        verify(refreshTokenService).revokeAllForUser(7L);
        verify(events).publishEvent(any(PasswordChangedEvent.class));
    }
}