package com.hiresphere.candidate.controller;

import com.hiresphere.candidate.dto.*;
import com.hiresphere.candidate.service.CandidateProfileService;
import com.hiresphere.candidate.service.ProfileSectionService;
import com.hiresphere.common.dto.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/candidates/me")
@PreAuthorize("hasRole('CANDIDATE')")
@RequiredArgsConstructor
public class CandidateProfileController {

    private final CandidateProfileService profileService;
    private final ProfileSectionService sectionService;

    @GetMapping
    public ApiResponse<ProfileResponse> getMyProfile(@AuthenticationPrincipal Jwt jwt) {
        return ApiResponse.ok(profileService.getMyProfile(userId(jwt)));
    }

    @PutMapping
    public ApiResponse<ProfileResponse> updateMyProfile(@AuthenticationPrincipal Jwt jwt,
                                                        @Valid @RequestBody ProfileUpdateRequest request) {
        return ApiResponse.ok("Profile updated", profileService.updateMyProfile(userId(jwt), request));
    }

    @PutMapping("/skills")
    public ApiResponse<ProfileResponse> replaceSkills(@AuthenticationPrincipal Jwt jwt,
                                                      @Valid @RequestBody SkillsRequest request) {
        return ApiResponse.ok("Skills updated", profileService.replaceSkills(userId(jwt), request));
    }

    // ----- Education -----

    @PostMapping("/education")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<EducationResponse> addEducation(@AuthenticationPrincipal Jwt jwt,
                                                       @Valid @RequestBody EducationRequest request) {
        return ApiResponse.ok("Education added", sectionService.addEducation(userId(jwt), request));
    }

    @PutMapping("/education/{id}")
    public ApiResponse<EducationResponse> updateEducation(@AuthenticationPrincipal Jwt jwt,
                                                          @PathVariable Long id,
                                                          @Valid @RequestBody EducationRequest request) {
        return ApiResponse.ok("Education updated", sectionService.updateEducation(userId(jwt), id, request));
    }

    @DeleteMapping("/education/{id}")
    public ApiResponse<Void> deleteEducation(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id) {
        sectionService.deleteEducation(userId(jwt), id);
        return ApiResponse.<Void>ok("Education deleted", null);
    }

    // ----- Experience -----

    @PostMapping("/experience")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<ExperienceResponse> addExperience(@AuthenticationPrincipal Jwt jwt,
                                                         @Valid @RequestBody ExperienceRequest request) {
        return ApiResponse.ok("Experience added", sectionService.addExperience(userId(jwt), request));
    }

    @PutMapping("/experience/{id}")
    public ApiResponse<ExperienceResponse> updateExperience(@AuthenticationPrincipal Jwt jwt,
                                                            @PathVariable Long id,
                                                            @Valid @RequestBody ExperienceRequest request) {
        return ApiResponse.ok("Experience updated", sectionService.updateExperience(userId(jwt), id, request));
    }

    @DeleteMapping("/experience/{id}")
    public ApiResponse<Void> deleteExperience(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id) {
        sectionService.deleteExperience(userId(jwt), id);
        return ApiResponse.<Void>ok("Experience deleted", null);
    }

    private Long userId(Jwt jwt) {
        return Long.valueOf(jwt.getSubject());
    }
}