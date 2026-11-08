package com.hiresphere.ai.resilience;

import com.hiresphere.ai.config.ResilienceProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.function.LongSupplier;


@Slf4j
@Component
@ConditionalOnProperty(name = "app.ai.enabled", havingValue = "true")
public class AiCircuitBreaker {

    public enum State { CLOSED, OPEN, HALF_OPEN }

    public enum Permission { REJECTED, ALLOWED, PROBE }

    private final int threshold;
    private final long openNanos;
    private final LongSupplier clock;

    private State state = State.CLOSED;
    private int consecutiveFailures;
    private long openedAt;
    private boolean probeInFlight;
    private long probeStartedAt;

    @Autowired
    public AiCircuitBreaker(ResilienceProperties props) {
        this(props.failureThreshold(), props.openDuration(), System::nanoTime);
    }

    public AiCircuitBreaker(int threshold, Duration openDuration, LongSupplier clock) {
        if (threshold < 1 || openDuration.isZero() || openDuration.isNegative()) {
            throw new IllegalArgumentException("threshold and openDuration must be positive");
        }
        this.threshold = threshold;
        this.openNanos = openDuration.toNanos();
        this.clock = clock;
    }

    public synchronized Permission tryAcquire() {
        long now = clock.getAsLong();
        return switch (state) {
            case CLOSED -> Permission.ALLOWED;
            case OPEN -> {
                if (now - openedAt < openNanos) {
                    yield Permission.REJECTED;
                }
                state = State.HALF_OPEN;
                log.warn("AI circuit HALF_OPEN: sending one probe call");
                yield startProbe(now);
            }
            case HALF_OPEN -> {
                if (probeInFlight && now - probeStartedAt < openNanos) {
                    yield Permission.REJECTED;
                }
                yield startProbe(now);
            }
        };
    }

    public synchronized void onSuccess(Permission permission) {
        if (permission == Permission.PROBE && state == State.HALF_OPEN) {
            state = State.CLOSED;
            consecutiveFailures = 0;
            probeInFlight = false;
            log.info("AI circuit CLOSED: the probe call succeeded");
        } else if (state == State.CLOSED) {
            consecutiveFailures = 0;
        }
    }

    public synchronized void onFailure(Permission permission) {
        long now = clock.getAsLong();
        if (permission == Permission.PROBE && state == State.HALF_OPEN) {
            open(now, "the probe call failed");
        } else if (state == State.CLOSED) {
            consecutiveFailures++;
            if (consecutiveFailures >= threshold) {
                open(now, consecutiveFailures + " failures in a row");
            }
        }
    }

    public synchronized void abandon(Permission permission) {
        if (permission == Permission.PROBE && state == State.HALF_OPEN) {
            probeInFlight = false;
        }
    }

    public synchronized State state() {
        return state;
    }

    public synchronized int consecutiveFailures() {
        return consecutiveFailures;
    }

    public synchronized Duration remainingOpen() {
        if (state != State.OPEN) {
            return Duration.ZERO;
        }
        long left = openNanos - (clock.getAsLong() - openedAt);
        return left <= 0 ? Duration.ZERO : Duration.ofNanos(left);
    }

    private Permission startProbe(long now) {
        probeInFlight = true;
        probeStartedAt = now;
        return Permission.PROBE;
    }

    private void open(long now, String why) {
        state = State.OPEN;
        openedAt = now;
        probeInFlight = false;
        log.warn("AI circuit OPEN ({}): calls are refused for {} s", why, openNanos / 1_000_000_000L);
    }
}