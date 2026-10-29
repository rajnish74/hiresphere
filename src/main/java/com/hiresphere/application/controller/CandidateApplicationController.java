package com.hiresphere.application.controller;

import com.hiresphere.application.dto.*;
import com.hiresphere.application.entity.ApplicationStatus;
import com.hiresphere.application.service.JobApplicationService;
import com.hiresphere.common.dto.ApiResponse;
import com.hiresphere.common.dto.PageResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1")
@PreAuthorize("hasRole('CANDIDATE')")
@RequiredArgsConstructor
public class CandidateApplicationController {

    private final JobApplicationService service;

    @PostMapping("/jobs/{jobId}/applications")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<ApplicationDetailResponse> apply(@AuthenticationPrincipal Jwt jwt,
                                                        @PathVariable Long jobId,
                                                        @Valid @RequestBody(required = false) ApplyRequest request) {
        return ApiResponse.ok("Application submitted", service.apply(userId(jwt), jobId, request));
    }

    @GetMapping("/candidates/me/applications")
    public ApiResponse<PageResponse<ApplicationSummary>> list(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam(required = false) ApplicationStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.ok(service.listMine(userId(jwt), status, page, size));
    }

    @GetMapping("/candidates/me/applications/{id}")
    public ApiResponse<ApplicationDetailResponse> get(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id) {
        return ApiResponse.ok(service.getMine(userId(jwt), id));
    }

    @PostMapping("/candidates/me/applications/{id}/withdraw")
    public ApiResponse<ApplicationDetailResponse> withdraw(@AuthenticationPrincipal Jwt jwt,
                                                           @PathVariable Long id) {
        return ApiResponse.ok("Application withdrawn", service.withdraw(userId(jwt), id));
    }

    private Long userId(Jwt jwt) {
        return Long.valueOf(jwt.getSubject());
    }
}