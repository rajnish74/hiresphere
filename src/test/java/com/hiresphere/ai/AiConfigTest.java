package com.hiresphere.ai;

import com.hiresphere.ai.config.AiConfig;
import com.hiresphere.ai.config.AiLimitsProperties;
import com.hiresphere.ai.config.AiProperties;
import com.hiresphere.ai.config.ResilienceProperties;
import com.hiresphere.ai.gemini.GeminiClient;
import com.hiresphere.ai.resilience.AiBulkhead;
import com.hiresphere.ai.resilience.AiCircuitBreaker;
import com.hiresphere.ai.resilience.AiUserLimiter;
import com.hiresphere.ai.resilience.MeteredAiClient;
import com.hiresphere.ai.resilience.ResilientAiClient;
import com.hiresphere.ai.usage.AiUsageStore;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class AiConfigTest {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withUserConfiguration(AiConfig.class, GeminiClient.class, ResilientAiClient.class, MeteredAiClient.class,
                    AiCircuitBreaker.class, AiBulkhead.class, AiUserLimiter.class, DisabledAiClient.class)
            .withBean(AiUsageStore.class, () -> mock(AiUsageStore.class));

    private static final String[] ENABLED = {"app.ai.enabled=true", "app.ai.gemini.api-key=k", "app.ai.gemini.model=m1"};

    @Test
    void withoutAnySettingsTheDisabledClientShouldBeTheOnlyOne() {
        runner.run(ctx -> {
            assertThat(ctx).hasSingleBean(AiClient.class);
            assertThat(ctx.getBean(AiClient.class)).isInstanceOf(DisabledAiClient.class);
            assertThat(ctx).doesNotHaveBean("geminiRestClient");
            assertThat(ctx).doesNotHaveBean(AiCircuitBreaker.class);
            assertThat(ctx).doesNotHaveBean(AiBulkhead.class);
            assertThat(ctx).doesNotHaveBean(AiUserLimiter.class);
        });
    }

    @Test
    void whenEnabledTheMeteredClientShouldBeOutermostOverTheResilientOneOverGemini() {
        runner.withPropertyValues(ENABLED).run(ctx -> {
            assertThat(ctx.getBean(AiClient.class)).isInstanceOf(MeteredAiClient.class);
            assertThat(ctx.getBean("resilientAiClient")).isInstanceOf(ResilientAiClient.class);
            assertThat(ctx).hasSingleBean(GeminiClient.class);
            assertThat(ctx).hasSingleBean(AiUserLimiter.class);
            assertThat(ctx).hasBean("geminiRestClient");
        });
    }

    @Test
    void enablingWithoutAKeyShouldFailTheStartupWithAClearReason() {
        runner.withPropertyValues("app.ai.enabled=true", "app.ai.gemini.model=m1").run(ctx -> {
            assertThat(ctx).hasFailed();
            assertThat(ctx.getStartupFailure()).rootCause().hasMessageContaining("GEMINI_API_KEY");
        });
    }

    @Test
    void valuesShouldBindFromThePropertyNamesUsedInTheYml() {
        runner.withPropertyValues("app.ai.enabled=true", "app.ai.gemini.api-key=k", "app.ai.gemini.model=models/m2",
                        "app.ai.gemini.connect-timeout=3s", "app.ai.gemini.read-timeout=45s",
                        "app.ai.gemini.max-output-tokens=2048", "app.ai.gemini.base-url=https://example.com/v1/")
                .run(ctx -> {
                    AiProperties.Gemini g = ctx.getBean(AiProperties.class).gemini();
                    assertThat(g.model()).isEqualTo("m2");
                    assertThat(g.connectTimeout()).isEqualTo(Duration.ofSeconds(3));
                    assertThat(g.readTimeout()).isEqualTo(Duration.ofSeconds(45));
                    assertThat(g.maxOutputTokens()).isEqualTo(2048);
                    assertThat(g.baseUrl()).isEqualTo("https://example.com/v1");
                });
    }

    @Test
    void theResilienceAndLimitSettingsShouldBindAndFallBackToDefaults() {
        runner.withPropertyValues(ENABLED).run(ctx -> {
            assertThat(ctx.getBean(ResilienceProperties.class).maxAttempts()).isEqualTo(3);
            assertThat(ctx.getBean(AiLimitsProperties.class).perUserPerMinute()).isEqualTo(10);
        });

        runner.withPropertyValues("app.ai.resilience.max-attempts=2", "app.ai.limits.per-user-per-minute=3",
                "app.ai.limits.per-user-daily-tokens=500", "app.ai.limits.cache-ttl=2h").run(ctx -> {
            assertThat(ctx.getBean(ResilienceProperties.class).maxAttempts()).isEqualTo(2);
            AiLimitsProperties limits = ctx.getBean(AiLimitsProperties.class);
            assertThat(limits.perUserPerMinute()).isEqualTo(3);
            assertThat(limits.perUserDailyTokens()).isEqualTo(500);
            assertThat(limits.cacheTtl()).isEqualTo(Duration.ofHours(2));
        });
    }
}