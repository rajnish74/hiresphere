package com.hiresphere.ai.resume;

import java.util.List;


public record ResumeSkillSuggestions(String resumeName, List<SuggestedSkill> skills, String source, boolean degraded,
                                     String degradedReason, boolean cached, boolean partial, int pagesRead,
                                     int totalPages, int contactDetailsRemoved) {

    public record SuggestedSkill(String name, boolean onProfile) {
    }
}