package com.hiresphere.job;

import com.hiresphere.job.entity.Job;
import com.hiresphere.job.entity.JobType;
import com.hiresphere.job.entity.WorkMode;
import com.hiresphere.job.repository.JobRepository;
import com.hiresphere.job.service.PublicJobService;
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
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
class JobApplicationsCountIntegrationTest {

    @Autowired
    private JobRepository jobRepository;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private CompanyRepository companyRepository;
    @Autowired
    private PublicJobService publicJobService;
    @Autowired
    private EntityManager em;

    private Job job;

    @BeforeEach
    void setUp() {
        String tag = "zt" + UUID.randomUUID().toString().replace("-", "").substring(0, 10);
        User owner = new User();
        owner.setFullName("Owner " + tag);
        owner.setEmail(tag + "@test.com");
        owner.setPasswordHash("x");
        owner.setRole(Role.RECRUITER);
        userRepository.saveAndFlush(owner);

        Company company = new Company();
        company.setOwnerId(owner.getId());
        company.setName("Company " + tag);
        company.approve(owner.getId());
        company = companyRepository.saveAndFlush(company);

        Job j = new Job();
        j.setCompanyId(company.getId());
        j.setTitle("Counted " + tag);
        j.setDescription("A reasonably long job description for testing.");
        j.setJobType(JobType.FULL_TIME);
        j.setWorkMode(WorkMode.REMOTE);
        j.publish();
        job = jobRepository.saveAndFlush(j);
    }

    private int storedCount() {
        em.clear();
        return jobRepository.findById(job.getId()).orElseThrow().getApplicationsCount();
    }

    @Test
    void counterShouldGoUpAndDownAndNeverBelowZero() {
        assertThat(jobRepository.incrementApplications(job.getId())).isEqualTo(1);
        jobRepository.incrementApplications(job.getId());
        jobRepository.decrementApplications(job.getId());
        assertThat(storedCount()).isEqualTo(1);

        assertThat(jobRepository.decrementApplications(job.getId())).isEqualTo(1);
        assertThat(jobRepository.decrementApplications(job.getId())).isZero();   // 0 se neeche nahi
        assertThat(storedCount()).isZero();
    }

    @Test
    void savingAStaleJobEntityMustNotOverwriteTheCounter() {
        jobRepository.incrementApplications(job.getId());

        job.setTitle("Edited by the recruiter");
        jobRepository.saveAndFlush(job);

        assertThat(storedCount()).isEqualTo(1);
    }

    @Test
    void publicDetailShouldExposeTheCounter() {
        jobRepository.incrementApplications(job.getId());
        jobRepository.incrementApplications(job.getId());
        em.clear();

        assertThat(publicJobService.get(job.getId()).applicants()).isEqualTo(2);
    }
}