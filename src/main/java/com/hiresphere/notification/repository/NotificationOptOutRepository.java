package com.hiresphere.notification.repository;

import com.hiresphere.notification.entity.NotificationCategory;
import com.hiresphere.notification.entity.NotificationOptOut;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

public interface NotificationOptOutRepository extends JpaRepository<NotificationOptOut, Long> {

    @Query("select o.category from NotificationOptOut o where o.userId = :userId")
    List<NotificationCategory> findOptedOutCategories(@Param("userId") Long userId);

    @Modifying
    @Query(value = """
            INSERT INTO notification_opt_outs (user_id, category)
            VALUES (:userId, :category)
            ON CONFLICT (user_id, category) DO NOTHING
            """, nativeQuery = true)
    int optOut(@Param("userId") Long userId, @Param("category") String category);

    @Modifying
    @Query("delete from NotificationOptOut o where o.userId = :userId and o.category in :categories")
    int optIn(@Param("userId") Long userId, @Param("categories") Collection<NotificationCategory> categories);
}