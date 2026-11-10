package com.hiresphere.ai.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;


@Component
public class AiMetrics {

    public static final String CALLS = "ai.calls";
    public static final String TOKENS = "ai.tokens";
    public static final String DURATION = "ai.call.duration";
    private static final String NONE = "none";

    public record Snapshot(long ok, long failed, long cached, long refused, long promptTokens, long outputTokens,
                           double meanLatencyMs) {
    }

    private final MeterRegistry registry;

    @Autowired
    public AiMetrics(ObjectProvider<MeterRegistry> registry) {
        this(registry.getIfAvailable(SimpleMeterRegistry::new));
    }

    public AiMetrics(MeterRegistry registry) {
        this.registry = registry;
    }

    public static AiMetrics standalone() {
        return new AiMetrics(new SimpleMeterRegistry());
    }

    public void ok(String feature, int promptTokens, int outputTokens, long latencyMs) {
        count(feature, "ok", NONE);
        registry.counter(TOKENS, "feature", feature, "type", "prompt").increment(Math.max(0, promptTokens));
        registry.counter(TOKENS, "feature", feature, "type", "output").increment(Math.max(0, outputTokens));
        registry.timer(DURATION, "feature", feature).record(Math.max(0, latencyMs), TimeUnit.MILLISECONDS);
    }

    public void failed(String feature, String kind) {
        count(feature, "failed", kind);
    }

    public void refused(String feature, String kind) {
        count(feature, "refused", kind);
    }

    public void cached(String feature) {
        count(feature, "cached", NONE);
    }

    public Snapshot snapshot() {
        double totalMs = registry.find(DURATION).timers().stream()
                .mapToDouble(t -> t.totalTime(TimeUnit.MILLISECONDS)).sum();
        long timed = registry.find(DURATION).timers().stream().mapToLong(Timer::count).sum();
        return new Snapshot(sum(CALLS, "outcome", "ok"), sum(CALLS, "outcome", "failed"),
                sum(CALLS, "outcome", "cached"), sum(CALLS, "outcome", "refused"),
                sum(TOKENS, "type", "prompt"), sum(TOKENS, "type", "output"), timed == 0 ? 0 : totalMs / timed);
    }

    private void count(String feature, String outcome, String kind) {
        registry.counter(CALLS, "feature", feature, "outcome", outcome, "kind", kind).increment();
    }

    private long sum(String name, String tag, String value) {
        return Math.round(registry.find(name).tag(tag, value).counters().stream().mapToDouble(Counter::count).sum());
    }
}