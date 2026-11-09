package com.hiresphere.ai.usage;

public record UsageEntry(Long userId, String feature, String model, UsageStatus status, String errorKind,
                         int promptTokens, int outputTokens, int totalTokens, long latencyMs) {
}