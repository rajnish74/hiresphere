package com.hiresphere.ai.gemini;

import com.hiresphere.ai.AiClient;
import com.hiresphere.ai.AiException;
import com.hiresphere.ai.AiException.Kind;
import com.hiresphere.ai.AiRequest;
import com.hiresphere.ai.AiResponse;
import com.hiresphere.ai.config.AiProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.io.IOException;
import java.net.SocketTimeoutException;
import java.net.http.HttpTimeoutException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Component
@ConditionalOnProperty(name = "app.ai.enabled", havingValue = "true")
public class GeminiClient implements AiClient {

    private static final Set<String> BLOCKED_FINISH =
            Set.of("SAFETY", "RECITATION", "BLOCKLIST", "PROHIBITED_CONTENT", "SPII", "IMAGE_SAFETY");
    private static final int MAX_DETAIL = 300;

    private final RestClient restClient;
    private final AiProperties.Gemini config;

    public GeminiClient(@Qualifier("geminiRestClient") RestClient restClient, AiProperties props) {
        this.restClient = restClient;
        this.config = props.gemini();
    }

    @Override
    public AiResponse generate(AiRequest request) {
        long started = System.nanoTime();
        try {
            GeminiDtos.Response raw = call(toBody(request));
            AiResponse response = toResponse(raw, (System.nanoTime() - started) / 1_000_000);
            log.info("AI call ok: model={}, finish={}, promptTokens={}, outputTokens={}, latencyMs={}",
                    response.model(), response.finishReason(), response.promptTokens(), response.outputTokens(),
                    response.latencyMs());
            return response;
        } catch (AiException e) {
            log.warn("AI call failed: kind={}, detail={}", e.getKind(), e.getDetail());
            throw e;
        }
    }

    private GeminiDtos.Response call(GeminiDtos.Request body) {
        try {
            return restClient.post()
                    .uri("/models/{model}:generateContent", config.model())
                    .header("x-goog-api-key", config.apiKey())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .onStatus(HttpStatusCode::isError, (req, res) -> {
                        throw fromStatus(res.getStatusCode().value(), readBody(res));
                    })
                    .body(GeminiDtos.Response.class);
        } catch (AiException e) {
            throw e;
        } catch (ResourceAccessException e) {
            throw new AiException(isTimeout(e) ? Kind.TIMEOUT : Kind.UPSTREAM,
                    e.getClass().getSimpleName() + ": " + clip(e.getMessage()));
        } catch (RestClientException e) {
            // 200 aaya lekin body padhi nahi ja saki (HTML, adhoora JSON)
            throw new AiException(Kind.UPSTREAM, "Unreadable response: " + e.getClass().getSimpleName());
        }
    }

    private GeminiDtos.Request toBody(AiRequest request) {
        GeminiDtos.Content system = request.systemInstruction() == null || request.systemInstruction().isBlank()
                ? null
                : new GeminiDtos.Content(null, List.of(new GeminiDtos.Part(request.systemInstruction(), null)));
        GeminiDtos.Content user = new GeminiDtos.Content("user", List.of(new GeminiDtos.Part(request.prompt(), null)));
        GeminiDtos.GenerationConfig generation = new GeminiDtos.GenerationConfig(
                request.temperature(),
                request.maxOutputTokens() != null ? request.maxOutputTokens() : config.maxOutputTokens(),
                request.wantsJson() ? "application/json" : null,
                request.jsonSchema());
        return new GeminiDtos.Request(system, List.of(user), generation);
    }

    private AiResponse toResponse(GeminiDtos.Response raw, long latencyMs) {
        if (raw == null) {
            throw new AiException(Kind.EMPTY, "Empty body");
        }
        if (raw.promptFeedback() != null && raw.promptFeedback().blockReason() != null) {
            throw new AiException(Kind.BLOCKED, "Prompt blocked: " + raw.promptFeedback().blockReason());
        }
        if (raw.candidates() == null || raw.candidates().isEmpty()) {
            throw new AiException(Kind.EMPTY, "No candidates");
        }

        GeminiDtos.Candidate candidate = raw.candidates().get(0);
        String finish = candidate.finishReason();
        String text = candidate.content() == null || candidate.content().parts() == null ? "" :
                candidate.content().parts().stream()
                        .filter(p -> p.text() != null && !Boolean.TRUE.equals(p.thought()))   // soch ke tukde nahi
                        .map(GeminiDtos.Part::text)
                        .collect(Collectors.joining());

        if (text.isBlank()) {
            if (finish != null && BLOCKED_FINISH.contains(finish)) {
                throw new AiException(Kind.BLOCKED, "Answer blocked: " + finish);
            }
            throw new AiException(Kind.EMPTY, "Blank answer, finishReason=" + finish);
        }

        GeminiDtos.UsageMetadata usage = raw.usageMetadata();
        return new AiResponse(text, finish, "MAX_TOKENS".equals(finish),
                usage == null || usage.promptTokenCount() == null ? 0 : usage.promptTokenCount(),
                usage == null || usage.candidatesTokenCount() == null ? 0 : usage.candidatesTokenCount(),
                usage == null || usage.totalTokenCount() == null ? 0 : usage.totalTokenCount(),
                config.model(), latencyMs);
    }

    private AiException fromStatus(int status, String body) {
        String detail = "HTTP " + status + ": " + clip(body);
        if (status == 429) {
            return new AiException(Kind.RATE_LIMITED, detail);
        }
        if (status == 400 || status == 401 || status == 403 || status == 404) {
            return new AiException(Kind.MISCONFIGURED, detail);
        }
        return new AiException(Kind.UPSTREAM, detail);
    }

    private static boolean isTimeout(Throwable error) {
        for (Throwable t = error; t != null; t = t.getCause() == t ? null : t.getCause()) {
            if (t instanceof SocketTimeoutException || t instanceof HttpTimeoutException) {
                return true;
            }
        }
        return false;
    }

    private static String readBody(org.springframework.http.client.ClientHttpResponse response) {
        try {
            return new String(response.getBody().readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            return "";
        }
    }

    private static String clip(String text) {
        if (text == null) {
            return "";
        }
        String flat = text.replaceAll("\\s+", " ").trim();
        return flat.length() <= MAX_DETAIL ? flat : flat.substring(0, MAX_DETAIL) + "...";
    }
}