package com.hiresphere.notification.controller;

import com.hiresphere.common.dto.ApiResponse;
import com.hiresphere.notification.dto.PreferenceItem;
import com.hiresphere.notification.dto.UpdatePreferencesRequest;
import com.hiresphere.notification.service.NotificationPreferenceService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/users/me/notification-preferences")
@RequiredArgsConstructor
public class NotificationPreferenceController {

    private final NotificationPreferenceService preferenceService;

    @GetMapping
    public ApiResponse<List<PreferenceItem>> get(@AuthenticationPrincipal Jwt jwt) {
        return ApiResponse.ok(preferenceService.get(Long.valueOf(jwt.getSubject())));
    }

    @PutMapping
    public ApiResponse<List<PreferenceItem>> update(@AuthenticationPrincipal Jwt jwt,
                                                    @Valid @RequestBody UpdatePreferencesRequest request) {
        return ApiResponse.ok("Preferences updated",
                preferenceService.update(Long.valueOf(jwt.getSubject()), request.categories()));
    }
}