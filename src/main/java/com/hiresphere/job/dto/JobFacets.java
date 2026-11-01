package com.hiresphere.job.dto;

import com.hiresphere.job.entity.JobType;
import com.hiresphere.job.entity.WorkMode;

import java.util.Map;

public record JobFacets(long total, Map<JobType, Long> jobType, Map<WorkMode, Long> workMode) {
}