package com.hiresphere.admin.controller;

import com.hiresphere.admin.service.AdminActionService;
import com.hiresphere.common.dto.ApiResponse;
import com.hiresphere.common.dto.PageResponse;
import com.hiresphere.job.dto.AdminJobReportResponse;
import com.hiresphere.job.dto.JobReportOutcome;
import com.hiresphere.job.dto.ResolveReportRequest;
import com.hiresphere.job.entity.JobReportStatus;
import com.hiresphere.job.service.JobReportService;
import com.hiresphere.recruiter.dto.ReasonRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/admin/job-reports")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class AdminJobReportController {

    private final JobReportService reportService;
    private final AdminActionService actions;

    @GetMapping
    public ApiResponse<PageResponse<AdminJobReportResponse>> list(
            @RequestParam(defaultValue = "OPEN") JobReportStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.ok(reportService.list(status, page, size));
    }

    @PostMapping("/{id}/resolve")
    public ApiResponse<JobReportOutcome> resolve(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id,
                                                 @Valid @RequestBody ResolveReportRequest request) {
        return ApiResponse.ok("Report resolved",
                actions.resolveJobReport(Long.valueOf(jwt.getSubject()), id, request.note(), request.hideJob()));
    }

    @PostMapping("/{id}/dismiss")
    public ApiResponse<JobReportOutcome> dismiss(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id,
                                                 @Valid @RequestBody ReasonRequest request) {
        return ApiResponse.ok("Report dismissed",
                actions.dismissJobReport(Long.valueOf(jwt.getSubject()), id, request.reason()));
    }
}