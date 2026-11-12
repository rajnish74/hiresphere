package com.hiresphere.match;

import com.hiresphere.candidate.repository.ExperiencePeriodRepository;
import com.hiresphere.candidate.service.CandidateProfileService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DbMatchProfileSourceTest {

    @Mock
    private CandidateProfileService profiles;
    @Mock
    private ExperiencePeriodRepository experience;

    private DbMatchProfileSource source;

    @BeforeEach
    void setUp() {
        source = new DbMatchProfileSource(profiles, experience,
                Clock.fixed(Instant.parse("2026-11-12T10:00:00Z"), ZoneOffset.UTC));
    }

    private static ExperiencePeriodRepository.Dates dates(String start, String end) {
        return new ExperiencePeriodRepository.Dates() {
            @Override
            public LocalDate getStartDate() {
                return start == null ? null : LocalDate.parse(start);
            }

            @Override
            public LocalDate getEndDate() {
                return end == null ? null : LocalDate.parse(end);
            }
        };
    }

    @Test
    void shouldCombineSkillsExperienceAndLocation() {
        when(profiles.findSkillKeys(7L)).thenReturn(Set.of("java", "sql"));
        when(profiles.findLocation(7L)).thenReturn(Optional.of("Patna"));
        when(experience.findDatesByUserId(7L)).thenReturn(List.of(dates("2023-11-12", null)));   // abhi bhi chal raha

        CandidateFacts facts = source.factsOf(7L);

        assertThat(facts.skillKeys()).containsExactlyInAnyOrder("java", "sql");
        assertThat(facts.location()).isEqualTo("Patna");
        assertThat(facts.experienceYears()).isEqualTo(3);
    }

    @Test
    void aCandidateWithNothingFilledInShouldGetEmptyFactsAndUnknownExperience() {
        CandidateFacts facts = source.factsOf(7L);

        assertThat(facts.skillKeys()).isEmpty();
        assertThat(facts.location()).isNull();
        assertThat(facts.experienceYears()).isNull();
    }

    @Test
    void overlappingEntriesShouldNotBeDoubleCounted() {
        when(experience.findDatesByUserId(7L)).thenReturn(List.of(
                dates("2019-01-01", "2021-01-01"), dates("2020-01-01", "2022-01-01")));

        assertThat(source.factsOf(7L).experienceYears()).isEqualTo(3);
    }

    @Test
    void anEntryWithoutAStartDateShouldBeIgnoredNotCrash() {
        when(experience.findDatesByUserId(7L)).thenReturn(List.of(dates(null, "2021-01-01"), dates("2020-01-01", "2021-01-01")));

        assertThat(source.factsOf(7L).experienceYears()).isEqualTo(1);
    }
}