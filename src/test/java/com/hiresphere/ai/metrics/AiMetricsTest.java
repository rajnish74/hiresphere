package com.hiresphere.ai.metrics;

import io.micrometer.core.instrument.Meter;
import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;

import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

class AiMetricsTest {

    private final SimpleMeterRegistry registry = new SimpleMeterRegistry();
    private final AiMetrics metrics = new AiMetrics(registry);

    @Test
    void aFreshRegistryShouldGiveAllZeros() {
        assertThat(metrics.snapshot()).isEqualTo(new AiMetrics.Snapshot(0, 0, 0, 0, 0, 0, 0.0));
    }

    @Test
    void anOkCallShouldCountTokensAndLatency() {
        metrics.ok("SKILLS_EXTRACT", 100, 20, 250);

        AiMetrics.Snapshot s = metrics.snapshot();
        assertThat(s.ok()).isEqualTo(1);
        assertThat(s.promptTokens()).isEqualTo(100);
        assertThat(s.outputTokens()).isEqualTo(20);
        assertThat(s.meanLatencyMs()).isCloseTo(250.0, within(0.5));
    }

    @Test
    void everyOutcomeShouldBeCountedOnItsOwn() {
        metrics.ok("A", 1, 1, 1);
        metrics.failed("A", "TIMEOUT");
        metrics.failed("A", "UPSTREAM");
        metrics.refused("A", "BUDGET_EXCEEDED");
        metrics.cached("A");
        metrics.cached("B");

        AiMetrics.Snapshot s = metrics.snapshot();
        assertThat(s.ok()).isEqualTo(1);
        assertThat(s.failed()).isEqualTo(2);
        assertThat(s.refused()).isEqualTo(1);
        assertThat(s.cached()).isEqualTo(2);
        assertThat(registry.get(AiMetrics.CALLS).tag("outcome", "failed").tag("kind", "TIMEOUT").counter().count())
                .isEqualTo(1.0);
    }

    @Test
    void featuresShouldBeSeparateSeries() {
        metrics.cached("A");
        metrics.cached("A");
        metrics.cached("B");

        assertThat(registry.get(AiMetrics.CALLS).tag("feature", "A").counter().count()).isEqualTo(2.0);
        assertThat(registry.get(AiMetrics.CALLS).tag("feature", "B").counter().count()).isEqualTo(1.0);
    }

    @Test
    void theMeanLatencyShouldAverageAllFeatures() {
        metrics.ok("A", 0, 0, 100);
        metrics.ok("B", 0, 0, 300);

        assertThat(metrics.snapshot().meanLatencyMs()).isCloseTo(200.0, within(0.5));
    }

    @Test
    void everyCallsMeterShouldUseTheSameTagKeysBecausePrometheusRequiresIt() {
        metrics.ok("A", 1, 1, 1);
        metrics.failed("A", "TIMEOUT");
        metrics.refused("A", "BUDGET_EXCEEDED");
        metrics.cached("A");

        for (Meter meter : registry.find(AiMetrics.CALLS).meters()) {
            Set<String> keys = meter.getId().getTags().stream().map(Tag::getKey).collect(Collectors.toSet());
            assertThat(keys).as(meter.getId().toString()).containsExactlyInAnyOrder("feature", "outcome", "kind");
        }
    }

    @Test
    void negativeNumbersShouldBeTreatedAsZeroAndNeverBreakACounter() {
        metrics.ok("A", -5, -1, -9);

        AiMetrics.Snapshot s = metrics.snapshot();
        assertThat(s.ok()).isEqualTo(1);
        assertThat(s.promptTokens()).isZero();
        assertThat(s.outputTokens()).isZero();
    }

    @Test
    void theStandaloneVersionShouldWorkWithoutAnyFramework() {
        AiMetrics standalone = AiMetrics.standalone();
        standalone.cached("A");

        assertThat(standalone.snapshot().cached()).isEqualTo(1);
    }
}