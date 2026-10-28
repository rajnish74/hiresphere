package com.hiresphere.skill;

import com.hiresphere.skill.entity.Skill;
import com.hiresphere.skill.repository.SkillRepository;
import com.hiresphere.skill.service.SkillService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SkillServiceTest {

    @Mock
    private SkillRepository skillRepository;

    private SkillService service;

    @BeforeEach
    void setUp() {
        service = new SkillService(skillRepository);
    }

    @Test
    void caseAndWhitespaceVariantsShouldCreateOnlyOneSkill() {
        when(skillRepository.findByNormalizedNameIn(any())).thenReturn(List.of());
        when(skillRepository.save(any(Skill.class))).thenAnswer(inv -> inv.getArgument(0));

        Set<Skill> result = service.resolveAll(List.of("Java", "java", " JAVA "));

        assertThat(result).hasSize(1);
        assertThat(result.iterator().next().getName()).isEqualTo("Java");   // pehli spelling bachi
        verify(skillRepository, times(1)).save(any(Skill.class));
    }

    @Test
    void existingSkillShouldBeReusedNotCreatedAgain() {
        Skill existing = new Skill("Java", "java");
        when(skillRepository.findByNormalizedNameIn(any())).thenReturn(List.of(existing));

        Set<Skill> result = service.resolveAll(List.of("JAVA"));

        assertThat(result).containsExactly(existing);
        verify(skillRepository, never()).save(any());
    }

    @Test
    void blankInputsShouldBeIgnored() {
        Set<Skill> result = service.resolveAll(List.of("  ", ""));

        assertThat(result).isEmpty();
        verify(skillRepository, never()).findByNormalizedNameIn(any());
    }
}