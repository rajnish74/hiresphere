package com.hiresphere.application.dto;

import java.time.LocalDate;

public record ProfileViewerResponse(Long companyId, String companyName, LocalDate lastViewedOn) {
}