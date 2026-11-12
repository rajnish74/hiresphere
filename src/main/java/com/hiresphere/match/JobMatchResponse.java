package com.hiresphere.match;

import com.hiresphere.match.explain.MatchExplanation;

import java.util.List;

public record JobMatchResponse(Long jobId, String jobTitle, String companyName, Integer fitScore, FitLabel label,
                               MatchBreakdown.Skills skills, MatchBreakdown.Experience experience,
                               MatchBreakdown.Location location, Explanation explanation) {

    public record Explanation(String summary, List<String> strengths, List<String> gaps, String tip, String source,
                              boolean degraded, String degradedReason, boolean cached) {

        static Explanation from(MatchExplanation e) {
            return new Explanation(e.summary(), e.strengths(), e.gaps(), e.tip(), e.source().name(), e.degraded(),
                    e.degradedReason(), e.cached());
        }
    }

    static JobMatchResponse of(JobFacts job, MatchBreakdown b, MatchExplanation explanation) {
        return new JobMatchResponse(job.id(), job.title(), job.companyName(), b.fitScore(), b.label(), b.skills(),
                b.experience(), b.location(), Explanation.from(explanation));
    }
}