package com.hiresphere.match.explain;

import com.hiresphere.job.entity.WorkMode;
import com.hiresphere.match.MatchBreakdown;
import com.hiresphere.match.MatchCalculator;
import com.hiresphere.match.MatchInput;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class TemplateExplainerTest {

    private static final List<String> JOB = List.of("Java", "Spring Boot", "PostgreSQL");

    private ExplanationText explain(List<String> mine, List<String> job, Integer years, Integer min,
                                    String candidateCity, String jobCity, WorkMode mode) {
        return TemplateExplainer.explain(MatchCalculator.calculate(
                new MatchInput(mine, job, years, min, null, candidateCity, jobCity, mode)));
    }

    @Test
    void aGoodMatchWithGapsShouldNameWhatFitsAndWhatDoesNot() {
        ExplanationText t = TemplateExplainer.explain(MatchCalculator.calculate(new MatchInput(
                List.of("Java", "Spring Boot", "SQL"), JOB, 2, 3, 6, "Patna", "Pune", WorkMode.HYBRID)));

        assertThat(t.summary()).isEqualTo("This is a good match. You have 2 of the 3 skills the job lists.");
        assertThat(t.strengths()).containsExactly("You already have Java and Spring Boot.");
        assertThat(t.gaps()).containsExactly("Not on your profile yet: PostgreSQL.",
                "The job asks for 3 years of experience and you have 2 years.",
                "The job is in Pune and your profile says Patna.");
        assertThat(t.tip()).isEqualTo("If you have worked with PostgreSQL, add it to your profile so it counts.");
    }

    @Test
    void aStrongMatchShouldHaveStrengthsInOrderAndNoGaps() {
        ExplanationText t = explain(JOB, JOB, 5, 3, null, null, WorkMode.REMOTE);

        assertThat(t.summary()).isEqualTo("This looks like a strong match. You have all 3 skills the job lists.");
        assertThat(t.strengths()).containsExactly("You already have Java, Spring Boot and PostgreSQL.",
                "Your 5 years of experience meets the 3 years the job asks for.",
                "The job is remote, so your city does not matter.");
        assertThat(t.gaps()).isEmpty();
        assertThat(t.tip()).isEqualTo("Read the full job description before you apply.");
    }

    @Test
    void theSameCityShouldBeAStrength() {
        ExplanationText t = explain(JOB, JOB, null, 0, "Pune", "Pune, MH", WorkMode.ONSITE);

        assertThat(t.strengths()).contains("The job is in your city.");
    }

    @Test
    void noSkillsOverlapShouldSayNoneAndSuggestTheFirstMissingSkill() {
        ExplanationText t = explain(List.of("Photoshop"), JOB, null, 3, null, null, WorkMode.ONSITE);

        assertThat(t.summary()).isEqualTo("This is a low match for now. None of the 3 skills this job lists are on your profile yet.");
        assertThat(t.strengths()).isEmpty();
        assertThat(t.tip()).isEqualTo("If you have worked with Java, add it to your profile so it counts.");
    }

    @Test
    void aCandidateWithoutSkillsShouldBeAskedToAddThemAndSeeWhatTheJobWants() {
        ExplanationText t = explain(List.of(), JOB, 2, 0, null, null, WorkMode.REMOTE);

        assertThat(t.summary()).isEqualTo("Add your skills to your profile and we can show how well you match this job.");
        assertThat(t.gaps()).containsExactly("The job asks for Java, Spring Boot and PostgreSQL.");
        assertThat(t.tip()).isEqualTo("Add your skills to your profile, or upload your resume to get suggestions.");
    }

    @Test
    void aJobWithoutSkillsShouldSayItCannotBeScored() {
        ExplanationText t = explain(List.of("Java"), List.of(), 2, 0, null, null, WorkMode.REMOTE);

        assertThat(t.summary()).isEqualTo("This job does not list any skills, so we cannot score how well you match.");
        assertThat(t.gaps()).isEmpty();
        assertThat(t.tip()).isEqualTo("Read the job description to judge the fit yourself.");
    }

    @Test
    void aSingleSkillJobShouldReadNaturally() {
        assertThat(explain(List.of("Java"), List.of("Java"), null, 0, null, null, WorkMode.REMOTE).summary())
                .isEqualTo("This looks like a strong match. You have the skill the job lists.");
        assertThat(explain(List.of("Photoshop"), List.of("Java"), null, 0, null, null, WorkMode.REMOTE).summary())
                .isEqualTo("This is a low match for now. The skill this job lists is not on your profile yet.");
    }

    @Test
    void aLongListOfSkillsShouldBeShortenedWithAndMore() {
        List<String> seven = List.of("Alpha", "Bravo", "Charlie", "Delta", "Echo", "Foxtrot", "Golf");

        ExplanationText t = explain(seven, seven, null, 0, null, null, WorkMode.REMOTE);

        assertThat(t.strengths().get(0)).isEqualTo("You already have Alpha, Bravo, Charlie, Delta, Echo and more.");
    }

    @Test
    void oneYearShouldNotBePlural() {
        ExplanationText t = explain(List.of("Java"), List.of("Java"), 0, 1, null, null, WorkMode.REMOTE);

        assertThat(t.gaps()).containsExactly("The job asks for 1 year of experience and you have 0 years.");
        assertThat(t.tip()).isEqualTo("Add projects that show related work to strengthen your profile.");
    }

    @Test
    void neitherListShouldEverBeLongerThanThreeItems() {
        for (WorkMode mode : WorkMode.values()) {
            for (Integer years : new Integer[]{null, 0, 1, 5}) {
                ExplanationText t = explain(List.of("Java"), JOB, years, 3, "Patna", "Pune", mode);

                assertThat(t.strengths()).hasSizeLessThanOrEqualTo(3);
                assertThat(t.gaps()).hasSizeLessThanOrEqualTo(3);
            }
        }
    }

    @Test
    void theTemplateShouldAlwaysPassTheSameCheckAsTheAiText() {
        List<String> catalogue = List.of("Java", "Spring Boot", "PostgreSQL", "Alpha", "Bravo", "Charlie", "Delta",
                "Echo", "Foxtrot", "Golf", "Photoshop");
        ExplanationValidator validator = new ExplanationValidator(() -> catalogue);
        List<List<String>> candidates = List.of(List.of(), List.of("Java"), JOB, List.of("Photoshop", "Java"));
        List<List<String>> jobs = List.of(List.of(), List.of("Java"), JOB, catalogue.subList(3, 10));

        for (List<String> mine : candidates) {
            for (List<String> job : jobs) {
                for (WorkMode mode : WorkMode.values()) {
                    for (Integer years : new Integer[]{null, 0, 2, 6}) {
                        MatchBreakdown b = MatchCalculator.calculate(
                                new MatchInput(mine, job, years, 3, 6, "Patna", "Pune", mode));
                        ExplanationText t = TemplateExplainer.explain(b);
                        ExplanationValidator.Outcome outcome = validator.validate(t.summary(), t.strengths(), t.gaps(),
                                t.tip(), FactSheet.build(b, "Backend Developer", "Acme"));

                        assertThat(outcome.ok()).as("%s vs %s, %s, %s years: %s", mine, job, mode, years, outcome.problem())
                                .isTrue();
                    }
                }
            }
        }
    }
}