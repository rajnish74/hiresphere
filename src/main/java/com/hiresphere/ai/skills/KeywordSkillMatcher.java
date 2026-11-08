package com.hiresphere.ai.skills;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;


public final class KeywordSkillMatcher {

    private static final int MIN_LENGTH = 2;
    private static final int CASE_SENSITIVE_UP_TO = 3;

    private KeywordSkillMatcher() {
    }

    public static List<String> match(String text, Collection<String> knownSkills, int max) {
        if (text == null || text.isBlank() || knownSkills == null || max < 1) {
            return List.of();
        }
        Set<String> seen = new HashSet<>();
        List<String> found = new ArrayList<>();
        for (String known : knownSkills) {
            String skill = known == null ? "" : known.strip().replaceAll("\\s+", " ");
            if (skill.length() < MIN_LENGTH || !SkillText.isPlausible(skill)
                    || !seen.add(skill.toLowerCase(Locale.ROOT))) {
                continue;
            }
            boolean ignoreCase = skill.length() > CASE_SENSITIVE_UP_TO;
            if (SkillText.appearsIn(skill, text, ignoreCase)) {
                found.add(skill);
                if (found.size() == max) {
                    break;
                }
            }
        }
        return List.copyOf(found);
    }
}