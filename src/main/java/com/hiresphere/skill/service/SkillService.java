package com.hiresphere.skill.service;

import com.hiresphere.skill.entity.Skill;
import com.hiresphere.skill.repository.SkillRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SkillService {

    private final SkillRepository skillRepository;

    @Transactional
    public Set<Skill> resolveAll(Collection<String> rawNames) {
        // normalized -> display name
        Map<String, String> unique = new LinkedHashMap<>();
        for (String raw : rawNames) {
            String display = raw.trim().replaceAll("\\s+", " ");
            if (!display.isEmpty()) {
                unique.putIfAbsent(display.toLowerCase(Locale.ROOT), display);
            }
        }
        if (unique.isEmpty()) {
            return new HashSet<>();
        }

        Map<String, Skill> existing = skillRepository.findByNormalizedNameIn(unique.keySet()).stream()
                .collect(Collectors.toMap(Skill::getNormalizedName, Function.identity()));

        Set<Skill> result = new HashSet<>();
        unique.forEach((normalized, display) -> {
            Skill skill = existing.get(normalized);
            if (skill == null) {
                skill = skillRepository.save(new Skill(display, normalized));
            }
            result.add(skill);
        });
        return result;
    }
}