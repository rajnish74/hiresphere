package com.hiresphere.candidate.dto;

import com.hiresphere.candidate.entity.CandidateProfile;
import com.hiresphere.skill.entity.Skill;

import java.util.List;

public record CandidateProfileView(Long userId, String fullName, String headline, String summary, String location,
                                   String linkedinUrl, String githubUrl, String portfolioUrl, boolean openToWork,
                                   List<String> skills, List<EducationResponse> education,
                                   List<ExperienceResponse> experience, List<ProjectResponse> projects) {

    public static CandidateProfileView from(CandidateProfile p, String fullName) {
        return new CandidateProfileView(p.getUserId(), fullName, p.getHeadline(), p.getSummary(), p.getLocation(),
                p.getLinkedinUrl(), p.getGithubUrl(), p.getPortfolioUrl(), p.isOpenToWork(),
                p.getSkills().stream().map(Skill::getName).sorted(String.CASE_INSENSITIVE_ORDER).toList(),
                p.getEducation().stream().map(EducationResponse::from).toList(),
                p.getExperience().stream().map(ExperienceResponse::from).toList(),
                p.getProjects().stream().map(ProjectResponse::from).toList());
    }
}