package com.hiresphere.ai.usage;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
class AiUsageRepositoryIntegrationTest {

    @Autowired
    private AiUsageRepository repository;

    private final long userId = 900_000_000L + (long) (Math.random() * 1_000_000);
    private final String feature = "ZT_" + UUID.randomUUID().toString().replace("-", "").substring(0, 12).toUpperCase();

    private void add(Long user, UsageStatus status, int tokens, Instant at) {
        repository.saveAndFlush(AiUsage.from(
                new UsageEntry(user, feature, "m", status, status == UsageStatus.FAILED ? "TIMEOUT" : null, 0, tokens,
                        tokens, 5), at));
    }

    @Test
    void theBudgetSumsShouldCountOnlyThatUsersTokensSinceTheGivenTime() {
        Instant now = Instant.now();
        Instant startOfDay = now.minus(6, ChronoUnit.HOURS);
        add(userId, UsageStatus.OK, 100, now);
        add(userId, UsageStatus.OK, 50, now.minusSeconds(60));
        add(userId, UsageStatus.OK, 999, now.minus(2, ChronoUnit.DAYS));   // kal se pehle ka: nahi ginta
        add(userId + 1, UsageStatus.OK, 7_000, now);                         // kisi aur ka

        assertThat(repository.sumTokensForUser(userId, startOfDay)).isEqualTo(150);
        assertThat(repository.sumTokensForUser(userId + 5, startOfDay)).isZero();
    }

    @Test
    void theGlobalSumShouldGrowByWhatWasAdded() {
        Instant since = Instant.now().minus(1, ChronoUnit.HOURS);
        long before = repository.sumTokensAll(since);

        add(null, UsageStatus.OK, 40, Instant.now());      // system call (koi user nahi)
        add(userId, UsageStatus.OK, 60, Instant.now());

        assertThat(repository.sumTokensAll(since) - before).isEqualTo(100);
    }

    @Test
    void theSummaryShouldCountEachStatusSeparately() {
        Instant now = Instant.now();
        add(userId, UsageStatus.OK, 30, now);
        add(userId, UsageStatus.OK, 20, now);
        add(userId, UsageStatus.FAILED, 0, now);
        add(userId, UsageStatus.CACHED, 0, now);

        List<Object[]> rows = repository.summarySince(now.minus(1, ChronoUnit.HOURS));

        Object[] mine = rows.stream().filter(r -> feature.equals(r[0])).findFirst().orElseThrow();
        assertThat(((Number) mine[1]).longValue()).isEqualTo(4);   // calls
        assertThat(((Number) mine[2]).longValue()).isEqualTo(2);   // ok
        assertThat(((Number) mine[3]).longValue()).isEqualTo(1);   // failed
        assertThat(((Number) mine[4]).longValue()).isEqualTo(1);   // cached
        assertThat(((Number) mine[5]).longValue()).isEqualTo(50);  // tokens
    }

    @Test
    void cleanupShouldRemoveOnlyOldRows() {
        AiUsage old = repository.saveAndFlush(AiUsage.from(
                new UsageEntry(userId, feature, "m", UsageStatus.OK, null, 1, 1, 2, 1),
                Instant.now().minus(100, ChronoUnit.DAYS)));
        AiUsage recent = repository.saveAndFlush(AiUsage.from(
                new UsageEntry(userId, feature, "m", UsageStatus.OK, null, 1, 1, 2, 1), Instant.now()));

        assertThat(repository.deleteOlderThan(Instant.now().minus(90, ChronoUnit.DAYS))).isGreaterThanOrEqualTo(1);

        assertThat(repository.findById(old.getId())).isEmpty();
        assertThat(repository.findById(recent.getId())).isPresent();
    }
}