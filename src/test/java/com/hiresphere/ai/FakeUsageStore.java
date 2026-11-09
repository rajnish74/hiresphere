package com.hiresphere.ai;

import com.hiresphere.ai.usage.AiUsageStore;
import com.hiresphere.ai.usage.FeatureUsage;
import com.hiresphere.ai.usage.UsageEntry;
import com.hiresphere.ai.usage.UsageStatus;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class FakeUsageStore implements AiUsageStore {

    public final List<UsageEntry> entries = new ArrayList<>();
    public boolean failOnRecord;
    public Instant today = Instant.parse("2026-11-09T00:00:00Z");

    @Override
    public void record(UsageEntry entry) {
        if (failOnRecord) {
            throw new IllegalStateException("database is down");
        }
        entries.add(entry);
    }

    @Override
    public long tokensUsedToday(Long userId) {
        return entries.stream().filter(e -> Objects.equals(e.userId(), userId)).mapToLong(UsageEntry::totalTokens).sum();
    }

    @Override
    public long tokensUsedTodayByEveryone() {
        return entries.stream().mapToLong(UsageEntry::totalTokens).sum();
    }

    @Override
    public List<FeatureUsage> summarySince(Instant since) {
        return List.of();
    }

    @Override
    public Instant startOfToday() {
        return today;
    }

    public void spend(Long userId, int tokens) {
        entries.add(new UsageEntry(userId, "SEED", "m", UsageStatus.OK, null, 0, tokens, tokens, 1));
    }
}