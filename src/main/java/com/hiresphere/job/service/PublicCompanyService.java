package com.hiresphere.job.service;

import com.hiresphere.common.exception.ResourceNotFoundException;
import com.hiresphere.job.dto.PublicCompanyProfile;
import com.hiresphere.job.repository.JobRepository;
import com.hiresphere.job.repository.JobSpecifications;
import com.hiresphere.recruiter.dto.PublicCompanyInfo;
import com.hiresphere.recruiter.service.CompanyService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
@RequiredArgsConstructor
public class PublicCompanyService {

    private final CompanyService companyService;
    private final JobRepository jobRepository;

    @Transactional(readOnly = true)
    public PublicCompanyProfile get(Long id) {
        PublicCompanyInfo c = companyService.findApproved(id)
                .orElseThrow(() -> new ResourceNotFoundException("Company not found"));
        long open = jobRepository.count(
                JobSpecifications.visibleToPublic(Instant.now()).and(JobSpecifications.hasCompanyId(id)));
        return new PublicCompanyProfile(c.id(), c.name(), c.website(), c.industry(), c.description(),
                c.location(), open);
    }
}