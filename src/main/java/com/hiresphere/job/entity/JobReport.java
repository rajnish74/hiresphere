package com.hiresphere.job.entity;

import com.hiresphere.common.exception.AppException;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.http.HttpStatus;

import java.time.Instant;

@Entity
@Table(name = "job_reports")
@Getter
@NoArgsConstructor
public class JobReport {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, updatable = false)
    private Long jobId;

    @Column(nullable = false, updatable = false)
    private Long reporterUserId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30, updatable = false)
    private JobReportReason reason;

    @Column(length = 1000, updatable = false)
    private String details;

    @Column(nullable = false, length = 150, updatable = false)
    private String jobTitle;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private JobReportStatus status = JobReportStatus.OPEN;

    private Long resolvedBy;

    @Column(length = 500)
    private String resolutionNote;

    private Instant resolvedAt;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @Version
    private Long version;

    public static JobReport create(Long jobId, Long reporterUserId, JobReportReason reason,
                                   String details, String jobTitle) {
        JobReport r = new JobReport();
        r.jobId = jobId;
        r.reporterUserId = reporterUserId;
        r.reason = reason;
        r.details = details;
        r.jobTitle = jobTitle;
        r.createdAt = Instant.now();
        return r;
    }

    public void resolve(Long adminId, String note) {
        close(JobReportStatus.RESOLVED, adminId, note);
    }

    public void dismiss(Long adminId, String note) {
        close(JobReportStatus.DISMISSED, adminId, note);
    }

    private void close(JobReportStatus target, Long adminId, String note) {
        if (status != JobReportStatus.OPEN) {
            throw new AppException(HttpStatus.CONFLICT, "This report is already " + status.name().toLowerCase());
        }
        status = target;
        resolvedBy = adminId;
        resolutionNote = note;
        resolvedAt = Instant.now();
    }
}