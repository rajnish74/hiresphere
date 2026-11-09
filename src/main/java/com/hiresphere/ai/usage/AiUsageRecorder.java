package com.hiresphere.ai.usage;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;

@Service
public class AiUsageRecorder implements AiUsageStore {

    private final AiUsageRepository repository;
    private final Clock clock;

    @Autowired
    public AiUsageRecorder(AiUsageRepository repository) {
        this(repository, Clock.systemUTC());
    }

    public AiUsageRecorder(AiUsageRepository repository, Clock clock) {
        this.repository = repository;
        this.clock = clock;
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void record(UsageEntry entry) {
        repository.save(AiUsage.from(entry, clock.instant()));
    }

    @Override
    @Transactional(readOnly = true)
    public long tokensUsedToday(Long userId) {
        return repository.sumTokensForUser(userId, startOfToday());
    }

    @Override
    @Transactional(readOnly = true)
    public long tokensUsedTodayByEveryone() {
        return repository.sumTokensAll(startOfToday());
    }

    @Override
    @Transactional(readOnly = true)
    public List<FeatureUsage> summarySince(Instant since) {
        return repository.summarySince(since).stream()
                .map(r -> new FeatureUsage((String) r[0], num(r[1]), num(r[2]), num(r[3]), num(r[4]), num(r[5])))
                .toList();
    }

    @Override
    public Instant startOfToday() {
        return clock.instant().atZone(ZoneOffset.UTC).toLocalDate().atStartOfDay(ZoneOffset.UTC).toInstant();
    }

    private static long num(Object value) {
        return ((Number) value).longValue();
    }
}