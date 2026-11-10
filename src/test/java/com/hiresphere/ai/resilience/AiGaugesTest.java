package com.hiresphere.ai.resilience;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.assertThat;

class AiGaugesTest {

    private final AtomicLong clock = new AtomicLong(0);
    private final AiCircuitBreaker breaker = new AiCircuitBreaker(2, Duration.ofSeconds(30), clock::get);
    private final AiBulkhead bulkhead = new AiBulkhead(3, Duration.ofMillis(10));
    private final SimpleMeterRegistry registry = new SimpleMeterRegistry();

    AiGaugesTest() {
        new AiGauges(registry, breaker, bulkhead);
    }

    private double circuit() {
        return registry.get("ai.circuit.state").gauge().value();
    }

    @Test
    void theCircuitGaugeShouldFollowTheBreakerThroughItsStates() {
        assertThat(circuit()).isEqualTo(0.0);   // closed

        breaker.onFailure(breaker.tryAcquire());
        breaker.onFailure(breaker.tryAcquire());
        assertThat(circuit()).isEqualTo(2.0);   // open

        clock.addAndGet(Duration.ofSeconds(30).toNanos());
        breaker.tryAcquire();                    // probe
        assertThat(circuit()).isEqualTo(1.0);   // half open
    }

    @Test
    void theBulkheadGaugeShouldShowFreeSlots() {
        assertThat(registry.get("ai.bulkhead.available").gauge().value()).isEqualTo(3.0);

        bulkhead.tryAcquire();
        bulkhead.tryAcquire();

        assertThat(registry.get("ai.bulkhead.available").gauge().value()).isEqualTo(1.0);
    }
}