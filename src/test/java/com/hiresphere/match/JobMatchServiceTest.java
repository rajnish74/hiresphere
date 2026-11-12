package com.hiresphere.match;

import com.hiresphere.common.exception.AppException;
import com.hiresphere.common.exception.ResourceNotFoundException;
import com.hiresphere.job.entity.WorkMode;
import com.hiresphere.match.explain.MatchExplainer;
import com.hiresphere.match.explain.MatchExplanation;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JobMatchServiceTest {

    private static final JobFacts JOB = new JobFacts(10L, "Java Developer", "Acme", "Pune", WorkMode.HYBRID, 3, 6,
            List.of("Java", "Spring Boot", "PostgreSQL"));
    private static final CandidateFacts ME = new CandidateFacts(Set.of("java", "spring boot", "sql"), 2, "Patna");

    private static final MatchExplanation TEMPLATE = new MatchExplanation("template summary", List.of("s"), List.of("g"),
            "template tip", MatchExplanation.Source.TEMPLATE, null, false);
    private static final MatchExplanation AI = new MatchExplanation("ai summary", List.of(), List.of(), "ai tip",
            MatchExplanation.Source.AI, null, true);

    @Mock
    private MatchExplainer explainer;

    private JobMatchService service;
    private int jobReads;
    private int profileReads;
    private CandidateFacts candidate = ME;

    @BeforeEach
    void setUp() {
        service = new JobMatchService(jobId -> {
            jobReads++;
            if (jobId != 10L) {
                throw new ResourceNotFoundException("Job not found");
            }
            return JOB;
        }, userId -> {
            profileReads++;
            return candidate;
        }, explainer);
    }

    @Test
    void shouldCalculateTheBreakdownFromTheCandidateAndTheJob() {
        when(explainer.templateOnly(any())).thenReturn(TEMPLATE);

        JobMatchResponse r = service.match(7L, 10L, false);

        assertThat(r.jobId()).isEqualTo(10L);
        assertThat(r.jobTitle()).isEqualTo("Java Developer");
        assertThat(r.companyName()).isEqualTo("Acme");
        assertThat(r.skills().matched()).containsExactly("Java", "Spring Boot");
        assertThat(r.skills().missing()).containsExactly("PostgreSQL");
        assertThat(r.skills().percent()).isEqualTo(67);
        assertThat(r.experience().status()).isEqualTo(MatchBreakdown.ExperienceStatus.BELOW);
        assertThat(r.experience().shortfallYears()).isEqualTo(1);
        assertThat(r.location().status()).isEqualTo(MatchBreakdown.LocationStatus.DIFFERENT_CITY);
        assertThat(r.fitScore()).isEqualTo(64);
        assertThat(r.label()).isEqualTo(FitLabel.GOOD);
    }

    @Test
    void theScoreShouldBeTheSameWhetherOrNotTheAiIsAsked() {
        when(explainer.templateOnly(any())).thenReturn(TEMPLATE);
        when(explainer.explain(eq(7L), any(), any(), any())).thenReturn(AI);

        assertThat(service.match(7L, 10L, true).fitScore()).isEqualTo(service.match(7L, 10L, false).fitScore());
    }

    @Test
    void withoutAiTheTemplateShouldAnswerAndTheAiIsNeverCalled() {
        when(explainer.templateOnly(any())).thenReturn(TEMPLATE);

        JobMatchResponse r = service.match(7L, 10L, false);

        assertThat(r.explanation().summary()).isEqualTo("template summary");
        assertThat(r.explanation().source()).isEqualTo("TEMPLATE");
        assertThat(r.explanation().degraded()).isFalse();
        verify(explainer, never()).explain(any(), any(), any(), any());
    }

    @Test
    void withAiTheExplainerShouldGetTheBreakdownTheTitleAndTheCompanyOnBehalfOfTheCaller() {
        when(explainer.explain(eq(7L), any(), eq("Java Developer"), eq("Acme"))).thenReturn(AI);

        JobMatchResponse r = service.match(7L, 10L, true);

        assertThat(r.explanation().summary()).isEqualTo("ai summary");
        assertThat(r.explanation().source()).isEqualTo("AI");
        assertThat(r.explanation().cached()).isTrue();
        ArgumentCaptor<MatchBreakdown> breakdown = ArgumentCaptor.forClass(MatchBreakdown.class);
        verify(explainer).explain(eq(7L), breakdown.capture(), any(), any());
        assertThat(breakdown.getValue().fitScore()).isEqualTo(64);
        verify(explainer, never()).templateOnly(any());
    }

    @Test
    void aDegradedExplanationShouldBeFlaggedWithItsReason() {
        when(explainer.explain(eq(7L), any(), any(), any())).thenReturn(new MatchExplanation("s", List.of(), List.of(),
                "t", MatchExplanation.Source.TEMPLATE, "TIMEOUT", false));

        JobMatchResponse.Explanation e = service.match(7L, 10L, true).explanation();

        assertThat(e.degraded()).isTrue();
        assertThat(e.degradedReason()).isEqualTo("TIMEOUT");
    }

    @Test
    void aJobThatIsNotPublicShouldBeNotFoundAndNothingElseShouldRun() {
        assertThatThrownBy(() -> service.match(7L, 99L, true)).isInstanceOf(ResourceNotFoundException.class);

        assertThat(profileReads).isZero();   // candidate ka data padha hi nahi
        verifyNoInteractions(explainer);
    }

    @Test
    void aCandidateWithNothingFilledInShouldStillGetAnAnswerWithoutAScore() {
        candidate = new CandidateFacts(Set.of(), null, null);
        when(explainer.templateOnly(any())).thenReturn(TEMPLATE);

        JobMatchResponse r = service.match(7L, 10L, false);

        assertThat(r.fitScore()).isNull();
        assertThat(r.label()).isEqualTo(FitLabel.UNKNOWN);
        assertThat(r.skills().status()).isEqualTo(MatchBreakdown.SkillsStatus.NO_CANDIDATE_SKILLS);
        assertThat(r.skills().missing()).containsExactly("Java", "Spring Boot", "PostgreSQL");
        assertThat(r.experience().status()).isEqualTo(MatchBreakdown.ExperienceStatus.UNKNOWN);
        assertThat(r.location().status()).isEqualTo(MatchBreakdown.LocationStatus.UNKNOWN);
    }

    @Test
    void tooManyRequestsShouldBeRefusedBeforeAnythingIsRead() {
        when(explainer.templateOnly(any())).thenReturn(TEMPLATE);
        for (int i = 0; i < JobMatchService.PER_MINUTE; i++) {
            service.match(7L, 10L, false);
        }
        int jobsBefore = jobReads;

        assertThatThrownBy(() -> service.match(7L, 10L, false))
                .isInstanceOfSatisfying(AppException.class,
                        e -> assertThat(e.getStatus()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS));

        assertThat(jobReads).isEqualTo(jobsBefore);
    }

    @Test
    void theLimitShouldBePerUser() {
        when(explainer.templateOnly(any())).thenReturn(TEMPLATE);
        for (int i = 0; i < JobMatchService.PER_MINUTE; i++) {
            service.match(7L, 10L, false);
        }

        assertThat(service.match(8L, 10L, false).jobId()).isEqualTo(10L);
    }
}