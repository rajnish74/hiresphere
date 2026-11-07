package com.hiresphere.notification.stream;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.function.Supplier;

@Component
public class NotificationStreamHub {

    public static final int MAX_CONNECTIONS_PER_USER = 5;
    static final long TIMEOUT_MS = Duration.ofMinutes(30).toMillis();

    private final Map<Long, Deque<SseEmitter>> connections = new ConcurrentHashMap<>();
    private final Supplier<SseEmitter> emitterFactory;

    @Autowired
    public NotificationStreamHub() {
        this(() -> new SseEmitter(TIMEOUT_MS));
    }

    public NotificationStreamHub(Supplier<SseEmitter> emitterFactory) {
        this.emitterFactory = emitterFactory;
    }

    public SseEmitter register(Long userId) {
        SseEmitter emitter = emitterFactory.get();
        emitter.onCompletion(() -> remove(userId, emitter));
        emitter.onError(error -> remove(userId, emitter));
        emitter.onTimeout(() -> {
            remove(userId, emitter);
            emitter.complete();
        });

        List<SseEmitter> evicted = new ArrayList<>();
        connections.compute(userId, (id, existing) -> {
            Deque<SseEmitter> deque = existing != null ? existing : new ConcurrentLinkedDeque<>();
            while (deque.size() >= MAX_CONNECTIONS_PER_USER) {
                SseEmitter oldest = deque.pollFirst();
                if (oldest == null) {
                    break;
                }
                evicted.add(oldest);
            }
            deque.addLast(emitter);
            return deque;
        });
        evicted.forEach(SseEmitter::complete);   // compute ke bahar: callbacks wapas isi map ko chhoo sakte hain

        send(userId, emitter, () -> SseEmitter.event().name("ready").data("ok"));
        return emitter;
    }

    public void nudge(Long userId) {
        sendToUser(userId, () -> SseEmitter.event().name("notification").data("ping"));
    }

    @Scheduled(fixedDelayString = "${app.notifications.heartbeat-ms:25000}")
    public void heartbeat() {
        for (Long userId : List.copyOf(connections.keySet())) {
            sendToUser(userId, () -> SseEmitter.event().comment("keep-alive"));
        }
    }

    public void closeAll(Long userId) {
        Deque<SseEmitter> removed = connections.remove(userId);
        if (removed != null) {
            removed.forEach(SseEmitter::complete);
        }
    }

    public int connectionCount(Long userId) {
        return Optional.ofNullable(connections.get(userId)).map(Deque::size).orElse(0);
    }

    private void sendToUser(Long userId, Supplier<SseEmitter.SseEventBuilder> event) {
        Deque<SseEmitter> deque = connections.get(userId);
        if (deque == null) {
            return;
        }
        for (SseEmitter emitter : List.copyOf(deque)) {
            send(userId, emitter, event);
        }
    }

    private void send(Long userId, SseEmitter emitter, Supplier<SseEmitter.SseEventBuilder> event) {
        try {
            emitter.send(event.get());
        } catch (IOException | IllegalStateException e) {
            remove(userId, emitter);
        }
    }

    private void remove(Long userId, SseEmitter emitter) {
        connections.computeIfPresent(userId, (id, deque) -> {
            deque.remove(emitter);
            return deque.isEmpty() ? null : deque;
        });
    }
}