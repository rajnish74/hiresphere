package com.hiresphere.job.service;

import com.hiresphere.common.dto.PageResponse;
import com.hiresphere.common.exception.AppException;
import com.hiresphere.common.exception.BadRequestException;
import com.hiresphere.common.exception.ResourceNotFoundException;
import com.hiresphere.common.util.PageUtil;
import com.hiresphere.common.util.TextUtil;
import com.hiresphere.job.dto.JobRequest;
import com.hiresphere.job.dto.JobResponse;
import com.hiresphere.job.dto.JobSummaryResponse;
import com.hiresphere.job.entity.Job;
import com.hiresphere.job.entity.JobStatus;
import com.hiresphere.job.repository.JobRepository;
import com.hiresphere.recruiter.dto.CompanySummary;
import com.hiresphere.recruiter.entity.CompanyStatus;
import com.hiresphere.recruiter.service.CompanyService;
import com.hiresphere.skill.service.SkillService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import lombok.extern.slf4j.Slf4j;

import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class JobService {

    private final JobRepository jobRepository;
    private final CompanyService companyService;
    private final SkillService skillService;

    @Transactional
    public JobResponse create(Long userId, JobRequest req) {
        CompanySummary company = companyService.getOwnCompanySummary(userId);
        validate(req);
        Job job = new Job();
        job.setCompanyId(company.id());
        apply(job, req);
        return JobResponse.from(jobRepository.save(job));
    }

    @Transactional(readOnly = true)
    public PageResponse<JobSummaryResponse> listMine(Long userId, JobStatus status, int page, int size) {
        CompanySummary company = companyService.getOwnCompanySummary(userId);
        Pageable pageable = PageUtil.newestFirst(page, size);
        Page<Job> result = (status == null)
                ? jobRepository.findByCompanyId(company.id(), pageable)
                : jobRepository.findByCompanyIdAndStatus(company.id(), status, pageable);
        return PageResponse.of(result, JobSummaryResponse::from);
    }

    @Transactional(readOnly = true)
    public JobResponse getMine(Long userId, Long jobId) {
        CompanySummary company = companyService.getOwnCompanySummary(userId);
        return JobResponse.from(findOwned(company.id(), jobId));
    }

    @Transactional
    public JobResponse update(Long userId, Long jobId, JobRequest req) {
        CompanySummary company = companyService.getOwnCompanySummary(userId);
        Job job = findOwned(company.id(), jobId);
        if (job.getStatus() == JobStatus.CLOSED) {
            throw new AppException(HttpStatus.CONFLICT, "Closed jobs cannot be edited. Reopen the job first.");
        }
        validate(req);
        apply(job, req);
        return JobResponse.from(job);
    }

    @Transactional
    public JobResponse publish(Long userId, Long jobId) {
        CompanySummary company = companyService.getOwnCompanySummary(userId);
        Job job = findOwned(company.id(), jobId);

        if (company.status() != CompanyStatus.APPROVED) {
            throw new AppException(HttpStatus.FORBIDDEN,
                    "Your company must be approved by an admin before you can publish jobs");
        }
        if (job.getExpiresAt() != null && job.getExpiresAt().isBefore(Instant.now())) {
            throw new BadRequestException("The expiry date is in the past. Update the job before publishing.");
        }
        job.publish();
        return JobResponse.from(job);
    }

    @Transactional
    public JobResponse close(Long userId, Long jobId) {
        CompanySummary company = companyService.getOwnCompanySummary(userId);
        Job job = findOwned(company.id(), jobId);
        job.close();
        return JobResponse.from(job);
    }

    @Transactional
    public void delete(Long userId, Long jobId) {
        CompanySummary company = companyService.getOwnCompanySummary(userId);
        Job job = findOwned(company.id(), jobId);
        if (job.getStatus() == JobStatus.PUBLISHED) {
            throw new AppException(HttpStatus.CONFLICT, "Close the job before deleting it");
        }
        job.markDeleted();
    }

    // ---------- helpers ----------

    // Dusri company ka job ho ya exist hi na kare: dono mein 404
    private Job findOwned(Long companyId, Long jobId) {
        return jobRepository.findByIdAndCompanyId(jobId, companyId)
                .orElseThrow(() -> new ResourceNotFoundException("Job not found"));
    }

    private void validate(JobRequest r) {
        if (r.salaryMin() != null && r.salaryMax() != null && r.salaryMax().compareTo(r.salaryMin()) < 0) {
            throw new BadRequestException("Maximum salary cannot be less than minimum salary");
        }
        if (r.maxExperienceYears() != null && r.maxExperienceYears() < r.minExperienceYears()) {
            throw new BadRequestException("Maximum experience cannot be less than minimum experience");
        }
    }

    private void apply(Job j, JobRequest r) {
        j.setTitle(r.title().trim());
        j.setDescription(r.description().trim());
        j.setLocation(TextUtil.blankToNull(r.location()));
        j.setJobType(r.jobType());
        j.setWorkMode(r.workMode());
        j.setMinExperienceYears(r.minExperienceYears());
        j.setMaxExperienceYears(r.maxExperienceYears());
        j.setSalaryMin(r.salaryMin());
        j.setSalaryMax(r.salaryMax());
        j.setOpenings(r.openings());
        j.setExpiresAt(r.expiresAt());

        List<String> skillNames = (r.skills() == null) ? List.of() : r.skills();
        j.getSkills().clear();
        j.getSkills().addAll(skillService.resolveAll(skillNames));
    }

    @Transactional
    public int closeExpiredJobs() {
        int closed = jobRepository.closeExpired(JobStatus.PUBLISHED, JobStatus.CLOSED, Instant.now());
        if (closed > 0) {
            log.info("Auto-closed {} expired jobs", closed);
        }
        return closed;
    }

    @Transactional
    public void closeIfPublished(Long jobId) {
        jobRepository.findById(jobId)
                .filter(job -> job.getStatus() == JobStatus.PUBLISHED)
                .ifPresent(job -> {
                    job.close();
                    log.info("Job {} auto-closed because all openings are filled", jobId);
                });
    }
}