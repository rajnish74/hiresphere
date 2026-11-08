package com.hiresphere.ai;

import com.hiresphere.ai.config.AiProperties;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AiPropertiesTest {

    private AiProperties.Gemini gemini(String key, String model) {
        return new AiProperties.Gemini(key, model, null, null, null, 0);
    }

    @Test
    void aDisabledConfigNeedsNothingAndHasSafeDefaults() {
        AiProperties props = new AiProperties(false, null);

        assertThat(props.enabled()).isFalse();
        assertThat(props.gemini().hasKey()).isFalse();
        assertThat(props.gemini().baseUrl()).isEqualTo(AiProperties.Gemini.DEFAULT_BASE_URL);
        assertThat(props.gemini().connectTimeout()).isEqualTo(Duration.ofSeconds(5));
        assertThat(props.gemini().readTimeout()).isEqualTo(Duration.ofSeconds(30));
        assertThat(props.gemini().maxOutputTokens()).isEqualTo(1024);
    }

    @Test
    void enablingAiWithoutAKeyShouldStopTheStartupAndNameTheVariable() {
        assertThatThrownBy(() -> new AiProperties(true, gemini("  ", "some-model")))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("GEMINI_API_KEY");
    }

    @Test
    void enablingAiWithoutAModelShouldStopTheStartupAndNameTheVariable() {
        assertThatThrownBy(() -> new AiProperties(true, gemini("secret", "")))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("GEMINI_MODEL");
    }

    @Test
    void theKeyShouldNeverAppearInTheErrorOrInToString() {
        assertThatThrownBy(() -> new AiProperties(true, gemini("super-secret-key", "")))
                .hasMessageNotContaining("super-secret-key");

        AiProperties props = new AiProperties(true, gemini("super-secret-key", "m1"));

        assertThat(props.toString()).doesNotContain("super-secret-key");
        assertThat(props.gemini().toString()).doesNotContain("super-secret-key").contains("<hidden>");
    }

    @Test
    void aModelNameCopiedFromTheListShouldLoseItsPrefix() {
        assertThat(gemini("k", "models/gemini-test-1.5").model()).isEqualTo("gemini-test-1.5");
        assertThat(gemini("k", "  gemini-x  ").model()).isEqualTo("gemini-x");
    }

    @Test
    void aModelNameThatCouldChangeTheUrlShouldBeRejected() {
        for (String bad : new String[]{"a/b", "a b", "a?key=1", "../x", "m:generateContent", "m#x"}) {
            assertThatThrownBy(() -> gemini("k", bad)).as(bad).isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Test
    void theBaseUrlShouldLoseATrailingSlashAndFallBackWhenBlank() {
        assertThat(new AiProperties.Gemini("k", "m", "https://example.com/v1/", null, null, 0).baseUrl())
                .isEqualTo("https://example.com/v1");
        assertThat(new AiProperties.Gemini("k", "m", "  ", null, null, 0).baseUrl())
                .isEqualTo(AiProperties.Gemini.DEFAULT_BASE_URL);
    }

    @Test
    void explicitValuesShouldBeKept() {
        AiProperties.Gemini g = new AiProperties.Gemini("k", "m", null, Duration.ofSeconds(2), Duration.ofSeconds(9), 256);

        assertThat(g.connectTimeout()).isEqualTo(Duration.ofSeconds(2));
        assertThat(g.readTimeout()).isEqualTo(Duration.ofSeconds(9));
        assertThat(g.maxOutputTokens()).isEqualTo(256);
    }
}