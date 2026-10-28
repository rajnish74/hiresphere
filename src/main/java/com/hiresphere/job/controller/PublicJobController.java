package com.hiresphere.job.controller;

import com.hiresphere.common.dto.ApiResponse;
import com.hiresphere.common.dto.PageResponse;
import com.hiresphere.job.dto.JobSearchCriteria;
import com.hiresphere.job.dto.JobSort;
import com.hiresphere.job.dto.PublicJobDetail;
import com.hiresphere.job.dto.PublicJobSummary;
import com.hiresphere.job.entity.JobType;
import com.hiresphere.job.entity.WorkMode;
import com.hiresphere.job.service.PublicJobService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/v1/jobs")
@RequiredArgsConstructor
public class PublicJobController {

    private final PublicJobService publicJobService;

    @GetMapping
    public ApiResponse<PageResponse<PublicJobSummary>> search(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String location,
            @RequestParam(required = false) List<JobType> jobType,
            @RequestParam(required = false) List<WorkMode> workMode,
            @RequestParam(required = false) Integer experience,
            @RequestParam(required = false) BigDecimal minSalary,
            @RequestParam(required = false) List<String> skills,
            @RequestParam(defaultValue = "NEWEST") JobSort sort,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        JobSearchCriteria criteria = new JobSearchCriteria(q, location, jobType, workMode,
                experience, minSalary, skills, sort);
        return ApiResponse.ok(publicJobService.search(criteria, page, size));
    }

    @GetMapping("/{id}")
    public ApiResponse<PublicJobDetail> get(@PathVariable Long id) {
        return ApiResponse.ok(publicJobService.get(id));
    }
}