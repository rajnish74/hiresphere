package com.hiresphere.recruiter.dto;

import com.hiresphere.recruiter.entity.CompanyStatus;

public record CompanySummary(Long id, String name, CompanyStatus status) {
}