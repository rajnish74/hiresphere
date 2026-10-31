package com.hiresphere.application.controller;

import com.hiresphere.application.dto.ProfileViewSummary;
import com.hiresphere.application.service.ProfileViewService;
import com.hiresphere.common.dto.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/candidates/me/profile-views")
@PreAuthorize("hasRole('CANDIDATE')")
@RequiredArgsConstructor
public class CandidateProfileViewController {

    private final ProfileViewService profileViewService;

    @GetMapping
    public ApiResponse<ProfileViewSummary> summary(@AuthenticationPrincipal Jwt jwt) {
        return ApiResponse.ok(profileViewService.summary(Long.valueOf(jwt.getSubject())));
    }
}