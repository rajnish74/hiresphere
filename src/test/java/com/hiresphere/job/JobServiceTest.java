package com.hiresphere.job;

import com.hiresphere.common.exception.AppException;
import com.hiresphere.job.dto.JobResponse;
import com.hiresphere.job.entity.Job;
import com.hiresphere.job.entity.JobStatus;
import com.hiresphere.job.repository.JobRepository;
import com.hiresphere.job.service.JobService;
import com.hiresphere.recruiter.dto.CompanySummary;
import com.hiresphere.recruiter.entity.CompanyStatus;
import com.hiresphere.recruiter.service.CompanyService;
import com.hiresphere.skill.service.SkillService;
import com.hiresphere.user.dto.UserResponse;
import com.hiresphere.user.entity.Role;
import com.hiresphere.user.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JobServiceTest {

    @Mock
    private JobRepository jobRepository;
    @Mock
    private CompanyService companyService;
    @Mock
    private SkillService skillService;

    private JobService service;

    @Mock
    private UserService userService;

    @BeforeEach
    void setUp() {
        service = new JobService(jobRepository, companyService, skillService, userService);
    }

    private UserResponse recruiter(boolean verified) {
        return new UserResponse(1L, "Maya", "maya@test.com", Role.RECRUITER, verified);
    }
    @Test
    void publishShouldFailWhenCompanyIsNotApproved() {
        Job job = new Job();
        when(companyService.getOwnCompanySummary(1L))
                .thenReturn(new CompanySummary(5L, "Demo Corp", CompanyStatus.PENDING));
        when(jobRepository.findByIdAndCompanyId(10L, 5L)).thenReturn(Optional.of(job));

        assertThatThrownBy(() -> service.publish(1L, 10L)).isInstanceOf(AppException.class);

        assertThat(job.getStatus()).isEqualTo(JobStatus.DRAFT);   // job draft hi raha
    }

    @Test
    void publishShouldWorkWhenCompanyIsApproved() {
        Job job = new Job();
        when(companyService.getOwnCompanySummary(1L))
                .thenReturn(new CompanySummary(5L, "Demo Corp", CompanyStatus.APPROVED));
        when(jobRepository.findByIdAndCompanyId(10L, 5L)).thenReturn(Optional.of(job));
        when(userService.getById(1L)).thenReturn(recruiter(true));

        JobResponse response = service.publish(1L, 10L);

        assertThat(response.status()).isEqualTo(JobStatus.PUBLISHED);
        assertThat(job.getPublishedAt()).isNotNull();
    }

    @Test
    void publishShouldReturnNotFoundForJobOfAnotherCompany() {
        when(companyService.getOwnCompanySummary(1L))
                .thenReturn(new CompanySummary(5L, "Demo Corp", CompanyStatus.APPROVED));
        when(jobRepository.findByIdAndCompanyId(10L, 5L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.publish(1L, 10L))
                .hasMessageContaining("Job not found");
    }

    @Test
    void closeExpiredJobsShouldDelegateToBulkUpdate() {
        when(jobRepository.closeExpired(eq(JobStatus.PUBLISHED), eq(JobStatus.CLOSED), any(Instant.class)))
                .thenReturn(3);

        int closed = service.closeExpiredJobs();

        assertThat(closed).isEqualTo(3);
        verify(jobRepository).closeExpired(eq(JobStatus.PUBLISHED), eq(JobStatus.CLOSED), any(Instant.class));
    }

    @Test
    void closeIfPublishedShouldCloseAPublishedJob() {
        Job job = new Job();
        job.publish();
        when(jobRepository.findById(10L)).thenReturn(Optional.of(job));

        service.closeIfPublished(10L);

        assertThat(job.getStatus()).isEqualTo(JobStatus.CLOSED);
    }

    @Test
    void closeIfPublishedShouldIgnoreAnAlreadyClosedJob() {
        Job job = new Job();
        job.publish();
        job.close();
        Instant closedAt = job.getClosedAt();
        when(jobRepository.findById(10L)).thenReturn(Optional.of(job));

        service.closeIfPublished(10L);   // exception nahi, kuch nahi badla

        assertThat(job.getStatus()).isEqualTo(JobStatus.CLOSED);
        assertThat(job.getClosedAt()).isEqualTo(closedAt);
    }

    @Test
    void publishShouldFailWhenRecruiterEmailIsNotVerified() {
        Job job = new Job();
        when(companyService.getOwnCompanySummary(1L))
                .thenReturn(new CompanySummary(5L, "Demo Corp", CompanyStatus.APPROVED));
        when(jobRepository.findByIdAndCompanyId(10L, 5L)).thenReturn(Optional.of(job));
        when(userService.getById(1L)).thenReturn(recruiter(false));

        assertThatThrownBy(() -> service.publish(1L, 10L))
                .isInstanceOf(AppException.class)
                .hasMessageContaining("verify your email");

        assertThat(job.getStatus()).isEqualTo(JobStatus.DRAFT);
    }

    @Test
    void publishShouldFailForAJobHiddenByAdmin() {
        Job job = new Job();
        job.hideByAdmin("Misleading salary");
        when(companyService.getOwnCompanySummary(1L))
                .thenReturn(new CompanySummary(5L, "Demo Corp", CompanyStatus.APPROVED));
        when(jobRepository.findByIdAndCompanyId(10L, 5L)).thenReturn(Optional.of(job));

        assertThatThrownBy(() -> service.publish(1L, 10L))
                .isInstanceOf(AppException.class)
                .hasMessageContaining("hidden");

        assertThat(job.getStatus()).isEqualTo(JobStatus.DRAFT);
    }
}