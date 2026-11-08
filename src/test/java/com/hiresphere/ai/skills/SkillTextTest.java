package com.hiresphere.ai.skills;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class SkillTextTest {

    @Test
    void realSkillNamesShouldBePlausible() {
        for (String skill : new String[]{"Java", "Spring Boot", "C++", "C#", "Node.js", ".NET", "REST APIs", "CI/CD"}) {
            assertThat(SkillText.isPlausible(skill)).as(skill).isTrue();
        }
    }

    @Test
    void strangeStringsShouldNotBePlausible() {
        for (String skill : new String[]{"", " ", "..hidden", "+Java", "#tag", "-x", ".", "http://x.com",
                "<script>", "a".repeat(41)}) {
            assertThat(SkillText.isPlausible(skill)).as(skill).isFalse();
        }
        assertThat(SkillText.isPlausible(null)).isFalse();
    }

    @Test
    void shouldMatchWholeWordsOnly() {
        assertThat(SkillText.appearsIn("Java", "I know JavaScript", true)).isFalse();
        assertThat(SkillText.appearsIn("Java", "Java, Spring", true)).isTrue();
        assertThat(SkillText.appearsIn(".NET", "ASP.NET developer", true)).isFalse();
        assertThat(SkillText.appearsIn("C++", "Systems in C++.", true)).isTrue();
    }

    @Test
    void caseSensitivityShouldBeAChoice() {
        assertThat(SkillText.appearsIn("Java", "JAVA developer", true)).isTrue();
        assertThat(SkillText.appearsIn("Java", "JAVA developer", false)).isFalse();
    }
}