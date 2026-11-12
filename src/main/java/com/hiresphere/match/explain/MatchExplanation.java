package com.hiresphere.match.explain;

import java.util.List;


public record MatchExplanation(String summary, List<String> strengths, List<String> gaps, String tip, Source source,
                               String degradedReason, boolean cached) {

    public enum Source { AI, TEMPLATE }

    public boolean degraded() {
        return source == Source.TEMPLATE && degradedReason != null;
    }

    static MatchExplanation ai(ExplanationText text, boolean cached) {
        return new MatchExplanation(text.summary(), text.strengths(), text.gaps(), text.tip(), Source.AI, null, cached);
    }

    static MatchExplanation template(ExplanationText text, String degradedReason) {
        return new MatchExplanation(text.summary(), text.strengths(), text.gaps(), text.tip(), Source.TEMPLATE,
                degradedReason, false);
    }
}