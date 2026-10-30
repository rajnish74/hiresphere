package com.hiresphere.application.service;

import com.hiresphere.application.entity.ApplicationStatus;
import com.hiresphere.application.repository.JobApplicationRepository;
import com.hiresphere.candidate.dto.CandidateProfileView;
import com.hiresphere.candidate.entity.ProfileVisibility;
import com.hiresphere.candidate.service.CandidateProfileService;
import com.hiresphere.candidate.service.ProfileAccessPolicy;
import com.hiresphere.common.exception.AppException;
import com.hiresphere.common.exception.ResourceNotFoundException;
import com.hiresphere.recruiter.dto.CompanySummary;
import com.hiresphere.recruiter.entity.CompanyStatus;
import com.hiresphere.recruiter.service.CompanyService;
import com.hiresphere.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CandidateAccessService {

    private final CompanyService companyService;
    private final UserService userService;
    private final CandidateProfileService candidateProfileService;
    private final JobApplicationRepository applicationRepository;

    @Transactional(readOnly = true)
    public CandidateProfileView viewProfile(Long recruiterId, Long candidateUserId) {
        CompanySummary company = companyService.getOwnCompanySummary(recruiterId);
        if (company.status() != CompanyStatus.APPROVED) {
            throw new AppException(HttpStatus.FORBIDDEN,
                    "Your company must be approved before you can view candidate profiles");
        }
        if (!userService.getById(recruiterId).emailVerified()) {
            throw new AppException(HttpStatus.FORBIDDEN,
                    "Please verify your email address before viewing candidate profiles");
        }

        String name = userService.findActiveCandidateName(candidateUserId).orElseThrow(this::notFound);
        ProfileVisibility visibility = candidateProfileService.findVisibility(candidateUserId)
                .orElseThrow(this::notFound);

        boolean hasActiveApplication = applicationRepository.existsByCompanyIdAndCandidateUserIdAndStatusNot(
                company.id(), candidateUserId, ApplicationStatus.WITHDRAWN);

        if (!ProfileAccessPolicy.canRecruiterView(visibility, hasActiveApplication)) {
            throw notFound();
        }
        return candidateProfileService.findView(candidateUserId, name).orElseThrow(this::notFound);
    }

    private ResourceNotFoundException notFound() {
        return new ResourceNotFoundException("Candidate not found");
    }
}