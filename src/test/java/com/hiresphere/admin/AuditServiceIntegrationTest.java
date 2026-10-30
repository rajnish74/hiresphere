package com.hiresphere.admin;

import com.hiresphere.admin.dto.AuditLogResponse;
import com.hiresphere.admin.entity.AuditAction;
import com.hiresphere.admin.entity.AuditTargetType;
import com.hiresphere.admin.service.AuditService;
import com.hiresphere.user.entity.Role;
import com.hiresphere.user.entity.User;
import com.hiresphere.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
class AuditServiceIntegrationTest {

    @Autowired
    private AuditService auditService;
    @Autowired
    private UserRepository userRepository;

    @Test
    void recordedActionsShouldBeListedAndFilterable() {
        String tag = "zt" + UUID.randomUUID().toString().replace("-", "").substring(0, 10);
        User admin = new User();
        admin.setFullName("Admin " + tag);
        admin.setEmail(tag + "@test.com");
        admin.setPasswordHash("x");
        admin.setRole(Role.ADMIN);
        userRepository.saveAndFlush(admin);

        auditService.record(admin.getId(), AuditAction.USER_BLOCKED, AuditTargetType.USER, 42L, "spam " + tag);
        auditService.record(admin.getId(), AuditAction.JOB_HIDDEN, AuditTargetType.JOB, 43L, "misleading " + tag);

        List<AuditLogResponse> blocked = auditService
                .list(AuditAction.USER_BLOCKED, null, null, admin.getId(), 0, 50).content();
        List<AuditLogResponse> all = auditService.list(null, null, null, admin.getId(), 0, 50).content();
        List<AuditLogResponse> byTarget = auditService
                .list(null, AuditTargetType.JOB, 43L, admin.getId(), 0, 50).content();

        assertThat(blocked).hasSize(1);
        assertThat(blocked.get(0).details()).isEqualTo("spam " + tag);
        assertThat(blocked.get(0).actorEmail()).isEqualTo(tag + "@test.com");
        assertThat(all).hasSize(2);
        assertThat(byTarget).hasSize(1);
        assertThat(byTarget.get(0).action()).isEqualTo(AuditAction.JOB_HIDDEN);
    }
}