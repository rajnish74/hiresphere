package com.hiresphere.candidate.controller;

import com.hiresphere.candidate.dto.ResumeInfo;
import com.hiresphere.candidate.service.ResumeService;
import com.hiresphere.common.dto.ApiResponse;
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
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;

@RestController
@RequestMapping("/api/v1/candidates/me/resume")
@PreAuthorize("hasRole('CANDIDATE')")
@RequiredArgsConstructor
public class ResumeController {

    private final ResumeService resumeService;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<ResumeInfo> upload(@AuthenticationPrincipal Jwt jwt,
                                          @RequestParam("file") MultipartFile file) {
        return ApiResponse.ok("Resume uploaded", resumeService.upload(Long.valueOf(jwt.getSubject()), file));
    }

    @GetMapping
    public ResponseEntity<Resource> download(@AuthenticationPrincipal Jwt jwt) {
        ResumeService.ResumeDownload download = resumeService.download(Long.valueOf(jwt.getSubject()));
        ContentDisposition disposition = ContentDisposition.attachment()
                .filename(download.filename(), StandardCharsets.UTF_8)
                .build();
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
                .body(download.resource());
    }

    @DeleteMapping
    public ApiResponse<Void> delete(@AuthenticationPrincipal Jwt jwt) {
        resumeService.delete(Long.valueOf(jwt.getSubject()));
        return ApiResponse.<Void>ok("Resume deleted", null);
    }
}