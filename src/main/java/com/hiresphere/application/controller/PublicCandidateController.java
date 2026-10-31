package com.hiresphere.application.controller;

import com.hiresphere.application.service.CandidateAccessService;
import com.hiresphere.candidate.dto.PublicCandidateView;
import com.hiresphere.common.dto.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/public/candidates")
@RequiredArgsConstructor
public class PublicCandidateController {

    private final CandidateAccessService accessService;

    @GetMapping("/{userId}")
    public ResponseEntity<ApiResponse<PublicCandidateView>> view(@PathVariable Long userId,
                                                                 HttpServletRequest http) {
        return ResponseEntity.ok()
                .header("X-Robots-Tag", "noindex, nofollow")
                .body(ApiResponse.ok(accessService.viewPublic(userId, http.getRemoteAddr())));
    }
}