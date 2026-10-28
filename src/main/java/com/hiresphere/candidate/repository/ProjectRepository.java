package com.hiresphere.candidate.repository;

import com.hiresphere.candidate.entity.Project;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ProjectRepository extends JpaRepository<Project, Long> {

    Optional<Project> findByIdAndCandidateId(Long id, Long candidateId);
}