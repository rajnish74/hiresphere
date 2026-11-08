package com.hiresphere.ai.skills;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class KeywordSkillMatcherTest {

    private static final List<String> KNOWN = List.of("Docker", "Go", "Java", "JavaScript", "Kubernetes", "SQL", "Spring Boot");

    @Test
    void shouldFindTheKnownSkillsThatAreWrittenInTheText() {
        List<String> found = KeywordSkillMatcher.match("Built services with Java and Docker, deployed on Kubernetes.", KNOWN, 40);

        assertThat(found).containsExactly("Docker", "Java", "Kubernetes");
    }

    @Test
    void javaShouldNotBeFoundInsideJavascript() {
        assertThat(KeywordSkillMatcher.match("Frontend work in JavaScript.", KNOWN, 40)).containsExactly("JavaScript");
    }

    @Test
    void longerSkillsShouldIgnoreCase() {
        assertThat(KeywordSkillMatcher.match("SPRING BOOT and docker", KNOWN, 40)).containsExactly("Docker", "Spring Boot");
    }

    @Test
    void shortSkillsShouldMatchOnlyInTheirOwnCase() {
        assertThat(KeywordSkillMatcher.match("Ready to go to market and write sql.", KNOWN, 40)).isEmpty();
        assertThat(KeywordSkillMatcher.match("Services written in Go and SQL.", KNOWN, 40)).containsExactly("Go", "SQL");
    }

    @Test
    void singleLetterSkillsShouldBeSkippedBecauseTheyMatchTooMuch() {
        assertThat(KeywordSkillMatcher.match("R&D and C programming", List.of("C", "R"), 40)).isEmpty();
    }

    @Test
    void duplicatesAndOddEntriesShouldBeIgnored() {
        List<String> known = Arrays.asList("Docker", "docker", "  Docker ", null, "", "<script>", "a".repeat(50));

        assertThat(KeywordSkillMatcher.match("We use Docker daily", known, 40)).containsExactly("Docker");
    }

    @Test
    void shouldStopAtTheLimit() {
        assertThat(KeywordSkillMatcher.match("Docker Java Kubernetes", KNOWN, 2)).hasSize(2);
    }

    @Test
    void emptyInputsShouldGiveNothing() {
        assertThat(KeywordSkillMatcher.match(null, KNOWN, 40)).isEmpty();
        assertThat(KeywordSkillMatcher.match("   ", KNOWN, 40)).isEmpty();
        assertThat(KeywordSkillMatcher.match("Java", null, 40)).isEmpty();
        assertThat(KeywordSkillMatcher.match("Java", KNOWN, 0)).isEmpty();
    }
}