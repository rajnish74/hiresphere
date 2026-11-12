
package com.hiresphere.match;

import com.hiresphere.job.entity.WorkMode;
import com.hiresphere.job.service.SkillMatch;
import com.hiresphere.match.MatchBreakdown.Experience;
import com.hiresphere.match.MatchBreakdown.ExperienceStatus;
import com.hiresphere.match.MatchBreakdown.Location;
import com.hiresphere.match.MatchBreakdown.LocationStatus;
import com.hiresphere.match.MatchBreakdown.Skills;
import com.hiresphere.match.MatchBreakdown.SkillsStatus;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

public final class MatchCalculator {

    static final int SKILLS_WEIGHT = 70;
    static final int EXPERIENCE_WEIGHT = 20;
    static final int LOCATION_WEIGHT = 10;
    static final int SHORTFALL_PENALTY = 35;
    static final int HYBRID_OTHER_CITY_SCORE = 40;

    /** Experience aur location skills ke score ko is se zyada upar nahi utha sakte: 0 skills ka matlab "low", chahe job remote ho. */
    static final int SUPPORT_BOOST = 15;

    private MatchCalculator() {
    }

    public static MatchBreakdown calculate(MatchInput input) {
        Objects.requireNonNull(input, "input");
        Skills skills = skills(input);
        Experience experience = experience(input);
        Location location = location(input);

        Integer fit = null;
        if (skills.percent() != null) {
            int sum = skills.percent() * SKILLS_WEIGHT;
            int weight = SKILLS_WEIGHT;
            if (experience.score() != null) {
                sum += experience.score() * EXPERIENCE_WEIGHT;
                weight += EXPERIENCE_WEIGHT;
            }
            if (location.score() != null) {
                sum += location.score() * LOCATION_WEIGHT;
                weight += LOCATION_WEIGHT;
            }
            fit = (int) Math.round((double) sum / weight);
            fit = Math.min(fit, skills.percent() + SUPPORT_BOOST);
        }
        return new MatchBreakdown(fit, FitLabel.of(fit), skills, experience, location);
    }

    // ---------- skills ----------

    private static Skills skills(MatchInput in) {
        Set<String> mine = in.candidateSkills().stream().map(SkillMatch::key)
                .filter(key -> !key.isEmpty()).collect(Collectors.toSet());

        Map<String, String> required = new LinkedHashMap<>();
        for (String skill : in.jobSkills()) {
            String key = SkillMatch.key(skill);
            if (!key.isEmpty()) {
                required.putIfAbsent(key, skill.strip().replaceAll("\\s+", " "));
            }
        }

        if (required.isEmpty()) {
            return new Skills(SkillsStatus.NO_JOB_SKILLS, null, List.of(), List.of());
        }
        if (mine.isEmpty()) {
            return new Skills(SkillsStatus.NO_CANDIDATE_SKILLS, null, List.of(), List.copyOf(required.values()));
        }

        List<String> matched = new ArrayList<>();
        List<String> missing = new ArrayList<>();
        required.forEach((key, display) -> (mine.contains(key) ? matched : missing).add(display));

        return new Skills(SkillsStatus.SCORED, SkillMatch.percent(in.candidateSkills(), in.jobSkills()),
                List.copyOf(matched), List.copyOf(missing));
    }

    // ---------- experience ----------

    private static Experience experience(MatchInput in) {
        int min = Math.max(0, in.jobMinYears() == null ? 0 : in.jobMinYears());
        Integer max = in.jobMaxYears();
        Integer years = in.candidateExperienceYears() == null ? null : Math.max(0, in.candidateExperienceYears());

        if (years == null) {
            return min == 0
                    ? new Experience(ExperienceStatus.MEETS, null, min, max, 0, false, 100)
                    : new Experience(ExperienceStatus.UNKNOWN, null, min, max, 0, false, null);
        }
        if (years >= min) {
            return new Experience(ExperienceStatus.MEETS, years, min, max, 0, max != null && years > max, 100);
        }
        int shortfall = min - years;
        return new Experience(ExperienceStatus.BELOW, years, min, max, shortfall, false,
                Math.max(0, 100 - SHORTFALL_PENALTY * shortfall));
    }

    // ---------- location ----------

    private static Location location(MatchInput in) {
        String jobShown = blankToNull(in.jobLocation());
        String candidateShown = blankToNull(in.candidateLocation());

        if (in.workMode() == WorkMode.REMOTE) {
            return new Location(LocationStatus.REMOTE, 100, jobShown, candidateShown);
        }
        String jobCity = city(in.jobLocation());
        String candidateCity = city(in.candidateLocation());
        if (jobCity.isEmpty() || candidateCity.isEmpty()) {
            return new Location(LocationStatus.UNKNOWN, null, jobShown, candidateShown);
        }
        if (jobCity.equals(candidateCity)) {
            return new Location(LocationStatus.SAME_CITY, 100, jobShown, candidateShown);
        }
        int score = in.workMode() == WorkMode.HYBRID ? HYBRID_OTHER_CITY_SCORE : 0;
        return new Location(LocationStatus.DIFFERENT_CITY, score, jobShown, candidateShown);
    }

    private static String city(String location) {
        if (location == null) {
            return "";
        }
        int comma = location.indexOf(',');
        return SkillMatch.key(comma < 0 ? location : location.substring(0, comma));
    }

    private static String blankToNull(String text) {
        return text == null || text.isBlank() ? null : text.strip();
    }
}
