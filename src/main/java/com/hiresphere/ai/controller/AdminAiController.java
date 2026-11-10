package com.hiresphere.ai.controller;

import com.hiresphere.ai.AiCallContext;
import com.hiresphere.ai.AiClient;
import com.hiresphere.ai.AiRequest;
import com.hiresphere.ai.AiResponse;
import com.hiresphere.ai.config.AiLimitsProperties;
import com.hiresphere.ai.config.AiProperties;
import com.hiresphere.ai.metrics.AiMetrics;
import com.hiresphere.ai.resilience.AiBulkhead;
import com.hiresphere.ai.resilience.AiCircuitBreaker;
import com.hiresphere.ai.skills.SkillSuggestionService;
import com.hiresphere.ai.skills.SkillSuggestions;
import com.hiresphere.ai.usage.AiUsageStore;
import com.hiresphere.ai.usage.FeatureUsage;
import com.hiresphere.common.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Tag(name = "Admin AI", description = "Check that the AI provider is configured, reachable, healthy and what it costs")
@RestController
@RequestMapping("/api/v1/admin/ai")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class AdminAiController {

    private static final String SYSTEM = "You are a health check. Answer with exactly one lowercase word.";
    private static final String PROMPT = "Reply with the word: pong";

    private final AiProperties properties;
    private final AiLimitsProperties limits;
    private final AiClient client;
    private final SkillSuggestionService suggestions;
    private final AiUsageStore usage;
    private final ObjectProvider<AiCircuitBreaker> breakers;
    private final ObjectProvider<AiBulkhead> bulkheads;
    private final AiMetrics metrics;

    public record AiStatus(boolean enabled, String provider, String model, boolean keyConfigured) {
    }

    public record AiPing(String reply, String model, String finishReason, int promptTokens, int outputTokens,
                         int totalTokens, long latencyMs) {
    }

    public record TextRequest(@NotBlank @Size(max = 20000) String text) {
    }

    public record SkillsPreview(List<String> skills, String source, boolean degraded, String degradedReason,
                                boolean cached, int droppedCount, boolean inputTruncated, int promptTokens,
                                int outputTokens, long latencyMs) {
    }

    public record AiHealth(boolean enabled, String circuit, int consecutiveFailures, long retryInSeconds,
                           int concurrencyLimit, int freeSlots) {
    }

    public record UsageReport(int days, long tokensToday, long globalDailyLimit, List<FeatureUsage> byFeature) {
    }

    @Operation(summary = "AI settings", description = "Never returns the key, only whether one is set")
    @GetMapping("/status")
    public ApiResponse<AiStatus> status() {
        AiProperties.Gemini gemini = properties.gemini();
        return ApiResponse.ok(new AiStatus(properties.enabled(), "gemini",
                gemini.model().isEmpty() ? null : gemini.model(), gemini.hasKey()));
    }

    @Operation(summary = "Circuit breaker and concurrency state")
    @GetMapping("/health")
    public ApiResponse<AiHealth> health() {
        AiCircuitBreaker breaker = breakers.getIfAvailable();
        AiBulkhead bulkhead = bulkheads.getIfAvailable();
        if (breaker == null || bulkhead == null) {
            return ApiResponse.ok(new AiHealth(false, "DISABLED", 0, 0, 0, 0));
        }
        long retryIn = (breaker.remainingOpen().toMillis() + 999) / 1000;
        return ApiResponse.ok(new AiHealth(true, breaker.state().name(), breaker.consecutiveFailures(), retryIn,
                bulkhead.max(), bulkhead.available()));
    }

    @Operation(summary = "AI usage by feature",
            description = "Calls, tokens and failures for the last N days (1 to 90). Only metadata, never any text")
    @GetMapping("/usage")
    public ApiResponse<UsageReport> usage(@RequestParam(defaultValue = "7") int days) {
        int clamped = Math.min(Math.max(days, 1), 90);
        Instant since = usage.startOfToday().minus(clamped - 1L, ChronoUnit.DAYS);
        return ApiResponse.ok(new UsageReport(clamped, usage.tokensUsedTodayByEveryone(), limits.globalDailyTokens(),
                usage.summarySince(since)));
    }

    @Operation(summary = "Send one tiny request to the AI", description = "Costs a few tokens. Admin only")
    @PostMapping("/ping")
    public ApiResponse<AiPing> ping(@AuthenticationPrincipal Jwt jwt) {
        AiResponse r = AiCallContext.with("HEALTH_CHECK", Long.valueOf(jwt.getSubject()),
                () -> client.generate(AiRequest.text(SYSTEM, PROMPT)));
        return ApiResponse.ok(new AiPing(r.text().trim(), r.model(), r.finishReason(), r.promptTokens(),
                r.outputTokens(), r.totalTokens(), r.latencyMs()));
    }

    @Operation(summary = "Try the skill suggestions on some text",
            description = "Uses the AI, a cache, and falls back to keyword matching when the AI is not available")
    @PostMapping("/skills-preview")
    public ApiResponse<SkillsPreview> skillsPreview(@AuthenticationPrincipal Jwt jwt,
                                                    @Valid @RequestBody TextRequest request) {
        SkillSuggestions s = suggestions.suggest(Long.valueOf(jwt.getSubject()), request.text());
        AiResponse ai = s.ai();
        return ApiResponse.ok(new SkillsPreview(s.skills(), s.source().name(), s.degraded(), s.degradedReason(),
                s.cached(), s.droppedCount(), s.inputTruncated(), ai == null ? 0 : ai.promptTokens(),
                ai == null ? 0 : ai.outputTokens(), ai == null ? 0 : ai.latencyMs()));
    }

    @Operation(summary = "AI call counts since the last restart",
            description = "ok, failed, cached and refused calls, tokens and mean latency. Resets when the app restarts")
    @GetMapping("/metrics")
    public ApiResponse<AiMetrics.Snapshot> metrics() {
        return ApiResponse.ok(metrics.snapshot());
    }
}