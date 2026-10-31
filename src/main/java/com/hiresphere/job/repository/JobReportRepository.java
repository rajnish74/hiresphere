package com.hiresphere.job.repository;

import com.hiresphere.job.dto.JobReportCount;
import com.hiresphere.job.entity.JobReport;
import com.hiresphere.job.entity.JobReportStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Collection;
import java.util.List;

public interface JobReportRepository extends JpaRepository<JobReport, Long> {

    boolean existsByReporterUserIdAndJobId(Long reporterUserId, Long jobId);

    long countByReporterUserIdAndCreatedAtAfter(Long reporterUserId, Instant since);

    Page<JobReport> findByStatus(JobReportStatus status, Pageable pageable);

    @Query("""
            select new com.hiresphere.job.dto.JobReportCount(r.jobId, count(r))
              from JobReport r
             where r.jobId in :jobIds and r.status = :status
             group by r.jobId
            """)
    List<JobReportCount> countByJobIdsAndStatus(@Param("jobIds") Collection<Long> jobIds,
                                                @Param("status") JobReportStatus status);

    Page<JobReport> findByReporterUserId(Long reporterUserId, Pageable pageable);
}