package com.hiresphere.notification;

import com.hiresphere.notification.dto.EmailLogResponse;
import com.hiresphere.notification.entity.EmailLog;
import com.hiresphere.notification.entity.EmailStatus;
import com.hiresphere.notification.repository.EmailLogRepository;
import com.hiresphere.notification.service.EmailLogService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
class EmailLogServiceTest {

    @Autowired
    private EmailLogService service;
    @Autowired
    private EmailLogRepository repository;

    @Test
    void statusFilterShouldReturnOnlyMatchingRows() {
        String tag = "zt" + UUID.randomUUID().toString().replace("-", "").substring(0, 10);
        String failedTo = tag + "-failed@test.com";
        String sentTo = tag + "-sent@test.com";
        repository.save(EmailLog.of(failedTo, "Subject", "tpl", EmailStatus.FAILED, 3, "boom"));
        repository.save(EmailLog.of(sentTo, "Subject", "tpl", EmailStatus.SENT, 1, null));
        repository.flush();

        List<EmailLogResponse> failed = service.list(EmailStatus.FAILED, 0, 50).content();
        List<EmailLogResponse> all = service.list(null, 0, 50).content();

        assertThat(failed).extracting(EmailLogResponse::recipient).contains(failedTo).doesNotContain(sentTo);
        assertThat(failed).allMatch(r -> r.status() == EmailStatus.FAILED);
        assertThat(all).extracting(EmailLogResponse::recipient).contains(failedTo, sentTo);
    }
}