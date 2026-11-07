package com.hiresphere.notification;

import com.hiresphere.notification.entity.NotificationCategory;
import com.hiresphere.notification.entity.NotificationChannel;
import com.hiresphere.notification.entity.NotificationOptOut;
import com.hiresphere.notification.repository.NotificationOptOutRepository;
import com.hiresphere.user.entity.Role;
import com.hiresphere.user.entity.User;
import com.hiresphere.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
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

    private User newUser() {
        User u = new User();
        u.setFullName("Pref Tester");
        u.setEmail("zt" + UUID.randomUUID().toString().replace("-", "").substring(0, 10) + "@test.com");
        u.setPasswordHash("x");
        u.setRole(Role.CANDIDATE);
        return userRepository.saveAndFlush(u);
    }

    @Test
    void optOutShouldBeIdempotentPerChannelAndOptInShouldRemoveOnlyThatChannel() {
        User u = newUser();

        assertThat(repository.optOut(u.getId(), "APPLICATION_UPDATES", "EMAIL")).isEqualTo(1);
        assertThat(repository.optOut(u.getId(), "APPLICATION_UPDATES", "EMAIL")).isZero();
        assertThat(repository.optOut(u.getId(), "APPLICATION_UPDATES", "IN_APP")).isEqualTo(1);   // alag channel, alag row
        repository.optOut(u.getId(), "PRODUCT_NEWS", "EMAIL");

        assertThat(repository.findOptedOutCategories(u.getId(), NotificationChannel.EMAIL))
                .containsExactlyInAnyOrder(NotificationCategory.APPLICATION_UPDATES, NotificationCategory.PRODUCT_NEWS);
        assertThat(repository.findOptedOutCategories(u.getId(), NotificationChannel.IN_APP))
                .containsExactly(NotificationCategory.APPLICATION_UPDATES);

        assertThat(repository.optIn(u.getId(), Set.of(NotificationCategory.APPLICATION_UPDATES),
                NotificationChannel.EMAIL)).isEqualTo(1);

        assertThat(repository.findOptedOutCategories(u.getId(), NotificationChannel.EMAIL))
                .containsExactly(NotificationCategory.PRODUCT_NEWS);
        assertThat(repository.findOptedOutCategories(u.getId(), NotificationChannel.IN_APP))
                .containsExactly(NotificationCategory.APPLICATION_UPDATES);   // bell wali bachi
    }

    @Test
    void theChannelShouldBeReadBackFromTheDatabase() {
        User u = newUser();
        repository.optOut(u.getId(), "NEW_APPLICANTS", "IN_APP");

        List<NotificationOptOut> rows = repository.findByUserId(u.getId());

        assertThat(rows).hasSize(1);
        assertThat(rows.get(0).getChannel()).isEqualTo(NotificationChannel.IN_APP);
        assertThat(rows.get(0).getCategory()).isEqualTo(NotificationCategory.NEW_APPLICANTS);
    }
}