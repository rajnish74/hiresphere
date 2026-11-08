package com.hiresphere.ai.gemini;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.List;
import java.util.Map;

public final class GeminiDtos {

    private GeminiDtos() {
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record Request(Content systemInstruction, List<Content> contents, GenerationConfig generationConfig) {
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Content(String role, List<Part> parts) {
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Part(String text, Boolean thought) {
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record GenerationConfig(Double temperature, Integer maxOutputTokens, String responseMimeType,
                                   Map<String, Object> responseJsonSchema) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Response(List<Candidate> candidates, PromptFeedback promptFeedback, UsageMetadata usageMetadata) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Candidate(Content content, String finishReason) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record PromptFeedback(String blockReason) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record UsageMetadata(Integer promptTokenCount, Integer candidatesTokenCount, Integer totalTokenCount) {
    }
}