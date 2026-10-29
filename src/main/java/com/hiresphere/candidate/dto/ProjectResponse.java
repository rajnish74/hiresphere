package com.hiresphere.candidate.dto;

import com.hiresphere.candidate.entity.Project;

public record ProjectResponse(Long id,
                              String title,
                              String description,
                              String techStack,
                              String projectUrl,
                              String repoUrl) {

    public static ProjectResponse from(Project p) {
        return new ProjectResponse(
                p.getId(),
                p.getTitle(),
                p.getDescription(),
                p.getTechStack(),
                p.getProjectUrl(),
                p.getRepoUrl());
    }
}