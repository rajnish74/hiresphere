package com.hiresphere.ai;

public record AiResponse(String text, String finishReason, boolean truncated, int promptTokens, int outputTokens,
                         int totalTokens, String model, long latencyMs) {
}