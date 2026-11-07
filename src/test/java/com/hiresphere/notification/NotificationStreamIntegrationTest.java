package com.hiresphere.notification;

import com.hiresphere.notification.stream.NotificationStreamHub;
import com.hiresphere.notification.stream.StreamTicketService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class NotificationStreamIntegrationTest {

    @Value("${local.server.port}")
    private int port;
    @Autowired
    private StreamTicketService tickets;
    @Autowired
    private NotificationStreamHub hub;

    private final HttpClient client = HttpClient.newHttpClient();

    private URI uri(String path) {
        return URI.create("http://localhost:" + port + path);
    }

    private int statusOf(HttpRequest request) throws Exception {
        return client.send(request, HttpResponse.BodyHandlers.discarding()).statusCode();
    }

    private void awaitLine(BlockingQueue<String> lines, String expected) throws InterruptedException {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(8);
        while (System.nanoTime() < deadline) {
            String line = lines.poll(200, TimeUnit.MILLISECONDS);
            if (line != null && line.replace(" ", "").equals(expected)) {
                return;
            }
        }
        fail("Did not receive '" + expected + "' in time");
    }

    @Test
    void aValidTicketShouldOpenAStreamThatGetsNudgesAndEndsWhenClosed() throws Exception {
        long userId = 987_654L;   // hub memory mein hai, DB ka user nahi chahiye
        String ticket = tickets.issue(userId);
        BlockingQueue<String> lines = new LinkedBlockingQueue<>();

        HttpRequest request = HttpRequest.newBuilder(uri("/api/v1/notifications/stream?ticket=" + ticket))
                .header("Accept", "text/event-stream").GET().build();
        CompletableFuture<Void> consumer = client.sendAsync(request, HttpResponse.BodyHandlers.ofLines())
                .thenAccept(response -> {
                    lines.add("status:" + response.statusCode());
                    response.body().forEach(lines::add);
                });

        awaitLine(lines, "status:200");
        awaitLine(lines, "event:ready");
        hub.nudge(userId);
        awaitLine(lines, "event:notification");

        hub.closeAll(userId);
        consumer.get(8, TimeUnit.SECONDS);   // stream ab khatam
        assertThat(hub.connectionCount(userId)).isZero();
    }

    @Test
    void aTicketThatWasAlreadyUsedShouldBeRejected() throws Exception {
        String ticket = tickets.issue(555L);
        assertThat(tickets.redeem(ticket)).contains(555L);

        int status = statusOf(HttpRequest.newBuilder(uri("/api/v1/notifications/stream?ticket=" + ticket)).GET().build());

        assertThat(status).isEqualTo(401);
    }

    @Test
    void anUnknownOrMissingTicketShouldBeRejectedWithoutALogin() throws Exception {
        assertThat(statusOf(HttpRequest.newBuilder(uri("/api/v1/notifications/stream?ticket=nope")).GET().build()))
                .isEqualTo(401);
        assertThat(statusOf(HttpRequest.newBuilder(uri("/api/v1/notifications/stream")).GET().build()))
                .isEqualTo(401);
    }

    @Test
    void gettingATicketShouldNeedALogin() throws Exception {
        int status = statusOf(HttpRequest.newBuilder(uri("/api/v1/notifications/stream-ticket"))
                .POST(HttpRequest.BodyPublishers.noBody()).build());

        assertThat(status).isEqualTo(401);
    }
}