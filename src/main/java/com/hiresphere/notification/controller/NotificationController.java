package com.hiresphere.notification.controller;

import com.hiresphere.common.dto.ApiResponse;
import com.hiresphere.common.dto.PageResponse;
import com.hiresphere.notification.dto.NotificationResponse;
import com.hiresphere.notification.dto.ReadAllResponse;
import com.hiresphere.notification.dto.UnreadCountResponse;
import com.hiresphere.notification.service.InAppNotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Notifications", description = "Your in-app notifications (the bell)")
@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final InAppNotificationService service;

    @Operation(summary = "List my notifications", description = "Newest first. unread=true returns only unread ones")
    @GetMapping
    public ApiResponse<PageResponse<NotificationResponse>> list(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam(defaultValue = "false") boolean unread,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.ok(service.list(userId(jwt), unread, page, size));
    }

    @Operation(summary = "Unread count", description = "Cheap call meant for polling the bell badge")
    @GetMapping("/unread-count")
    public ApiResponse<UnreadCountResponse> unreadCount(@AuthenticationPrincipal Jwt jwt) {
        return ApiResponse.ok(new UnreadCountResponse(service.unreadCount(userId(jwt))));
    }

    @Operation(summary = "Mark one notification as read", description = "Safe to repeat. 404 if it is not yours")
    @PostMapping("/{id}/read")
    public ApiResponse<Void> markRead(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id) {
        service.markRead(userId(jwt), id);
        return ApiResponse.<Void>ok("Marked as read", null);
    }

    @Operation(summary = "Mark all notifications as read")
    @PostMapping("/read-all")
    public ApiResponse<ReadAllResponse> markAllRead(@AuthenticationPrincipal Jwt jwt) {
        return ApiResponse.ok(new ReadAllResponse(service.markAllRead(userId(jwt))));
    }

    private Long userId(Jwt jwt) {
        return Long.valueOf(jwt.getSubject());
    }
}