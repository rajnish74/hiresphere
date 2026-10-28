package com.hiresphere.candidate.service;

import com.hiresphere.candidate.dto.ProjectRequest;
import com.hiresphere.candidate.dto.ProjectResponse;
import com.hiresphere.candidate.entity.CandidateProfile;
import com.hiresphere.candidate.entity.Project;
import com.hiresphere.candidate.repository.ProjectRepository;
import com.hiresphere.common.exception.ResourceNotFoundException;
import com.hiresphere.common.util.TextUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ProjectService {

    private final CandidateProfileService profileService;
    private final ProjectRepository projectRepository;

    @Transactional
    public ProjectResponse add(Long userId, ProjectRequest req) {
        Project project = new Project();
        project.setCandidate(profileService.getOrCreate(userId));
        apply(project, req);
        return ProjectResponse.from(projectRepository.save(project));
    }

    @Transactional
    public ProjectResponse update(Long userId, Long id, ProjectRequest req) {
        Project project = findOwned(userId, id);
        apply(project, req);
        return ProjectResponse.from(project);
    }

    @Transactional
    public void delete(Long userId, Long id) {
        Project project = findOwned(userId, id);
        project.getCandidate().getProjects().remove(project);
    }

    private Project findOwned(Long userId, Long id) {
        CandidateProfile profile = profileService.getOrCreate(userId);
        return projectRepository.findByIdAndCandidateId(id, profile.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Project not found"));
    }

    private void apply(Project p, ProjectRequest r) {
        p.setTitle(r.title().trim());
        p.setDescription(TextUtil.blankToNull(r.description()));
        p.setTechStack(TextUtil.blankToNull(r.techStack()));
        p.setProjectUrl(r.projectUrl());
        p.setRepoUrl(r.repoUrl());
    }
}