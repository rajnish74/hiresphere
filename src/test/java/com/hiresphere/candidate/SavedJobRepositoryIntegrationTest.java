package com.hiresphere.candidate;

import com.hiresphere.candidate.entity.SavedJob;
import com.hiresphere.candidate.repository.SavedJobRepository;
import com.hiresphere.job.entity.Job;
import com.hiresphere.job.entity.JobType;
import com.hiresphere.job.entity.WorkMode;
import com.hiresphere.job.repository.JobRepository;
import com.hiresphere.recruiter.entity.Company;
import com.hiresphere.recruiter.repository.CompanyRepository;
import com.hiresphere.user.entity.Role;
import com.hiresphere.user.entity.User;
import com.hiresphere.user.repository.UserRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
class SavedJobRepositoryIntegrationTest {

    @Autowired
    private SavedJobRepository savedJobRepository;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private CompanyRepository companyRepository;
    @Autowired
    private JobRepository jobRepository;
    @Autowired
    private EntityManager em;

    private User candidate;
    private Job job;

    @BeforeEach
    void setUp() {
        String tag = "zt" + UUID.randomUUID().toString().replace("-", "").substring(0, 10);
        User owner = newUser(tag + "-owner@test.com", Role.RECRUITER);
        candidate = newUser(tag + "-cand@test.com", Role.CANDIDATE);

        Company company = new Company();
        company.setOwnerId(owner.getId());
        company.setName("Company " + tag);
        company = companyRepository.saveAndFlush(company);

        Job j = new Job();
        j.setCompanyId(company.getId());
        j.setTitle("Saved " + tag);
        j.setDescription("A reasonably long job description for testing.");
        j.setJobType(JobType.FULL_TIME);
        j.setWorkMode(WorkMode.REMOTE);
        job = jobRepository.saveAndFlush(j);
    }

    private User newUser(String email, Role role) {
        User u = new User();
        u.setFullName("User " + email);
        u.setEmail(email);
        u.setPasswordHash("x");
        u.setRole(role);
        return userRepository.saveAndFlush(u);
    }

    @Test
    void insertIfAbsentShouldBeIdempotent() {
        assertThat(savedJobRepository.insertIfAbsent(candidate.getId(), job.getId(), "Title", "Acme")).isEqualTo(1);
        assertThat(savedJobRepository.insertIfAbsent(candidate.getId(), job.getId(), "Title", "Acme")).isZero();

        assertThat(savedJobRepository.countByCandidateUserId(candidate.getId())).isEqualTo(1);
        em.clear();
        Page<SavedJob> page = savedJobRepository.findByCandidateUserId(candidate.getId(), PageRequest.of(0, 10));
        assertThat(page.getContent()).hasSize(1);
        assertThat(page.getContent().get(0).getCreatedAt()).isNotNull();   // DB ka default bhara
        assertThat(page.getContent().get(0).getJobTitle()).isEqualTo("Title");
    }

    @Test
    void deleteOneShouldRemoveTheRowAndBeSafeToRepeat() {
        savedJobRepository.insertIfAbsent(candidate.getId(), job.getId(), "Title", "Acme");

        assertThat(savedJobRepository.deleteOne(candidate.getId(), job.getId())).isEqualTo(1);
        assertThat(savedJobRepository.deleteOne(candidate.getId(), job.getId())).isZero();
        assertThat(savedJobRepository.findJobIdsByCandidateUserId(candidate.getId())).isEmpty();
    }
}