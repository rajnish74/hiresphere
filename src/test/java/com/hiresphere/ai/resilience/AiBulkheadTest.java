package com.hiresphere.ai.resilience;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AiBulkheadTest {

    @Test
    void shouldGiveOutOnlyAsManySlotsAsConfigured() {
        AiBulkhead bulkhead = new AiBulkhead(2, Duration.ofMillis(30));

        assertThat(bulkhead.tryAcquire()).isTrue();
        assertThat(bulkhead.tryAcquire()).isTrue();
        assertThat(bulkhead.available()).isZero();
        assertThat(bulkhead.tryAcquire()).isFalse();   // thodi der ruka, phir haara
    }

    @Test
    void aReleasedSlotShouldBeUsableAgain() {
        AiBulkhead bulkhead = new AiBulkhead(1, Duration.ofMillis(30));
        bulkhead.tryAcquire();

        bulkhead.release();

        assertThat(bulkhead.available()).isEqualTo(1);
        assertThat(bulkhead.tryAcquire()).isTrue();
        assertThat(bulkhead.max()).isEqualTo(1);
    }

    @Test
    void aWaitingCallerShouldGetTheSlotWhenItIsReleasedInTime() throws Exception {
        AiBulkhead bulkhead = new AiBulkhead(1, Duration.ofSeconds(2));
        bulkhead.tryAcquire();
        ExecutorService pool = Executors.newSingleThreadExecutor();
        try {
            Future<Boolean> waiter = pool.submit(bulkhead::tryAcquire);
            Thread.sleep(50);
            bulkhead.release();

            assertThat(waiter.get(3, TimeUnit.SECONDS)).isTrue();
        } finally {
            pool.shutdownNow();
        }
    }

    @Test
    void aninterruptedThreadShouldGiveUpAndKeepItsInterruptFlag() {
        AiBulkhead bulkhead = new AiBulkhead(1, Duration.ofSeconds(5));
        Thread.currentThread().interrupt();

        assertThat(bulkhead.tryAcquire()).isFalse();
        assertThat(Thread.interrupted()).isTrue();   // flag wapas set hua tha, ab saaf
        assertThat(bulkhead.available()).isEqualTo(1);
    }

    @Test
    void neverMoreCallsThanSlotsShouldRunAtTheSameTime() throws Exception {
        AiBulkhead bulkhead = new AiBulkhead(2, Duration.ofSeconds(10));
        AtomicInteger running = new AtomicInteger();
        AtomicInteger highest = new AtomicInteger();
        ExecutorService pool = Executors.newFixedThreadPool(8);
        try {
            List<Future<?>> tasks = new ArrayList<>();
            for (int i = 0; i < 12; i++) {
                tasks.add(pool.submit(() -> {
                    assertThat(bulkhead.tryAcquire()).isTrue();
                    try {
                        highest.accumulateAndGet(running.incrementAndGet(), Math::max);
                        Thread.sleep(15);
                        running.decrementAndGet();
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    } finally {
                        bulkhead.release();
                    }
                }));
            }
            for (Future<?> task : tasks) {
                task.get(10, TimeUnit.SECONDS);
            }
        } finally {
            pool.shutdownNow();
        }

        assertThat(highest.get()).isLessThanOrEqualTo(2).isGreaterThanOrEqualTo(1);
        assertThat(bulkhead.available()).isEqualTo(2);
    }

    @Test
    void zeroSlotsShouldBeRejected() {
        assertThatThrownBy(() -> new AiBulkhead(0, Duration.ofMillis(1))).isInstanceOf(IllegalArgumentException.class);
    }
}