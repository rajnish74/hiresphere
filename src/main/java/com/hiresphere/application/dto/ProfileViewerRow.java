package com.hiresphere.application.dto;

import java.time.LocalDate;

public record ProfileViewerRow(Long companyId, LocalDate lastViewedOn) {
}