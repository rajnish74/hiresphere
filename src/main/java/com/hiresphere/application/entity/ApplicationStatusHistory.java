package com.hiresphere.application.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Entity
@Table(name = "application_status_history")
@Getter
@NoArgsConstructor
public class ApplicationStatusHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "application_id", nullable = false)
    private JobApplication application;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private ApplicationStatus fromStatus;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ApplicationStatus toStatus;

    @Column(nullable = false, updatable = false)
    private Long changedBy;

    @Column(length = 500)
    private String note;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    static ApplicationStatusHistory of(JobApplication application, ApplicationStatus from,
                                       ApplicationStatus to, Long changedBy, String note) {
        ApplicationStatusHistory h = new ApplicationStatusHistory();
        h.application = application;
        h.fromStatus = from;
        h.toStatus = to;
        h.changedBy = changedBy;
        h.note = note;
        h.createdAt = Instant.now();
        return h;
    }
}