package com.hiresphere.match;

import com.hiresphere.job.entity.WorkMode;
import com.hiresphere.job.service.SkillMatch;
import com.hiresphere.match.MatchBreakdown.Experience;
import com.hiresphere.match.MatchBreakdown.ExperienceStatus;
import com.hiresphere.match.MatchBreakdown.Location;
import com.hiresphere.match.MatchBreakdown.LocationStatus;
import com.hiresphere.match.MatchBreakdown.Skills;
import com.hiresphere.match.MatchBreakdown.SkillsStatus;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MatchCalculatorTest {

    private static final List<String> JOB = List.of("Java", "Spring Boot", "PostgreSQL");

    private MatchBreakdown calc(MatchInput input) {
        return MatchCalculator.calculate(input);
    }

    /** Skills ke ilawa sab "pata nahi": fit seedha skills ka percent hoga. */
    private MatchInput skillsOnly(List<String> mine, List<String> job) {
        return new MatchInput(mine, job, null, 3, null, null, null, WorkMode.ONSITE);
    }

    private Experience experience(Integer years, Integer min, Integer max) {
        return calc(new MatchInput(List.of("Java"), List.of("Java"), years, min, max, null, null, WorkMode.REMOTE)).experience();
    }

    private Location location(WorkMode mode, String jobLocation, String candidateLocation) {
        return calc(new MatchInput(List.of("Java"), List.of("Java"), 5, 0, null, candidateLocation, jobLocation, mode)).location();
    }

    // ---------- skills ----------

    @Test
    void matchedAndMissingShouldUseTheJobsSpellingAndOrderAndIgnoreExtraCandidateSkills() {
        Skills s = calc(skillsOnly(List.of("spring  BOOT", "java", "Kotlin"), JOB)).skills();

        assertThat(s.status()).isEqualTo(SkillsStatus.SCORED);
        assertThat(s.matched()).containsExactly("Java", "Spring Boot");
        assertThat(s.missing()).containsExactly("PostgreSQL");
        assertThat(s.percent()).isEqualTo(67);
    }

    @Test
    void duplicateJobSkillsShouldCountOnce() {
        Skills s = calc(skillsOnly(List.of("java"), List.of("Java", "java", " JAVA "))).skills();

        assertThat(s.matched()).containsExactly("Java");
        assertThat(s.percent()).isEqualTo(100);
    }

    @Test
    void blankAndNullEntriesShouldBeIgnored() {
        Skills s = calc(skillsOnly(Arrays.asList("Java", null, "  "), Arrays.asList("Java", null, "   ", "Docker"))).skills();

        assertThat(s.matched()).containsExactly("Java");
        assertThat(s.missing()).containsExactly("Docker");
    }

    @Test
    void thePercentShouldAlwaysBeTheSameNumberThatTheJobCardShows() {
        List<List<List<String>>> cases = List.of(
                List.of(List.of("Java"), JOB),
                List.of(List.of("java", "spring boot", "postgresql"), JOB),
                List.of(List.of("Photoshop"), JOB),
                List.of(List.of("Java", "SQL", "Docker", "React"), List.of("Java", "Docker", "Kubernetes", "Terraform", "Go")),
                List.of(List.of(" JAVA "), List.of("Java", "java")));

        for (List<List<String>> c : cases) {
            assertThat(calc(skillsOnly(c.get(0), c.get(1))).skills().percent())
                    .as("%s vs %s", c.get(0), c.get(1)).isEqualTo(SkillMatch.percent(c.get(0), c.get(1)));
        }
    }

    @Test
    void aJobWithoutSkillsShouldGiveNoScoreButKeepTheOtherFacts() {
        MatchBreakdown b = calc(new MatchInput(List.of("Java"), List.of(), 5, 2, null, "Patna", "Patna", WorkMode.ONSITE));

        assertThat(b.skills().status()).isEqualTo(SkillsStatus.NO_JOB_SKILLS);
        assertThat(b.skills().percent()).isNull();
        assertThat(b.fitScore()).isNull();
        assertThat(b.label()).isEqualTo(FitLabel.UNKNOWN);
        assertThat(b.experience().status()).isEqualTo(ExperienceStatus.MEETS);
        assertThat(b.location().status()).isEqualTo(LocationStatus.SAME_CITY);
    }

    @Test
    void aCandidateWithoutSkillsShouldGiveNoScoreAndListEverythingTheJobAsksFor() {
        MatchBreakdown b = calc(skillsOnly(List.of(), JOB));

        assertThat(b.skills().status()).isEqualTo(SkillsStatus.NO_CANDIDATE_SKILLS);
        assertThat(b.skills().matched()).isEmpty();
        assertThat(b.skills().missing()).containsExactly("Java", "Spring Boot", "PostgreSQL");
        assertThat(b.fitScore()).isNull();
        assertThat(b.label()).isEqualTo(FitLabel.UNKNOWN);
    }

    @Test
    void noOverlapAtAllIsARealZeroNotUnknown() {
        MatchBreakdown b = calc(skillsOnly(List.of("Photoshop"), JOB));

        assertThat(b.skills().percent()).isZero();
        assertThat(b.fitScore()).isZero();
        assertThat(b.label()).isEqualTo(FitLabel.LOW);
    }

    // ---------- experience ----------

    @Test
    void meetingTheMinimumShouldScoreFull() {
        Experience e = experience(5, 3, 8);

        assertThat(e.status()).isEqualTo(ExperienceStatus.MEETS);
        assertThat(e.score()).isEqualTo(100);
        assertThat(e.overqualified()).isFalse();
        assertThat(e.shortfallYears()).isZero();
    }

    @Test
    void moreThanTheMaximumIsOnlyNotedNeverPenalised() {
        Experience e = experience(10, 3, 8);

        assertThat(e.status()).isEqualTo(ExperienceStatus.MEETS);
        assertThat(e.score()).isEqualTo(100);
        assertThat(e.overqualified()).isTrue();
    }

    @Test
    void eachYearShortShouldCostThirtyFivePointsDownToZero() {
        assertThat(experience(2, 3, null).score()).isEqualTo(65);
        assertThat(experience(1, 3, null).score()).isEqualTo(30);
        assertThat(experience(0, 3, null).score()).isZero();
        assertThat(experience(0, 6, null).score()).isZero();   // 100 - 210 ke bajay 0
        assertThat(experience(2, 3, null).status()).isEqualTo(ExperienceStatus.BELOW);
        assertThat(experience(2, 3, null).shortfallYears()).isEqualTo(1);
    }

    @Test
    void anUnknownCandidateExperienceShouldBeLeftOutUnlessTheJobAsksForNone() {
        Experience asked = experience(null, 2, null);
        Experience none = experience(null, 0, null);

        assertThat(asked.status()).isEqualTo(ExperienceStatus.UNKNOWN);
        assertThat(asked.score()).isNull();
        assertThat(none.status()).isEqualTo(ExperienceStatus.MEETS);
        assertThat(none.score()).isEqualTo(100);
    }

    @Test
    void oddNumbersShouldBeTreatedSafely() {
        assertThat(experience(-3, 0, null).candidateYears()).isZero();      // negative saal 0 ban jate hain
        assertThat(experience(4, null, null).status()).isEqualTo(ExperienceStatus.MEETS);   // job ka min na ho
        assertThat(experience(0, -2, null).requiredMinYears()).isZero();
    }

    // ---------- location ----------

    @Test
    void aRemoteJobShouldAlwaysScoreFullEvenWhenTheCandidatesCityIsUnknown() {
        Location l = location(WorkMode.REMOTE, "Pune", null);

        assertThat(l.status()).isEqualTo(LocationStatus.REMOTE);
        assertThat(l.score()).isEqualTo(100);
    }

    @Test
    void theSameCityShouldMatchEvenWithAStateCaseAndSpacesDifference() {
        assertThat(location(WorkMode.ONSITE, "Patna, Bihar", "patna").status()).isEqualTo(LocationStatus.SAME_CITY);
        assertThat(location(WorkMode.ONSITE, "New Delhi, India", "  new   DELHI ").status()).isEqualTo(LocationStatus.SAME_CITY);
        assertThat(location(WorkMode.ONSITE, "Pune", "Pune, Maharashtra").score()).isEqualTo(100);
    }

    @Test
    void anotherCityShouldCostEverythingForOnsiteAndLessForHybrid() {
        assertThat(location(WorkMode.ONSITE, "Pune", "Patna").status()).isEqualTo(LocationStatus.DIFFERENT_CITY);
        assertThat(location(WorkMode.ONSITE, "Pune", "Patna").score()).isZero();
        assertThat(location(WorkMode.HYBRID, "Pune", "Patna").score()).isEqualTo(40);
    }

    @Test
    void aStateAloneIsNotTreatedAsTheSameCity() {
        assertThat(location(WorkMode.ONSITE, "Patna, Bihar", "Bihar").status()).isEqualTo(LocationStatus.DIFFERENT_CITY);
    }

    @Test
    void aMissingCityOnEitherSideShouldBeLeftOut() {
        assertThat(location(WorkMode.ONSITE, "Pune", null).status()).isEqualTo(LocationStatus.UNKNOWN);
        assertThat(location(WorkMode.ONSITE, "Pune", "   ").score()).isNull();
        assertThat(location(WorkMode.HYBRID, null, "Pune").status()).isEqualTo(LocationStatus.UNKNOWN);
    }

    @Test
    void theLocationTextsShouldBeCarriedAlongTidiedForTheExplanation() {
        Location l = location(WorkMode.ONSITE, " Pune, MH ", "Patna ");

        assertThat(l.jobLocation()).isEqualTo("Pune, MH");
        assertThat(l.candidateLocation()).isEqualTo("Patna");
    }

    // ---------- overall ----------

    @Test
    void allThreePartsShouldBeWeightedSeventyTwentyTen() {
        MatchBreakdown b = calc(new MatchInput(List.of("Java", "Spring Boot", "SQL"), JOB, 2, 0, 2, null, null, WorkMode.REMOTE));

        // (67 x 70 + 100 x 20 + 100 x 10) / 100 = 76.9
        assertThat(b.fitScore()).isEqualTo(77);
        assertThat(b.label()).isEqualTo(FitLabel.STRONG);
    }

    @Test
    void anUnknownPartShouldBeLeftOutAndNotCountedAsZero() {
        MatchBreakdown b = calc(new MatchInput(List.of("Java", "Spring Boot", "SQL"), JOB, 1, 3, null, null, null, WorkMode.ONSITE));

        // skills 67 (x70) aur experience 1 saal kam = 30 (x20). Location pata nahi: (4690 + 600) / 90 = 58.8
        assertThat(b.location().status()).isEqualTo(LocationStatus.UNKNOWN);
        assertThat(b.fitScore()).isEqualTo(59);
        assertThat(b.label()).isEqualTo(FitLabel.GOOD);
    }

    @Test
    void whenOnlySkillsAreKnownTheFitIsJustTheSkillPercent() {
        assertThat(calc(skillsOnly(JOB, JOB)).fitScore()).isEqualTo(100);
        assertThat(calc(skillsOnly(List.of("Java"), JOB)).fitScore()).isEqualTo(33);
    }

    @Test
    void anotherCityShouldPullAGoodSkillMatchDownButNotBelowWhatSkillsAndExperienceEarned() {
        MatchInput same = new MatchInput(JOB, JOB, 5, 3, null, "Patna", "Patna", WorkMode.ONSITE);
        MatchInput onsiteElsewhere = new MatchInput(JOB, JOB, 5, 3, null, "Patna", "Pune", WorkMode.ONSITE);
        MatchInput hybridElsewhere = new MatchInput(JOB, JOB, 5, 3, null, "Patna", "Pune", WorkMode.HYBRID);

        assertThat(calc(same).fitScore()).isEqualTo(100);
        assertThat(calc(onsiteElsewhere).fitScore()).isEqualTo(90);   // (7000 + 2000 + 0) / 100
        assertThat(calc(hybridElsewhere).fitScore()).isEqualTo(94);   // (7000 + 2000 + 400) / 100
    }

    @Test
    void theLabelsShouldChangeExactlyAtTheirBoundaries() {
        assertThat(FitLabel.of(100)).isEqualTo(FitLabel.STRONG);
        assertThat(FitLabel.of(75)).isEqualTo(FitLabel.STRONG);
        assertThat(FitLabel.of(74)).isEqualTo(FitLabel.GOOD);
        assertThat(FitLabel.of(50)).isEqualTo(FitLabel.GOOD);
        assertThat(FitLabel.of(49)).isEqualTo(FitLabel.PARTIAL);
        assertThat(FitLabel.of(25)).isEqualTo(FitLabel.PARTIAL);
        assertThat(FitLabel.of(24)).isEqualTo(FitLabel.LOW);
        assertThat(FitLabel.of(0)).isEqualTo(FitLabel.LOW);
        assertThat(FitLabel.of(null)).isEqualTo(FitLabel.UNKNOWN);
    }

    @Test
    void theLabelInTheBreakdownShouldMatchItsScore() {
        assertThat(calc(skillsOnly(List.of("Java", "Spring Boot", "PostgreSQL"), List.of("Java", "Spring Boot", "PostgreSQL", "Docker"))).label())
                .isEqualTo(FitLabel.STRONG);                 // 75
        assertThat(calc(skillsOnly(List.of("Java"), List.of("Java", "Docker"))).label()).isEqualTo(FitLabel.GOOD);    // 50
        assertThat(calc(skillsOnly(List.of("Java"), List.of("Java", "A", "B", "C"))).label()).isEqualTo(FitLabel.PARTIAL);   // 25
        assertThat(calc(skillsOnly(List.of("Java"), List.of("Java", "A", "B", "C", "D"))).label()).isEqualTo(FitLabel.LOW);   // 20
    }

    // ---------- suraksha ----------

    @Test
    void itShouldNeverChangeItsInputAndTheListsItReturnsShouldBeReadOnly() {
        List<String> mine = new ArrayList<>(List.of("Java"));
        List<String> job = new ArrayList<>(List.of("Java", "Docker"));
        MatchBreakdown b = calc(new MatchInput(mine, job, 3, 0, null, null, null, WorkMode.REMOTE));

        assertThat(mine).containsExactly("Java");
        assertThat(job).containsExactly("Java", "Docker");
        assertThatThrownBy(() -> b.skills().matched().add("x")).isInstanceOf(UnsupportedOperationException.class);
        assertThatThrownBy(() -> b.skills().missing().add("x")).isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void theSameInputShouldAlwaysGiveTheSameAnswer() {
        MatchInput input = new MatchInput(List.of("Java", "SQL"), JOB, 2, 3, 6, "Patna", "Pune", WorkMode.HYBRID);

        assertThat(calc(input)).isEqualTo(calc(input));
    }

    @Test
    void aMissingInputObjectIsAProgrammingError() {
        assertThatThrownBy(() -> MatchCalculator.calculate(null)).isInstanceOf(NullPointerException.class);
    }

    @Test
    void anInputWithEverythingMissingShouldGiveAnEmptyButValidAnswer() {
        MatchBreakdown b = calc(new MatchInput(null, null, null, null, null, null, null, null));

        assertThat(b.fitScore()).isNull();
        assertThat(b.label()).isEqualTo(FitLabel.UNKNOWN);
        assertThat(b.skills().status()).isEqualTo(SkillsStatus.NO_JOB_SKILLS);
        assertThat(b.experience().status()).isEqualTo(ExperienceStatus.MEETS);   // job ne kuch nahi maanga
        assertThat(b.location().status()).isEqualTo(LocationStatus.UNKNOWN);
    }
}