package com.hiresphere.job.dto;

public record PublicCompanyProfile(Long id, String name, String website, String industry,
                                   String description, String location, long openJobs) {
}