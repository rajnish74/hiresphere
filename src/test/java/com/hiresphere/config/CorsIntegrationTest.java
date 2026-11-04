package com.hiresphere.config;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestPropertySource(properties = "app.cors.allowed-origins=http://localhost:5173")
class CorsIntegrationTest {

    private static final String ALLOWED = "http://localhost:5173";
    private static final String EVIL = "http://evil.example.com";

    @Value("${local.server.port}")
    private int port;

    private final HttpClient client = HttpClient.newHttpClient();

    private HttpResponse<String> call(String method, String path, String origin, String... headers)
            throws Exception {
        HttpRequest.Builder builder = HttpRequest.newBuilder(URI.create("http://localhost:" + port + path))
                .method(method, HttpRequest.BodyPublishers.noBody())
                .header("Origin", origin);
        for (int i = 0; i < headers.length; i += 2) {
            builder.header(headers[i], headers[i + 1]);
        }
        return client.send(builder.build(), HttpResponse.BodyHandlers.ofString());
    }

    @Test
    void preflightFromAListedOriginShouldBeAllowed() throws Exception {
        HttpResponse<String> r = call("OPTIONS", "/api/v1/jobs", ALLOWED,
                "Access-Control-Request-Method", "GET", "Access-Control-Request-Headers", "authorization");

        assertThat(r.statusCode()).isEqualTo(200);
        assertThat(r.headers().firstValue("access-control-allow-origin")).contains(ALLOWED);
    }

    @Test
    void preflightForAProtectedEndpointShouldNotNeedALogin() throws Exception {
        HttpResponse<String> r = call("OPTIONS", "/api/v1/admin/stats", ALLOWED,
                "Access-Control-Request-Method", "GET", "Access-Control-Request-Headers", "authorization");

        assertThat(r.statusCode()).isEqualTo(200);   // 401 nahi: preflight mein token hota hi nahi
    }

    @Test
    void anUnlistedOriginShouldBeRejected() throws Exception {
        HttpResponse<String> r = call("OPTIONS", "/api/v1/jobs", EVIL, "Access-Control-Request-Method", "GET");

        assertThat(r.statusCode()).isEqualTo(403);
        assertThat(r.headers().firstValue("access-control-allow-origin")).isEmpty();
    }

    @Test
    void aSimpleGetFromAListedOriginShouldSucceedAndExposeTheRequestId() throws Exception {
        HttpResponse<String> r = call("GET", "/api/v1/jobs", ALLOWED);

        assertThat(r.statusCode()).isEqualTo(200);
        assertThat(r.headers().firstValue("access-control-allow-origin")).contains(ALLOWED);
        assertThat(r.headers().firstValue("access-control-expose-headers").orElse("")).contains("X-Request-Id");
        assertThat(r.headers().firstValue("x-request-id")).isPresent();   // B4 ka filter chal raha hai
    }

    @Test
    void aSimpleGetFromAnUnlistedOriginShouldBeRejected() throws Exception {
        assertThat(call("GET", "/api/v1/jobs", EVIL).statusCode()).isEqualTo(403);
    }
}