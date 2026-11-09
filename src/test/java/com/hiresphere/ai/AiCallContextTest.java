package com.hiresphere.ai;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AiCallContextTest {

    @Test
    void withoutAContextTheCallIsUnspecifiedAndHasNoUser() {
        assertThat(AiCallContext.current()).isEqualTo(new AiCallContext.Context("UNSPECIFIED", null));
    }

    @Test
    void shouldCarryTheFeatureAndUserInsideAndLeaveNothingBehind() {
        String inside = AiCallContext.with("SKILLS_EXTRACT", 7L, () -> {
            AiCallContext.Context c = AiCallContext.current();
            return c.feature() + ":" + c.userId();
        });

        assertThat(inside).isEqualTo("SKILLS_EXTRACT:7");
        assertThat(AiCallContext.current().userId()).isNull();
    }

    @Test
    void nestedContextsShouldRestoreTheOuterOne() {
        AiCallContext.with("OUTER", 1L, () -> {
            AiCallContext.with("INNER", 2L, () -> assertThat(AiCallContext.current().userId()).isEqualTo(2L));
            assertThat(AiCallContext.current().feature()).isEqualTo("OUTER");
            return null;
        });
    }

    @Test
    void anExceptionInsideShouldStillClearTheContext() {
        assertThatThrownBy(() -> AiCallContext.with("BROKEN", 9L, () -> {
            throw new IllegalStateException("boom");
        })).isInstanceOf(IllegalStateException.class);

        assertThat(AiCallContext.current().userId()).isNull();
    }

    @Test
    void theFeatureNameMustBeSafeForLogsAndTheDatabase() {
        for (String bad : new String[]{null, "", "lower", "HAS SPACE", "WITH-DASH", "X", "A".repeat(41), "1START"}) {
            assertThatThrownBy(() -> AiCallContext.with(bad, 1L, () -> null)).as(String.valueOf(bad))
                    .isInstanceOf(IllegalArgumentException.class);
        }
        AiCallContext.with("A".repeat(40), 1L, () -> null);   // 40 theek hai
    }
}