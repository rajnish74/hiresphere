package com.hiresphere.auth.controller;

import com.hiresphere.auth.dto.*;
import com.hiresphere.auth.service.AuthService;
import com.hiresphere.auth.service.EmailVerificationService;
import com.hiresphere.auth.service.PasswordService;
import com.hiresphere.common.dto.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final EmailVerificationService emailVerificationService;
    private final PasswordService passwordService;

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        return ApiResponse.ok("Registered successfully", authService.register(request));
    }

    @PostMapping("/login")
    public ApiResponse<AuthResponse> login(@Valid @RequestBody LoginRequest request,
                                           HttpServletRequest http) {
        return ApiResponse.ok("Login successful", authService.login(request, http.getRemoteAddr()));
    }

    @PostMapping("/refresh")
    public ApiResponse<AuthResponse> refresh(@Valid @RequestBody RefreshTokenRequest request) {
        return ApiResponse.ok("Token refreshed", authService.refresh(request.refreshToken()));
    }

    @PostMapping("/logout")
    public ApiResponse<Void> logout(@Valid @RequestBody RefreshTokenRequest request) {
        authService.logout(request.refreshToken());
        return ApiResponse.<Void>ok("Logged out", null);
    }

    @PostMapping("/logout-all")
    public ApiResponse<Void> logoutAll(@AuthenticationPrincipal Jwt jwt) {
        authService.logoutAll(Long.valueOf(jwt.getSubject()));
        return ApiResponse.<Void>ok("Logged out from all devices", null);
    }

    @PostMapping("/verify-email")
    public ApiResponse<Void> verifyEmail(@Valid @RequestBody VerifyEmailRequest request) {
        emailVerificationService.verify(request.token());
        return ApiResponse.<Void>ok("Email verified", null);
    }

    @PostMapping("/resend-verification")
    public ApiResponse<Void> resendVerification(@AuthenticationPrincipal Jwt jwt) {
        emailVerificationService.resend(Long.valueOf(jwt.getSubject()));
        return ApiResponse.<Void>ok("Verification email sent", null);
    }

    @PostMapping("/forgot-password")
    public ApiResponse<Void> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request,
                                            HttpServletRequest http) {
        passwordService.requestReset(request.email(), http.getRemoteAddr());
        return ApiResponse.<Void>ok("If an account exists for this email, a reset link has been sent", null);
    }

    @PostMapping("/reset-password")
    public ApiResponse<Void> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        passwordService.reset(request.token(), request.newPassword());
        return ApiResponse.<Void>ok("Password updated. Please log in again.", null);
    }

    @PostMapping("/change-password")
    public ApiResponse<Void> changePassword(@AuthenticationPrincipal Jwt jwt,
                                            @Valid @RequestBody ChangePasswordRequest request) {
        passwordService.change(Long.valueOf(jwt.getSubject()), request.currentPassword(), request.newPassword());
        return ApiResponse.<Void>ok("Password changed. Please log in again.", null);
    }
}