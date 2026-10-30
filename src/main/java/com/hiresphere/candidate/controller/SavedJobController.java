package com.hiresphere.candidate.controller;

import com.hiresphere.candidate.dto.SavedJobResponse;
import com.hiresphere.candidate.service.SavedJobService;
import com.hiresphere.common.dto.ApiResponse;
import com.hiresphere.common.dto.PageResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/candidates/me/saved-jobs")
@PreAuthorize("hasRole('CANDIDATE')")
@RequiredArgsConstructor
public class SavedJobController {

    private final SavedJobService savedJobService;

    @PutMapping("/{jobId}")
    public ApiResponse<Void> save(@AuthenticationPrincipal Jwt jwt, @PathVariable Long jobId) {
        savedJobService.save(userId(jwt), jobId);
        return ApiResponse.<Void>ok("Job saved", null);
    }

    @DeleteMapping("/{jobId}")
    public ApiResponse<Void> remove(@AuthenticationPrincipal Jwt jwt, @PathVariable Long jobId) {
        savedJobService.remove(userId(jwt), jobId);
        return ApiResponse.<Void>ok("Job removed from saved jobs", null);
    }

    @GetMapping
    public ApiResponse<PageResponse<SavedJobResponse>> list(@AuthenticationPrincipal Jwt jwt,
                                                            @RequestParam(defaultValue = "0") int page,
                                                            @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.ok(savedJobService.list(userId(jwt), page, size));
    }

    @GetMapping("/ids")
    public ApiResponse<List<Long>> ids(@AuthenticationPrincipal Jwt jwt) {
        return ApiResponse.ok(savedJobService.savedJobIds(userId(jwt)));
    }

    private Long userId(Jwt jwt) {
        return Long.valueOf(jwt.getSubject());
    }
}