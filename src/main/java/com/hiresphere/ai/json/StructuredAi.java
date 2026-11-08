package com.hiresphere.ai.json;

import com.hiresphere.ai.AiClient;
import com.hiresphere.ai.AiException;
import com.hiresphere.ai.AiException.Kind;
import com.hiresphere.ai.AiRequest;
import com.hiresphere.ai.AiResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;

import java.util.Map;


@Slf4j
@Component
@RequiredArgsConstructor
public class StructuredAi {

    private final AiClient client;
    private final JsonMapper json;

    public record Structured<T>(T value, AiResponse response) {
    }

    public <T> Structured<T> ask(String system, String prompt, Map<String, Object> schema, Class<T> type) {
        AiResponse response = client.generate(AiRequest.json(system, prompt, schema));

        if (response.truncated()) {
            throw invalid("Answer cut off at the token limit, finishReason=" + response.finishReason());
        }
        String text = unwrap(response.text());
        if (!text.startsWith("{") || !text.endsWith("}")) {
            throw invalid("Answer is not a single JSON object");
        }
        try {
            T value = json.readValue(text, type);
            if (value == null) {
                throw invalid("Empty value");
            }
            return new Structured<>(value, response);
        } catch (JacksonException e) {
            throw invalid("Unparseable JSON: " + e.getClass().getSimpleName() + ": " + firstLine(e.getMessage()));
        }
    }

    static String unwrap(String raw) {
        String text = raw.strip();
        if (text.startsWith("```")) {
            int newline = text.indexOf('\n');
            text = newline < 0 ? "" : text.substring(newline + 1).stripTrailing();
            if (text.endsWith("```")) {
                text = text.substring(0, text.length() - 3);
            }
            text = text.strip();
        }
        return text;
    }

    private AiException invalid(String detail) {
        AiException e = new AiException(Kind.INVALID_OUTPUT, detail);
        log.warn("AI output rejected: {}", detail);
        return e;
    }

    private static String firstLine(String message) {
        if (message == null) {
            return "";
        }
        String line = message.lines().findFirst().orElse("");
        return line.length() <= 120 ? line : line.substring(0, 120) + "...";
    }
}