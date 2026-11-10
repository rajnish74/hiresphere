package com.hiresphere.ai.controller;

import com.hiresphere.ai.resume.ResumeSkillSuggestionService;
import com.hiresphere.ai.resume.ResumeSkillSuggestions;
import com.hiresphere.ai.resume.ResumeSkillsRequest;
import com.hiresphere.common.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Candidate AI", description = "AI help for your own profile")
@RestController
@RequestMapping("/api/v1/candidates/me/ai")
@PreAuthorize("hasRole('CANDIDATE')")
@RequiredArgsConstructor
public class CandidateResumeAiController {

    private final ResumeSkillSuggestionService service;

    @Operation(summary = "Suggest skills from my uploaded resume",
            description = "Reads your resume PDF, removes contact details and asks the AI which skills it mentions. "
                    + "Nothing is saved: you choose which ones to add. consent must be true")
    @PostMapping("/resume-skills")
    public ApiResponse<ResumeSkillSuggestions> resumeSkills(@AuthenticationPrincipal Jwt jwt,
                                                            @Valid @RequestBody ResumeSkillsRequest request) {
        return ApiResponse.ok(service.suggest(Long.valueOf(jwt.getSubject())));
    }
}