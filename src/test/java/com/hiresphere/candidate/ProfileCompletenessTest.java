package com.hiresphere.candidate;

import com.hiresphere.candidate.entity.CandidateProfile;
import com.hiresphere.candidate.entity.Education;
import com.hiresphere.candidate.entity.Experience;
import com.hiresphere.candidate.service.ProfileCompleteness;
import com.hiresphere.skill.entity.Skill;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ProfileCompletenessTest {

    @Test
    void emptyProfileShouldBeZeroPercent() {
        ProfileCompleteness.Result result = ProfileCompleteness.calculate(new CandidateProfile());

        assertThat(result.percentage()).isZero();
        assertThat(result.missing()).containsExactly(
                "headline", "summary", "skills", "education", "experience", "resume");
    }

    @Test
    void fullProfileShouldBeHundredPercent() {
        CandidateProfile p = new CandidateProfile();
        p.setHeadline("Java Developer");
        p.setSummary("About me");
        p.getSkills().add(new Skill("Java", "java"));
        p.getEducation().add(new Education());
        p.getExperience().add(new Experience());
        p.setResumeStorageKey("abc.pdf");

        ProfileCompleteness.Result result = ProfileCompleteness.calculate(p);

        assertThat(result.percentage()).isEqualTo(100);
        assertThat(result.missing()).isEmpty();
    }

    @Test
    void partialProfileShouldSumOnlyFilledSections() {
        CandidateProfile p = new CandidateProfile();
        p.setHeadline("Java Developer");
        p.getSkills().add(new Skill("Java", "java"));

        ProfileCompleteness.Result result = ProfileCompleteness.calculate(p);

        assertThat(result.percentage()).isEqualTo(35);   // 15 + 20
        assertThat(result.missing()).containsExactly("summary", "education", "experience", "resume");
    }

    @Test
    void blankHeadlineShouldNotCount() {
        CandidateProfile p = new CandidateProfile();
        p.setHeadline("   ");

        assertThat(ProfileCompleteness.calculate(p).percentage()).isZero();
    }
}