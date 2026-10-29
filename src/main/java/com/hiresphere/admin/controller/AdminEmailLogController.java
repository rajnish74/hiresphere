package com.hiresphere.admin.controller;

import com.hiresphere.common.dto.ApiResponse;
import com.hiresphere.common.dto.PageResponse;
import com.hiresphere.notification.dto.EmailLogResponse;
import com.hiresphere.notification.entity.EmailStatus;
import com.hiresphere.notification.service.EmailLogService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/admin/email-logs")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class AdminEmailLogController {

    private final EmailLogService emailLogService;

    @GetMapping
    public ApiResponse<PageResponse<EmailLogResponse>> list(
            @RequestParam(required = false) EmailStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.ok(emailLogService.list(status, page, size));
    }
}