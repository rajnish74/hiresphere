package com.hiresphere.recruiter.service;

import com.hiresphere.common.dto.PageResponse;
import com.hiresphere.common.exception.BadRequestException;
import com.hiresphere.common.exception.DuplicateResourceException;
import com.hiresphere.common.exception.ResourceNotFoundException;
import com.hiresphere.common.util.PageUtil;
import com.hiresphere.common.util.TextUtil;
import com.hiresphere.recruiter.dto.CompanyRequest;
import com.hiresphere.recruiter.dto.CompanyResponse;
import com.hiresphere.recruiter.dto.CompanySummary;
import com.hiresphere.recruiter.entity.Company;
import com.hiresphere.recruiter.entity.CompanyStatus;
import com.hiresphere.recruiter.repository.CompanyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;

@Service
@RequiredArgsConstructor
public class CompanyService {

    private final CompanyRepository companyRepository;

    // ---------- Recruiter side ----------

    @Transactional
    public CompanyResponse create(Long userId, CompanyRequest req) {
        if (companyRepository.findByOwnerId(userId).isPresent()) {
            throw new DuplicateResourceException("You already have a company profile");
        }
        Company company = new Company();
        company.setOwnerId(userId);
        apply(company, req);
        return CompanyResponse.from(companyRepository.save(company));
    }

    @Transactional(readOnly = true)
    public CompanyResponse getMine(Long userId) {
        return CompanyResponse.from(findOwn(userId));
    }

    @Transactional
    public CompanyResponse update(Long userId, CompanyRequest req) {
        Company company = findOwn(userId);

        boolean identityChanged = !company.getName().equals(req.name().trim())
                || !Objects.equals(company.getWebsite(), req.website());

        apply(company, req);

        CompanyStatus status = company.getVerificationStatus();
        if (status == CompanyStatus.REJECTED || (status == CompanyStatus.APPROVED && identityChanged)) {
            company.resubmit();
        }
        return CompanyResponse.from(company);
    }

    @Transactional(readOnly = true)
    public CompanySummary getOwnCompanySummary(Long userId) {
        Company c = companyRepository.findByOwnerId(userId)
                .orElseThrow(() -> new BadRequestException("Create your company profile first"));
        return new CompanySummary(c.getId(), c.getName(), c.getVerificationStatus());
    }

    // ---------- Admin side ----------

    @Transactional(readOnly = true)
    public PageResponse<CompanyResponse> listByStatus(CompanyStatus status, int page, int size) {
        return PageResponse.of(
                companyRepository.findByVerificationStatus(status, PageUtil.newestFirst(page, size)),
                CompanyResponse::from);
    }

    @Transactional
    public CompanyResponse approve(Long companyId, Long adminId) {
        Company company = findById(companyId);
        company.approve(adminId);
        return CompanyResponse.from(company);
    }

    @Transactional
    public CompanyResponse reject(Long companyId, Long adminId, String reason) {
        Company company = findById(companyId);
        company.reject(adminId, reason.trim());
        return CompanyResponse.from(company);
    }

    @Transactional
    public CompanyResponse suspend(Long companyId, Long adminId, String reason) {
        Company company = findById(companyId);
        company.suspend(adminId, reason.trim());
        return CompanyResponse.from(company);
    }

    // ---------- helpers ----------

    private Company findOwn(Long userId) {
        return companyRepository.findByOwnerId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Company profile not created yet"));
    }

    private Company findById(Long id) {
        return companyRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Company not found"));
    }

    private void apply(Company c, CompanyRequest r) {
        c.setName(r.name().trim());
        c.setWebsite(r.website());
        c.setIndustry(TextUtil.blankToNull(r.industry()));
        c.setDescription(TextUtil.blankToNull(r.description()));
        c.setLocation(TextUtil.blankToNull(r.location()));
    }
}