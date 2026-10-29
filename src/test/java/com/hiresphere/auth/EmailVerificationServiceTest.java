package com.hiresphere.auth;

import com.hiresphere.auth.entity.AuthTokenType;
import com.hiresphere.auth.event.EmailVerificationRequestedEvent;
import com.hiresphere.auth.service.AuthTokenService;
import com.hiresphere.auth.service.EmailVerificationService;
import com.hiresphere.common.exception.AppException;
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

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EmailVerificationServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private AuthTokenService authTokenService;
    @Mock
    private ApplicationEventPublisher events;

    private EmailVerificationService service;

    @BeforeEach
    void setUp() {
        service = new EmailVerificationService(userRepository, authTokenService, events,
                new AuthTokenProperties(30, 24, 3, 15));
    }

    private User user() {
        User u = new User();
        u.setId(7L);
        u.setFullName("Asha");
        u.setEmail("asha@test.com");
        u.setRole(Role.CANDIDATE);
        return u;
    }

    @Test
    void sendVerificationShouldIssueTokenAndPublishEventForTheUsersAddress() {
        when(authTokenService.issue(7L, AuthTokenType.EMAIL_VERIFICATION)).thenReturn("raw-token");

        service.sendVerification(user());

        ArgumentCaptor<EmailVerificationRequestedEvent> event =
                ArgumentCaptor.forClass(EmailVerificationRequestedEvent.class);
        verify(events).publishEvent(event.capture());
        assertThat(event.getValue().email()).isEqualTo("asha@test.com");
        assertThat(event.getValue().token()).isEqualTo("raw-token");
        assertThat(event.getValue().validHours()).isEqualTo(24);
    }

    @Test
    void verifyShouldMarkTheEmailAsVerified() {
        User u = user();
        when(authTokenService.consume("raw", AuthTokenType.EMAIL_VERIFICATION)).thenReturn(7L);
        when(userRepository.findById(7L)).thenReturn(Optional.of(u));

        service.verify("raw");

        assertThat(u.isEmailVerified()).isTrue();
    }

    @Test
    void resendShouldFailWhenAlreadyVerified() {
        User u = user();
        u.setEmailVerified(true);
        when(userRepository.findById(7L)).thenReturn(Optional.of(u));

        assertThatThrownBy(() -> service.resend(7L))
                .isInstanceOfSatisfying(AppException.class,
                        e -> assertThat(e.getStatus()).isEqualTo(HttpStatus.CONFLICT));
    }

    @Test
    void resendShouldBeThrottledAfterThreeRequests() {
        when(userRepository.findById(7L)).thenReturn(Optional.of(user()));
        when(authTokenService.issue(7L, AuthTokenType.EMAIL_VERIFICATION)).thenReturn("t");

        service.resend(7L);
        service.resend(7L);
        service.resend(7L);

        assertThatThrownBy(() -> service.resend(7L))
                .isInstanceOfSatisfying(AppException.class,
                        e -> assertThat(e.getStatus()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS));
    }
}