package com.hiresphere.auth.service;

import com.hiresphere.auth.dto.*;
import com.hiresphere.common.exception.*;
import com.hiresphere.user.dto.UserResponse;
import com.hiresphere.user.entity.Role;
import com.hiresphere.user.entity.User;
import com.hiresphere.user.repository.UserRepository;
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

    @Transactional
    public AuthResponse register(RegisterRequest req) {
        if (req.role() == Role.ADMIN) {
            throw new BadRequestException("Cannot register with this role");
        }
        String email = normalize(req.email());
        if (userRepository.existsByEmail(email)) {
            throw new DuplicateResourceException("Email already registered");
        }

        User user = new User();
        user.setFullName(req.fullName().trim());
        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode(req.password()));
        user.setRole(req.role());
        userRepository.save(user);

        return buildResponse(user);
    }

    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest req) {
        User user = userRepository.findByEmail(normalize(req.email()))
                .orElseThrow(this::invalidCredentials);

        if (!passwordEncoder.matches(req.password(), user.getPasswordHash())) {
            throw invalidCredentials();
        }
        if (!user.isEnabled()) {
            throw new AppException(HttpStatus.FORBIDDEN, "Account is disabled");
        }
        return buildResponse(user);
    }

    private AuthResponse buildResponse(User user) {
        return new AuthResponse(jwtService.generateAccessToken(user), "Bearer",
                jwtService.accessTokenTtlSeconds(), UserResponse.from(user));
    }

    private AppException invalidCredentials() {
        return new AppException(HttpStatus.UNAUTHORIZED, "Invalid email or password");
    }

    private String normalize(String email) {
        return email.trim().toLowerCase();
    }
}