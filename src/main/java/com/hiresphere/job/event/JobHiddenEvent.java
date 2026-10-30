package com.hiresphere.job.event;

public record JobHiddenEvent(Long jobId, Long companyId, String jobTitle, String reason) {
}