package com.hiresphere.job.controller;

import com.hiresphere.common.dto.ApiResponse;
import com.hiresphere.common.dto.PageResponse;
import com.hiresphere.job.dto.MyJobReportResponse;
import com.hiresphere.job.service.JobReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/candidates/me/job-reports")
@PreAuthorize("hasRole('CANDIDATE')")
@RequiredArgsConstructor
public class CandidateJobReportController {

    private final JobReportService reportService;

    @GetMapping
    public ApiResponse<PageResponse<MyJobReportResponse>> mine(@AuthenticationPrincipal Jwt jwt,
                                                               @RequestParam(defaultValue = "0") int page,
                                                               @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.ok(reportService.listMine(Long.valueOf(jwt.getSubject()), page, size));
    }
}