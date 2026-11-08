package com.hiresphere.notification;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class NotificationDeleteApiTest {

    @Value("${local.server.port}")
    private int port;

    private int deleteStatus(String path) throws Exception {
        HttpRequest request = HttpRequest.newBuilder(URI.create("http://localhost:" + port + path)).DELETE().build();
        return HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.discarding()).statusCode();
    }

    @Test
    void deletingAnythingShouldNeedALogin() throws Exception {
        assertThat(deleteStatus("/api/v1/notifications/1")).isEqualTo(401);
        assertThat(deleteStatus("/api/v1/notifications?scope=ALL")).isEqualTo(401);
        assertThat(deleteStatus("/api/v1/notifications")).isEqualTo(401);
    }
}