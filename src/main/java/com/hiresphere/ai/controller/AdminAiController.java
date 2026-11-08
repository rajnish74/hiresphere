package com.hiresphere.ai.controller;

import com.hiresphere.ai.AiClient;
import com.hiresphere.ai.AiRequest;
import com.hiresphere.ai.AiResponse;
import com.hiresphere.ai.config.AiProperties;
import com.hiresphere.common.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Admin AI", description = "Check that the AI provider is configured and reachable")
@RestController
@RequestMapping("/api/v1/admin/ai")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class AdminAiController {

    private static final String SYSTEM = "You are a health check. Answer with exactly one lowercase word.";
    private static final String PROMPT = "Reply with the word: pong";

    private final AiProperties properties;
    private final AiClient client;

    public record AiStatus(boolean enabled, String provider, String model, boolean keyConfigured) {
    }

    public record AiPing(String reply, String model, String finishReason, int promptTokens, int outputTokens,
                         int totalTokens, long latencyMs) {
    }

    @Operation(summary = "AI settings", description = "Never returns the key, only whether one is set")
    @GetMapping("/status")
    public ApiResponse<AiStatus> status() {
        AiProperties.Gemini gemini = properties.gemini();
        return ApiResponse.ok(new AiStatus(properties.enabled(), "gemini",
                gemini.model().isEmpty() ? null : gemini.model(), gemini.hasKey()));
    }

    @Operation(summary = "Send one tiny request to the AI", description = "Costs a few tokens. Admin only")
    @PostMapping("/ping")
    public ApiResponse<AiPing> ping() {
        AiResponse r = client.generate(AiRequest.text(SYSTEM, PROMPT));
        return ApiResponse.ok(new AiPing(r.text().trim(), r.model(), r.finishReason(), r.promptTokens(),
                r.outputTokens(), r.totalTokens(), r.latencyMs()));
    }
}