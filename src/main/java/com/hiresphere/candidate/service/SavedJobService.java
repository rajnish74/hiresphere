package com.hiresphere.candidate.service;

import com.hiresphere.candidate.dto.SavedJobResponse;
import com.hiresphere.candidate.entity.SavedJob;
import com.hiresphere.candidate.repository.SavedJobRepository;
import com.hiresphere.common.dto.PageResponse;
import com.hiresphere.common.exception.AppException;
import com.hiresphere.common.util.PageUtil;
import com.hiresphere.job.dto.JobRef;
import com.hiresphere.job.dto.PublicJobSummary;
import com.hiresphere.job.service.JobQueryService;
import com.hiresphere.job.service.PublicJobService;
import com.hiresphere.recruiter.service.CompanyService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SavedJobService {

    static final int MAX_SAVED_JOBS = 200;

    private final SavedJobRepository repository;
    private final JobQueryService jobQueryService;
    private final PublicJobService publicJobService;
    private final CompanyService companyService;

    @Transactional
    public void save(Long userId, Long jobId) {
        JobRef job = jobQueryService.getPublicJob(jobId);

        if (repository.existsByCandidateUserIdAndJobId(userId, jobId)) {
            return;
        }
        if (repository.countByCandidateUserId(userId) >= MAX_SAVED_JOBS) {
            throw new AppException(HttpStatus.CONFLICT,
                    "You can save at most " + MAX_SAVED_JOBS + " jobs. Remove some to save more.");
        }
        String companyName = Objects.requireNonNullElse(
                companyService.getNames(Set.of(job.companyId())).get(job.companyId()), "Unknown company");
        repository.insertIfAbsent(userId, jobId, job.title(), companyName);
    }

    @Transactional
    public void remove(Long userId, Long jobId) {
        repository.deleteOne(userId, jobId);
    }

    @Transactional(readOnly = true)
    public PageResponse<SavedJobResponse> list(Long userId, int page, int size) {
        Page<SavedJob> result = repository.findByCandidateUserId(userId, PageUtil.newestFirst(page, size));

        Set<Long> jobIds = result.getContent().stream().map(SavedJob::getJobId).collect(Collectors.toSet());
        Map<Long, PublicJobSummary> live = publicJobService.findVisibleSummaries(jobIds);   // ek hi query

        return PageResponse.of(result, s -> SavedJobResponse.of(s, live.get(s.getJobId())));
    }

    @Transactional(readOnly = true)
    public List<Long> savedJobIds(Long userId) {
        return repository.findJobIdsByCandidateUserId(userId);
    }
}