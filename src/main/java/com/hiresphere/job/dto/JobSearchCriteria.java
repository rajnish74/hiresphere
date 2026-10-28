package com.hiresphere.job.dto;

import com.hiresphere.job.entity.JobType;
import com.hiresphere.job.entity.WorkMode;

import java.math.BigDecimal;
import java.util.List;

public record JobSearchCriteria(
        String keyword,
        String location,
        List<JobType> jobTypes,
        List<WorkMode> workModes,
        Integer experience,
        BigDecimal minSalary,
        List<String> skills,
        JobSort sort) {
}