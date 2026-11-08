package com.hiresphere.ai.skills;

import com.hiresphere.ai.AiResponse;

import java.util.List;

public record SkillSuggestions(List<String> skills, Source source, String degradedReason, boolean inputTruncated,
                               int droppedCount, AiResponse ai) {

    public enum Source { AI, KEYWORD }

    public boolean degraded() {
        return source == Source.KEYWORD;
    }
}