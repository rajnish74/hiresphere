package com.hiresphere.job.entity;

import com.hiresphere.common.entity.BaseEntity;
import com.hiresphere.common.exception.AppException;
import com.hiresphere.skill.entity.Skill;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.SQLRestriction;
import org.springframework.http.HttpStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "jobs")
@SQLRestriction("deleted = false")
@Getter
@Setter
@NoArgsConstructor
public class Job extends BaseEntity {

    @Column(nullable = false, updatable = false)
    private Long companyId;

    @Column(nullable = false, length = 150)
    private String title;

    @Column(nullable = false, columnDefinition = "text")
    private String description;

    @Column(length = 100)
    private String location;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private JobType jobType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private WorkMode workMode;

    @Column(nullable = false)
    private Integer minExperienceYears = 0;

    private Integer maxExperienceYears;

    @Column(precision = 12, scale = 2)
    private BigDecimal salaryMin;

    @Column(precision = 12, scale = 2)
    private BigDecimal salaryMax;

    @Column(nullable = false, length = 3)
    private String currency = "INR";

    @Column(nullable = false)
    private Integer openings = 1;

    @Setter(AccessLevel.NONE)
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private JobStatus status = JobStatus.DRAFT;

    private Instant publishedAt;

    private Instant closedAt;

    private Instant expiresAt;

    @ManyToMany
    @JoinTable(name = "job_skills",
            joinColumns = @JoinColumn(name = "job_id"),
            inverseJoinColumns = @JoinColumn(name = "skill_id"))
    private Set<Skill> skills = new HashSet<>();

    public void publish() {
        moveTo(JobStatus.PUBLISHED);
        if (publishedAt == null) {
            publishedAt = Instant.now();
        }
        closedAt = null;
    }

    public void close() {
        moveTo(JobStatus.CLOSED);
        closedAt = Instant.now();
    }

    private void moveTo(JobStatus target) {
        if (!status.canMoveTo(target)) {
            throw new AppException(HttpStatus.CONFLICT,
                    "A " + status + " job cannot be moved to " + target);
        }
        status = target;
    }

    @Setter(AccessLevel.NONE)
    @Column(nullable = false)
    private boolean hidden = false;

    @Setter(AccessLevel.NONE)
    @Column(length = 500)
    private String moderationReason;

    public void hideByAdmin(String reason) {
        if (hidden) {
            throw new AppException(HttpStatus.CONFLICT, "Job is already hidden");
        }
        hidden = true;
        moderationReason = reason;
    }

    public void unhideByAdmin() {
        if (!hidden) {
            throw new AppException(HttpStatus.CONFLICT, "Job is not hidden");
        }
        hidden = false;
        moderationReason = null;
    }
}