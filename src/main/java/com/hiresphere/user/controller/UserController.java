package com.hiresphere.user.controller;

import com.hiresphere.common.dto.ApiResponse;
import com.hiresphere.user.dto.UpdateMeRequest;
import com.hiresphere.user.dto.UserResponse;
import com.hiresphere.user.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping("/me")
    public ApiResponse<UserResponse> me(@AuthenticationPrincipal Jwt jwt) {
        return ApiResponse.ok(userService.getById(Long.valueOf(jwt.getSubject())));
    }

    @PutMapping("/me")
    public ApiResponse<UserResponse> updateMe(@AuthenticationPrincipal Jwt jwt,
                                              @Valid @RequestBody UpdateMeRequest request) {
        return ApiResponse.ok("Account updated", userService.updateMe(Long.valueOf(jwt.getSubject()), request));
    }
}