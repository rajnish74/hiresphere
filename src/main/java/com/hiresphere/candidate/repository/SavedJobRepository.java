package com.hiresphere.candidate.repository;

import com.hiresphere.candidate.entity.SavedJob;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface SavedJobRepository extends JpaRepository<SavedJob, Long> {

    boolean existsByCandidateUserIdAndJobId(Long candidateUserId, Long jobId);

    long countByCandidateUserId(Long candidateUserId);

    Page<SavedJob> findByCandidateUserId(Long candidateUserId, Pageable pageable);

    @Query("select s.jobId from SavedJob s where s.candidateUserId = :userId")
    List<Long> findJobIdsByCandidateUserId(@Param("userId") Long userId);

    @Modifying
    @Query(value = """
            INSERT INTO saved_jobs (candidate_user_id, job_id, job_title, company_name)
            VALUES (:userId, :jobId, :title, :company)
            ON CONFLICT (candidate_user_id, job_id) DO NOTHING
            """, nativeQuery = true)
    int insertIfAbsent(@Param("userId") Long userId, @Param("jobId") Long jobId,
                       @Param("title") String title, @Param("company") String company);

    @Modifying
    @Query("delete from SavedJob s where s.candidateUserId = :userId and s.jobId = :jobId")
    int deleteOne(@Param("userId") Long userId, @Param("jobId") Long jobId);
}