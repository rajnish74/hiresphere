
package com.hiresphere.job.service;

import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

public final class SkillMatch {

    private SkillMatch() {
    }

    public static String key(String skill) {
        return skill == null ? "" : skill.trim().replaceAll("\\s+", " ").toLowerCase(Locale.ROOT);
    }

    public static Integer percent(Collection<String> candidateSkills, Collection<String> requiredSkills) {
        Set<String> required = normalize(requiredSkills);
        Set<String> mine = normalize(candidateSkills);
        if (required.isEmpty() || mine.isEmpty()) {
            return null;
        }
        long overlap = required.stream().filter(mine::contains).count();
        return (int) Math.round(100.0 * overlap / required.size());
    }

    public static List<String> missing(Collection<String> candidateSkills, Collection<String> requiredSkills) {
        Set<String> mine = normalize(candidateSkills);
        return normalize(requiredSkills).stream().filter(s -> !mine.contains(s)).sorted().toList();
    }

    private static Set<String> normalize(Collection<String> skills) {
        if (skills == null) {
            return Set.of();
        }
        return skills.stream()
                .filter(Objects::nonNull)
                .map(SkillMatch::key)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toSet());
    }
}
