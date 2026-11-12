package com.hiresphere.match;

import com.hiresphere.candidate.repository.ExperiencePeriodRepository;
import com.hiresphere.candidate.service.CandidateProfileService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;

@Component
public class DbMatchProfileSource implements MatchProfileSource {

    private final CandidateProfileService profiles;
    private final ExperiencePeriodRepository experience;
    private final Clock clock;

    @Autowired
    public DbMatchProfileSource(CandidateProfileService profiles, ExperiencePeriodRepository experience) {
        this(profiles, experience, Clock.systemUTC());
    }

    public DbMatchProfileSource(CandidateProfileService profiles, ExperiencePeriodRepository experience, Clock clock) {
        this.profiles = profiles;
        this.experience = experience;
        this.clock = clock;
    }

    @Override
    public CandidateFacts factsOf(Long userId) {
        List<ExperienceYears.Period> periods = experience.findDatesByUserId(userId).stream()
                .map(d -> new ExperienceYears.Period(d.getStartDate(), d.getEndDate()))
                .toList();
        return new CandidateFacts(
                profiles.findSkillKeys(userId),
                ExperienceYears.totalYears(periods, LocalDate.now(clock.withZone(ZoneOffset.UTC))),
                profiles.findLocation(userId).orElse(null));
    }
}