package com.hiresphere.recruiter.event;

import com.hiresphere.recruiter.entity.CompanyStatus;

public record CompanyStatusChangedEvent(Long companyId,
                                        Long ownerId,
                                        String companyName,
                                        CompanyStatus status,
                                        String reason) {
}