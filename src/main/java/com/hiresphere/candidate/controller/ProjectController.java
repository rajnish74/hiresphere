package com.hiresphere.candidate.controller;

import com.hiresphere.candidate.dto.ProjectRequest;
import com.hiresphere.candidate.dto.ProjectResponse;
import com.hiresphere.candidate.service.ProjectService;
import com.hiresphere.common.dto.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/candidates/me/projects")
@PreAuthorize("hasRole('CANDIDATE')")
@RequiredArgsConstructor
public class ProjectController {

    private final ProjectService projectService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<ProjectResponse> add(@AuthenticationPrincipal Jwt jwt,
                                            @Valid @RequestBody ProjectRequest request) {
        return ApiResponse.ok("Project added", projectService.add(Long.valueOf(jwt.getSubject()), request));
    }

    @PutMapping("/{id}")
    public ApiResponse<ProjectResponse> update(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id,
                                               @Valid @RequestBody ProjectRequest request) {
        return ApiResponse.ok("Project updated",
                projectService.update(Long.valueOf(jwt.getSubject()), id, request));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id) {
        projectService.delete(Long.valueOf(jwt.getSubject()), id);
        return ApiResponse.<Void>ok("Project deleted", null);
    }
}