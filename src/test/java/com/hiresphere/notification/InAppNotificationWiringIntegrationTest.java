package com.hiresphere.notification;

import com.hiresphere.application.entity.ApplicationStatus;
import com.hiresphere.application.event.ApplicationStatusChangedEvent;
import com.hiresphere.application.event.ApplicationSubmittedEvent;
import com.hiresphere.common.exception.ResourceNotFoundException;
import com.hiresphere.notification.dto.NotificationResponse;
import com.hiresphere.notification.entity.NotificationTarget;
import com.hiresphere.notification.entity.NotificationType;
import com.hiresphere.notification.service.InAppNotificationService;
import com.hiresphere.recruiter.entity.Company;
import com.hiresphere.recruiter.repository.CompanyRepository;
import com.hiresphere.user.entity.Role;
import com.hiresphere.user.entity.User;
import com.hiresphere.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Transactional
class InAppNotificationWiringIntegrationTest {

    @Autowired
    private ApplicationEventPublisher publisher;
    @Autowired
    private InAppNotificationService notifications;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private CompanyRepository companyRepository;

    private User recruiter;
    private User candidate;
    private Company company;

    @BeforeEach
    void setUp() {
        String tag = "zt" + UUID.randomUUID().toString().replace("-", "").substring(0, 10);
        recruiter = newUser(tag + "-rec@test.com", Role.RECRUITER);
        candidate = newUser(tag + "-cand@test.com", Role.CANDIDATE);
        Company c = new Company();
        c.setOwnerId(recruiter.getId());
        c.setName("Company " + tag);
        company = companyRepository.saveAndFlush(c);
    }

    private User newUser(String email, Role role) {
        User u = new User();
        u.setFullName("User " + email);
        u.setEmail(email);
        u.setPasswordHash("x");
        u.setRole(role);
        return userRepository.saveAndFlush(u);
    }

    @Test
    void applicationEventsShouldReachTheRightInboxes() {
        publisher.publishEvent(new ApplicationSubmittedEvent(77L, company.getId(), candidate.getId(), "Asha",
                "Java Dev", "Acme"));

        assertThat(notifications.unreadCount(recruiter.getId())).isEqualTo(1);
        assertThat(notifications.unreadCount(candidate.getId())).isZero();

        publisher.publishEvent(new ApplicationStatusChangedEvent(77L, company.getId(), candidate.getId(), "Asha",
                "Java Dev", "Acme", ApplicationStatus.APPLIED, ApplicationStatus.SHORTLISTED, "Good", false));

        List<NotificationResponse> inbox = notifications.list(candidate.getId(), false, 0, 20).content();
        assertThat(inbox).hasSize(1);
        assertThat(inbox.get(0).type()).isEqualTo(NotificationType.APPLICATION_STATUS_CHANGED);
        assertThat(inbox.get(0).detail()).isEqualTo("SHORTLISTED");
        assertThat(inbox.get(0).note()).isEqualTo("Good");
        assertThat(inbox.get(0).targetType()).isEqualTo(NotificationTarget.APPLICATION);
        assertThat(inbox.get(0).targetId()).isEqualTo(77L);
        assertThat(inbox.get(0).read()).isFalse();
    }

    @Test
    void aWithdrawalShouldGoToTheRecruiterOnly() {
        publisher.publishEvent(new ApplicationStatusChangedEvent(77L, company.getId(), candidate.getId(), "Asha",
                "Java Dev", "Acme", ApplicationStatus.APPLIED, ApplicationStatus.WITHDRAWN, null, true));

        assertThat(notifications.list(recruiter.getId(), false, 0, 20).content())
                .extracting(NotificationResponse::type).containsExactly(NotificationType.APPLICATION_WITHDRAWN);
        assertThat(notifications.unreadCount(candidate.getId())).isZero();
    }

    @Test
    void readingShouldBeLimitedToTheOwnerAndBeRepeatable() {
        publisher.publishEvent(new ApplicationSubmittedEvent(77L, company.getId(), candidate.getId(), "Asha",
                "Java Dev", "Acme"));
        Long id = notifications.list(recruiter.getId(), false, 0, 20).content().get(0).id();

        assertThatThrownBy(() -> notifications.markRead(candidate.getId(), id))
                .isInstanceOf(ResourceNotFoundException.class);   // doosre ka
        notifications.markRead(recruiter.getId(), id);
        notifications.markRead(recruiter.getId(), id);            // dobara: theek

        assertThat(notifications.unreadCount(recruiter.getId())).isZero();
        assertThat(notifications.list(recruiter.getId(), true, 0, 20).content()).isEmpty();
        assertThat(notifications.list(recruiter.getId(), false, 0, 20).content().get(0).read()).isTrue();
    }
}