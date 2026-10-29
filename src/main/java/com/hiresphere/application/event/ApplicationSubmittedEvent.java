package com.hiresphere.application.event;

public record ApplicationSubmittedEvent(Long applicationId, Long companyId, Long candidateUserId,
                                        String candidateName, String jobTitle, String companyName) {
}