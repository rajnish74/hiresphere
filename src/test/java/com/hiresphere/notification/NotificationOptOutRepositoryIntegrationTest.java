package com.hiresphere.notification;

import com.hiresphere.notification.entity.NotificationCategory;
import com.hiresphere.notification.repository.NotificationOptOutRepository;
import com.hiresphere.user.entity.Role;
import com.hiresphere.user.entity.User;
import com.hiresphere.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
class NotificationOptOutRepositoryIntegrationTest {

    @Autowired
    private NotificationOptOutRepository repository;
    @Autowired
    private UserRepository userRepository;

    @Test
    void optOutShouldBeIdempotentAndOptInShouldRemoveIt() {
        User u = new User();
        u.setFullName("Pref Tester");
        u.setEmail("zt" + UUID.randomUUID().toString().replace("-", "").substring(0, 10) + "@test.com");
        u.setPasswordHash("x");
        u.setRole(Role.CANDIDATE);
        userRepository.saveAndFlush(u);

        assertThat(repository.optOut(u.getId(), "APPLICATION_UPDATES")).isEqualTo(1);
        assertThat(repository.optOut(u.getId(), "APPLICATION_UPDATES")).isZero();
        repository.optOut(u.getId(), "PRODUCT_NEWS");
        assertThat(repository.findOptedOutCategories(u.getId()))
                .containsExactlyInAnyOrder(NotificationCategory.APPLICATION_UPDATES, NotificationCategory.PRODUCT_NEWS);

        assertThat(repository.optIn(u.getId(), Set.of(NotificationCategory.APPLICATION_UPDATES))).isEqualTo(1);
        assertThat(repository.findOptedOutCategories(u.getId())).containsExactly(NotificationCategory.PRODUCT_NEWS);
    }
}