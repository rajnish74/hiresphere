package com.hiresphere.candidate.repository;

import com.hiresphere.candidate.entity.Education;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface EducationRepository extends JpaRepository<Education, Long> {

    Optional<Education> findByIdAndCandidateId(Long id, Long candidateId);
}