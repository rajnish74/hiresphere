package com.hiresphere.notification.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Entity
@Table(name = "notification_opt_outs")
@Getter
@NoArgsConstructor
public class NotificationOptOut {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, updatable = false)
    private Long userId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30, updatable = false)
    private NotificationCategory category;

    @Column(nullable = false, insertable = false, updatable = false)
    private Instant createdAt;
}