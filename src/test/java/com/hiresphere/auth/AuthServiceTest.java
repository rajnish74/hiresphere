package com.hiresphere.auth;

import com.hiresphere.auth.dto.LoginRequest;
import com.hiresphere.auth.service.AuthService;
import com.hiresphere.auth.service.EmailVerificationService;
import com.hiresphere.auth.service.JwtService;
import com.hiresphere.auth.service.LoginAttemptService;
import com.hiresphere.auth.service.RefreshTokenService;
import com.hiresphere.common.exception.AppException;
import com.hiresphere.user.entity.Role;
import com.hiresphere.user.entity.User;
import com.hiresphere.user.event.SessionsInvalidatedEvent;
import com.hiresphere.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Duration;
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
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private JwtService jwtService;
    @Mock
    private RefreshTokenService refreshTokenService;
    @Mock
    private LoginAttemptService loginAttemptService;
    @Mock
    private EmailVerificationService emailVerificationService;
    @Mock
    private ApplicationEventPublisher events;

    @InjectMocks
    private AuthService service;

    private User user() {
        User u = new User();
        u.setId(7L);
        u.setFullName("Asha");
        u.setEmail("asha@test.com");
        u.setPasswordHash("HASH");
        u.setRole(Role.CANDIDATE);
        return u;
    }

    private LoginRequest login() {
        return new LoginRequest("asha@test.com", "Pass-Word-123");
    }

    // ---------- logout-all ----------

    @Test
    void logoutAllShouldBumpTokenVersionRevokeRefreshTokensAndAnnounce() {
        User u = user();
        when(userRepository.findById(7L)).thenReturn(Optional.of(u));

        service.logoutAll(7L);

        assertThat(u.getTokenVersion()).isEqualTo(1);
        verify(refreshTokenService).revokeAllForUser(7L);
        verify(events).publishEvent(new SessionsInvalidatedEvent(7L));
    }

    @Test
    void logoutAllForUnknownUserShouldFailAndRevokeNothing() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.logoutAll(99L))
                .isInstanceOfSatisfying(AppException.class,
                        e -> assertThat(e.getStatus()).isEqualTo(HttpStatus.UNAUTHORIZED));

        verify(refreshTokenService, never()).revokeAllForUser(any());
        verify(events, never()).publishEvent(any(Object.class));
    }

    // ---------- last login ----------

    @Test
    void successfulLoginShouldTouchLastLoginWithAFiveMinuteThreshold() {
        when(userRepository.findByEmail("asha@test.com")).thenReturn(Optional.of(user()));
        when(passwordEncoder.matches("Pass-Word-123", "HASH")).thenReturn(true);

        service.login(login(), "1.1.1.1");

        ArgumentCaptor<Instant> now = ArgumentCaptor.forClass(Instant.class);
        ArgumentCaptor<Instant> threshold = ArgumentCaptor.forClass(Instant.class);
        verify(userRepository).touchLastLogin(eq(7L), now.capture(), threshold.capture());
        assertThat(Duration.between(threshold.getValue(), now.getValue())).isEqualTo(Duration.ofMinutes(5));
        verify(loginAttemptService).reset("1.1.1.1|asha@test.com");
    }

    @Test
    void wrongPasswordShouldNotTouchLastLogin() {
        when(userRepository.findByEmail("asha@test.com")).thenReturn(Optional.of(user()));
        when(passwordEncoder.matches("Pass-Word-123", "HASH")).thenReturn(false);

        assertThatThrownBy(() -> service.login(login(), "1.1.1.1"))
                .isInstanceOfSatisfying(AppException.class,
                        e -> assertThat(e.getStatus()).isEqualTo(HttpStatus.UNAUTHORIZED));

        verify(userRepository, never()).touchLastLogin(any(), any(), any());
        verify(loginAttemptService).recordFailure("1.1.1.1|asha@test.com");
    }

    @Test
    void blockedUserShouldNotTouchLastLogin() {
        User blocked = user();
        blocked.setEnabled(false);
        when(userRepository.findByEmail("asha@test.com")).thenReturn(Optional.of(blocked));
        when(passwordEncoder.matches("Pass-Word-123", "HASH")).thenReturn(true);

        assertThatThrownBy(() -> service.login(login(), "1.1.1.1"))
                .isInstanceOfSatisfying(AppException.class,
                        e -> assertThat(e.getStatus()).isEqualTo(HttpStatus.FORBIDDEN));

        verify(userRepository, never()).touchLastLogin(any(), any(), any());
    }
}