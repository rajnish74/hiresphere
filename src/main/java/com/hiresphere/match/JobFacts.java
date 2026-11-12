package com.hiresphere.match;

import com.hiresphere.job.entity.WorkMode;

import java.util.List;

public record JobFacts(Long id, String title, String companyName, String location, WorkMode workMode,
                       Integer minYears, Integer maxYears, List<String> skills) {

    public JobFacts {
        skills = skills == null ? List.of() : List.copyOf(skills);
    }
}