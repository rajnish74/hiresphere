package com.hiresphere.candidate.dto;

import com.hiresphere.candidate.entity.Education;

public record EducationResponse(Long id, String institution, String degree, String fieldOfStudy,
                                Integer startYear, Integer endYear, String grade) {

    public static EducationResponse from(Education e) {
        return new EducationResponse(
                e.getId(),
                e.getInstitution(),
                e.getDegree(),
                e.getFieldOfStudy(),
                e.getStartYear(),
                e.getEndYear(),
                e.getGrade());
    }
}