package com.hiresphere.ai.usage;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public final class UsageLog {

    private UsageLog() {
    }

    public static void quietly(AiUsageStore store, UsageEntry entry) {
        try {
            store.record(entry);
        } catch (RuntimeException e) {
            log.warn("Could not write the AI usage row (feature {}): {}", entry.feature(), e.toString());
        }
    }
}