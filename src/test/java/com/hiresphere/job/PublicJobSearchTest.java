package com.hiresphere.job;

import com.hiresphere.common.exception.BadRequestException;
import com.hiresphere.common.exception.ResourceNotFoundException;
import com.hiresphere.job.dto.JobSearchCriteria;
import com.hiresphere.job.dto.JobSort;
import com.hiresphere.job.dto.PublicJobSummary;
import com.hiresphere.job.entity.Job;
import com.hiresphere.job.entity.JobType;
import com.hiresphere.job.entity.WorkMode;
import com.hiresphere.job.repository.JobRepository;
import com.hiresphere.job.service.PublicJobService;
import com.hiresphere.recruiter.entity.Company;
import com.hiresphere.recruiter.repository.CompanyRepository;
import com.hiresphere.skill.entity.Skill;
import com.hiresphere.skill.repository.SkillRepository;
import com.hiresphere.user.entity.Role;
import com.hiresphere.user.entity.User;
import com.hiresphere.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Transactional
class PublicJobSearchTest {

    @Autowired
    private PublicJobService service;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private CompanyRepository companyRepository;
    @Autowired
    private JobRepository jobRepository;
    @Autowired
    private SkillRepository skillRepository;

    private String tag;
    private User owner;

    @BeforeEach
    void setUp() {
        tag = "zt" + UUID.randomUUID().toString().replace("-", "").substring(0, 10);
        owner = new User();
        owner.setFullName("Owner " + tag);
        owner.setEmail(tag + "@test.com");
        owner.setPasswordHash("x");
        owner.setRole(Role.RECRUITER);
        userRepository.saveAndFlush(owner);
    }

    private Company company(boolean approved) {
        Company c = new Company();
        c.setOwnerId(owner.getId());
        c.setName("Company " + tag);
        if (approved) {
            c.approve(owner.getId());
        }
        return companyRepository.saveAndFlush(c);
    }

    private Job job(Company company, String title, boolean publish) {
        Job j = new Job();
        j.setCompanyId(company.getId());
        j.setTitle(title + " " + tag);
        j.setDescription("A reasonably long job description for testing.");
        j.setJobType(JobType.FULL_TIME);
        j.setWorkMode(WorkMode.REMOTE);
        if (publish) {
            j.publish();
        }
        return jobRepository.saveAndFlush(j);
    }

    private List<PublicJobSummary> searchByTag() {
        return service.search(new JobSearchCriteria(tag, null, null, null, null, null, null, null), 0, 50)
                .content();
    }

    @Test
    void publishedJobOfApprovedCompanyShouldBeVisible() {
        job(company(true), "Visible", true);

        assertThat(searchByTag()).hasSize(1);
    }

    @Test
    void draftJobShouldNotBeVisible() {
        job(company(true), "Draft", false);

        assertThat(searchByTag()).isEmpty();
    }

    @Test
    void jobOfPendingCompanyShouldNotBeVisible() {
        job(company(false), "Pending Co", true);

        assertThat(searchByTag()).isEmpty();
    }

    @Test
    void expiredJobShouldNotBeVisible() {
        Job j = job(company(true), "Expired", true);
        j.setExpiresAt(Instant.now().minusSeconds(3600));
        jobRepository.saveAndFlush(j);

        assertThat(searchByTag()).isEmpty();
    }

    @Test
    void suspendedCompanyJobsShouldDisappearAndReappearWhenReinstated() {
        Company c = company(true);
        Job j = job(c, "Suspension", true);
        assertThat(searchByTag()).hasSize(1);

        c.suspend(owner.getId(), "Reported as fake");
        companyRepository.saveAndFlush(c);
        assertThat(searchByTag()).isEmpty();
        assertThatThrownBy(() -> service.get(j.getId())).isInstanceOf(ResourceNotFoundException.class);

        c.approve(owner.getId());
        companyRepository.saveAndFlush(c);
        assertThat(searchByTag()).hasSize(1);
        assertThat(service.get(j.getId()).title()).contains("Suspension");
    }

    @Test
    void salaryFilterShouldKeepOnlyJobsPayingEnough() {
        Company c = company(true);
        Job high = job(c, "High", true);
        high.setSalaryMax(new BigDecimal("1000000"));
        Job low = job(c, "Low", true);
        low.setSalaryMax(new BigDecimal("500000"));
        job(c, "NoSalary", true);
        jobRepository.flush();

        List<PublicJobSummary> result = service.search(
                new JobSearchCriteria(tag, null, null, null, null, new BigDecimal("800000"), null, null),
                0, 50).content();

        assertThat(result).extracting(PublicJobSummary::title).containsExactly(high.getTitle());
    }

    @Test
    void salarySortShouldPutJobsWithoutSalaryLast() {
        Company c = company(true);
        Job a = job(c, "A", true);
        a.setSalaryMax(new BigDecimal("900000"));
        job(c, "B no salary", true);
        Job cc = job(c, "C", true);
        cc.setSalaryMax(new BigDecimal("500000"));
        jobRepository.flush();

        List<String> titles = service.search(
                new JobSearchCriteria(tag, null, null, null, null, null, null, JobSort.SALARY_HIGH),
                0, 50).content().stream().map(PublicJobSummary::title).toList();

        assertThat(titles).hasSize(3);
        assertThat(titles.get(0)).startsWith("A ");          // 9 lakh, sabse zyada
        assertThat(titles.get(1)).startsWith("C ");          // 5 lakh
        assertThat(titles.get(2)).startsWith("B no salary"); // salary nahi, sabse last
    }

    @Test
    void skillFilterShouldReturnEachMatchingJobOnlyOnce() {
        Company c = company(true);
        Skill s1 = skillRepository.save(new Skill("Zz1" + tag, ("zz1" + tag)));
        Skill s2 = skillRepository.save(new Skill("Zz2" + tag, ("zz2" + tag)));
        Job withBoth = job(c, "Both skills", true);
        withBoth.getSkills().add(s1);
        withBoth.getSkills().add(s2);
        job(c, "No skills", true);
        jobRepository.flush();

        List<PublicJobSummary> result = service.search(
                new JobSearchCriteria(tag, null, null, null, null, null,
                        List.of("ZZ1" + tag, "zz2" + tag), null),
                0, 50).content();

        assertThat(result).hasSize(1);   // join hota to 2 baar aati
        assertThat(result.get(0).title()).startsWith("Both skills");
    }

    @Test
    void percentSignInKeywordShouldBeTreatedAsPlainText() {
        job(company(true), "Plain", true);

        List<PublicJobSummary> result = service.search(
                new JobSearchCriteria(tag + "%", null, null, null, null, null, null, null), 0, 50).content();

        assertThat(result).isEmpty();
    }

    @Test
    void postedWithinDaysShouldHideOlderJobs() {
        Company c = company(true);
        job(c, "Recent", true);
        Job old = job(c, "Old", true);
        old.setPublishedAt(Instant.now().minus(10, ChronoUnit.DAYS));
        jobRepository.saveAndFlush(old);

        List<PublicJobSummary> lastWeek = service.search(
                new JobSearchCriteria(tag, null, null, null, null, null, null, null, 7), 0, 50).content();
        List<PublicJobSummary> lastMonth = service.search(
                new JobSearchCriteria(tag, null, null, null, null, null, null, null, 30), 0, 50).content();

        assertThat(lastWeek).hasSize(1);
        assertThat(lastWeek.get(0).title()).startsWith("Recent");
        assertThat(lastMonth).hasSize(2);
    }

    @Test
    void postedWithinDaysOutsideAllowedRangeShouldBeRejected() {
        for (int invalid : new int[]{0, -5, 91}) {
            assertThatThrownBy(() -> service.search(
                    new JobSearchCriteria(tag, null, null, null, null, null, null, null, invalid), 0, 10))
                    .isInstanceOf(BadRequestException.class);
        }
    }


    @Test
    void facetsShouldCountOnlyVisibleJobsPerType() {
        Map<JobType, Long> before = service.jobTypeFacets();

        Company c = company(true);
        job(c, "Full time visible", true);
        Job intern = job(c, "Intern visible", true);
        intern.setJobType(JobType.INTERNSHIP);
        jobRepository.saveAndFlush(intern);
        job(c, "Draft not counted", false);

        Map<JobType, Long> after = service.jobTypeFacets();

        assertThat(after.get(JobType.FULL_TIME)).isEqualTo(before.get(JobType.FULL_TIME) + 1);
        assertThat(after.get(JobType.INTERNSHIP)).isEqualTo(before.get(JobType.INTERNSHIP) + 1);
        assertThat(after.get(JobType.CONTRACT)).isEqualTo(before.get(JobType.CONTRACT));
    }

    @Test
    void jobHiddenByAdminShouldDisappearAndReappearWhenUnhidden() {
        Company c = company(true);
        Job j = job(c, "Moderated", true);
        assertThat(searchByTag()).hasSize(1);

        j.hideByAdmin("Misleading salary");
        jobRepository.saveAndFlush(j);
        assertThat(searchByTag()).isEmpty();
        assertThatThrownBy(() -> service.get(j.getId())).isInstanceOf(ResourceNotFoundException.class);

        j.unhideByAdmin();
        jobRepository.saveAndFlush(j);
        assertThat(searchByTag()).hasSize(1);
    }
}