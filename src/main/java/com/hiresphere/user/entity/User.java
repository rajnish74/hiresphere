package com.hiresphere.user.entity;

import com.hiresphere.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.SQLRestriction;

import java.time.Instant;

@Entity
@Table(name = "users")
@SQLRestriction("deleted = false")
@Getter
@Setter
@NoArgsConstructor
public class User extends BaseEntity {

    @Column(nullable = false, length = 100)
    private String fullName;

    @Column(nullable = false, length = 150)
    private String email;

    @Column(nullable = false)
    private String passwordHash;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Role role;

    @Column(nullable = false)
    private boolean enabled = true;

    @Column(length = 15)
    private String phone;

    @Column(nullable = false)
    private boolean emailVerified = false;

    @Setter(AccessLevel.NONE)
    @Column(nullable = false)
    private int tokenVersion = 0;

    public void invalidateSessions() {
        tokenVersion++;
    }

    @Setter(AccessLevel.NONE)
    private Instant lastLoginAt;
}