package com.hiresphere.ai.skills;

import com.hiresphere.ai.AiCallContext;
import com.hiresphere.ai.AiException;
import com.hiresphere.ai.AiException.Kind;
import com.hiresphere.ai.AiResponse;
import com.hiresphere.common.exception.BadRequestException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SkillSuggestionServiceTest {

    @Mock
    private ResumeSkillExtractor extractor;
    @Mock
    private KnownSkillSource knownSkills;

    private SkillSuggestionService service;

    @BeforeEach
    void setUp() {
        service = new SkillSuggestionService(extractor, knownSkills);
    }

    @Test
    void whenTheAiWorksTheAiAnswerShouldBeUsedAndKeywordsNeverConsulted() {
        AiResponse ai = new AiResponse("x", "STOP", false, 10, 5, 15, "m", 40);
        when(extractor.extract("Java dev")).thenReturn(new ExtractedSkills(List.of("Java"), 1, false, ai));

        SkillSuggestions result = service.suggest("Java dev");

        assertThat(result.skills()).containsExactly("Java");
        assertThat(result.source()).isEqualTo(SkillSuggestions.Source.AI);
        assertThat(result.degraded()).isFalse();
        assertThat(result.degradedReason()).isNull();
        assertThat(result.droppedCount()).isEqualTo(1);
        assertThat(result.ai()).isSameAs(ai);
        verifyNoInteractions(knownSkills);
    }

    @ParameterizedTest
    @EnumSource(Kind.class)
    void whateverWentWrongWithTheAiTheKeywordFallbackShouldAnswerAndSayWhy(Kind kind) {
        when(extractor.extract("We use Docker and Java")).thenThrow(new AiException(kind, "internal"));
        when(knownSkills.names()).thenReturn(List.of("Docker", "Java", "Rust"));

        SkillSuggestions result = service.suggest("We use Docker and Java");

        assertThat(result.skills()).containsExactly("Docker", "Java");
        assertThat(result.source()).isEqualTo(SkillSuggestions.Source.KEYWORD);
        assertThat(result.degraded()).isTrue();
        assertThat(result.degradedReason()).isEqualTo(kind.name());
        assertThat(result.ai()).isNull();
    }

    @Test
    void theFallbackShouldReportThatTheInputWasCut() {
        String longText = "Java ".repeat(5000);
        when(extractor.extract(longText)).thenThrow(new AiException(Kind.TIMEOUT, "slow"));
        when(knownSkills.names()).thenReturn(List.of("Java"));

        assertThat(service.suggest(longText).inputTruncated()).isTrue();
    }

    @Test
    void aTextWithNothingInItShouldBeRejectedBeforeAnythingIsCalled() {
        assertThatThrownBy(() -> service.suggest("\u200B  \n")).isInstanceOf(BadRequestException.class);
        assertThatThrownBy(() -> service.suggest(null)).isInstanceOf(BadRequestException.class);

        verifyNoInteractions(extractor, knownSkills);
    }

    @Test
    void mistakesThatAreNotTheAisShouldNotBeHiddenByTheFallback() {
        when(extractor.extract("Java")).thenThrow(new IllegalStateException("bug"));

        assertThatThrownBy(() -> service.suggest("Java")).isInstanceOf(IllegalStateException.class);

        verifyNoInteractions(knownSkills);
    }

    @Test
    void anEmptyKnownSkillListShouldGiveAnEmptyFallbackNotAnError() {
        when(extractor.extract("Java")).thenThrow(new AiException(Kind.DISABLED, "off"));
        when(knownSkills.names()).thenReturn(List.of());

        assertThat(service.suggest("Java").skills()).isEmpty();
    }

    @Test
    void theUserVersionShouldRunUnderTheSkillsFeatureAndThatUser() {
        AtomicReference<AiCallContext.Context> seen = new AtomicReference<>();
        when(extractor.extract("Java dev")).thenAnswer(invocation -> {
            seen.set(AiCallContext.current());
            return new ExtractedSkills(List.of("Java"), 0, false, null);
        });

        service.suggest(42L, "Java dev");

        assertThat(seen.get().userId()).isEqualTo(42L);
        assertThat(seen.get().feature()).isEqualTo(SkillSuggestionService.FEATURE);
        assertThat(AiCallContext.current().userId()).isNull();   // baad mein context saaf
    }

    @Test
    void anAnswerFromTheCacheShouldBeMarkedAsCachedNotAsDegraded() {
        when(extractor.extract("Java dev")).thenReturn(new ExtractedSkills(List.of("Java"), 0, false, null));

        SkillSuggestions result = service.suggest("Java dev");

        assertThat(result.cached()).isTrue();
        assertThat(result.degraded()).isFalse();
        assertThat(result.source()).isEqualTo(SkillSuggestions.Source.AI);
    }
}