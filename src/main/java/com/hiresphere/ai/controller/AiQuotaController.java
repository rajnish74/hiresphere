package com.hiresphere.ai.controller;

import com.hiresphere.ai.usage.AiQuotaService;
import com.hiresphere.common.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "AI", description = "Your AI allowance")
@RestController
@RequestMapping("/api/v1/ai")
@RequiredArgsConstructor
public class AiQuotaController {

    private final AiQuotaService quotas;

    @Operation(summary = "My AI allowance for today", description = "Tokens used, tokens left and when it resets (UTC)")
    @GetMapping("/quota")
    public ApiResponse<AiQuotaService.Quota> quota(@AuthenticationPrincipal Jwt jwt) {
        return ApiResponse.ok(quotas.quota(Long.valueOf(jwt.getSubject())));
    }
}