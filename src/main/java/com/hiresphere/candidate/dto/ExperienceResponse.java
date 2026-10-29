package com.hiresphere.candidate.dto;

import com.hiresphere.candidate.entity.Experience;

import java.time.LocalDate;

public record ExperienceResponse(Long id,
                                 String company,
                                 String title,
                                 String location,
                                 LocalDate startDate,
                                 LocalDate endDate,
                                 String description) {

    public static ExperienceResponse from(Experience e) {
        return new ExperienceResponse(
                e.getId(),
                e.getCompany(),
                e.getTitle(),
                e.getLocation(),
                e.getStartDate(),
                e.getEndDate(),
                e.getDescription());
    }
}