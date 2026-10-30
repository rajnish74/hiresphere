package com.hiresphere.user;

import com.hiresphere.user.entity.Role;
import com.hiresphere.user.entity.User;
import com.hiresphere.user.repository.UserRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
class UserLastLoginIntegrationTest {

    @Autowired
    private UserRepository userRepository;
    @Autowired
    private EntityManager em;

    private User newUser() {
        String tag = "zt" + UUID.randomUUID().toString().replace("-", "").substring(0, 10);
        User u = new User();
        u.setFullName("Login " + tag);
        u.setEmail(tag + "@test.com");
        u.setPasswordHash("x");
        u.setRole(Role.CANDIDATE);
        return userRepository.saveAndFlush(u);
    }

    @Test
    void touchShouldWriteOnlyWhenNeverLoggedInOrOlderThanTheThreshold() {
        User u = newUser();
        Instant now = Instant.now();
        Instant threshold = now.minus(5, ChronoUnit.MINUTES);

        assertThat(userRepository.touchLastLogin(u.getId(), now.minus(10, ChronoUnit.MINUTES),
                now.minus(15, ChronoUnit.MINUTES))).isEqualTo(1);
        assertThat(userRepository.touchLastLogin(u.getId(), now, threshold)).isEqualTo(1);
        assertThat(userRepository.touchLastLogin(u.getId(), now.plusSeconds(30), threshold)).isZero();

        em.clear();
        Instant stored = userRepository.findById(u.getId()).orElseThrow().getLastLoginAt();
        assertThat(Duration.between(stored, now).abs()).isLessThan(Duration.ofSeconds(1));
    }

    @Test
    void touchShouldNotBumpTheVersionOrUpdatedAt() {
        User u = newUser();
        Long versionBefore = u.getVersion();
        Instant updatedAtBefore = u.getUpdatedAt();

        userRepository.touchLastLogin(u.getId(), Instant.now(), Instant.now().minus(5, ChronoUnit.MINUTES));

        em.clear();
        User reloaded = userRepository.findById(u.getId()).orElseThrow();
        assertThat(reloaded.getLastLoginAt()).isNotNull();
        assertThat(reloaded.getVersion()).isEqualTo(versionBefore);
        assertThat(reloaded.getUpdatedAt()).isEqualTo(updatedAtBefore);
    }
}