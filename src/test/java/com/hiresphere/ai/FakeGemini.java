package com.hiresphere.ai;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** Asli HTTP server jo Gemini jaisa bartav karta hai. Jawab pehle se tay (script) hote hain, aur har request record hoti hai. */
public final class FakeGemini implements AutoCloseable {

    private static final JsonMapper JSON = JsonMapper.builder().build();

    public record Reply(int status, String body, long delayMs) {
    }

    public record Seen(String path, String query, String apiKey, String body) {
    }

    public final List<Seen> seen = new CopyOnWriteArrayList<>();
    private final Queue<Reply> script = new ConcurrentLinkedQueue<>();
    private volatile Reply fallback = new Reply(500, "{\"error\":{\"message\":\"unscripted call\"}}", 0);
    private final HttpServer server;
    private final ExecutorService executor = Executors.newFixedThreadPool(4);

    public FakeGemini() throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/", this::handle);
        server.setExecutor(executor);
        server.start();
    }

    public String baseUrl() {
        return "http://127.0.0.1:" + server.getAddress().getPort() + "/v1beta";
    }

    public int requests() {
        return seen.size();
    }

    /** Agli request ke liye ek jawab. Script khatam ho to `always` wala. */
    public FakeGemini then(int status, String body) {
        script.add(new Reply(status, body, 0));
        return this;
    }

    public FakeGemini thenAfter(long delayMs, int status, String body) {
        script.add(new Reply(status, body, delayMs));
        return this;
    }

    public FakeGemini always(int status, String body) {
        fallback = new Reply(status, body, 0);
        return this;
    }

    /** Gemini ka sahi jawab jiska text `{"skills": [...]}` hai. */
    public static String skillsAnswer(int promptTokens, int outputTokens, String... skills) {
        return textAnswer(JSON.writeValueAsString(Map.of("skills", List.of(skills))), promptTokens, outputTokens);
    }

    public static String textAnswer(String text, int promptTokens, int outputTokens) {
        return JSON.writeValueAsString(Map.of(
                "candidates", List.of(Map.of(
                        "content", Map.of("role", "model", "parts", List.of(Map.of("text", text))),
                        "finishReason", "STOP")),
                "usageMetadata", Map.of("promptTokenCount", promptTokens, "candidatesTokenCount", outputTokens,
                        "totalTokenCount", promptTokens + outputTokens)));
    }

    private void handle(HttpExchange exchange) throws IOException {
        byte[] request = exchange.getRequestBody().readAllBytes();
        seen.add(new Seen(exchange.getRequestURI().getPath(), exchange.getRequestURI().getRawQuery(),
                exchange.getRequestHeaders().getFirst("x-goog-api-key"), new String(request, StandardCharsets.UTF_8)));

        Reply reply = script.poll();
        if (reply == null) {
            reply = fallback;
        }
        if (reply.delayMs() > 0) {
            try {
                Thread.sleep(reply.delayMs());
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        byte[] out = reply.body().getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        try {
            exchange.sendResponseHeaders(reply.status(), out.length);
            try (OutputStream body = exchange.getResponseBody()) {
                body.write(out);
            }
        } catch (IOException ignored) {
            // client timeout par pehle hi chala gaya ho sakta hai
        }
    }

    @Override
    public void close() {
        server.stop(0);
        executor.shutdownNow();
    }
}