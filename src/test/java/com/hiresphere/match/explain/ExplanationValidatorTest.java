package com.hiresphere.match.explain;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ExplanationValidatorTest {

    private static final String SHEET = """
            job_title: Java Backend Developer
            company: Acme
            fit_label: GOOD
            fit_score: 64
            skills_percent: 67
            job_skills_total: 3
            skills_matched_count: 2
            skills_missing_count: 1
            skills_matched: Java; Spring Boot
            skills_missing: PostgreSQL
            candidate_experience_years: 2
            job_min_years: 3
            experience_shortfall_years: 1
            job_location: Pune
            candidate_location: Patna""";

    private final ExplanationValidator validator = new ExplanationValidator(
            () -> List.of("Java", "Spring Boot", "PostgreSQL", "Docker", "Kubernetes", "Go", "SQL"));

    private ExplanationValidator.Outcome check(String summary, List<String> strengths, List<String> gaps, String tip) {
        return validator.validate(summary, strengths, gaps, tip, SHEET);
    }

    private ExplanationValidator.Outcome summary(String text) {
        return check(text, List.of(), List.of(), "Add your skills to your profile.");
    }

    private void assertRejected(ExplanationValidator.Outcome outcome, String reasonPart) {
        assertThat(outcome.ok()).isFalse();
        assertThat(outcome.problem()).contains(reasonPart);
        assertThat(outcome.text()).isNull();
    }

    // ---------- sahi jawab ----------

    @Test
    void aGoodAnswerShouldPassAndComeBackTidied() {
        ExplanationValidator.Outcome o = check("  This is a good   match with 2 of the 3 skills.  ",
                List.of("You already have Java and Spring Boot.", "  "), List.of("PostgreSQL is not on your profile yet."),
                "If you have used PostgreSQL, add it to your profile.");

        assertThat(o.ok()).isTrue();
        assertThat(o.text().summary()).isEqualTo("This is a good match with 2 of the 3 skills.");
        assertThat(o.text().strengths()).containsExactly("You already have Java and Spring Boot.");   // khali item hat gaya
        assertThat(o.text().gaps()).containsExactly("PostgreSQL is not on your profile yet.");
    }

    @Test
    void nullListsShouldBeTreatedAsEmpty() {
        ExplanationValidator.Outcome o = check("A fair match.", null, null, "Read the description.");

        assertThat(o.ok()).isTrue();
        assertThat(o.text().strengths()).isEmpty();
        assertThat(o.text().gaps()).isEmpty();
    }

    @Test
    void numbersThatAreInTheFactSheetShouldBeAllowed() {
        assertThat(summary("Your fit score is 64 and 67% of the skills match, with 2 years against 3.").ok()).isTrue();
    }

    @Test
    void skillsAndNamesFromTheFactSheetShouldBeAllowedIncludingOnesInTheJobTitle() {
        assertThat(summary("Java and Spring Boot fit this Java Backend Developer role at Acme.").ok()).isTrue();
    }

    // ---------- ghadhe hue numbers aur skills ----------

    @Test
    void anInventedNumberShouldBeRejected() {
        assertRejected(summary("You are a 92% match."), "invented number 92");
        assertRejected(summary("You need 8 years of experience."), "invented number 8");
        assertRejected(summary("About 100% of the skills match."), "invented number 100");
    }

    @Test
    void aSkillThatIsNotInTheFactsShouldBeRejected() {
        assertRejected(summary("You should also learn Docker."), "Docker");
        assertRejected(check("Fine.", List.of("Your Kubernetes work helps."), List.of(), "Add skills."), "Kubernetes");
        assertRejected(check("Fine.", List.of(), List.of(), "Try learning SQL."), "SQL");
    }

    @Test
    void shortOrCommonWordsThatAreAlsoSkillNamesShouldNotCauseARejection() {
        assertThat(summary("Go ahead and read the full description.").ok()).isTrue();   // "Go" bahut chhota naam hai
    }

    @Test
    void aMissingOrOddKnownSkillListShouldNotBreakTheCheck() {
        ExplanationValidator odd = new ExplanationValidator(() -> Arrays.asList("Java", null, "", " "));
        ExplanationValidator none = new ExplanationValidator(() -> null);

        assertThat(odd.validate("Fine.", List.of(), List.of(), "Tip.", SHEET).ok()).isTrue();
        assertThat(none.validate("Fine.", List.of(), List.of(), "Tip.", SHEET).ok()).isTrue();
    }

    // ---------- khatarnak ya bekaar text ----------

    @Test
    void markupLinksAndSymbolsShouldBeRejected() {
        for (String bad : new String[]{"<b>Great</b> match.", "Mail me at asha@x.co.", "**Strong** match.", "# Match",
                "Use `Java` more."}) {
            assertThat(summary(bad).ok()).as(bad).isFalse();
        }
        assertRejected(summary("See https://example.com for more."), "link");
        assertRejected(summary("See www.example.com for more."), "link");
        assertRejected(summary("A good match \uD83D\uDE00."), "emoji");
    }

    @Test
    void textThatIsNotMostlyEnglishShouldBeRejected() {
        assertRejected(summary("आपकी स्किल्स इस नौकरी से अच्छी तरह मिलती हैं"), "English");
    }

    @Test
    void aFewForeignLettersInAnEnglishSentenceShouldBeFine() {
        assertThat(summary("This is a good match for a café-style startup role.").ok()).isTrue();
    }

    @Test
    void promisesAndDecisionsShouldBeRejected() {
        for (String bad : new String[]{"You will be hired for sure.", "This is guaranteed to work.", "Do not apply to this job.",
                "You should not apply here.", "You will get the job.", "You are unqualified.", "Applying is a waste of time."}) {
            assertThat(summary(bad).ok()).as(bad).isFalse();
        }
    }

    @Test
    void mentioningPersonalAttributesShouldBeRejectedButSimilarWordsShouldNotBe() {
        assertRejected(summary("At your age this is a good step."), "personal attribute");
        assertRejected(summary("As a woman you have good chances."), "personal attribute");
        assertThat(summary("You manage projects and the average match is high.").ok()).isTrue();
    }

    // ---------- shape ----------

    @Test
    void emptyPartsShouldBeRejected() {
        assertRejected(check("  ", List.of(), List.of(), "Tip."), "summary");
        assertRejected(check("Summary.", List.of(), List.of(), null), "tip");
    }

    @Test
    void moreThanThreeItemsShouldBeRejected() {
        assertRejected(check("Fine.", List.of("a.", "b.", "c.", "d."), List.of(), "Tip."), "more than 3");
        assertRejected(check("Fine.", List.of(), List.of("a.", "b.", "c.", "d."), "Tip."), "more than 3");
    }

    @Test
    void sentencesThatAreTooLongShouldBeRejected() {
        assertRejected(summary("word ".repeat(70)), "too long");
        assertRejected(check("Fine.", List.of("word ".repeat(40)), List.of(), "Tip."), "too long");
        assertRejected(check("Fine.", List.of(), List.of(), "word ".repeat(50)), "too long");
    }

    @Test
    void ordinaryEnglishWordsThatAreAlsoSkillNamesAreNotCheckedAndThatIsAKnownLimit() {
        ExplanationValidator withAmbiguous = new ExplanationValidator(() -> List.of("Rust", "Swift", "Spring", "Go"));

        assertThat(withAmbiguous.validate("Rust is not in the facts but we cannot tell it from the word.",
                List.of(), List.of(), "Spring is a busy season for hiring.", SHEET).ok()).isTrue();
    }
}