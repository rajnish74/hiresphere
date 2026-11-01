package com.hiresphere.job.service;

import com.hiresphere.job.repository.JobRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class JobStatsService {

    private final JobRepository jobRepository;

    @Transactional(propagation = Propagation.MANDATORY)
    public void applicationSubmitted(Long jobId) {
        jobRepository.incrementApplications(jobId);
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void applicationWithdrawn(Long jobId) {
        jobRepository.decrementApplications(jobId);
    }
}