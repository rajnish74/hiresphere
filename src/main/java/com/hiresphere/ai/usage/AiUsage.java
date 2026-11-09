package com.hiresphere.ai.usage;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.Objects;

@Entity
@Table(name = "ai_usage")
@Getter
@NoArgsConstructor
public class AiUsage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(updatable = false)
    private Long userId;

    @Column(nullable = false, length = 40, updatable = false)
    private String feature;

    @Column(length = 100, updatable = false)
    private String model;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10, updatable = false)
    private UsageStatus status;

    @Column(length = 30, updatable = false)
    private String errorKind;

    @Column(nullable = false, updatable = false)
    private int promptTokens;

    @Column(nullable = false, updatable = false)
    private int outputTokens;

    @Column(nullable = false, updatable = false)
    private int totalTokens;

    @Column(nullable = false, updatable = false)
    private long latencyMs;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    public static AiUsage from(UsageEntry e, Instant now) {
        AiUsage u = new AiUsage();
        u.userId = e.userId();
        u.feature = clip(e.feature() == null || e.feature().isBlank() ? "UNSPECIFIED" : e.feature(), 40);
        u.model = clip(e.model(), 100);
        u.status = Objects.requireNonNull(e.status(), "status");
        u.errorKind = clip(e.errorKind(), 30);
        u.promptTokens = Math.max(0, e.promptTokens());
        u.outputTokens = Math.max(0, e.outputTokens());
        u.totalTokens = Math.max(0, e.totalTokens());
        u.latencyMs = Math.max(0, e.latencyMs());
        u.createdAt = now;
        return u;
    }

    private static String clip(String value, int max) {
        return value == null || value.length() <= max ? value : value.substring(0, max);
    }
}