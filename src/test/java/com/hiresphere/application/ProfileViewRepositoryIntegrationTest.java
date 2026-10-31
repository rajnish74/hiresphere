package com.hiresphere.application;

import com.hiresphere.application.dto.ProfileViewerRow;
import com.hiresphere.application.repository.ProfileViewRepository;
import com.hiresphere.recruiter.entity.Company;
import com.hiresphere.recruiter.repository.CompanyRepository;
import com.hiresphere.user.entity.Role;
import com.hiresphere.user.entity.User;
import com.hiresphere.user.repository.UserRepository;
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
class ProfileViewRepositoryIntegrationTest {

    @Autowired
    private ProfileViewRepository repository;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private CompanyRepository companyRepository;

    private User candidate;
    private Company companyA;
    private Company companyB;

    @BeforeEach
    void setUp() {
        String tag = "zt" + UUID.randomUUID().toString().replace("-", "").substring(0, 10);
        candidate = newUser(tag + "-cand@test.com", Role.CANDIDATE);
        companyA = newCompany(newUser(tag + "-a@test.com", Role.RECRUITER), "A " + tag);
        companyB = newCompany(newUser(tag + "-b@test.com", Role.RECRUITER), "B " + tag);
    }

    private User newUser(String email, Role role) {
        User u = new User();
        u.setFullName("User " + email);
        u.setEmail(email);
        u.setPasswordHash("x");
        u.setRole(role);
        return userRepository.saveAndFlush(u);
    }

    private Company newCompany(User owner, String name) {
        Company c = new Company();
        c.setOwnerId(owner.getId());
        c.setName(name);
        return companyRepository.saveAndFlush(c);
    }

    @Test
    void sameCompanySameDayShouldBeCountedOnce() {
        LocalDate today = LocalDate.now();

        assertThat(repository.insertIfAbsent(companyA.getId(), candidate.getId(), today)).isEqualTo(1);
        assertThat(repository.insertIfAbsent(companyA.getId(), candidate.getId(), today)).isZero();
        assertThat(repository.insertIfAbsent(companyA.getId(), candidate.getId(), today.minusDays(1))).isEqualTo(1);
    }

    @Test
    void viewersShouldBeGroupedPerCompanyNewestFirstAndRespectTheWindow() {
        LocalDate today = LocalDate.now();
        repository.insertIfAbsent(companyA.getId(), candidate.getId(), today.minusDays(5));
        repository.insertIfAbsent(companyA.getId(), candidate.getId(), today.minusDays(2));
        repository.insertIfAbsent(companyB.getId(), candidate.getId(), today.minusDays(1));
        repository.insertIfAbsent(companyB.getId(), candidate.getId(), today.minusDays(60));   // window ke bahar

        List<ProfileViewerRow> viewers = repository.findViewers(candidate.getId(), today.minusDays(30));

        assertThat(viewers).hasSize(2);
        assertThat(viewers.get(0).companyId()).isEqualTo(companyB.getId());      // sabse nayi (1 din pehle)
        assertThat(viewers.get(0).lastViewedOn()).isEqualTo(today.minusDays(1));
        assertThat(viewers.get(1).lastViewedOn()).isEqualTo(today.minusDays(2)); // A ka sabse naya
    }

    @Test
    void deleteOlderThanShouldRemoveOnlyOldRows() {
        LocalDate today = LocalDate.now();
        repository.insertIfAbsent(companyA.getId(), candidate.getId(), today.minusDays(100));
        repository.insertIfAbsent(companyA.getId(), candidate.getId(), today);

        assertThat(repository.deleteOlderThan(today.minusDays(90))).isGreaterThanOrEqualTo(1);

        assertThat(repository.findViewers(candidate.getId(), today.minusDays(200))).hasSize(1);
    }
}