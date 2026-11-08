package com.hiresphere.ai.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;
import java.util.regex.Pattern;

@ConfigurationProperties(prefix = "app.ai")
public record AiProperties(boolean enabled, Gemini gemini) {

    public AiProperties {
        gemini = gemini == null ? new Gemini(null, null, null, null, null, 0) : gemini;
        if (enabled) {
            if (gemini.apiKey().isEmpty()) {
                throw new IllegalStateException("AI is enabled but the Gemini key is empty. Set GEMINI_API_KEY.");
            }
            if (gemini.model().isEmpty()) {
                throw new IllegalStateException(
                        "AI is enabled but no model is chosen. Set GEMINI_MODEL to a model your key can use.");
            }
        }
    }

    public record Gemini(String apiKey, String model, String baseUrl, Duration connectTimeout,
                         Duration readTimeout, int maxOutputTokens) {

        public static final String DEFAULT_BASE_URL = "https://generativelanguage.googleapis.com/v1beta";
        private static final Pattern MODEL_NAME = Pattern.compile("[A-Za-z0-9._-]+");

        public Gemini {
            apiKey = apiKey == null ? "" : apiKey.trim();

            String cleanModel = model == null ? "" : model.trim();
            if (cleanModel.startsWith("models/")) {
                cleanModel = cleanModel.substring("models/".length());
            }
            if (!cleanModel.isEmpty() && !MODEL_NAME.matcher(cleanModel).matches()) {
                throw new IllegalArgumentException("Model name may only contain letters, digits, '.', '_' and '-'");
            }
            model = cleanModel;

            String url = baseUrl == null || baseUrl.isBlank() ? DEFAULT_BASE_URL : baseUrl.trim();
            baseUrl = url.endsWith("/") ? url.substring(0, url.length() - 1) : url;

            connectTimeout = connectTimeout == null ? Duration.ofSeconds(5) : connectTimeout;
            readTimeout = readTimeout == null ? Duration.ofSeconds(30) : readTimeout;
            maxOutputTokens = maxOutputTokens <= 0 ? 1024 : maxOutputTokens;
        }

        public boolean hasKey() {
            return !apiKey.isEmpty();
        }

        @Override
        public String toString() {
            return "Gemini[model=" + model + ", baseUrl=" + baseUrl + ", apiKey=" + (hasKey() ? "<hidden>" : "<empty>")
                    + ", connectTimeout=" + connectTimeout + ", readTimeout=" + readTimeout
                    + ", maxOutputTokens=" + maxOutputTokens + "]";
        }
    }
}