package com.hiresphere.application.repository;

import com.hiresphere.application.entity.ApplicationStatus;
import com.hiresphere.application.entity.ApplicationStatusHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;

public interface ApplicationStatusHistoryRepository extends JpaRepository<ApplicationStatusHistory, Long> {

    @Query("""
            select count(h) from ApplicationStatusHistory h
             where h.application.candidateUserId = :userId
               and h.toStatus = :status
               and h.createdAt > :since
            """)
    long countTransitionsForCandidate(@Param("userId") Long userId, @Param("status") ApplicationStatus status,
                                      @Param("since") Instant since);
}