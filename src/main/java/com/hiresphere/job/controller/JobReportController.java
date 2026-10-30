package com.hiresphere.job.controller;

import com.hiresphere.common.dto.ApiResponse;
import com.hiresphere.job.dto.JobReportResponse;
import com.hiresphere.job.dto.ReportJobRequest;
import com.hiresphere.job.service.JobReportService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/jobs/{jobId}/reports")
@PreAuthorize("hasRole('CANDIDATE')")
@RequiredArgsConstructor
public class JobReportController {

    private final JobReportService reportService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<JobReportResponse> report(@AuthenticationPrincipal Jwt jwt, @PathVariable Long jobId,
                                                 @Valid @RequestBody ReportJobRequest request) {
        return ApiResponse.ok("Thank you. Our team will review this report.",
                reportService.report(Long.valueOf(jwt.getSubject()), jobId, request));
    }
}