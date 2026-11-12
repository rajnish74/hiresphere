package com.hiresphere.candidate;

import com.hiresphere.candidate.entity.CandidateProfile;
import com.hiresphere.candidate.entity.Experience;
import com.hiresphere.candidate.repository.CandidateProfileRepository;
import com.hiresphere.candidate.repository.ExperiencePeriodRepository;
import com.hiresphere.candidate.service.CandidateProfileService;
import com.hiresphere.user.entity.Role;
import com.hiresphere.user.entity.User;
import com.hiresphere.user.repository.UserRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
class ExperiencePeriodRepositoryIntegrationTest {

    @Autowired
    private ExperiencePeriodRepository repository;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private CandidateProfileService profileService;
    @Autowired
    private CandidateProfileRepository profileRepository;
    @Autowired
    private EntityManager em;

    private User alice;
    private User bob;

    @BeforeEach
    void setUp() {
        String tag = "zt" + UUID.randomUUID().toString().replace("-", "").substring(0, 10);
        alice = newUser(tag + "-a@test.com");
        bob = newUser(tag + "-b@test.com");
    }

    private User newUser(String email) {
        User u = new User();
        u.setFullName("User " + email);
        u.setEmail(email);
        u.setPasswordHash("x");
        u.setRole(Role.CANDIDATE);
        User saved = userRepository.saveAndFlush(u);
        profileService.getSnapshot(saved.getId());   // profile row bana deta hai
        return saved;
    }

    private void addExperience(User user, String start, String end) {
        CandidateProfile profile = profileRepository.findByUserId(user.getId()).orElseThrow();
        Experience e = new Experience();
        e.setCandidate(profile);
        e.setCompany("Acme");
        e.setTitle("Developer");
        e.setStartDate(LocalDate.parse(start));
        e.setEndDate(end == null ? null : LocalDate.parse(end));
        em.persist(e);
        em.flush();
    }

    @Test
    void shouldReturnOnlyTheCandidatesOwnDatesIncludingAnOpenEndedOne() {
        addExperience(alice, "2019-01-01", "2021-01-01");
        addExperience(alice, "2022-06-01", null);
        addExperience(bob, "2010-01-01", "2012-01-01");

        List<ExperiencePeriodRepository.Dates> dates = repository.findDatesByUserId(alice.getId());

        assertThat(dates).hasSize(2);
        assertThat(dates).extracting(ExperiencePeriodRepository.Dates::getStartDate)
                .containsExactlyInAnyOrder(LocalDate.parse("2019-01-01"), LocalDate.parse("2022-06-01"));
        assertThat(dates).extracting(ExperiencePeriodRepository.Dates::getEndDate)
                .containsExactlyInAnyOrder(LocalDate.parse("2021-01-01"), null);
    }

    @Test
    void aCandidateWithoutExperienceShouldGetAnEmptyList() {
        assertThat(repository.findDatesByUserId(alice.getId())).isEmpty();
        assertThat(repository.findDatesByUserId(987_654_321L)).isEmpty();
    }
}