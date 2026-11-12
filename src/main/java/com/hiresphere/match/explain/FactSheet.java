package com.hiresphere.match.explain;

import com.hiresphere.ai.prompt.UntrustedText;
import com.hiresphere.match.MatchBreakdown;
import com.hiresphere.match.MatchBreakdown.Experience;
import com.hiresphere.match.MatchBreakdown.Location;
import com.hiresphere.match.MatchBreakdown.Skills;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;


public final class FactSheet {

    private static final int MAX_NAME = 40;
    private static final String UNKNOWN = "unknown";

    private FactSheet() {
    }

    public static String build(MatchBreakdown b, String jobTitle, String company) {
        Skills s = b.skills();
        Experience e = b.experience();
        Location l = b.location();

        Map<String, String> facts = new LinkedHashMap<>();
        facts.put("job_title", one(jobTitle, 100));
        facts.put("company", one(company, 100));
        facts.put("fit_label", b.label().name());
        facts.put("fit_score", b.fitScore() == null ? UNKNOWN : String.valueOf(b.fitScore()));
        facts.put("skills_status", s.status().name());
        facts.put("skills_percent", s.percent() == null ? UNKNOWN : String.valueOf(s.percent()));
        facts.put("job_skills_total", String.valueOf(s.matched().size() + s.missing().size()));
        facts.put("skills_matched_count", String.valueOf(s.matched().size()));
        facts.put("skills_missing_count", String.valueOf(s.missing().size()));
        facts.put("skills_matched", list(s.matched()));
        facts.put("skills_missing", list(s.missing()));
        facts.put("experience_status", e.status().name());
        facts.put("candidate_experience_years", e.candidateYears() == null ? UNKNOWN : String.valueOf(e.candidateYears()));
        facts.put("job_min_years", String.valueOf(e.requiredMinYears()));
        facts.put("job_max_years", e.requiredMaxYears() == null ? "not specified" : String.valueOf(e.requiredMaxYears()));
        facts.put("experience_shortfall_years", String.valueOf(e.shortfallYears()));
        facts.put("overqualified", e.overqualified() ? "yes" : "no");
        facts.put("location_status", l.status().name());
        facts.put("job_location", one(l.jobLocation(), 80));
        facts.put("candidate_location", one(l.candidateLocation(), 80));

        return facts.entrySet().stream().map(entry -> entry.getKey() + ": " + entry.getValue())
                .collect(Collectors.joining("\n"));
    }

    private static String list(List<String> names) {
        return names.isEmpty() ? "none"
                : names.stream().map(name -> one(name, MAX_NAME)).collect(Collectors.joining("; "));
    }

    private static String one(String value, int max) {
        if (value == null) {
            return UNKNOWN;
        }
        String clean = UntrustedText.clean(value, max).text()
                .replaceAll("\\s+", " ")
                .replace(';', ',')
                .replaceAll("<{3,}", "<<")
                .replaceAll(">{3,}", ">>")
                .strip();
        return clean.isEmpty() ? UNKNOWN : clean;
    }
}