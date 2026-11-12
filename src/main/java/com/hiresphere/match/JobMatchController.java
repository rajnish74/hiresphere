package com.hiresphere.match;

import com.hiresphere.common.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Job match", description = "How well your profile fits a job, and why")
@RestController
@RequestMapping("/api/v1/candidates/me/job-matches")
@PreAuthorize("hasRole('CANDIDATE')")
@RequiredArgsConstructor
public class JobMatchController {

    private final JobMatchService service;

    @Operation(summary = "How well do I match this job?",
            description = "The score and breakdown are calculated by code and are free. explain=true asks the AI "
                    + "to put the breakdown into words (it only sees skill names, years and cities, never your "
                    + "resume); without it you get a fixed-wording explanation.")
    @GetMapping("/{jobId}")
    public ApiResponse<JobMatchResponse> match(@AuthenticationPrincipal Jwt jwt, @PathVariable Long jobId,
                                               @RequestParam(defaultValue = "false") boolean explain) {
        return ApiResponse.ok(service.match(Long.valueOf(jwt.getSubject()), jobId, explain));
    }
}