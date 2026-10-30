package com.hiresphere.admin.controller;

import com.hiresphere.admin.service.AdminActionService;
import com.hiresphere.common.dto.ApiResponse;
import com.hiresphere.common.dto.PageResponse;
import com.hiresphere.job.dto.AdminJobSummary;
import com.hiresphere.job.entity.JobStatus;
import com.hiresphere.job.service.JobModerationService;
import com.hiresphere.recruiter.dto.ReasonRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/admin/jobs")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class AdminJobController {

    private final JobModerationService moderationService;
    private final AdminActionService actions;

    @GetMapping
    public ApiResponse<PageResponse<AdminJobSummary>> list(
            @RequestParam(required = false) Boolean hidden,
            @RequestParam(required = false) JobStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.ok(moderationService.list(hidden, status, page, size));
    }

    @PostMapping("/{id}/hide")
    public ApiResponse<AdminJobSummary> hide(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id,
                                             @Valid @RequestBody ReasonRequest request) {
        return ApiResponse.ok("Job hidden", actions.hideJob(Long.valueOf(jwt.getSubject()), id, request.reason()));
    }

    @PostMapping("/{id}/unhide")
    public ApiResponse<AdminJobSummary> unhide(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id) {
        return ApiResponse.ok("Job is visible again", actions.unhideJob(Long.valueOf(jwt.getSubject()), id));
    }
}