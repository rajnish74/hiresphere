package com.hiresphere.match;

import java.util.Set;

public record CandidateFacts(Set<String> skillKeys, Integer experienceYears, String location) {

    public CandidateFacts {
        skillKeys = skillKeys == null ? Set.of() : Set.copyOf(skillKeys);
    }
}