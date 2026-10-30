package com.hiresphere.auth.service;

import com.hiresphere.auth.dto.*;
import com.hiresphere.common.exception.*;
import com.hiresphere.user.dto.UserResponse;
import com.hiresphere.user.entity.Role;
import com.hiresphere.user.entity.User;
import com.hiresphere.user.repository.UserRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final RefreshTokenService refreshTokenService;
    private final LoginAttemptService loginAttemptService;
    private String dummyHash;
    private final EmailVerificationService emailVerificationService;



    @PostConstruct
    void initDummyHash() {
        this.dummyHash = passwordEncoder.encode("dummy-password-for-timing-defense");
    }

    @Transactional
    public AuthResponse register(RegisterRequest req) {
        if (req.role() == Role.ADMIN) {
            throw new BadRequestException("Cannot register with this role");
        }
        String email = normalize(req.email());
        PasswordPolicy.require(req.password(), email);
        if (userRepository.existsByEmail(email)) {
            throw new DuplicateResourceException("Email already registered");
        }

        User user = new User();
        user.setFullName(req.fullName().trim());
        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode(req.password()));
        user.setRole(req.role());
        userRepository.save(user);

        emailVerificationService.sendVerification(user);
        return buildResponse(user, refreshTokenService.issueNewFamily(user.getId()));
    }
    // readOnly nahi:
    @Transactional
    public AuthResponse login(LoginRequest req, String clientIp) {
        String email = normalize(req.email());
        String key = clientIp + "|" + email;
        loginAttemptService.checkNotBlocked(key);

        User user = userRepository.findByEmail(email).orElse(null);

        String hashToCheck = (user != null) ? user.getPasswordHash() : dummyHash;
        boolean passwordOk = passwordEncoder.matches(req.password(), hashToCheck);

        if (user == null || !passwordOk) {
            loginAttemptService.recordFailure(key);
            throw new AppException(HttpStatus.UNAUTHORIZED, "Invalid email or password");
        }
        if (user == null || !passwordEncoder.matches(req.password(), user.getPasswordHash())) {
            loginAttemptService.recordFailure(key);
            throw new AppException(HttpStatus.UNAUTHORIZED, "Invalid email or password");
        }
        if (!user.isEnabled()) {
            throw new AppException(HttpStatus.FORBIDDEN, "Account is disabled");
        }

        loginAttemptService.reset(key);
        return buildResponse(user, refreshTokenService.issueNewFamily(user.getId()));
    }

    // Yahan @Transactional nahi: rotate() ka apna transaction hai
    public AuthResponse refresh(String rawRefreshToken) {
        RefreshTokenService.Rotation rotation = refreshTokenService.rotate(rawRefreshToken);

        User user = userRepository.findById(rotation.userId())
                .orElseThrow(() -> new AppException(HttpStatus.UNAUTHORIZED, "Invalid or expired refresh token"));
        if (!user.isEnabled()) {
            refreshTokenService.revokeAllForUser(user.getId());
            throw new AppException(HttpStatus.FORBIDDEN, "Account is disabled");
        }
        return buildResponse(user, rotation.refreshToken());
    }

    public void logout(String rawRefreshToken) {
        refreshTokenService.revoke(rawRefreshToken);
    }

    public void logoutAll(Long userId) {
        refreshTokenService.revokeAllForUser(userId);
    }

    private AuthResponse buildResponse(User user, String refreshToken) {
        return new AuthResponse(jwtService.generateAccessToken(user), refreshToken, "Bearer",
                jwtService.accessTokenTtlSeconds(), UserResponse.from(user));
    }

    private String normalize(String email) {
        return email.trim().toLowerCase();
    }
}