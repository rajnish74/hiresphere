package com.hiresphere.job.service;

import com.hiresphere.common.exception.ResourceNotFoundException;
import com.hiresphere.job.dto.JobRef;
import com.hiresphere.job.entity.Job;
import com.hiresphere.job.repository.JobRepository;
import com.hiresphere.job.repository.JobSpecifications;
import lombok.RequiredArgsConstructor;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
@RequiredArgsConstructor
public class JobQueryService {

    private final JobRepository jobRepository;

    @Transactional(readOnly = true)
    public JobRef getPublicJob(Long jobId) {
        Specification<Job> spec = JobSpecifications.visibleToPublic(Instant.now())
                .and(JobSpecifications.hasId(jobId));
        return jobRepository.findOne(spec)
                .map(JobRef::from)
                .orElseThrow(() -> new ResourceNotFoundException("Job not found"));
    }

    @Transactional(readOnly = true)
    public JobRef getCompanyJob(Long companyId, Long jobId) {
        return jobRepository.findByIdAndCompanyId(jobId, companyId)
                .map(JobRef::from)
                .orElseThrow(() -> new ResourceNotFoundException("Job not found"));
    }
}