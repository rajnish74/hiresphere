package com.hiresphere.skill.repository;

import com.hiresphere.skill.entity.Skill;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface SkillRepository extends JpaRepository<Skill, Long> {

    List<Skill> findByNormalizedNameIn(Collection<String> normalizedNames);
}