package com.hiresphere.application.dto;

import java.util.List;

public record ProfileViewSummary(int days, int companiesCount, List<ProfileViewerResponse> viewers) {
}