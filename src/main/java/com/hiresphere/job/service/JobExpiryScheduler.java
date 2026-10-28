package com.hiresphere.job.service;

import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class JobExpiryScheduler {

    private final JobService jobService;

    @Scheduled(cron = "${app.jobs.expiry-cron:0 0 * * * *}")
    public void closeExpiredJobs() {
        jobService.closeExpiredJobs();
    }
}