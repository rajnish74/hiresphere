package com.hiresphere.notification.repository;

import com.hiresphere.notification.entity.EmailLog;
import com.hiresphere.notification.entity.EmailStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EmailLogRepository extends JpaRepository<EmailLog, Long> {

    Page<EmailLog> findByStatus(EmailStatus status, Pageable pageable);
}