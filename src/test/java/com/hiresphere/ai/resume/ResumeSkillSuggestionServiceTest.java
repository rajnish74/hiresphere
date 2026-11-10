package com.hiresphere.ai.resume;

import com.hiresphere.ai.AiResponse;
import com.hiresphere.ai.resume.ResumeTextException.Kind;
import com.hiresphere.ai.skills.SkillSuggestionService;
import com.hiresphere.ai.skills.SkillSuggestions;
import com.hiresphere.common.exception.AppException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ResumeSkillSuggestionServiceTest {

    private static final String RESUME = """
            Asha Verma - Backend Developer
            asha@mailtest.com | +91 98765 43210 | https://github.com/asha
            Skills: Java, Spring Boot, PostgreSQL, Docker
            Built hiring APIs at Acme with Spring Boot, 2019 - 2023""";

    private static final AiResponse AI = new AiResponse("x", "STOP", false, 120, 20, 140, "m", 30);

    @Mock
    private SkillSuggestionService suggestions;

    private final PdfResumeTextExtractor extractor = new PdfResumeTextExtractor();
    private ResumeSkillSuggestionService service;

    private byte[] pdf;
    private boolean hasResume = true;
    private int reads;
    private Set<String> onProfile = Set.of();

    @BeforeEach
    void setUp() throws Exception {
        pdf = TestPdfs.withPages(RESUME);
        service = new ResumeSkillSuggestionService(
                userId -> {
                    reads++;
                    return hasResume ? Optional.of(new ResumeFile("asha-cv.pdf", pdf)) : Optional.empty();
                },
                extractor, suggestions, userId -> onProfile);
    }

    private SkillSuggestions ai(String... skills) {
        return new SkillSuggestions(List.of(skills), SkillSuggestions.Source.AI, null, false, 0, AI);
    }

    @Test
    void shouldReadTheResumeAndReturnTheSuggestionsWithTheirDetails() {
        when(suggestions.suggest(eq(7L), anyString())).thenReturn(ai("Java", "Spring Boot", "Docker"));

        ResumeSkillSuggestions result = service.suggest(7L);

        assertThat(result.resumeName()).isEqualTo("asha-cv.pdf");
        assertThat(result.skills()).extracting(ResumeSkillSuggestions.SuggestedSkill::name)
                .containsExactly("Java", "Spring Boot", "Docker");
        assertThat(result.source()).isEqualTo("AI");
        assertThat(result.degraded()).isFalse();
        assertThat(result.degradedReason()).isNull();
        assertThat(result.cached()).isFalse();
        assertThat(result.partial()).isFalse();
        assertThat(result.pagesRead()).isEqualTo(1);
        assertThat(result.totalPages()).isEqualTo(1);
    }

    @Test
    void theAiShouldNeverSeeTheContactDetailsAndTheCandidateShouldBeToldHowManyWereRemoved() {
        when(suggestions.suggest(eq(7L), anyString())).thenReturn(ai("Java"));

        ResumeSkillSuggestions result = service.suggest(7L);

        ArgumentCaptor<String> sent = ArgumentCaptor.forClass(String.class);
        verify(suggestions).suggest(eq(7L), sent.capture());
        assertThat(sent.getValue()).contains("[email]", "[phone]", "[link]", "Spring Boot", "2019 - 2023")
                .doesNotContain("asha@mailtest.com", "98765", "github.com");
        assertThat(result.contactDetailsRemoved()).isEqualTo(3);
    }

    @Test
    void shouldAskTheAiOnBehalfOfTheCallerAndNobodyElse() {
        when(suggestions.suggest(anyLong(), anyString())).thenReturn(ai("Java"));

        service.suggest(42L);

        verify(suggestions).suggest(eq(42L), anyString());
    }

    @Test
    void suggestionsAlreadyOnTheProfileShouldBeMarkedEvenWhenCaseOrSpacingDiffers() {
        onProfile = Set.of("java", "spring boot");
        when(suggestions.suggest(eq(7L), anyString())).thenReturn(ai("Java", "Spring Boot", "Docker"));

        List<ResumeSkillSuggestions.SuggestedSkill> skills = service.suggest(7L).skills();

        assertThat(skills).containsExactly(
                new ResumeSkillSuggestions.SuggestedSkill("Java", true),
                new ResumeSkillSuggestions.SuggestedSkill("Spring Boot", true),
                new ResumeSkillSuggestions.SuggestedSkill("Docker", false));
    }

    @Test
    void whenTheAiWasNotAvailableTheKeywordResultShouldBeFlaggedWithTheReason() {
        when(suggestions.suggest(eq(7L), anyString())).thenReturn(
                new SkillSuggestions(List.of("Java"), SkillSuggestions.Source.KEYWORD, "CIRCUIT_OPEN", false, 0, null));

        ResumeSkillSuggestions result = service.suggest(7L);

        assertThat(result.source()).isEqualTo("KEYWORD");
        assertThat(result.degraded()).isTrue();
        assertThat(result.degradedReason()).isEqualTo("CIRCUIT_OPEN");
        assertThat(result.cached()).isFalse();   // KEYWORD cache nahi hota
    }

    @Test
    void anAnswerFromTheCacheShouldBeMarkedCachedAndNotDegraded() {
        when(suggestions.suggest(eq(7L), anyString())).thenReturn(
                new SkillSuggestions(List.of("Java"), SkillSuggestions.Source.AI, null, false, 0, null));

        ResumeSkillSuggestions result = service.suggest(7L);

        assertThat(result.cached()).isTrue();
        assertThat(result.degraded()).isFalse();
    }

    @Test
    void aLongResumeShouldBeReportedAsPartial() throws Exception {
        String[] pages = new String[12];
        for (int i = 0; i < pages.length; i++) {
            pages[i] = "Page " + (i + 1) + " mentions Java, Docker and many other readable words";
        }
        pdf = TestPdfs.withPages(pages);
        when(suggestions.suggest(eq(7L), anyString())).thenReturn(ai("Java"));

        ResumeSkillSuggestions result = service.suggest(7L);

        assertThat(result.partial()).isTrue();
        assertThat(result.pagesRead()).isEqualTo(10);
        assertThat(result.totalPages()).isEqualTo(12);
    }

    @Test
    void anInputTheAiHadToCutShouldAlsoBeReportedAsPartial() {
        when(suggestions.suggest(eq(7L), anyString())).thenReturn(
                new SkillSuggestions(List.of("Java"), SkillSuggestions.Source.AI, null, true, 0, AI));

        assertThat(service.suggest(7L).partial()).isTrue();
    }

    @Test
    void noResumeShouldAskTheUserToUploadAndCostNothing() {
        hasResume = false;

        assertThatThrownBy(() -> service.suggest(7L))
                .isInstanceOfSatisfying(ResumeTextException.class, e -> assertThat(e.getKind()).isEqualTo(Kind.NO_RESUME));

        verifyNoInteractions(suggestions);
    }

    @Test
    void aScannedPdfShouldBeRefusedBeforeAnyAiCall() throws Exception {
        pdf = TestPdfs.blank(2);

        assertThatThrownBy(() -> service.suggest(7L))
                .isInstanceOfSatisfying(ResumeTextException.class, e -> assertThat(e.getKind()).isEqualTo(Kind.NO_TEXT));

        verifyNoInteractions(suggestions);
    }

    @Test
    void aProtectedPdfShouldBeRefusedBeforeAnyAiCall() throws Exception {
        pdf = TestPdfs.encrypted("user-secret", true, RESUME);

        assertThatThrownBy(() -> service.suggest(7L))
                .isInstanceOfSatisfying(ResumeTextException.class, e -> assertThat(e.getKind()).isEqualTo(Kind.ENCRYPTED));

        verifyNoInteractions(suggestions);
    }

    @Test
    void aFileThatIsNotAPdfShouldBeRefusedBeforeAnyAiCall() {
        pdf = "this is just text".getBytes();

        assertThatThrownBy(() -> service.suggest(7L))
                .isInstanceOfSatisfying(ResumeTextException.class, e -> assertThat(e.getKind()).isEqualTo(Kind.UNREADABLE));

        verifyNoInteractions(suggestions);
    }

    @Test
    void tooManyRequestsShouldBeRefusedBeforeTheResumeIsEvenRead() {
        when(suggestions.suggest(eq(7L), anyString())).thenReturn(ai("Java"));
        for (int i = 0; i < ResumeSkillSuggestionService.PER_MINUTE; i++) {
            service.suggest(7L);
        }
        int readsBefore = reads;

        assertThatThrownBy(() -> service.suggest(7L))
                .isInstanceOfSatisfying(AppException.class,
                        e -> assertThat(e.getStatus()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS));

        assertThat(reads).isEqualTo(readsBefore);   // limit lagne par resume padha hi nahi
    }

    @Test
    void theLimitShouldBePerUser() {
        when(suggestions.suggest(anyLong(), anyString())).thenReturn(ai("Java"));
        for (int i = 0; i < ResumeSkillSuggestionService.PER_MINUTE; i++) {
            service.suggest(7L);
        }

        assertThat(service.suggest(8L).skills()).hasSize(1);
    }
}