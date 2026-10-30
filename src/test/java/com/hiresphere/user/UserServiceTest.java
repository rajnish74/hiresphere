package com.hiresphere.user;

import com.hiresphere.common.exception.AppException;
import com.hiresphere.common.exception.BadRequestException;
import com.hiresphere.common.exception.ResourceNotFoundException;
import com.hiresphere.user.dto.AdminUserResponse;
import com.hiresphere.user.entity.Role;
import com.hiresphere.user.entity.User;
import com.hiresphere.user.event.SessionsInvalidatedEvent;
import com.hiresphere.user.event.UserBlockedEvent;
import com.hiresphere.user.repository.UserRepository;
import com.hiresphere.user.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private ApplicationEventPublisher events;

    private UserService service;

    @BeforeEach
    void setUp() {
        service = new UserService(userRepository, events);
    }

    private User user(long id, Role role, boolean enabled) {
        User u = new User();
        u.setId(id);
        u.setFullName("User " + id);
        u.setEmail("user" + id + "@test.com");
        u.setRole(role);
        u.setEnabled(enabled);
        return u;
    }

    @Test
    void adminCannotBlockOwnAccount() {
        assertThatThrownBy(() -> service.block(1L, 1L, "x")).isInstanceOf(BadRequestException.class);

        verify(userRepository, never()).findById(any());
        verify(events, never()).publishEvent(any(Object.class));
    }

    @Test
    void blockingWithoutAReasonShouldFail() {
        assertThatThrownBy(() -> service.block(1L, 5L, "  ")).isInstanceOf(BadRequestException.class);

        verify(userRepository, never()).findById(any());
    }

    @Test
    void adminAccountsCannotBeBlockedThroughTheApi() {
        when(userRepository.findById(2L)).thenReturn(Optional.of(user(2L, Role.ADMIN, true)));

        assertThatThrownBy(() -> service.block(1L, 2L, "x"))
                .isInstanceOfSatisfying(AppException.class,
                        e -> assertThat(e.getStatus()).isEqualTo(HttpStatus.FORBIDDEN));
    }

    @Test
    void alreadyBlockedUserShouldGiveConflict() {
        when(userRepository.findById(5L)).thenReturn(Optional.of(user(5L, Role.CANDIDATE, false)));

        assertThatThrownBy(() -> service.block(1L, 5L, "x"))
                .isInstanceOfSatisfying(AppException.class,
                        e -> assertThat(e.getStatus()).isEqualTo(HttpStatus.CONFLICT));
        verify(events, never()).publishEvent(any(Object.class));
    }

    @Test
    void blockingShouldDisableBumpTokenVersionAndAnnounceBothEvents() {
        User target = user(5L, Role.CANDIDATE, true);
        when(userRepository.findById(5L)).thenReturn(Optional.of(target));

        AdminUserResponse response = service.block(1L, 5L, "  Spam applications ");

        assertThat(target.isEnabled()).isFalse();
        assertThat(target.getTokenVersion()).isEqualTo(1);
        assertThat(response.enabled()).isFalse();

        ArgumentCaptor<Object> published = ArgumentCaptor.forClass(Object.class);
        verify(events, times(2)).publishEvent(published.capture());
        assertThat(published.getAllValues()).contains(
                new SessionsInvalidatedEvent(5L),
                new UserBlockedEvent(5L, "Spam applications"));
    }

    @Test
    void unblockingShouldReEnableTheUser() {
        User target = user(5L, Role.CANDIDATE, false);
        when(userRepository.findById(5L)).thenReturn(Optional.of(target));

        AdminUserResponse response = service.unblock(5L);

        assertThat(target.isEnabled()).isTrue();
        assertThat(response.enabled()).isTrue();
    }

    @Test
    void unblockingAnActiveUserShouldGiveConflict() {
        when(userRepository.findById(5L)).thenReturn(Optional.of(user(5L, Role.CANDIDATE, true)));

        assertThatThrownBy(() -> service.unblock(5L))
                .isInstanceOfSatisfying(AppException.class,
                        e -> assertThat(e.getStatus()).isEqualTo(HttpStatus.CONFLICT));
    }

    @Test
    void unknownUserShouldGiveNotFound() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.block(1L, 99L, "x")).isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void activeCandidateNameShouldBeReturnedOnlyForEnabledCandidates() {
        when(userRepository.findById(5L)).thenReturn(Optional.of(user(5L, Role.CANDIDATE, true)));
        when(userRepository.findById(6L)).thenReturn(Optional.of(user(6L, Role.CANDIDATE, false)));
        when(userRepository.findById(7L)).thenReturn(Optional.of(user(7L, Role.RECRUITER, true)));
        when(userRepository.findById(8L)).thenReturn(Optional.empty());

        assertThat(service.findActiveCandidateName(5L)).contains("User 5");
        assertThat(service.findActiveCandidateName(6L)).isEmpty();
        assertThat(service.findActiveCandidateName(7L)).isEmpty();
        assertThat(service.findActiveCandidateName(8L)).isEmpty();
    }
}