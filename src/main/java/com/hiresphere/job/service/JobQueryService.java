package com.hiresphere.job.service;

import com.hiresphere.common.exception.ResourceNotFoundException;
import com.hiresphere.job.dto.JobRef;
import com.hiresphere.job.entity.Job;
import com.hiresphere.job.repository.JobRepository;
import com.hiresphere.job.repository.JobSpecifications;
import com.hiresphere.skill.entity.Skill;
import lombok.RequiredArgsConstructor;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

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

    @Transactional(readOnly = true)
    public Map<Long, Set<String>> publicJobSkillKeys(Collection<Long> jobIds) {
        if (jobIds.isEmpty()) {
            return Map.of();
        }
        Specification<Job> spec = JobSpecifications.visibleToPublic(Instant.now())
                .and(JobSpecifications.hasIdIn(jobIds));
        return jobRepository.findAll(spec).stream().collect(Collectors.toMap(Job::getId,
                j -> j.getSkills().stream().map(Skill::getNormalizedName).collect(Collectors.toSet())));
    }
}