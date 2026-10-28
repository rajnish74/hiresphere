package com.hiresphere.job.repository;

import com.hiresphere.job.entity.Job;
import com.hiresphere.job.entity.JobStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Optional;

public interface JobRepository extends JpaRepository<Job, Long>, JpaSpecificationExecutor<Job> {

    Optional<Job> findByIdAndCompanyId(Long id, Long companyId);

    Page<Job> findByCompanyId(Long companyId, Pageable pageable);

    Page<Job> findByCompanyIdAndStatus(Long companyId, JobStatus status, Pageable pageable);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("""
        update Job j
           set j.status = :closed,
               j.closedAt = :now,
               j.updatedAt = :now,
               j.version = j.version + 1
         where j.status = :published
           and j.expiresAt is not null
           and j.expiresAt < :now
           and j.deleted = false
        """)
    int closeExpired(@Param("published") JobStatus published,
                     @Param("closed") JobStatus closed,
                     @Param("now") Instant now);
}