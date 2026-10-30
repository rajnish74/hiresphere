package com.hiresphere.admin.controller;

import com.hiresphere.admin.dto.PlatformStats;
import com.hiresphere.admin.service.AdminStatsService;
import com.hiresphere.common.dto.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/stats")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class AdminStatsController {

    private final AdminStatsService statsService;

    @GetMapping
    public ApiResponse<PlatformStats> stats() {
        return ApiResponse.ok(statsService.get());
    }
}