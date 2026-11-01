package com.hiresphere.application.controller;

import com.hiresphere.application.dto.JobFlags;
import com.hiresphere.application.service.JobFlagsService;
import com.hiresphere.common.dto.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/candidates/me/job-flags")
@PreAuthorize("hasRole('CANDIDATE')")
@RequiredArgsConstructor
public class CandidateJobFlagsController {

    private final JobFlagsService flagsService;

    @GetMapping
    public ApiResponse<Map<Long, JobFlags>> flags(@AuthenticationPrincipal Jwt jwt,
                                                  @RequestParam List<Long> jobIds) {
        return ApiResponse.ok(flagsService.flags(Long.valueOf(jwt.getSubject()), jobIds));
    }
}