package com.hiresphere.candidate.dto;

import com.hiresphere.candidate.entity.CandidateProfile;
import com.hiresphere.candidate.entity.ProfileVisibility;
import com.hiresphere.skill.entity.Skill;

import java.time.Instant;
import java.util.List;

public record ProfileResponse(
        Long id, String headline, String summary, String location,
        String linkedinUrl, String githubUrl, String portfolioUrl,
        ProfileVisibility visibility, boolean openToWork,
        List<String> skills, List<EducationResponse> education,
        List<ExperienceResponse> experience, List<ProjectResponse> projects,
        ResumeInfo resume, Instant updatedAt) {

    public static ProfileResponse from(CandidateProfile p) {
        return new ProfileResponse(
                p.getId(),
                p.getHeadline(),
                p.getSummary(),
                p.getLocation(),
                p.getLinkedinUrl(),
                p.getGithubUrl(),
                p.getPortfolioUrl(),
                p.getVisibility(),
                p.isOpenToWork(),
                p.getSkills().stream().map(Skill::getName)
                        .sorted(String.CASE_INSENSITIVE_ORDER).toList(),
                p.getEducation().stream().map(EducationResponse::from).toList(),
                p.getExperience().stream().map(ExperienceResponse::from).toList(),
                p.getProjects().stream().map(ProjectResponse::from).toList(),
                ResumeInfo.from(p),
                p.getUpdatedAt());
    }
}