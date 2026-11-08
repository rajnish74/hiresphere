package com.hiresphere.ai;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class AdminAiApiTest {

    @Value("${local.server.port}")
    private int port;

    private int status(String method, String path) throws Exception {
        HttpRequest request = HttpRequest.newBuilder(URI.create("http://localhost:" + port + path))
                .method(method, HttpRequest.BodyPublishers.noBody()).build();
        return HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.discarding()).statusCode();
    }

    @Test
    void theAiAdminEndpointsShouldNeedALogin() throws Exception {
        assertThat(status("GET", "/api/v1/admin/ai/status")).isEqualTo(401);
        assertThat(status("POST", "/api/v1/admin/ai/ping")).isEqualTo(401);
    }
}