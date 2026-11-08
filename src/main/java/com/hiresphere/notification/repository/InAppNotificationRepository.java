package com.hiresphere.notification.repository;

import com.hiresphere.notification.entity.InAppNotification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;

public interface InAppNotificationRepository extends JpaRepository<InAppNotification, Long> {

    Page<InAppNotification> findByUserId(Long userId, Pageable pageable);

    Page<InAppNotification> findByUserIdAndReadAtIsNull(Long userId, Pageable pageable);

    long countByUserIdAndReadAtIsNull(Long userId);

    boolean existsByIdAndUserId(Long id, Long userId);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("update InAppNotification n set n.readAt = :now where n.id = :id and n.userId = :userId and n.readAt is null")
    int markRead(@Param("id") Long id, @Param("userId") Long userId, @Param("now") Instant now);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("update InAppNotification n set n.readAt = :now where n.userId = :userId and n.readAt is null")
    int markAllRead(@Param("userId") Long userId, @Param("now") Instant now);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("delete from InAppNotification n where n.createdAt < :cutoff")
    int deleteOlderThan(@Param("cutoff") Instant cutoff);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query(value = """
            DELETE FROM notifications WHERE id IN (
                SELECT id FROM (
                    SELECT id, row_number() OVER (PARTITION BY user_id ORDER BY id DESC) AS rn
                      FROM notifications
                ) ranked
                WHERE rn > :keep)
            """, nativeQuery = true)
    int trimPerUser(@Param("keep") int keep);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("delete from InAppNotification n where n.id = :id and n.userId = :userId")
    int deleteOwned(@Param("id") Long id, @Param("userId") Long userId);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("delete from InAppNotification n where n.userId = :userId and n.readAt is not null")
    int deleteAllRead(@Param("userId") Long userId);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("delete from InAppNotification n where n.userId = :userId")
    int deleteAllFor(@Param("userId") Long userId);
}