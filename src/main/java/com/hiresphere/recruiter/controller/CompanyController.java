package com.hiresphere.recruiter.controller;

import com.hiresphere.common.dto.ApiResponse;
import com.hiresphere.recruiter.dto.CompanyRequest;
import com.hiresphere.recruiter.dto.CompanyResponse;
import com.hiresphere.recruiter.service.CompanyService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/recruiter/company")
@PreAuthorize("hasRole('RECRUITER')")
@RequiredArgsConstructor
public class CompanyController {

    private final CompanyService companyService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<CompanyResponse> create(@AuthenticationPrincipal Jwt jwt,
                                               @Valid @RequestBody CompanyRequest request) {
        return ApiResponse.ok("Company created and sent for verification",
                companyService.create(Long.valueOf(jwt.getSubject()), request));
    }

    @GetMapping
    public ApiResponse<CompanyResponse> getMine(@AuthenticationPrincipal Jwt jwt) {
        return ApiResponse.ok(companyService.getMine(Long.valueOf(jwt.getSubject())));
    }

    @PutMapping
    public ApiResponse<CompanyResponse> update(@AuthenticationPrincipal Jwt jwt,
                                               @Valid @RequestBody CompanyRequest request) {
        return ApiResponse.ok("Company updated", companyService.update(Long.valueOf(jwt.getSubject()), request));
    }
}