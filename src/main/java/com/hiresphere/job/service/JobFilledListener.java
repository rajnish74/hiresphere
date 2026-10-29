package com.hiresphere.job.service;

import com.hiresphere.job.event.JobFilledEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class JobFilledListener {

    private final JobService jobService;

    @EventListener
    public void onJobFilled(JobFilledEvent event) {
        jobService.closeIfPublished(event.jobId());
    }
}