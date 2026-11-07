package com.hiresphere.notification.controller;

import com.hiresphere.common.dto.ApiResponse;
import com.hiresphere.notification.dto.StreamTicketResponse;
import com.hiresphere.notification.stream.NotificationStreamHub;
import com.hiresphere.notification.stream.StreamTicketService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
public class NotificationStreamController {

    private final StreamTicketService tickets;
    private final NotificationStreamHub hub;

    @PostMapping("/stream-ticket")
    public ApiResponse<StreamTicketResponse> ticket(@AuthenticationPrincipal Jwt jwt) {
        String ticket = tickets.issue(Long.valueOf(jwt.getSubject()));
        return ApiResponse.ok(new StreamTicketResponse(ticket, (int) StreamTicketService.TTL.toSeconds()));
    }

    @GetMapping("/stream")
    public ResponseEntity<SseEmitter> stream(@RequestParam(required = false) String ticket) {
        return tickets.redeem(ticket)
                .map(userId -> ResponseEntity.ok()
                        .cacheControl(CacheControl.noStore())
                        .header("X-Accel-Buffering", "no")   // nginx SSE ko buffer na kare
                        .body(hub.register(userId)))
                .orElseGet(() -> ResponseEntity.status(HttpStatus.UNAUTHORIZED).build());
    }
}