package com.hiresphere.application.controller;

import com.hiresphere.application.service.CandidateAccessService;
import com.hiresphere.candidate.dto.CandidateProfileView;
import com.hiresphere.common.dto.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/recruiter/candidates")
@PreAuthorize("hasRole('RECRUITER')")
@RequiredArgsConstructor
public class RecruiterCandidateController {

    private final CandidateAccessService accessService;

    @GetMapping("/{userId}")
    public ApiResponse<CandidateProfileView> view(@AuthenticationPrincipal Jwt jwt, @PathVariable Long userId) {
        return ApiResponse.ok(accessService.viewProfile(Long.valueOf(jwt.getSubject()), userId));
    }
}