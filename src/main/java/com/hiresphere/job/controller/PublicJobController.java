package com.hiresphere.job.controller;

import com.hiresphere.common.dto.ApiResponse;
import com.hiresphere.common.dto.PageResponse;
import com.hiresphere.job.dto.JobFacets;
import com.hiresphere.job.dto.JobSearchCriteria;
import com.hiresphere.job.dto.JobSort;
import com.hiresphere.job.dto.PublicJobDetail;
import com.hiresphere.job.dto.PublicJobSummary;
import com.hiresphere.job.entity.JobType;
import com.hiresphere.job.entity.WorkMode;
import com.hiresphere.job.service.PublicJobService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@Tag(name = "Jobs (public)", description = "Search and view published jobs. No login needed")
@RestController
@RequestMapping("/api/v1/jobs")
@RequiredArgsConstructor
public class PublicJobController {

    private final PublicJobService publicJobService;

    @Operation(summary = "Search jobs",
            description = "Filters combine with AND. sort is NEWEST, SALARY_HIGH or EXPIRING_SOON")
    @GetMapping
    public ApiResponse<PageResponse<PublicJobSummary>> search(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String location,
            @RequestParam(required = false) List<JobType> jobType,
            @RequestParam(required = false) List<WorkMode> workMode,
            @RequestParam(required = false) Integer experience,
            @RequestParam(required = false) BigDecimal minSalary,
            @RequestParam(required = false) List<String> skills,
            @RequestParam(required = false) Integer postedWithinDays,
            @RequestParam(required = false) Long companyId,
            @RequestParam(defaultValue = "NEWEST") JobSort sort,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        JobSearchCriteria criteria = new JobSearchCriteria(q, location, jobType, workMode, experience,
                minSalary, skills, sort, postedWithinDays, companyId);
        return ApiResponse.ok(publicJobService.search(criteria, page, size));
    }

    @Operation(summary = "Filter counts",
            description = "Same filters as search. Each group ignores its own filter, so counts show what selecting that option would give")
    @GetMapping("/facets")
    public ApiResponse<JobFacets> facets(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String location,
            @RequestParam(required = false) List<JobType> jobType,
            @RequestParam(required = false) List<WorkMode> workMode,
            @RequestParam(required = false) Integer experience,
            @RequestParam(required = false) BigDecimal minSalary,
            @RequestParam(required = false) List<String> skills,
            @RequestParam(required = false) Integer postedWithinDays,
            @RequestParam(required = false) Long companyId) {

        JobSearchCriteria criteria = new JobSearchCriteria(q, location, jobType, workMode, experience,
                minSalary, skills, null, postedWithinDays, companyId);
        return ApiResponse.ok(publicJobService.facets(criteria));
    }

    @GetMapping("/{id}")
    public ApiResponse<PublicJobDetail> get(@PathVariable Long id) {
        return ApiResponse.ok(publicJobService.get(id));
    }
}