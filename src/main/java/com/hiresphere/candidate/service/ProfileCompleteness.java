package com.hiresphere.candidate.service;

import com.hiresphere.candidate.entity.CandidateProfile;

import java.util.ArrayList;
import java.util.List;

public final class ProfileCompleteness {

    public record Result(int percentage, List<String> missing) {
    }

    private ProfileCompleteness() {
    }

    public static Result calculate(CandidateProfile p) {
        int score = 0;
        List<String> missing = new ArrayList<>();

        score += award(hasText(p.getHeadline()), 15, "headline", missing);
        score += award(hasText(p.getSummary()), 15, "summary", missing);
        score += award(!p.getSkills().isEmpty(), 20, "skills", missing);
        score += award(!p.getEducation().isEmpty(), 15, "education", missing);
        score += award(!p.getExperience().isEmpty(), 15, "experience", missing);
        score += award(p.getResumeStorageKey() != null, 20, "resume", missing);

        return new Result(score, List.copyOf(missing));
    }

    private static int award(boolean condition, int points, String name, List<String> missing) {
        if (condition) {
            return points;
        }
        missing.add(name);
        return 0;
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}