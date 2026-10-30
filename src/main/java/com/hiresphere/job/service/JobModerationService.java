package com.hiresphere.job.service;

import com.hiresphere.common.dto.PageResponse;
import com.hiresphere.common.exception.ResourceNotFoundException;
import com.hiresphere.common.util.CountUtil;
import com.hiresphere.common.util.PageUtil;
import com.hiresphere.job.dto.AdminJobSummary;
import com.hiresphere.job.entity.Job;
import com.hiresphere.job.entity.JobStatus;
import com.hiresphere.job.repository.JobRepository;
import com.hiresphere.recruiter.service.CompanyService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class JobModerationService {

    private final JobRepository jobRepository;
    private final CompanyService companyService;

    @Transactional(readOnly = true)
    public PageResponse<AdminJobSummary> list(Boolean hidden, JobStatus status, int page, int size) {
        Specification<Job> spec = (root, query, cb) -> cb.conjunction();
        if (hidden != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("hidden"), hidden));
        }
        if (status != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("status"), status));
        }
        Page<Job> result = jobRepository.findAll(spec, PageUtil.newestFirst(page, size));

        Set<Long> companyIds = result.getContent().stream().map(Job::getCompanyId).collect(Collectors.toSet());
        Map<Long, String> names = companyService.getNames(companyIds);
        return PageResponse.of(result, j -> AdminJobSummary.from(j, names.get(j.getCompanyId())));
    }

    @Transactional
    public AdminJobSummary hide(Long jobId, String reason) {
        Job job = findJob(jobId);
        job.hideByAdmin(reason.trim());
        return summary(job);
    }

    @Transactional
    public AdminJobSummary unhide(Long jobId) {
        Job job = findJob(jobId);
        job.unhideByAdmin();
        return summary(job);
    }

    @Transactional(readOnly = true)
    public Map<JobStatus, Long> countByStatus() {
        return CountUtil.toEnumMap(JobStatus.class, jobRepository.countGroupedByStatus());
    }

    @Transactional(readOnly = true)
    public long countHidden() {
        return jobRepository.countByHiddenTrue();
    }

    private Job findJob(Long id) {
        return jobRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Job not found"));
    }

    private AdminJobSummary summary(Job job) {
        String name = companyService.getNames(Set.of(job.getCompanyId())).get(job.getCompanyId());
        return AdminJobSummary.from(job, name);
    }
}