package com.hiresphere.ai;

import com.hiresphere.ai.config.AiConfig;
import com.hiresphere.ai.config.AiProperties;
import com.hiresphere.ai.gemini.GeminiClient;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

class AiConfigTest {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withUserConfiguration(AiConfig.class, GeminiClient.class, DisabledAiClient.class);

    @Test
    void withoutAnySettingsTheDisabledClientShouldBeUsed() {
        runner.run(ctx -> {
            assertThat(ctx).hasSingleBean(AiClient.class);
            assertThat(ctx.getBean(AiClient.class)).isInstanceOf(DisabledAiClient.class);
            assertThat(ctx).doesNotHaveBean("geminiRestClient");
        });
    }

    @Test
    void whenEnabledTheGeminiClientShouldReplaceIt() {
        runner.withPropertyValues("app.ai.enabled=true", "app.ai.gemini.api-key=k", "app.ai.gemini.model=m1")
                .run(ctx -> {
                    assertThat(ctx).hasSingleBean(AiClient.class);
                    assertThat(ctx.getBean(AiClient.class)).isInstanceOf(GeminiClient.class);
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
}