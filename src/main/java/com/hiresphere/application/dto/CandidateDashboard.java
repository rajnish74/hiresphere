package com.hiresphere.application.dto;

import com.hiresphere.application.entity.ApplicationStatus;
import com.hiresphere.candidate.service.ProfileCompleteness;

import java.util.List;
import java.util.Map;

public record CandidateDashboard(ProfileCompleteness.Result profile, long totalApplications,
                                 Map<ApplicationStatus, Long> applicationsByStatus, WeeklyChange thisWeek,
                                 long savedJobs, int profileViewers30d,
                                 List<ApplicationSummary> recentApplications) {

    public record WeeklyChange(long applications, long shortlisted, long interviews) {
    }
}