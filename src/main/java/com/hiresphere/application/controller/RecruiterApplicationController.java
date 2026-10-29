package com.hiresphere.application.controller;

import com.hiresphere.application.dto.RecruiterApplicationResponse;
import com.hiresphere.application.dto.RecruiterApplicationSummary;
import com.hiresphere.application.dto.StatusChangeRequest;
import com.hiresphere.application.entity.ApplicationStatus;
import com.hiresphere.application.service.JobApplicationService;
import com.hiresphere.common.dto.ApiResponse;
import com.hiresphere.common.dto.PageResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/recruiter")
@PreAuthorize("hasRole('RECRUITER')")
@RequiredArgsConstructor
public class RecruiterApplicationController {

    private final JobApplicationService service;

    @GetMapping("/jobs/{jobId}/applications")
    public ApiResponse<PageResponse<RecruiterApplicationSummary>> list(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Long jobId,
            @RequestParam(required = false) ApplicationStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.ok(service.listForJob(userId(jwt), jobId, status, page, size));
    }

    @GetMapping("/applications/{id}")
    public ApiResponse<RecruiterApplicationResponse> get(@AuthenticationPrincipal Jwt jwt,
                                                         @PathVariable Long id) {
        return ApiResponse.ok(service.getForRecruiter(userId(jwt), id));
    }

    @PatchMapping("/applications/{id}/status")
    public ApiResponse<RecruiterApplicationResponse> changeStatus(@AuthenticationPrincipal Jwt jwt,
                                                                  @PathVariable Long id,
                                                                  @Valid @RequestBody StatusChangeRequest request) {
        return ApiResponse.ok("Status updated", service.changeStatus(userId(jwt), id, request));
    }

    @GetMapping("/applications/{id}/resume")
    public ResponseEntity<Resource> resume(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id) {
        JobApplicationService.ResumeDownload download = service.downloadResume(userId(jwt), id);
        ContentDisposition disposition = ContentDisposition.attachment()
                .filename(download.filename(), StandardCharsets.UTF_8)
                .build();
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
                .body(download.resource());
    }

    private Long userId(Jwt jwt) {
        return Long.valueOf(jwt.getSubject());
    }

    @GetMapping("/jobs/{jobId}/applications/summary")
    public ApiResponse<Map<ApplicationStatus, Long>> summary(@AuthenticationPrincipal Jwt jwt,
                                                             @PathVariable Long jobId) {
        return ApiResponse.ok(service.statusCounts(userId(jwt), jobId));
    }
}