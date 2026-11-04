package com.hiresphere.notification;

import com.hiresphere.notification.entity.InAppNotification;
import com.hiresphere.notification.entity.NotificationTarget;
import com.hiresphere.notification.entity.NotificationType;
import com.hiresphere.notification.repository.InAppNotificationRepository;
import com.hiresphere.user.entity.Role;
import com.hiresphere.user.entity.User;
import com.hiresphere.user.repository.UserRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
class InAppNotificationRepositoryIntegrationTest {

    @Autowired
    private InAppNotificationRepository repository;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private EntityManager em;

    private User alice;
    private User bob;

    @BeforeEach
    void setUp() {
        String tag = "zt" + UUID.randomUUID().toString().replace("-", "").substring(0, 10);
        alice = newUser(tag + "-a@test.com");
        bob = newUser(tag + "-b@test.com");
    }

    private User newUser(String email) {
        User u = new User();
        u.setFullName("User " + email);
        u.setEmail(email);
        u.setPasswordHash("x");
        u.setRole(Role.CANDIDATE);
        return userRepository.saveAndFlush(u);
    }

    private InAppNotification add(User user, String subject) {
        return repository.saveAndFlush(InAppNotification.create(user.getId(),
                NotificationType.APPLICATION_STATUS_CHANGED, subject, "SHORTLISTED", null,
                NotificationTarget.APPLICATION, 1L));
    }

    private void backdate(InAppNotification n, int days) {
        em.createNativeQuery("update notifications set created_at = :t where id = :id")
                .setParameter("t", Instant.now().minus(days, ChronoUnit.DAYS))
                .setParameter("id", n.getId())
                .executeUpdate();
        em.clear();
    }

    @Test
    void theInboxShouldBeNewestFirstAndPrivateToTheUser() {
        add(alice, "first");
        add(alice, "second");
        add(alice, "third");
        add(bob, "bobs");

        List<String> subjects = repository.findByUserId(alice.getId(), PageRequest.of(0, 10,
                        org.springframework.data.domain.Sort.by(org.springframework.data.domain.Sort.Order.desc("createdAt"),
                                org.springframework.data.domain.Sort.Order.desc("id"))))
                .map(InAppNotification::getSubject).getContent();

        assertThat(subjects).containsExactly("third", "second", "first");
    }

    @Test
    void theUnreadCountShouldFallAsNotificationsAreRead() {
        InAppNotification first = add(alice, "first");
        add(alice, "second");
        assertThat(repository.countByUserIdAndReadAtIsNull(alice.getId())).isEqualTo(2);

        assertThat(repository.markRead(first.getId(), alice.getId(), Instant.now())).isEqualTo(1);

        assertThat(repository.countByUserIdAndReadAtIsNull(alice.getId())).isEqualTo(1);
    }

    @Test
    void markingShouldBeIdempotentAndNeverTouchAnotherUsersRow() {
        InAppNotification mine = add(alice, "mine");

        assertThat(repository.markRead(mine.getId(), bob.getId(), Instant.now())).isZero();   // doosre ki
        assertThat(repository.countByUserIdAndReadAtIsNull(alice.getId())).isEqualTo(1);

        assertThat(repository.markRead(mine.getId(), alice.getId(), Instant.now())).isEqualTo(1);
        assertThat(repository.markRead(mine.getId(), alice.getId(), Instant.now())).isZero();  // pehle se read
        assertThat(repository.existsByIdAndUserId(mine.getId(), alice.getId())).isTrue();
        assertThat(repository.existsByIdAndUserId(mine.getId(), bob.getId())).isFalse();
    }

    @Test
    void markAllShouldOnlyAffectTheCallerAndCountWhatChanged() {
        add(alice, "a1");
        add(alice, "a2");
        add(bob, "b1");

        assertThat(repository.markAllRead(alice.getId(), Instant.now())).isEqualTo(2);

        assertThat(repository.countByUserIdAndReadAtIsNull(alice.getId())).isZero();
        assertThat(repository.countByUserIdAndReadAtIsNull(bob.getId())).isEqualTo(1);
        assertThat(repository.markAllRead(alice.getId(), Instant.now())).isZero();
    }

    @Test
    void theUnreadListShouldSkipReadOnes() {
        InAppNotification first = add(alice, "first");
        add(alice, "second");
        repository.markRead(first.getId(), alice.getId(), Instant.now());

        assertThat(repository.findByUserIdAndReadAtIsNull(alice.getId(), PageRequest.of(0, 10))
                .map(InAppNotification::getSubject).getContent()).containsExactly("second");
    }

    @Test
    void cleanupShouldDeleteOnlyOldRows() {
        InAppNotification old = add(alice, "old");
        InAppNotification recent = add(alice, "recent");
        backdate(old, 100);

        assertThat(repository.deleteOlderThan(Instant.now().minus(90, ChronoUnit.DAYS))).isGreaterThanOrEqualTo(1);

        assertThat(repository.findById(old.getId())).isEmpty();
        assertThat(repository.findById(recent.getId())).isPresent();
    }

    @Test
    void trimShouldKeepOnlyTheNewestRowsOfEachUser() {
        for (int i = 1; i <= 5; i++) {
            add(alice, "a" + i);
        }
        add(bob, "b1");

        repository.trimPerUser(2);

        List<String> aliceLeft = repository.findByUserId(alice.getId(), PageRequest.of(0, 10))
                .map(InAppNotification::getSubject).getContent();
        assertThat(aliceLeft).containsExactlyInAnyOrder("a4", "a5");
        assertThat(repository.findByUserId(bob.getId(), PageRequest.of(0, 10)).getContent()).hasSize(1);
    }
}