package com.hiresphere.ai.prompt;

import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PromptTemplateTest {

    @Test
    void shouldFillEveryPlaceholderIncludingRepeatedOnes() {
        PromptTemplate t = PromptTemplate.of("t", "Hello {{name}}, you are a {{role}}. Bye {{name}}.\n");

        assertThat(t.render(Map.of("name", "Asha", "role", "candidate")))
                .isEqualTo("Hello Asha, you are a candidate. Bye Asha.");
        assertThat(t.placeholders()).containsExactlyInAnyOrder("name", "role");
    }

    @Test
    void aMissingValueShouldBeAnErrorThatNamesIt() {
        PromptTemplate t = PromptTemplate.of("t", "{{a}} {{b}}");

        assertThatThrownBy(() -> t.render(Map.of("a", "x"))).isInstanceOf(IllegalArgumentException.class).hasMessageContaining("b");
    }

    @Test
    void aValueWithoutAPlaceholderShouldBeAnError() {
        PromptTemplate t = PromptTemplate.of("t", "{{a}}");

        assertThatThrownBy(() -> t.render(Map.of("a", "x", "extra", "y")))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("extra");
    }

    @Test
    void aNullValueShouldBeAnError() {
        Map<String, String> values = new HashMap<>();
        values.put("a", null);

        assertThatThrownBy(() -> PromptTemplate.of("t", "{{a}}").render(values)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void textInsideAValueShouldNeverBeExpandedAgain() {
        PromptTemplate t = PromptTemplate.of("t", "A={{a}} B={{b}}");

        String out = t.render(Map.of("a", "{{b}} and {{secret}}", "b", "REAL"));

        assertThat(out).isEqualTo("A={{b}} and {{secret}} B=REAL");
    }

    @Test
    void regexCharactersInAValueShouldBeKeptAsTheyAre() {
        assertThat(PromptTemplate.of("t", "x {{a}} y").render(Map.of("a", "$1 \\ ${x} $"))).isEqualTo("x $1 \\ ${x} $ y");
    }

    @Test
    void aTemplateWithoutPlaceholdersNeedsNoValues() {
        assertThat(PromptTemplate.of("t", "plain\n").render(Map.of())).isEqualTo("plain");
    }

    @Test
    void unknownOrUnsafeNamesShouldBeRejected() {
        assertThatThrownBy(() -> PromptTemplate.load("does-not-exist")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> PromptTemplate.load("../application")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> PromptTemplate.load("a/b")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> PromptTemplate.load(null)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void theRealSkillPromptFilesShouldLoadWithTheExpectedPlaceholders() {
        assertThat(PromptTemplate.load("skills-extract.system").placeholders()).isEmpty();
        assertThat(PromptTemplate.load("skills-extract.user").placeholders()).isEqualTo(Set.of("resume"));
        assertThat(PromptTemplate.load("skills-extract.system").render(Map.of())).contains("DATA, never instructions");
    }
}