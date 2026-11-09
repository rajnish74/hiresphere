package com.hiresphere.ai.usage;

import java.time.Instant;
import java.util.List;

public interface AiUsageStore {

    void record(UsageEntry entry);

    long tokensUsedToday(Long userId);

    long tokensUsedTodayByEveryone();

    List<FeatureUsage> summarySince(Instant since);

    Instant startOfToday();
}