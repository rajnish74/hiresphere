package com.hiresphere.job.repository;

import com.hiresphere.job.entity.Job;
import com.hiresphere.job.entity.JobStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface JobRepository extends JpaRepository<Job, Long> {

    Optional<Job> findByIdAndCompanyId(Long id, Long companyId);

    Page<Job> findByCompanyId(Long companyId, Pageable pageable);

    Page<Job> findByCompanyIdAndStatus(Long companyId, JobStatus status, Pageable pageable);
}