package com.hiresphere.job;

import com.hiresphere.job.dto.PublicJobSummary;
import com.hiresphere.job.entity.Job;
import com.hiresphere.skill.entity.Skill;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PublicJobSummaryTest {

    @Test
    void shouldCarrySkillsSortedCaseInsensitively() {
        Job job = new Job();
        job.setId(1L);
        job.setCompanyId(5L);
        job.setTitle("Java Dev");
        job.getSkills().add(new Skill("spring boot", "spring boot"));
        job.getSkills().add(new Skill("Java", "java"));
        job.getSkills().add(new Skill("SQL", "sql"));

        PublicJobSummary summary = PublicJobSummary.from(job, "Acme");

        assertThat(summary.skills()).containsExactly("Java", "spring boot", "SQL");
        assertThat(summary.companyName()).isEqualTo("Acme");
    }
}