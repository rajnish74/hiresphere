package com.hiresphere.notification.repository;

import com.hiresphere.notification.entity.NotificationCategory;
import com.hiresphere.notification.entity.NotificationChannel;
import com.hiresphere.notification.entity.NotificationOptOut;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

public interface NotificationOptOutRepository extends JpaRepository<NotificationOptOut, Long> {

    List<NotificationOptOut> findByUserId(Long userId);

    @Query("select o.category from NotificationOptOut o where o.userId = :userId and o.channel = :channel")
    List<NotificationCategory> findOptedOutCategories(@Param("userId") Long userId,
                                                      @Param("channel") NotificationChannel channel);

    @Modifying
    @Query(value = """
            INSERT INTO notification_opt_outs (user_id, category, channel)
            VALUES (:userId, :category, :channel)
            ON CONFLICT (user_id, category, channel) DO NOTHING
            """, nativeQuery = true)
    int optOut(@Param("userId") Long userId, @Param("category") String category, @Param("channel") String channel);

    @Modifying
    @Query("delete from NotificationOptOut o where o.userId = :userId and o.category in :categories and o.channel = :channel")
    int optIn(@Param("userId") Long userId, @Param("categories") Collection<NotificationCategory> categories,
              @Param("channel") NotificationChannel channel);
}