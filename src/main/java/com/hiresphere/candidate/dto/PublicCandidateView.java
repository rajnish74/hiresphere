package com.hiresphere.candidate.dto;

import com.hiresphere.candidate.entity.CandidateProfile;
import com.hiresphere.skill.entity.Skill;

import java.util.List;

public record PublicCandidateView(String fullName, String headline, String summary, String location,
                                  String githubUrl, String portfolioUrl, boolean openToWork,
                                  List<String> skills, List<ProjectResponse> projects) {

    public static PublicCandidateView from(CandidateProfile p, String fullName) {
        return new PublicCandidateView(fullName, p.getHeadline(), p.getSummary(), p.getLocation(),
                p.getGithubUrl(), p.getPortfolioUrl(), p.isOpenToWork(),
                p.getSkills().stream().map(Skill::getName).sorted(String.CASE_INSENSITIVE_ORDER).toList(),
                p.getProjects().stream().map(ProjectResponse::from).toList());
    }
}