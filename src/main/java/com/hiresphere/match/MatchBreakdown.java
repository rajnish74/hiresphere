package com.hiresphere.match;

import java.util.List;

public record MatchBreakdown(Integer fitScore, FitLabel label, Skills skills, Experience experience,
                             Location location) {

    public enum SkillsStatus {
        SCORED,
        NO_JOB_SKILLS,
        NO_CANDIDATE_SKILLS
    }

    public record Skills(SkillsStatus status, Integer percent, List<String> matched, List<String> missing) {
    }

    public enum ExperienceStatus {
        MEETS,
        BELOW,
        UNKNOWN
    }

    public record Experience(ExperienceStatus status, Integer candidateYears, int requiredMinYears,
                             Integer requiredMaxYears, int shortfallYears, boolean overqualified, Integer score) {
    }

    public enum LocationStatus {
        REMOTE,
        SAME_CITY,
        DIFFERENT_CITY,
        UNKNOWN
    }

    public record Location(LocationStatus status, Integer score, String jobLocation, String candidateLocation) {
    }
}