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
        JobSort sort,
        Integer postedWithinDays,
        Long companyId) {

    public JobSearchCriteria(String keyword, String location, List<JobType> jobTypes, List<WorkMode> workModes,
                             Integer experience, BigDecimal minSalary, List<String> skills, JobSort sort,
                             Integer postedWithinDays) {
        this(keyword, location, jobTypes, workModes, experience, minSalary, skills, sort, postedWithinDays, null);
    }

    public JobSearchCriteria(String keyword, String location, List<JobType> jobTypes, List<WorkMode> workModes,
                             Integer experience, BigDecimal minSalary, List<String> skills, JobSort sort) {
        this(keyword, location, jobTypes, workModes, experience, minSalary, skills, sort, null, null);
    }

    public JobSearchCriteria withoutJobTypes() {
        return new JobSearchCriteria(keyword, location, null, workModes, experience, minSalary, skills, sort,
                postedWithinDays, companyId);
    }

    public JobSearchCriteria withoutWorkModes() {
        return new JobSearchCriteria(keyword, location, jobTypes, null, experience, minSalary, skills, sort,
                postedWithinDays, companyId);
    }
}