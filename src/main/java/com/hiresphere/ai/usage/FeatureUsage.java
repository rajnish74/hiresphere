package com.hiresphere.ai.usage;

public record FeatureUsage(String feature, long calls, long ok, long failed, long cached, long tokens) {
}