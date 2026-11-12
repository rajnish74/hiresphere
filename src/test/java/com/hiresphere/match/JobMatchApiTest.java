package com.hiresphere.match;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class JobMatchApiTest {

    @Value("${local.server.port}")
    private int port;

    @Test
    void theEndpointShouldNeedALogin() throws Exception {
        for (String path : new String[]{"/api/v1/candidates/me/job-matches/1", "/api/v1/candidates/me/job-matches/1?explain=true"}) {
            HttpRequest request = HttpRequest.newBuilder(URI.create("http://localhost:" + port + path)).GET().build();

            int status = HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.discarding()).statusCode();

            assertThat(status).as(path).isEqualTo(401);
        }
    }
}