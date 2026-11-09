package com.hiresphere.ai.usage;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;

public interface AiUsageRepository extends JpaRepository<AiUsage, Long> {

    @Query(value = "SELECT coalesce(sum(total_tokens), 0) FROM ai_usage WHERE user_id = :userId AND created_at >= :since",
            nativeQuery = true)
    long sumTokensForUser(@Param("userId") Long userId, @Param("since") Instant since);

    @Query(value = "SELECT coalesce(sum(total_tokens), 0) FROM ai_usage WHERE created_at >= :since", nativeQuery = true)
    long sumTokensAll(@Param("since") Instant since);

    @Query(value = """
            SELECT feature,
                   count(*) AS calls,
                   count(*) FILTER (WHERE status = 'OK') AS ok,
                   count(*) FILTER (WHERE status = 'FAILED') AS failed,
                   count(*) FILTER (WHERE status = 'CACHED') AS cached,
                   coalesce(sum(total_tokens), 0) AS tokens
              FROM ai_usage
             WHERE created_at >= :since
             GROUP BY feature
             ORDER BY tokens DESC, feature
            """, nativeQuery = true)
    List<Object[]> summarySince(@Param("since") Instant since);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("delete from AiUsage u where u.createdAt < :cutoff")
    int deleteOlderThan(@Param("cutoff") Instant cutoff);
}