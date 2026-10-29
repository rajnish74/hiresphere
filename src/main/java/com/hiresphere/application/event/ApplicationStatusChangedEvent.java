package com.hiresphere.application.event;

import com.hiresphere.application.entity.ApplicationStatus;

public record ApplicationStatusChangedEvent(Long applicationId, Long companyId, Long candidateUserId,
                                            String candidateName, String jobTitle, String companyName,
                                            ApplicationStatus from, ApplicationStatus to,
                                            String note, boolean byCandidate) {
}