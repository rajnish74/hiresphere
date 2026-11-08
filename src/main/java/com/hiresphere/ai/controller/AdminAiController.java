package com.hiresphere.ai.controller;

import com.hiresphere.ai.AiClient;
import com.hiresphere.ai.AiRequest;
import com.hiresphere.ai.AiResponse;
import com.hiresphere.ai.config.AiProperties;
import com.hiresphere.ai.skills.ExtractedSkills;
import com.hiresphere.ai.skills.ResumeSkillExtractor;
import com.hiresphere.common.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

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
    private final ResumeSkillExtractor extractor;

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


    public record TextRequest(@NotBlank @Size(max = 20000) String text) {
    }

    public record SkillsPreview(List<String> skills, int droppedCount, boolean inputTruncated, int promptTokens,
                                int outputTokens, long latencyMs) {
    }

    @Operation(summary = "Try the skill extraction on some text", description = "Costs tokens. Admin only")
    @PostMapping("/skills-preview")
    public ApiResponse<SkillsPreview> skillsPreview(@Valid @RequestBody TextRequest request) {
        ExtractedSkills r = extractor.extract(request.text());
        return ApiResponse.ok(new SkillsPreview(r.skills(), r.droppedCount(), r.inputTruncated(),
                r.ai().promptTokens(), r.ai().outputTokens(), r.ai().latencyMs()));
    }
}