package com.hiresphere.ai;

import java.util.Map;


public record AiRequest(String systemInstruction, String prompt, Double temperature, Integer maxOutputTokens,
                        Map<String, Object> jsonSchema) {

    public AiRequest {
        if (prompt == null || prompt.isBlank()) {
            throw new IllegalArgumentException("prompt must not be blank");
        }
        if (temperature != null && (temperature < 0 || temperature > 2)) {
            throw new IllegalArgumentException("temperature must be between 0 and 2");
        }
        if (maxOutputTokens != null && maxOutputTokens < 1) {
            throw new IllegalArgumentException("maxOutputTokens must be positive");
        }
    }

    public static AiRequest text(String systemInstruction, String prompt) {
        return new AiRequest(systemInstruction, prompt, null, null, null);
    }

    public static AiRequest json(String systemInstruction, String prompt, Map<String, Object> schema) {
        return new AiRequest(systemInstruction, prompt, 0.2, null, schema);
    }

    public boolean wantsJson() {
        return jsonSchema != null;
    }
}