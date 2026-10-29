package com.hiresphere.recruiter.dto;

import com.hiresphere.recruiter.entity.Company;
import com.hiresphere.recruiter.entity.CompanyStatus;

import java.time.Instant;

public record AdminCompanyResponse(Long id,
                                   Long ownerId,
                                   String ownerEmail,
                                   String name,
                                   String website,
                                   String industry,
                                   String description,
                                   String location,
                                   CompanyStatus status,
                                   String rejectionReason,
                                   Instant verifiedAt,
                                   Instant createdAt) {

    public static AdminCompanyResponse from(Company c, String ownerEmail) {
        return new AdminCompanyResponse(c.getId(),
                c.getOwnerId(),
                ownerEmail,
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