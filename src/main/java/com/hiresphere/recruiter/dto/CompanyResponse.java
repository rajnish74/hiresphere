package com.hiresphere.recruiter.dto;

import com.hiresphere.recruiter.entity.Company;
import com.hiresphere.recruiter.entity.CompanyStatus;

import java.time.Instant;

public record CompanyResponse(Long id, Long ownerId, String name, String website, String industry,
                              String description, String location, CompanyStatus status,
                              String rejectionReason, Instant verifiedAt, Instant createdAt) {

    public static CompanyResponse from(Company c) {
        return new CompanyResponse(
                c.getId(),
                c.getOwnerId(),
                c.getName(),
                c.getWebsite(),
                c.getIndustry(),
                c.getDescription(),
                c.getLocation(),
                c.getVerificationStatus(),
                c.getRejectionReason(),
                c.getVerifiedAt(),
                c.getCreatedAt());
    }
}