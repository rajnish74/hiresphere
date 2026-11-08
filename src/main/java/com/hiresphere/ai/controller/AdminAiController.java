package com.hiresphere.ai.controller;

import com.hiresphere.ai.AiClient;
import com.hiresphere.ai.AiRequest;
import com.hiresphere.ai.AiResponse;
import com.hiresphere.ai.config.AiProperties;
import com.hiresphere.ai.resilience.AiBulkhead;
import com.hiresphere.ai.resilience.AiCircuitBreaker;
import com.hiresphere.ai.skills.SkillSuggestionService;
import com.hiresphere.ai.skills.SkillSuggestions;
import com.hiresphere.common.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "Admin AI", description = "Check that the AI provider is configured, reachable and healthy")
@RestController
@RequestMapping("/api/v1/admin/ai")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class AdminAiController {

    private static final String SYSTEM = "You are a health check. Answer with exactly one lowercase word.";
    private static final String PROMPT = "Reply with the word: pong";

    private final AiProperties properties;
    private final AiClient client;
    private final SkillSuggestionService suggestions;
    private final ObjectProvider<AiCircuitBreaker> breakers;
    private final ObjectProvider<AiBulkhead> bulkheads;

    public record AiStatus(boolean enabled, String provider, String model, boolean keyConfigured) {
    }

    public record AiPing(String reply, String model, String finishReason, int promptTokens, int outputTokens,
                         int totalTokens, long latencyMs) {
    }

    public record TextRequest(@NotBlank @Size(max = 20000) String text) {
    }

    public record SkillsPreview(List<String> skills, String source, boolean degraded, String degradedReason,
                                int droppedCount, boolean inputTruncated, int promptTokens, int outputTokens,
                                long latencyMs) {
    }

    public record AiHealth(boolean enabled, String circuit, int consecutiveFailures, long retryInSeconds,
                           int concurrencyLimit, int freeSlots) {
    }

    @Operation(summary = "AI settings", description = "Never returns the key, only whether one is set")
    @GetMapping("/status")
    public ApiResponse<AiStatus> status() {
        AiProperties.Gemini gemini = properties.gemini();
        return ApiResponse.ok(new AiStatus(properties.enabled(), "gemini",
                gemini.model().isEmpty() ? null : gemini.model(), gemini.hasKey()));
    }

    @Operation(summary = "Circuit breaker and concurrency state",
            description = "circuit is CLOSED (normal), OPEN (calls refused for a while) or HALF_OPEN (one probe call)")
    @GetMapping("/health")
    public ApiResponse<AiHealth> health() {
        AiCircuitBreaker breaker = breakers.getIfAvailable();
        AiBulkhead bulkhead = bulkheads.getIfAvailable();
        if (breaker == null || bulkhead == null) {
            return ApiResponse.ok(new AiHealth(false, "DISABLED", 0, 0, 0, 0));
        }
        long retryIn = (breaker.remainingOpen().toMillis() + 999) / 1000;   // upar ki taraf
        return ApiResponse.ok(new AiHealth(true, breaker.state().name(), breaker.consecutiveFailures(), retryIn,
                bulkhead.max(), bulkhead.available()));
    }

    @Operation(summary = "Send one tiny request to the AI", description = "Costs a few tokens. Admin only")
    @PostMapping("/ping")
    public ApiResponse<AiPing> ping() {
        AiResponse r = client.generate(AiRequest.text(SYSTEM, PROMPT));
        return ApiResponse.ok(new AiPing(r.text().trim(), r.model(), r.finishReason(), r.promptTokens(),
                r.outputTokens(), r.totalTokens(), r.latencyMs()));
    }

    @Operation(summary = "Try the skill suggestions on some text",
            description = "Uses the AI, and falls back to keyword matching when the AI is not available")
    @PostMapping("/skills-preview")
    public ApiResponse<SkillsPreview> skillsPreview(@Valid @RequestBody TextRequest request) {
        SkillSuggestions s = suggestions.suggest(request.text());
        AiResponse ai = s.ai();
        return ApiResponse.ok(new SkillsPreview(s.skills(), s.source().name(), s.degraded(), s.degradedReason(),
                s.droppedCount(), s.inputTruncated(), ai == null ? 0 : ai.promptTokens(),
                ai == null ? 0 : ai.outputTokens(), ai == null ? 0 : ai.latencyMs()));
    }
}