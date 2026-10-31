package com.hiresphere.job.service;

import com.hiresphere.common.dto.PageResponse;
import com.hiresphere.common.exception.AppException;
import com.hiresphere.common.exception.BadRequestException;
import com.hiresphere.common.exception.DuplicateResourceException;
import com.hiresphere.common.exception.ResourceNotFoundException;
import com.hiresphere.common.util.PageUtil;
import com.hiresphere.common.util.TextUtil;
import com.hiresphere.job.dto.*;
import com.hiresphere.job.entity.JobReport;
import com.hiresphere.job.entity.JobReportReason;
import com.hiresphere.job.entity.JobReportStatus;
import com.hiresphere.job.event.JobReportClosedEvent;
import com.hiresphere.job.repository.JobReportRepository;
import com.hiresphere.user.dto.UserResponse;
import com.hiresphere.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class JobReportService {

    static final int DAILY_LIMIT = 10;
    private static final int MIN_DETAILS_FOR_OTHER = 10;

    private final JobReportRepository repository;
    private final JobQueryService jobQueryService;
    private final UserService userService;
    private final ApplicationEventPublisher eventPublisher;

    // ---------- candidate side ----------

    @Transactional
    public JobReportResponse report(Long userId, Long jobId, ReportJobRequest req) {
        UserResponse user = userService.getById(userId);
        if (!user.emailVerified()) {
            throw new AppException(HttpStatus.FORBIDDEN, "Please verify your email address before reporting a job");
        }
        String details = TextUtil.blankToNull(req.details());
        if (req.reason() == JobReportReason.OTHER && (details == null || details.length() < MIN_DETAILS_FOR_OTHER)) {
            throw new BadRequestException(
                    "Please describe the problem (at least 10 characters) when the reason is OTHER");
        }

        JobRef job = jobQueryService.getPublicJob(jobId);   // jo job tum dekh hi nahi sakte, usse report nahi kar sakte

        if (repository.existsByReporterUserIdAndJobId(userId, jobId)) {
            throw new DuplicateResourceException("You have already reported this job");
        }
        if (repository.countByReporterUserIdAndCreatedAtAfter(userId, Instant.now().minus(1, ChronoUnit.DAYS))
                >= DAILY_LIMIT) {
            throw new AppException(HttpStatus.TOO_MANY_REQUESTS,
                    "You have reached today's report limit. Please try again tomorrow.");
        }

        JobReport report = JobReport.create(jobId, userId, req.reason(), details, job.title());
        try {
            repository.saveAndFlush(report);
        } catch (DataIntegrityViolationException e) {
            throw new DuplicateResourceException("You have already reported this job");   // race: do tabs ek saath
        }
        return JobReportResponse.from(report);
    }

    // ---------- admin side ----------

    @Transactional(readOnly = true)
    public PageResponse<AdminJobReportResponse> list(JobReportStatus status, int page, int size) {
        Page<JobReport> result = repository.findByStatus(status, PageUtil.newestFirst(page, size));
        List<JobReport> reports = result.getContent();

        Set<Long> jobIds = reports.stream().map(JobReport::getJobId).collect(Collectors.toSet());
        Set<Long> reporterIds = reports.stream().map(JobReport::getReporterUserId).collect(Collectors.toSet());

        Map<Long, Long> openCounts = jobIds.isEmpty() ? Map.of()
                : repository.countByJobIdsAndStatus(jobIds, JobReportStatus.OPEN).stream()
                .collect(Collectors.toMap(JobReportCount::jobId, JobReportCount::total));
        Map<Long, String> emails = userService.getEmails(reporterIds);   // ek hi query

        return PageResponse.of(result, r -> AdminJobReportResponse.from(
                r, emails.get(r.getReporterUserId()), openCounts.getOrDefault(r.getJobId(), 0L)));
    }

    @Transactional
    public JobReportOutcome resolve(Long reportId, Long adminId, String note) {
        JobReport report = findReport(reportId);
        report.resolve(adminId, note.trim());
        publishClosed(report);
        return JobReportOutcome.of(report);
    }

    @Transactional
    public JobReportOutcome dismiss(Long reportId, Long adminId, String note) {
        JobReport report = findReport(reportId);
        report.dismiss(adminId, note.trim());
        publishClosed(report);
        return JobReportOutcome.of(report);
    }

    @Transactional(readOnly = true)
    public PageResponse<MyJobReportResponse> listMine(Long userId, int page, int size) {
        return PageResponse.of(
                repository.findByReporterUserId(userId, PageUtil.newestFirst(page, size)),
                MyJobReportResponse::from);
    }

    private void publishClosed(JobReport r) {
        eventPublisher.publishEvent(
                new JobReportClosedEvent(r.getReporterUserId(), r.getJobId(), r.getJobTitle(), r.getStatus()));
    }

    private JobReport findReport(Long id) {
        return repository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Report not found"));
    }
}