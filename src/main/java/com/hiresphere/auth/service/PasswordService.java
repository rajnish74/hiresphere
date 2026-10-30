package com.hiresphere.auth.service;

import com.hiresphere.auth.entity.AuthTokenType;
import com.hiresphere.auth.event.PasswordChangedEvent;
import com.hiresphere.auth.event.PasswordResetRequestedEvent;
import com.hiresphere.common.exception.AppException;
import com.hiresphere.common.exception.BadRequestException;
import com.hiresphere.common.util.FixedWindowLimiter;
import com.hiresphere.config.AuthTokenProperties;
import com.hiresphere.user.entity.User;
import com.hiresphere.user.event.SessionsInvalidatedEvent;
import com.hiresphere.user.repository.UserRepository;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;

@Service
public class PasswordService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthTokenService authTokenService;
    private final RefreshTokenService refreshTokenService;
    private final ApplicationEventPublisher events;
    private final AuthTokenProperties props;
    private final FixedWindowLimiter emailLimiter;
    private final FixedWindowLimiter ipLimiter;

    public PasswordService(UserRepository userRepository, PasswordEncoder passwordEncoder,
                           AuthTokenService authTokenService, RefreshTokenService refreshTokenService,
                           ApplicationEventPublisher events, AuthTokenProperties props) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.authTokenService = authTokenService;
        this.refreshTokenService = refreshTokenService;
        this.events = events;
        this.props = props;
        Duration window = Duration.ofMinutes(props.requestWindowMinutes());
        this.emailLimiter = new FixedWindowLimiter(props.requestLimit(), window);
        this.ipLimiter = new FixedWindowLimiter(props.requestLimit() * 5, window);
    }


    @Transactional
    public void requestReset(String rawEmail, String clientIp) {
        String email = rawEmail.trim().toLowerCase();
        if (!ipLimiter.tryAcquire(clientIp) || !emailLimiter.tryAcquire(email)) {
            throw new AppException(HttpStatus.TOO_MANY_REQUESTS,
                    "Too many requests. Please try again in a few minutes.");
        }
        userRepository.findByEmail(email).filter(User::isEnabled).ifPresent(user -> {
            String token = authTokenService.issue(user.getId(), AuthTokenType.PASSWORD_RESET);
            events.publishEvent(new PasswordResetRequestedEvent(
                    user.getFullName(), user.getEmail(), token, props.resetTtlMinutes()));
        });
    }

    @Transactional
    public void reset(String rawToken, String newPassword) {
        Long userId = authTokenService.consume(rawToken, AuthTokenType.PASSWORD_RESET);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AppException(HttpStatus.BAD_REQUEST, "Invalid or expired token"));

        PasswordPolicy.require(newPassword, user.getEmail());

        user.setPasswordHash(passwordEncoder.encode(newPassword));
        user.setEmailVerified(true);
        user.invalidateSessions();   // purane access tokens bhi turant bekar

        refreshTokenService.revokeAllForUser(userId);
        authTokenService.invalidateAll(userId, AuthTokenType.PASSWORD_RESET);

        events.publishEvent(new PasswordChangedEvent(user.getFullName(), user.getEmail()));
        events.publishEvent(new SessionsInvalidatedEvent(userId));
    }

    @Transactional
    public void change(Long userId, String currentPassword, String newPassword) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AppException(HttpStatus.UNAUTHORIZED, "User not found"));

        if (!passwordEncoder.matches(currentPassword, user.getPasswordHash())) {
            throw new BadRequestException("Current password is incorrect");
        }
        if (currentPassword.equals(newPassword)) {
            throw new BadRequestException("New password must be different from the current password");
        }
        PasswordPolicy.require(newPassword, user.getEmail());

        user.setPasswordHash(passwordEncoder.encode(newPassword));
        user.invalidateSessions();

        refreshTokenService.revokeAllForUser(userId);
        authTokenService.invalidateAll(userId, AuthTokenType.PASSWORD_RESET);

        events.publishEvent(new PasswordChangedEvent(user.getFullName(), user.getEmail()));
        events.publishEvent(new SessionsInvalidatedEvent(userId));
    }
}