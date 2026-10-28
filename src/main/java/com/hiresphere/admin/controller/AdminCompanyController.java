
package com.hiresphere.admin.controller;

import com.hiresphere.common.dto.ApiResponse;
import com.hiresphere.common.dto.PageResponse;
import com.hiresphere.recruiter.dto.AdminCompanyResponse;
import com.hiresphere.recruiter.dto.CompanyResponse;
import com.hiresphere.recruiter.dto.ReasonRequest;
import com.hiresphere.recruiter.entity.CompanyStatus;
import com.hiresphere.recruiter.service.CompanyService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/admin/companies")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class AdminCompanyController {

    private final CompanyService companyService;

    @GetMapping
    public ApiResponse<PageResponse<AdminCompanyResponse>> list(
            @RequestParam(defaultValue = "PENDING") CompanyStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        return ApiResponse.ok(
                companyService.listByStatus(status, page, size)
        );
    }

    @PostMapping("/{id}/approve")
    public ApiResponse<CompanyResponse> approve(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Long id) {

        return ApiResponse.ok(
                "Company approved",
                companyService.approve(id, Long.valueOf(jwt.getSubject()))
        );
    }

    @PostMapping("/{id}/reject")
    public ApiResponse<CompanyResponse> reject(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Long id,
            @Valid @RequestBody ReasonRequest request) {

        return ApiResponse.ok(
                "Company rejected",
                companyService.reject(
                        id,
                        Long.valueOf(jwt.getSubject()),
                        request.reason()
                )
        );
    }

    @PostMapping("/{id}/suspend")
    public ApiResponse<CompanyResponse> suspend(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Long id,
            @Valid @RequestBody ReasonRequest request) {

        return ApiResponse.ok(
                "Company suspended",
                companyService.suspend(
                        id,
                        Long.valueOf(jwt.getSubject()),
                        request.reason()
                )
        );
    }
}
