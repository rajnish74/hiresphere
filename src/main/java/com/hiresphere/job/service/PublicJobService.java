package com.hiresphere.job.service;

import com.hiresphere.common.dto.PageResponse;
import com.hiresphere.common.exception.BadRequestException;
import com.hiresphere.common.exception.ResourceNotFoundException;
import com.hiresphere.common.util.PageUtil;
import com.hiresphere.job.dto.JobSearchCriteria;
import com.hiresphere.job.dto.JobSort;
import com.hiresphere.job.dto.PublicJobDetail;
import com.hiresphere.job.dto.PublicJobSummary;
import com.hiresphere.job.entity.Job;
import com.hiresphere.job.entity.JobType;
import com.hiresphere.job.repository.JobFacetQueries;
import com.hiresphere.job.repository.JobRepository;
import com.hiresphere.job.repository.JobSpecifications;
import com.hiresphere.recruiter.service.CompanyService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PublicJobService {

    private final JobRepository jobRepository;
    private final CompanyService companyService;
    private final JobFacetQueries facetQueries;

    @Transactional(readOnly = true)
    public PageResponse<PublicJobSummary> search(JobSearchCriteria criteria, int page, int size) {
        validate(criteria);
        Specification<Job> spec = JobSpecifications.search(criteria, Instant.now());
        Page<Job> result = jobRepository.findAll(spec, PageUtil.of(page, size, sortFor(criteria.sort())));

        Set<Long> companyIds = result.getContent().stream().map(Job::getCompanyId).collect(Collectors.toSet());
        Map<Long, String> names = companyService.getNames(companyIds);

        return PageResponse.of(result, j -> PublicJobSummary.from(j, names.get(j.getCompanyId())));
    }

    @Transactional(readOnly = true)
    public PublicJobDetail get(Long id) {
        Specification<Job> spec = JobSpecifications.visibleToPublic(Instant.now())
                .and(JobSpecifications.hasId(id));
        Job job = jobRepository.findOne(spec)
                .orElseThrow(() -> new ResourceNotFoundException("Job not found"));
        String companyName = companyService.getNames(Set.of(job.getCompanyId())).get(job.getCompanyId());
        return PublicJobDetail.from(job, companyName);
    }

    private Sort sortFor(JobSort sort) {
        JobSort effective = (sort == null) ? JobSort.NEWEST : sort;
        return switch (effective) {
            case NEWEST -> Sort.by(Sort.Order.desc("publishedAt"), Sort.Order.desc("id"));
            case SALARY_HIGH -> Sort.by(Sort.Order.desc("salaryMax").nullsLast(), Sort.Order.desc("id"));
            case EXPIRING_SOON -> Sort.by(Sort.Order.asc("expiresAt").nullsLast(), Sort.Order.desc("id"));
        };
    }

    private void validate(JobSearchCriteria c) {
        if (c.keyword() != null && c.keyword().length() > 100) {
            throw new BadRequestException("Search keyword is too long (max 100 characters)");
        }
        if (c.location() != null && c.location().length() > 100) {
            throw new BadRequestException("Location is too long (max 100 characters)");
        }
        if (c.skills() != null && c.skills().size() > 10) {
            throw new BadRequestException("At most 10 skills can be used in a filter");
        }
        if (c.experience() != null && (c.experience() < 0 || c.experience() > 50)) {
            throw new BadRequestException("Experience must be between 0 and 50 years");
        }
        if (c.minSalary() != null && c.minSalary().signum() < 0) {
            throw new BadRequestException("Minimum salary cannot be negative");
        }
        if (c.postedWithinDays() != null && (c.postedWithinDays() < 1 || c.postedWithinDays() > 90)) {
            throw new BadRequestException("postedWithinDays must be between 1 and 90");
        }
    }

    @Transactional(readOnly = true)
    public Map<JobType, Long> jobTypeFacets() {
        Map<JobType, Long> counts = facetQueries.countByJobType(Instant.now());
        Map<JobType, Long> complete = new EnumMap<>(JobType.class);
        for (JobType type : JobType.values()) {
            complete.put(type, counts.getOrDefault(type, 0L));
        }
        return complete;
    }

    @Transactional(readOnly = true)
    public Map<Long, PublicJobSummary> findVisibleSummaries(Collection<Long> jobIds) {
        if (jobIds.isEmpty()) {
            return Map.of();
        }
        Specification<Job> spec = JobSpecifications.visibleToPublic(Instant.now())
                .and(JobSpecifications.hasIdIn(jobIds));
        List<Job> jobs = jobRepository.findAll(spec);

        Set<Long> companyIds = jobs.stream().map(Job::getCompanyId).collect(Collectors.toSet());
        Map<Long, String> names = companyService.getNames(companyIds);   // ek hi query
        return jobs.stream().collect(Collectors.toMap(Job::getId,
                j -> PublicJobSummary.from(j, names.get(j.getCompanyId()))));
    }
}