package com.hiresphere.match.explain;

import java.util.List;

public record ExplanationText(String summary, List<String> strengths, List<String> gaps, String tip) {

    public ExplanationText {
        strengths = strengths == null ? List.of() : List.copyOf(strengths);
        gaps = gaps == null ? List.of() : List.copyOf(gaps);
    }
}