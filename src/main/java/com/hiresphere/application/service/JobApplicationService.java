package com.hiresphere.application.service;

import com.hiresphere.application.dto.*;
import com.hiresphere.application.entity.ApplicationStatus;
import com.hiresphere.application.entity.JobApplication;
import com.hiresphere.application.event.ApplicationStatusChangedEvent;
import com.hiresphere.application.event.ApplicationSubmittedEvent;
import com.hiresphere.application.repository.JobApplicationRepository;
import com.hiresphere.candidate.dto.ApplicantSnapshot;
import com.hiresphere.candidate.service.CandidateProfileService;
import com.hiresphere.common.dto.PageResponse;
import com.hiresphere.common.exception.AppException;
import com.hiresphere.common.exception.BadRequestException;
import com.hiresphere.common.exception.DuplicateResourceException;
import com.hiresphere.common.exception.ResourceNotFoundException;
import com.hiresphere.common.storage.FileStorageService;
import com.hiresphere.common.util.CountUtil;
import com.hiresphere.common.util.PageUtil;
import com.hiresphere.common.util.TextUtil;
import com.hiresphere.job.dto.JobRef;
import com.hiresphere.job.event.JobFilledEvent;
import com.hiresphere.job.service.JobQueryService;
import com.hiresphere.recruiter.dto.CompanySummary;
import com.hiresphere.recruiter.entity.CompanyStatus;
import com.hiresphere.recruiter.service.CompanyService;
import com.hiresphere.user.dto.UserResponse;
import com.hiresphere.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.core.io.Resource;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.Instant;
import java.util.*;

@Service
@RequiredArgsConstructor
public class JobApplicationService {



    private static final Set<ApplicationStatus> RECRUITER_ALLOWED = EnumSet.of(
            ApplicationStatus.SHORTLISTED, ApplicationStatus.INTERVIEW, ApplicationStatus.OFFER,
            ApplicationStatus.HIRED, ApplicationStatus.REJECTED);

    private final JobApplicationRepository applicationRepository;
    private final JobQueryService jobQueryService;
    private final CompanyService companyService;
    private final CandidateProfileService candidateProfileService;
    private final UserService userService;
    private final FileStorageService storage;
    private final ApplicationEventPublisher eventPublisher;

    public record ResumeDownload(Resource resource, String filename) {
    }

    // =====================  Candidate side  =====================

    @Transactional
    public ApplicationDetailResponse apply(Long userId, Long jobId, ApplyRequest req) {
        JobRef job = jobQueryService.getPublicJob(jobId);   // draft/closed/expired/suspended = 404

        if (applicationRepository.existsByJobIdAndCandidateUserId(jobId, userId)) {
            throw new DuplicateResourceException("You have already applied to this job");
        }

        ApplicantSnapshot snapshot = candidateProfileService.getSnapshot(userId);
        if (snapshot.resumeStorageKey() == null) {
            throw new BadRequestException("Upload your resume before applying");
        }

        UserResponse user = userService.getById(userId);
        if (!user.emailVerified()) {
            throw new AppException(HttpStatus.FORBIDDEN, "Please verify your email address before applying");
        }
        String candidateName = user.fullName();
        String companyName = Objects.requireNonNullElse(
                companyService.getNames(Set.of(job.companyId())).get(job.companyId()), "Unknown company");

        String resumeCopyKey = storage.copy(snapshot.resumeStorageKey());
        deleteFileIfTransactionFails(resumeCopyKey);

        JobApplication application = new JobApplication();
        application.setJobId(job.id());
        application.setCompanyId(job.companyId());
        application.setCandidateUserId(userId);
        application.setCoverLetter(req == null ? null : TextUtil.blankToNull(req.coverLetter()));
        application.setJobTitle(job.title());
        application.setCompanyName(companyName);
        application.setCandidateName(candidateName);
        application.setCandidateHeadline(snapshot.headline());
        application.setResumeStorageKey(resumeCopyKey);
        application.setResumeOriginalName(snapshot.resumeOriginalName());
        application.recordSubmitted(userId);

        try {
            applicationRepository.saveAndFlush(application);
        } catch (DataIntegrityViolationException e) {
            throw new DuplicateResourceException("You have already applied to this job");
        }

        eventPublisher.publishEvent(new ApplicationSubmittedEvent(
                application.getId(), job.companyId(), userId, candidateName, job.title(), companyName));

        return ApplicationDetailResponse.from(application);
    }

    @Transactional(readOnly = true)
    public PageResponse<ApplicationSummary> listMine(Long userId, ApplicationStatus status, int page, int size) {
        Pageable pageable = PageUtil.newestFirst(page, size);
        Page<JobApplication> result = (status == null)
                ? applicationRepository.findByCandidateUserId(userId, pageable)
                : applicationRepository.findByCandidateUserIdAndStatus(userId, status, pageable);
        return PageResponse.of(result, ApplicationSummary::from);
    }

    @Transactional(readOnly = true)
    public ApplicationDetailResponse getMine(Long userId, Long applicationId) {
        return ApplicationDetailResponse.from(findOwnedByCandidate(userId, applicationId));
    }

    @Transactional
    public ApplicationDetailResponse withdraw(Long userId, Long applicationId) {
        JobApplication application = findOwnedByCandidate(userId, applicationId);
        ApplicationStatus from = application.getStatus();
        application.transitionTo(ApplicationStatus.WITHDRAWN, userId, null);
        eventPublisher.publishEvent(
                statusChanged(application, from, ApplicationStatus.WITHDRAWN, null, true));
        return ApplicationDetailResponse.from(application);
    }

    // =====================  Recruiter side  =====================

    @Transactional(readOnly = true)
    public PageResponse<RecruiterApplicationSummary> listForJob(Long recruiterId, Long jobId,
                                                                ApplicationStatus status, int page, int size) {
        CompanySummary company = companyService.getOwnCompanySummary(recruiterId);
        jobQueryService.getCompanyJob(company.id(), jobId);   // apni company ki job nahi to 404

        Pageable pageable = PageUtil.newestFirst(page, size);
        Page<JobApplication> result = (status == null)
                ? applicationRepository.findByJobId(jobId, pageable)
                : applicationRepository.findByJobIdAndStatus(jobId, status, pageable);
        return PageResponse.of(result, RecruiterApplicationSummary::from);
    }

    @Transactional(readOnly = true)
    public RecruiterApplicationResponse getForRecruiter(Long recruiterId, Long applicationId) {
        CompanySummary company = companyService.getOwnCompanySummary(recruiterId);
        return RecruiterApplicationResponse.from(findOwnedByCompany(company.id(), applicationId));
    }

    @Transactional
    public RecruiterApplicationResponse changeStatus(Long recruiterId, Long applicationId,
                                                     StatusChangeRequest req) {
        CompanySummary company = companyService.getOwnCompanySummary(recruiterId);
        if (company.status() != CompanyStatus.APPROVED) {
            throw new AppException(HttpStatus.FORBIDDEN,
                    "Your company must be approved to manage applications");
        }
        if (!RECRUITER_ALLOWED.contains(req.status())) {
            throw new BadRequestException("Recruiters cannot set the status to " + req.status());
        }

        JobApplication application = findOwnedByCompany(company.id(), applicationId);

        JobRef job = null;
        boolean fillsLastOpening = false;
        if (req.status() == ApplicationStatus.HIRED && application.getStatus().canMoveTo(ApplicationStatus.HIRED)) {
            job = jobQueryService.getCompanyJob(company.id(), application.getJobId());
            long alreadyHired = applicationRepository.countByJobIdAndStatus(job.id(), ApplicationStatus.HIRED);
            if (alreadyHired >= job.openings()) {
                throw new AppException(HttpStatus.CONFLICT, "All openings for this job are already filled");
            }
            fillsLastOpening = alreadyHired + 1 >= job.openings();
        }

        ApplicationStatus from = application.getStatus();
        String note = TextUtil.blankToNull(req.note());
        application.transitionTo(req.status(), recruiterId, note);
        eventPublisher.publishEvent(statusChanged(application, from, req.status(), note, false));
        if (fillsLastOpening) {
            eventPublisher.publishEvent(new JobFilledEvent(job.id()));
        }
        return RecruiterApplicationResponse.from(application);
    }

    @Transactional(readOnly = true)
    public ResumeDownload downloadResume(Long recruiterId, Long applicationId) {
        CompanySummary company = companyService.getOwnCompanySummary(recruiterId);
        JobApplication application = findOwnedByCompany(company.id(), applicationId);
        return new ResumeDownload(storage.load(application.getResumeStorageKey()),
                application.getResumeOriginalName());
    }


    @Transactional(readOnly = true)
    public Map<ApplicationStatus, Long> statusCounts(Long recruiterId, Long jobId) {
        CompanySummary company = companyService.getOwnCompanySummary(recruiterId);
        jobQueryService.getCompanyJob(company.id(), jobId);

        Map<ApplicationStatus, Long> counts = new EnumMap<>(ApplicationStatus.class);
        for (ApplicationStatus s : ApplicationStatus.values()) {
            counts.put(s, 0L);
        }
        applicationRepository.countByStatus(jobId).forEach(row -> counts.put(row.status(), row.total()));
        return counts;
    }


    @Transactional(readOnly = true)
    public Map<ApplicationStatus, Long> platformStatusCounts() {
        return CountUtil.toEnumMap(ApplicationStatus.class, applicationRepository.countGroupedByStatus());
    }

    @Transactional(readOnly = true)
    public long countSince(Instant since) {
        return applicationRepository.countByCreatedAtAfter(since);
    }

    // =====================  helpers  =====================

    private JobApplication findOwnedByCandidate(Long userId, Long applicationId) {
        return applicationRepository.findByIdAndCandidateUserId(applicationId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Application not found"));
    }

    private JobApplication findOwnedByCompany(Long companyId, Long applicationId) {
        return applicationRepository.findByIdAndCompanyId(applicationId, companyId)
                .orElseThrow(() -> new ResourceNotFoundException("Application not found"));
    }

    private void deleteFileIfTransactionFails(String key) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCompletion(int status) {
                if (status != STATUS_COMMITTED) {
                    storage.deleteQuietly(key);
                }
            }
        });
    }

    private ApplicationStatusChangedEvent statusChanged(JobApplication a, ApplicationStatus from,
                                                        ApplicationStatus to, String note, boolean byCandidate) {
        return new ApplicationStatusChangedEvent(a.getId(), a.getCompanyId(), a.getCandidateUserId(),
                a.getCandidateName(), a.getJobTitle(), a.getCompanyName(), from, to, note, byCandidate);
    }
}