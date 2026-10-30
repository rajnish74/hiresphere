package com.hiresphere.candidate.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Entity
@Table(name = "saved_jobs")
@Getter
@NoArgsConstructor
public class SavedJob {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, updatable = false)
    private Long candidateUserId;

    @Column(nullable = false, updatable = false)
    private Long jobId;

    @Column(nullable = false, length = 150, updatable = false)
    private String jobTitle;

    @Column(nullable = false, length = 150, updatable = false)
    private String companyName;

    @Column(nullable = false, insertable = false, updatable = false)
    private Instant createdAt;
}