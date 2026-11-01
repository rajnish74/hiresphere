package com.hiresphere.application.controller;

import com.hiresphere.application.dto.CandidateDashboard;
import com.hiresphere.application.service.CandidateDashboardService;
import com.hiresphere.common.dto.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/candidates/me/dashboard")
@PreAuthorize("hasRole('CANDIDATE')")
@RequiredArgsConstructor
public class CandidateDashboardController {

    private final CandidateDashboardService dashboardService;

    @GetMapping
    public ApiResponse<CandidateDashboard> dashboard(@AuthenticationPrincipal Jwt jwt) {
        return ApiResponse.ok(dashboardService.get(Long.valueOf(jwt.getSubject())));
    }
}