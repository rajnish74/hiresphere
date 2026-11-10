package com.hiresphere.ai;

import com.github.benmanes.caffeine.cache.Ticker;
import com.hiresphere.ai.cache.AiResultCache;
import com.hiresphere.ai.config.AiConfig;
import com.hiresphere.ai.config.AiLimitsProperties;
import com.hiresphere.ai.config.AiProperties;
import com.hiresphere.ai.config.ResilienceProperties;
import com.hiresphere.ai.gemini.GeminiClient;
import com.hiresphere.ai.json.StructuredAi;
import com.hiresphere.ai.metrics.AiMetrics;
import com.hiresphere.ai.resilience.AiBulkhead;
import com.hiresphere.ai.resilience.AiCircuitBreaker;
import com.hiresphere.ai.resilience.AiUserLimiter;
import com.hiresphere.ai.resilience.MeteredAiClient;
import com.hiresphere.ai.resilience.ResilientAiClient;
import com.hiresphere.ai.resilience.Sleeper;
import com.hiresphere.ai.skills.ResumeSkillExtractor;
import com.hiresphere.ai.skills.SkillSuggestionService;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.json.JsonMapper;

import java.util.List;

/** Spring ke bina, asli jaisa poora AI stack: HTTP, retry, circuit, limits, cache, validation, fallback. */
public final class AiTestStack {

    public final FakeUsageStore usage = new FakeUsageStore();
    public final SimpleMeterRegistry registry = new SimpleMeterRegistry();
    public final AiMetrics metrics = new AiMetrics(registry);
    public final AiCircuitBreaker breaker;
    public final AiBulkhead bulkhead;
    public final AiClient client;
    public final ResumeSkillExtractor extractor;
    public final SkillSuggestionService suggestions;

    public AiTestStack(AiProperties props, ResilienceProperties resilience, AiLimitsProperties limits,
                       List<String> knownSkills) {
        RestClient rest = new AiConfig().geminiRestClient(props);   // asli timeouts samet
        GeminiClient gemini = new GeminiClient(rest, props);
        breaker = new AiCircuitBreaker(resilience.failureThreshold(), resilience.openDuration(), System::nanoTime);
        bulkhead = new AiBulkhead(resilience.maxConcurrent(), resilience.acquireTimeout());
        ResilientAiClient resilient = new ResilientAiClient(gemini, resilience, breaker, bulkhead, Sleeper.THREAD,
                () -> 0.5, System::nanoTime);
        client = new MeteredAiClient(resilient, new AiUserLimiter(limits.perUserPerMinute(), Ticker.systemTicker()),
                limits, usage, metrics);
        StructuredAi structured = new StructuredAi(client, JsonMapper.builder().build());
        extractor = new ResumeSkillExtractor(structured, new AiResultCache(limits), usage, metrics);
        suggestions = new SkillSuggestionService(extractor, () -> knownSkills);
    }
}