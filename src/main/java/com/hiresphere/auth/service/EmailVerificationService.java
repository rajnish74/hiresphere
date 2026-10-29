package com.hiresphere.auth.service;

import com.hiresphere.auth.entity.AuthTokenType;
import com.hiresphere.auth.event.EmailVerificationRequestedEvent;
import com.hiresphere.common.exception.AppException;
import com.hiresphere.common.util.FixedWindowLimiter;
import com.hiresphere.config.AuthTokenProperties;
import com.hiresphere.user.entity.User;
import com.hiresphere.user.repository.UserRepository;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;

@Service
public class EmailVerificationService {

    private final UserRepository userRepository;
    private final AuthTokenService authTokenService;
    private final ApplicationEventPublisher events;
    private final AuthTokenProperties props;
    private final FixedWindowLimiter resendLimiter;

    public EmailVerificationService(UserRepository userRepository, AuthTokenService authTokenService,
                                    ApplicationEventPublisher events, AuthTokenProperties props) {
        this.userRepository = userRepository;
        this.authTokenService = authTokenService;
        this.events = events;
        this.props = props;
        this.resendLimiter = new FixedWindowLimiter(props.requestLimit(),
                Duration.ofMinutes(props.requestWindowMinutes()));
    }

    @Transactional
    public void sendVerification(User user) {
        String token = authTokenService.issue(user.getId(), AuthTokenType.EMAIL_VERIFICATION);
        events.publishEvent(new EmailVerificationRequestedEvent(
                user.getFullName(), user.getEmail(), token, props.verificationTtlHours()));
    }

    @Transactional
    public void verify(String rawToken) {
        Long userId = authTokenService.consume(rawToken, AuthTokenType.EMAIL_VERIFICATION);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AppException(HttpStatus.BAD_REQUEST, "Invalid or expired token"));
        user.setEmailVerified(true);
    }

    @Transactional
    public void resend(Long userId) {
        if (!resendLimiter.tryAcquire("user:" + userId)) {
            throw new AppException(HttpStatus.TOO_MANY_REQUESTS,
                    "Too many requests. Please try again in a few minutes.");
        }
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AppException(HttpStatus.UNAUTHORIZED, "User not found"));
        if (user.isEmailVerified()) {
            throw new AppException(HttpStatus.CONFLICT, "Email is already verified");
        }
        sendVerification(user);
    }
}