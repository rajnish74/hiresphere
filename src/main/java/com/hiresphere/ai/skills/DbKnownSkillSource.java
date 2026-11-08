package com.hiresphere.ai.skills;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.List;

@Component
@RequiredArgsConstructor
public class DbKnownSkillSource implements KnownSkillSource {

    private static final int LIMIT = 5000;

    private final EntityManager em;
    private final Cache<String, List<String>> cache =
            Caffeine.newBuilder().expireAfterWrite(Duration.ofMinutes(10)).maximumSize(1).build();

    @Override
    public List<String> names() {
        return cache.get("all", key -> List.copyOf(
                em.createQuery("select s.name from Skill s order by s.name", String.class)
                        .setMaxResults(LIMIT).getResultList()));
    }
}