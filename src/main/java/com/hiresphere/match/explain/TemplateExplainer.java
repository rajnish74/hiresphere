package com.hiresphere.match.explain;

import com.hiresphere.match.FitLabel;
import com.hiresphere.match.MatchBreakdown;
import com.hiresphere.match.MatchBreakdown.Experience;
import com.hiresphere.match.MatchBreakdown.ExperienceStatus;
import com.hiresphere.match.MatchBreakdown.Location;
import com.hiresphere.match.MatchBreakdown.LocationStatus;
import com.hiresphere.match.MatchBreakdown.Skills;
import com.hiresphere.match.MatchBreakdown.SkillsStatus;

import java.util.ArrayList;
import java.util.List;


public final class TemplateExplainer {

    private static final int MAX_NAMES = 5;

    private TemplateExplainer() {
    }

    public static ExplanationText explain(MatchBreakdown b) {
        return new ExplanationText(summary(b), strengths(b), gaps(b), tip(b));
    }

    private static String summary(MatchBreakdown b) {
        Skills s = b.skills();
        if (s.status() == SkillsStatus.NO_JOB_SKILLS) {
            return "This job does not list any skills, so we cannot score how well you match.";
        }
        if (s.status() == SkillsStatus.NO_CANDIDATE_SKILLS) {
            return "Add your skills to your profile and we can show how well you match this job.";
        }
        int matched = s.matched().size();
        int total = matched + s.missing().size();
        String have;
        if (matched == total) {
            have = total == 1 ? "You have the skill the job lists." : "You have all " + total + " skills the job lists.";
        } else if (matched == 0) {
            have = total == 1 ? "The skill this job lists is not on your profile yet."
                    : "None of the " + total + " skills this job lists are on your profile yet.";
        } else {
            have = "You have " + matched + " of the " + total + " skills the job lists.";
        }
        return lead(b.label()) + have;
    }

    private static String lead(FitLabel label) {
        return switch (label) {
            case STRONG -> "This looks like a strong match. ";
            case GOOD -> "This is a good match. ";
            case PARTIAL -> "This is a partial match. ";
            case LOW -> "This is a low match for now. ";
            case UNKNOWN -> "";
        };
    }

    private static List<String> strengths(MatchBreakdown b) {
        List<String> out = new ArrayList<>();
        Skills s = b.skills();
        if (!s.matched().isEmpty()) {
            out.add("You already have " + names(s.matched()) + ".");
        }
        Experience e = b.experience();
        if (e.status() == ExperienceStatus.MEETS && e.candidateYears() != null && e.requiredMinYears() > 0) {
            out.add("Your " + years(e.candidateYears()) + " of experience meets the " + years(e.requiredMinYears())
                    + " the job asks for.");
        }
        Location l = b.location();
        if (l.status() == LocationStatus.REMOTE) {
            out.add("The job is remote, so your city does not matter.");
        } else if (l.status() == LocationStatus.SAME_CITY) {
            out.add("The job is in your city.");
        }
        return List.copyOf(out);
    }

    private static List<String> gaps(MatchBreakdown b) {
        List<String> out = new ArrayList<>();
        Skills s = b.skills();
        if (!s.missing().isEmpty()) {
            out.add(s.status() == SkillsStatus.NO_CANDIDATE_SKILLS
                    ? "The job asks for " + names(s.missing()) + "."
                    : "Not on your profile yet: " + names(s.missing()) + ".");
        }
        Experience e = b.experience();
        if (e.status() == ExperienceStatus.BELOW && e.candidateYears() != null) {
            out.add("The job asks for " + years(e.requiredMinYears()) + " of experience and you have "
                    + years(e.candidateYears()) + ".");
        }
        Location l = b.location();
        if (l.status() == LocationStatus.DIFFERENT_CITY) {
            out.add("The job is in " + l.jobLocation() + " and your profile says " + l.candidateLocation() + ".");
        }
        return List.copyOf(out);
    }

    private static String tip(MatchBreakdown b) {
        Skills s = b.skills();
        if (s.status() == SkillsStatus.NO_JOB_SKILLS) {
            return "Read the job description to judge the fit yourself.";
        }
        if (s.status() == SkillsStatus.NO_CANDIDATE_SKILLS) {
            return "Add your skills to your profile, or upload your resume to get suggestions.";
        }
        if (!s.missing().isEmpty()) {
            return "If you have worked with " + s.missing().get(0) + ", add it to your profile so it counts.";
        }
        if (b.experience().status() == ExperienceStatus.BELOW) {
            return "Add projects that show related work to strengthen your profile.";
        }
        return "Read the full job description before you apply.";
    }

    static String names(List<String> list) {
        if (list.size() > MAX_NAMES) {
            return String.join(", ", list.subList(0, MAX_NAMES)) + " and more";
        }
        if (list.size() == 1) {
            return list.get(0);
        }
        return String.join(", ", list.subList(0, list.size() - 1)) + " and " + list.get(list.size() - 1);
    }

    static String years(int n) {
        return n == 1 ? "1 year" : n + " years";
    }
}