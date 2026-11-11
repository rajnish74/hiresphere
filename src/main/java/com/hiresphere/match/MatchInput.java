package com.hiresphere.match;

import com.hiresphere.job.entity.WorkMode;

import java.util.List;
import java.util.Objects;


public record MatchInput(List<String> candidateSkills, List<String> jobSkills, Integer candidateExperienceYears,
                         Integer jobMinYears, Integer jobMaxYears, String candidateLocation, String jobLocation,
                         WorkMode workMode) {

    public MatchInput {
        candidateSkills = candidateSkills == null ? List.of() : candidateSkills.stream().filter(Objects::nonNull).toList();
        jobSkills = jobSkills == null ? List.of() : jobSkills.stream().filter(Objects::nonNull).toList();
    }
}