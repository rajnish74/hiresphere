package com.hiresphere.application.repository;

import com.hiresphere.application.dto.StatusCount;
import com.hiresphere.application.entity.ApplicationStatus;
import com.hiresphere.application.entity.JobApplication;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface JobApplicationRepository extends JpaRepository<JobApplication, Long> {

    boolean existsByJobIdAndCandidateUserId(Long jobId, Long candidateUserId);

    Optional<JobApplication> findByIdAndCandidateUserId(Long id, Long candidateUserId);

    Optional<JobApplication> findByIdAndCompanyId(Long id, Long companyId);

    Page<JobApplication> findByCandidateUserId(Long candidateUserId, Pageable pageable);

    Page<JobApplication> findByCandidateUserIdAndStatus(Long candidateUserId, ApplicationStatus status,
                                                        Pageable pageable);

    Page<JobApplication> findByJobId(Long jobId, Pageable pageable);

    Page<JobApplication> findByJobIdAndStatus(Long jobId, ApplicationStatus status, Pageable pageable);

    @Query("""
        select new com.hiresphere.application.dto.StatusCount(a.status, count(a))
          from JobApplication a
         where a.jobId = :jobId
         group by a.status
        """)
    List<StatusCount> countByStatus(@Param("jobId") Long jobId);

    long countByJobIdAndStatus(Long jobId, ApplicationStatus status);

    @Query("select a.status, count(a) from JobApplication a group by a.status")
    List<Object[]> countGroupedByStatus();

    long countByCreatedAtAfter(Instant since);
}