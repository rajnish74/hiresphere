package com.hiresphere.ai.resilience;

import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "app.ai.enabled", havingValue = "true")
public class AiGauges {

    @Autowired
    public AiGauges(ObjectProvider<MeterRegistry> registry, AiCircuitBreaker breaker, AiBulkhead bulkhead) {
        this(registry.getIfAvailable(SimpleMeterRegistry::new), breaker, bulkhead);
    }

    public AiGauges(MeterRegistry registry, AiCircuitBreaker breaker, AiBulkhead bulkhead) {
        Gauge.builder("ai.circuit.state", breaker, b -> switch (b.state()) {
            case CLOSED -> 0;
            case HALF_OPEN -> 1;
            case OPEN -> 2;
        }).description("0 closed, 1 half open, 2 open").register(registry);
        Gauge.builder("ai.bulkhead.available", bulkhead, AiBulkhead::available)
                .description("Free AI call slots").register(registry);
    }
}