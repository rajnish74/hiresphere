package com.hiresphere.user;

import com.hiresphere.user.event.SessionsInvalidatedEvent;
import com.hiresphere.user.repository.UserRepository;
import com.hiresphere.user.service.TokenVersionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TokenVersionServiceTest {

    @Mock
    private UserRepository userRepository;

    private TokenVersionService service;

    @BeforeEach
    void setUp() {
        service = new TokenVersionService(userRepository);
    }

    @Test
    void matchingVersionShouldBeAcceptedAndCached() {
        when(userRepository.findTokenVersionById(7L)).thenReturn(Optional.of(2));

        assertThat(service.isCurrent(7L, 2)).isTrue();
        assertThat(service.isCurrent(7L, 2)).isTrue();

        verify(userRepository, times(1)).findTokenVersionById(7L);   // doosri baar cache se
    }

    @Test
    void staleVersionShouldBeRejected() {
        when(userRepository.findTokenVersionById(7L)).thenReturn(Optional.of(3));

        assertThat(service.isCurrent(7L, 2)).isFalse();
    }

    @Test
    void newerTokenThanCachedVersionShouldTriggerReloadAndBeAccepted() {
        when(userRepository.findTokenVersionById(7L)).thenReturn(Optional.of(1), Optional.of(2));

        assertThat(service.isCurrent(7L, 1)).isTrue();   // cache mein 1
        assertThat(service.isCurrent(7L, 2)).isTrue();   // mismatch, DB se reload, ab 2
    }

    @Test
    void evictionShouldMakeOldTokensFailImmediately() {
        when(userRepository.findTokenVersionById(7L)).thenReturn(Optional.of(1), Optional.of(2));
        assertThat(service.isCurrent(7L, 1)).isTrue();

        service.onSessionsInvalidated(new SessionsInvalidatedEvent(7L));

        assertThat(service.isCurrent(7L, 1)).isFalse();
    }

    @Test
    void unknownUserShouldBeRejected() {
        when(userRepository.findTokenVersionById(99L)).thenReturn(Optional.empty());

        assertThat(service.isCurrent(99L, 0)).isFalse();
    }
}