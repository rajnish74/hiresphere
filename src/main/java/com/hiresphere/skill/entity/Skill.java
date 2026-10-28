package com.hiresphere.skill.entity;

import jakarta.persistence.*;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "skills")
@Getter
@NoArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class Skill {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 60)
    private String name;

    @EqualsAndHashCode.Include
    @Column(nullable = false, length = 60)
    private String normalizedName;

    public Skill(String name, String normalizedName) {
        this.name = name;
        this.normalizedName = normalizedName;
    }
}