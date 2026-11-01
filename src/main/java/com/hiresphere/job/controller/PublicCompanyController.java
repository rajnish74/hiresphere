package com.hiresphere.job.controller;

import com.hiresphere.common.dto.ApiResponse;
import com.hiresphere.job.dto.PublicCompanyProfile;
import com.hiresphere.job.service.PublicCompanyService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/public/companies")
@RequiredArgsConstructor
public class PublicCompanyController {

    private final PublicCompanyService companyService;

    @GetMapping("/{id}")
    public ApiResponse<PublicCompanyProfile> get(@PathVariable Long id) {
        return ApiResponse.ok(companyService.get(id));
    }
}