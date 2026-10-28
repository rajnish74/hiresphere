package com.hiresphere.candidate.entity;

import com.hiresphere.common.entity.BaseEntity;
import com.hiresphere.skill.entity.Skill;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.SQLRestriction;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Entity
@Table(name = "candidate_profiles")
@SQLRestriction("deleted = false")
@Getter
@Setter
@NoArgsConstructor
public class CandidateProfile extends BaseEntity {

    @Column(nullable = false, updatable = false)
    private Long userId;

    @Column(length = 150)
    private String headline;

    @Column(columnDefinition = "text")
    private String summary;

    @Column(length = 100)
    private String location;

    @Column(length = 255)
    private String linkedinUrl;

    @Column(length = 255)
    private String githubUrl;

    @Column(length = 255)
    private String portfolioUrl;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ProfileVisibility visibility = ProfileVisibility.RECRUITERS_ONLY;

    @Column(nullable = false)
    private boolean openToWork = true;

    @Column(length = 100)
    private String resumeStorageKey;

    @Column(length = 255)
    private String resumeOriginalName;

    private Long resumeSizeBytes;

    private Instant resumeUploadedAt;

    @ManyToMany
    @JoinTable(name = "candidate_skills",
            joinColumns = @JoinColumn(name = "candidate_id"),
            inverseJoinColumns = @JoinColumn(name = "skill_id"))
    private Set<Skill> skills = new HashSet<>();

    @OneToMany(mappedBy = "candidate", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("startYear DESC")
    private List<Education> education = new ArrayList<>();

    @OneToMany(mappedBy = "candidate", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("startDate DESC")
    private List<Experience> experience = new ArrayList<>();
}