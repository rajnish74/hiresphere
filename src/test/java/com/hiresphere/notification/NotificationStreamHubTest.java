package com.hiresphere.notification;

import com.hiresphere.notification.stream.NotificationStreamHub;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

class NotificationStreamHubTest {

    private final List<SseEmitter> created = new ArrayList<>();
    private final NotificationStreamHub hub = new NotificationStreamHub(() -> {
        SseEmitter emitter = mock(SseEmitter.class);
        created.add(emitter);
        return emitter;
    });

    private static SseEmitter.SseEventBuilder anyEvent() {
        return any(SseEmitter.SseEventBuilder.class);
    }

    @Test
    void registeringShouldKeepTheConnectionAndSendTheReadyEvent() throws IOException {
        hub.register(1L);

        assertThat(hub.connectionCount(1L)).isEqualTo(1);
        verify(created.get(0), times(1)).send(anyEvent());
    }

    @Test
    void aNudgeShouldReachOnlyThatUsersConnections() throws IOException {
        hub.register(1L);
        hub.register(1L);   // doosra tab
        hub.register(2L);

        hub.nudge(1L);

        verify(created.get(0), times(2)).send(anyEvent());   // ready aur nudge
        verify(created.get(1), times(2)).send(anyEvent());
        verify(created.get(2), times(1)).send(anyEvent());   // sirf ready
    }

    @Test
    void nudgingAUserWithoutConnectionsShouldBeHarmless() {
        assertThatCode(() -> hub.nudge(99L)).doesNotThrowAnyException();
        assertThat(hub.connectionCount(99L)).isZero();
    }

    @Test
    void aUserShouldHaveAtMostFiveConnectionsAndTheOldestIsClosed() {
        for (int i = 0; i < NotificationStreamHub.MAX_CONNECTIONS_PER_USER + 1; i++) {
            hub.register(1L);
        }

        assertThat(hub.connectionCount(1L)).isEqualTo(NotificationStreamHub.MAX_CONNECTIONS_PER_USER);
        verify(created.get(0)).complete();
        verify(created.get(1), times(0)).complete();
    }

    @Test
    void aConnectionThatCannotBeWrittenToShouldBeDropped() throws IOException {
        hub.register(1L);
        doThrow(new IOException("client gone")).when(created.get(0)).send(anyEvent());

        hub.nudge(1L);

        assertThat(hub.connectionCount(1L)).isZero();
    }

    @Test
    void completionShouldRemoveTheConnection() {
        hub.register(1L);
        ArgumentCaptor<Runnable> onCompletion = ArgumentCaptor.forClass(Runnable.class);
        verify(created.get(0)).onCompletion(onCompletion.capture());

        onCompletion.getValue().run();

        assertThat(hub.connectionCount(1L)).isZero();
    }

    @Test
    void aTimeoutShouldRemoveAndCloseTheConnection() {
        hub.register(1L);
        ArgumentCaptor<Runnable> onTimeout = ArgumentCaptor.forClass(Runnable.class);
        verify(created.get(0)).onTimeout(onTimeout.capture());

        onTimeout.getValue().run();

        assertThat(hub.connectionCount(1L)).isZero();
        verify(created.get(0)).complete();
    }

    @Test
    void closingAllShouldEndOneUsersStreamsAndLeaveOthersAlone() {
        hub.register(1L);
        hub.register(1L);
        hub.register(2L);

        hub.closeAll(1L);

        assertThat(hub.connectionCount(1L)).isZero();
        verify(created.get(0)).complete();
        verify(created.get(1)).complete();
        assertThat(hub.connectionCount(2L)).isEqualTo(1);
        verify(created.get(2), times(0)).complete();
    }

    @Test
    void theHeartbeatShouldReachEveryConnection() throws IOException {
        hub.register(1L);
        hub.register(2L);

        hub.heartbeat();

        verify(created.get(0), times(2)).send(anyEvent());   // ready aur heartbeat
        verify(created.get(1), times(2)).send(anyEvent());
    }
}