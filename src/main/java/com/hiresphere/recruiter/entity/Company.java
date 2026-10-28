package com.hiresphere.recruiter.entity;

import com.hiresphere.common.entity.BaseEntity;
import com.hiresphere.common.exception.AppException;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.SQLRestriction;
import org.springframework.http.HttpStatus;

import java.time.Instant;

@Entity
@Table(name = "companies")
@SQLRestriction("deleted = false")
@Getter
@Setter
@NoArgsConstructor
public class Company extends BaseEntity {

    @Column(nullable = false, updatable = false)
    private Long ownerId;

    @Column(nullable = false, length = 150)
    private String name;

    @Column(length = 255)
    private String website;

    @Column(length = 100)
    private String industry;

    @Column(columnDefinition = "text")
    private String description;

    @Column(length = 100)
    private String location;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private CompanyStatus verificationStatus = CompanyStatus.PENDING;

    @Column(length = 500)
    private String rejectionReason;

    private Instant verifiedAt;

    private Long verifiedBy;

    public void approve(Long adminId) {
        if (verificationStatus == CompanyStatus.APPROVED) {
            throw conflict("Company is already approved");
        }
        verificationStatus = CompanyStatus.APPROVED;
        rejectionReason = null;
        verifiedAt = Instant.now();
        verifiedBy = adminId;
    }

    public void reject(Long adminId, String reason) {
        if (verificationStatus != CompanyStatus.PENDING) {
            throw conflict("Only pending companies can be rejected");
        }
        verificationStatus = CompanyStatus.REJECTED;
        rejectionReason = reason;
        verifiedAt = Instant.now();
        verifiedBy = adminId;
    }

    public void suspend(Long adminId, String reason) {
        if (verificationStatus != CompanyStatus.APPROVED) {
            throw conflict("Only approved companies can be suspended");
        }
        verificationStatus = CompanyStatus.SUSPENDED;
        rejectionReason = reason;
        verifiedAt = Instant.now();
        verifiedBy = adminId;
    }

    public void resubmit() {
        verificationStatus = CompanyStatus.PENDING;
        rejectionReason = null;
    }

    private AppException conflict(String message) {
        return new AppException(HttpStatus.CONFLICT, message);
    }
}