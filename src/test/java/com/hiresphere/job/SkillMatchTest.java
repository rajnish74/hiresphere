package com.hiresphere.job;

import com.hiresphere.job.service.SkillMatch;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class SkillMatchTest {

    private final List<String> jobSkills = List.of("java", "spring boot", "postgresql");

    @Test
    void shouldBeTheShareOfRequiredSkillsTheCandidateHas() {
        assertThat(SkillMatch.percent(List.of("java", "spring boot"), jobSkills)).isEqualTo(67);
        assertThat(SkillMatch.percent(List.of("java"), jobSkills)).isEqualTo(33);
        assertThat(SkillMatch.percent(List.of("java", "spring boot", "postgresql"), jobSkills)).isEqualTo(100);
    }

    @Test
    void extraCandidateSkillsShouldNotRaiseTheScore() {
        assertThat(SkillMatch.percent(List.of("java", "react", "docker", "kotlin"), jobSkills)).isEqualTo(33);
    }

    @Test
    void noOverlapAtAllShouldBeZeroNotNull() {
        assertThat(SkillMatch.percent(List.of("photoshop"), jobSkills)).isZero();
    }

    @Test
    void caseAndWhitespaceDifferencesShouldStillMatch() {
        assertThat(SkillMatch.percent(List.of(" JAVA ", "Spring   Boot"), List.of("Java", "spring boot")))
                .isEqualTo(100);
    }

    @Test
    void duplicatesInTheRequiredListShouldCountOnce() {
        assertThat(SkillMatch.percent(List.of("java"), List.of("Java", "java", " JAVA "))).isEqualTo(100);
    }

    @Test
    void aJobWithoutSkillsHasNoScore() {
        assertThat(SkillMatch.percent(List.of("java"), List.of())).isNull();
        assertThat(SkillMatch.percent(List.of("java"), null)).isNull();
        assertThat(SkillMatch.percent(List.of("java"), Arrays.asList("  ", null))).isNull();
    }

    @Test
    void aCandidateWithoutSkillsHasNoScore() {
        assertThat(SkillMatch.percent(List.of(), jobSkills)).isNull();
        assertThat(SkillMatch.percent(null, jobSkills)).isNull();
    }

    @Test
    void missingShouldListTheRequiredSkillsTheCandidateLacks() {
        assertThat(SkillMatch.missing(List.of("Java"), List.of("java", "Spring Boot", "PostgreSQL")))
                .containsExactly("postgresql", "spring boot");
        assertThat(SkillMatch.missing(List.of("java", "spring boot", "postgresql"), jobSkills)).isEmpty();
        assertThat(SkillMatch.missing(List.of(), jobSkills)).containsExactly("java", "postgresql", "spring boot");
    }

    @Test
    void keyShouldIgnoreCaseAndExtraSpacesAndNeverReturnNull() {
        assertThat(SkillMatch.key("  Spring   BOOT ")).isEqualTo("spring boot");
        assertThat(SkillMatch.key("C++")).isEqualTo("c++");
        assertThat(SkillMatch.key(null)).isEmpty();
    }
}