package com.hiresphere.job.controller;

import com.hiresphere.common.dto.ApiResponse;
import com.hiresphere.common.dto.PageResponse;
import com.hiresphere.job.dto.JobRequest;
import com.hiresphere.job.dto.JobResponse;
import com.hiresphere.job.dto.JobSummaryResponse;
import com.hiresphere.job.entity.JobStatus;
import com.hiresphere.job.service.JobService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/recruiter/jobs")
@PreAuthorize("hasRole('RECRUITER')")
@RequiredArgsConstructor
public class RecruiterJobController {

    private final JobService jobService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<JobResponse> create(@AuthenticationPrincipal Jwt jwt,
                                           @Valid @RequestBody JobRequest request) {
        return ApiResponse.ok("Job saved as draft", jobService.create(userId(jwt), request));
    }

    @GetMapping
    public ApiResponse<PageResponse<JobSummaryResponse>> list(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam(required = false) JobStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.ok(jobService.listMine(userId(jwt), status, page, size));
    }

    @GetMapping("/{id}")
    public ApiResponse<JobResponse> get(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id) {
        return ApiResponse.ok(jobService.getMine(userId(jwt), id));
    }

    @PutMapping("/{id}")
    public ApiResponse<JobResponse> update(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id,
                                           @Valid @RequestBody JobRequest request) {
        return ApiResponse.ok("Job updated", jobService.update(userId(jwt), id, request));
    }

    /** DRAFT ya CLOSED job ko PUBLISHED banata hai (reopen bhi yahi hai). */
    @PostMapping("/{id}/publish")
    public ApiResponse<JobResponse> publish(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id) {
        return ApiResponse.ok("Job published", jobService.publish(userId(jwt), id));
    }

    @PostMapping("/{id}/close")
    public ApiResponse<JobResponse> close(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id) {
        return ApiResponse.ok("Job closed", jobService.close(userId(jwt), id));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id) {
        jobService.delete(userId(jwt), id);
        return ApiResponse.<Void>ok("Job deleted", null);
    }

    private Long userId(Jwt jwt) {
        return Long.valueOf(jwt.getSubject());
    }
}