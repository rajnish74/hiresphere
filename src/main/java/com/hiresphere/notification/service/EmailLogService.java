package com.hiresphere.notification.service;

import com.hiresphere.common.dto.PageResponse;
import com.hiresphere.common.util.PageUtil;
import com.hiresphere.notification.dto.EmailLogResponse;
import com.hiresphere.notification.entity.EmailLog;
import com.hiresphere.notification.entity.EmailStatus;
import com.hiresphere.notification.repository.EmailLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class EmailLogService {

    private final EmailLogRepository repository;

    @Transactional(readOnly = true)
    public PageResponse<EmailLogResponse> list(EmailStatus status, int page, int size) {
        Pageable pageable = PageUtil.newestFirst(page, size);
        Page<EmailLog> result = (status == null)
                ? repository.findAll(pageable)
                : repository.findByStatus(status, pageable);
        return PageResponse.of(result, EmailLogResponse::from);
    }
}