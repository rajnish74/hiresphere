package com.hiresphere.auth.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Entity
@Table(name = "auth_tokens")
@Getter
@NoArgsConstructor
public class AuthToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, updatable = false)
    private Long userId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30, updatable = false)
    private AuthTokenType type;

    @Column(nullable = false, unique = true, length = 64, updatable = false)
    private String tokenHash;

    @Column(nullable = false, updatable = false)
    private Instant expiresAt;

    private Instant usedAt;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    public static AuthToken create(Long userId, AuthTokenType type, String tokenHash, Instant expiresAt) {
        AuthToken t = new AuthToken();
        t.userId = userId;
        t.type = type;
        t.tokenHash = tokenHash;
        t.expiresAt = expiresAt;
        t.createdAt = Instant.now();
        return t;
    }
}