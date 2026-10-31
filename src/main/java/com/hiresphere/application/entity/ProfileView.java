package com.hiresphere.application.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Entity
@Table(name = "profile_views")
@Getter
@NoArgsConstructor
public class ProfileView {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, updatable = false)
    private Long companyId;

    @Column(nullable = false, updatable = false)
    private Long candidateUserId;

    @Column(nullable = false, updatable = false)
    private LocalDate viewDate;
}