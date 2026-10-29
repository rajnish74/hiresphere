package com.hiresphere.application.entity;

import com.hiresphere.common.entity.BaseEntity;
import com.hiresphere.common.exception.AppException;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.SQLRestriction;
import org.springframework.http.HttpStatus;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "applications")
@SQLRestriction("deleted = false")
@Getter
@Setter
@NoArgsConstructor
public class JobApplication extends BaseEntity {

    @Column(nullable = false, updatable = false)
    private Long jobId;

    @Column(nullable = false, updatable = false)
    private Long companyId;

    @Column(nullable = false, updatable = false)
    private Long candidateUserId;

    @Setter(AccessLevel.NONE)
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ApplicationStatus status = ApplicationStatus.APPLIED;

    @Column(columnDefinition = "text")
    private String coverLetter;

    @Column(nullable = false, length = 150)
    private String jobTitle;

    @Column(nullable = false, length = 150)
    private String companyName;

    @Column(nullable = false, length = 100)
    private String candidateName;

    @Column(length = 150)
    private String candidateHeadline;

    @Column(nullable = false, length = 100)
    private String resumeStorageKey;

    @Column(nullable = false, length = 255)
    private String resumeOriginalName;

    @Setter(AccessLevel.NONE)
    @OneToMany(mappedBy = "application", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("createdAt ASC, id ASC")
    private List<ApplicationStatusHistory> history = new ArrayList<>();

    public void recordSubmitted(Long candidateUserId) {
        history.add(ApplicationStatusHistory.of(this, null, ApplicationStatus.APPLIED, candidateUserId, null));
    }

    public void transitionTo(ApplicationStatus target, Long changedBy, String note) {
        if (!status.canMoveTo(target)) {
            throw new AppException(HttpStatus.CONFLICT,
                    "An application that is " + status + " cannot be moved to " + target);
        }
        ApplicationStatus from = status;
        status = target;
        history.add(ApplicationStatusHistory.of(this, from, target, changedBy, note));
    }
}