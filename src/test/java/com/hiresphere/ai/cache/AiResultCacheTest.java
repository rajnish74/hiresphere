package com.hiresphere.ai.cache;

import com.hiresphere.ai.config.AiLimitsProperties;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AiResultCacheTest {

    private final AtomicLong nanos = new AtomicLong(1_000);
    private final AiResultCache cache =
            new AiResultCache(new AiLimitsProperties(0, 0, 0, Duration.ofHours(24), 100, 0), nanos::get);

    @Test
    void theFirstLookupShouldRunTheLoaderAndTheSecondShouldNot() {
        AtomicInteger loads = new AtomicInteger();

        AiResultCache.Lookup<String> first = cache.get("k", () -> "value-" + loads.incrementAndGet());
        AiResultCache.Lookup<String> second = cache.get("k", () -> "value-" + loads.incrementAndGet());

        assertThat(first.hit()).isFalse();
        assertThat(second.hit()).isTrue();
        assertThat(second.value()).isEqualTo("value-1");
        assertThat(loads).hasValue(1);
    }

    @Test
    void differentKeysShouldNotShareAnAnswer() {
        cache.get("a", () -> "A");

        assertThat(cache.get("b", () -> "B").value()).isEqualTo("B");
    }

    @Test
    void aFailingLoaderShouldNotLeaveAnythingBehind() {
        assertThatThrownBy(() -> cache.get("k", () -> {
            throw new IllegalStateException("provider down");
        })).isInstanceOf(IllegalStateException.class);

        AiResultCache.Lookup<String> retry = cache.get("k", () -> "ok");

        assertThat(retry.hit()).isFalse();
        assertThat(retry.value()).isEqualTo("ok");
    }

    @Test
    void anAnswerShouldExpireAfterTheTimeToLive() {
        cache.get("k", () -> "old");

        nanos.addAndGet(Duration.ofHours(23).toNanos());
        assertThat(cache.get("k", () -> "new").hit()).isTrue();

        nanos.addAndGet(Duration.ofHours(2).toNanos());
        AiResultCache.Lookup<String> after = cache.get("k", () -> "new");
        assertThat(after.hit()).isFalse();
        assertThat(after.value()).isEqualTo("new");
    }

    @Test
    void aLoaderThatReturnsNothingIsAProgrammingError() {
        assertThatThrownBy(() -> cache.get("k", () -> null)).isInstanceOf(NullPointerException.class);
    }

    @Test
    void keysShouldBeLongHashesThatDoNotContainTheText() {
        String key = AiResultCache.keyOf("skills", "my secret resume text");

        assertThat(key).hasSize(64).matches("[0-9a-f]{64}").doesNotContain("secret");
        assertThat(AiResultCache.keyOf("skills", "my secret resume text")).isEqualTo(key);
    }

    @Test
    void everyPartAndTheirBoundariesShouldChangeTheKey() {
        assertThat(AiResultCache.keyOf("a", "b")).isNotEqualTo(AiResultCache.keyOf("a", "c"));
        assertThat(AiResultCache.keyOf("ab", "c")).isNotEqualTo(AiResultCache.keyOf("a", "bc"));
        assertThat(AiResultCache.keyOf("a", "b")).isNotEqualTo(AiResultCache.keyOf("b", "a"));
        assertThatThrownBy(() -> AiResultCache.keyOf("a", null)).isInstanceOf(IllegalArgumentException.class);
    }
}