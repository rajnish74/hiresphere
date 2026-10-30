package com.hiresphere.user;

import com.hiresphere.common.exception.AppException;
import com.hiresphere.common.exception.BadRequestException;
import com.hiresphere.common.exception.ResourceNotFoundException;
import com.hiresphere.user.dto.AdminUserResponse;
import com.hiresphere.user.entity.Role;
import com.hiresphere.user.entity.User;
import com.hiresphere.user.event.SessionsInvalidatedEvent;
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
        assertThatThrownBy(() -> service.block(1L, 1L)).isInstanceOf(BadRequestException.class);

        verify(userRepository, never()).findById(any());
    }

    @Test
    void adminAccountsCannotBeBlockedThroughTheApi() {
        when(userRepository.findById(2L)).thenReturn(Optional.of(user(2L, Role.ADMIN, true)));

        assertThatThrownBy(() -> service.block(1L, 2L))
                .isInstanceOfSatisfying(AppException.class,
                        e -> assertThat(e.getStatus()).isEqualTo(HttpStatus.FORBIDDEN));
    }

    @Test
    void alreadyBlockedUserShouldGiveConflict() {
        when(userRepository.findById(5L)).thenReturn(Optional.of(user(5L, Role.CANDIDATE, false)));

        assertThatThrownBy(() -> service.block(1L, 5L))
                .isInstanceOfSatisfying(AppException.class,
                        e -> assertThat(e.getStatus()).isEqualTo(HttpStatus.CONFLICT));
    }

    @Test
    void blockingShouldDisableTheUserBumpTokenVersionAndAnnounceIt() {
        User target = user(5L, Role.CANDIDATE, true);
        when(userRepository.findById(5L)).thenReturn(Optional.of(target));

        AdminUserResponse response = service.block(1L, 5L);

        assertThat(target.isEnabled()).isFalse();
        assertThat(target.getTokenVersion()).isEqualTo(1);
        assertThat(response.enabled()).isFalse();
        ArgumentCaptor<SessionsInvalidatedEvent> event = ArgumentCaptor.forClass(SessionsInvalidatedEvent.class);
        verify(events).publishEvent(event.capture());
        assertThat(event.getValue().userId()).isEqualTo(5L);
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

        assertThatThrownBy(() -> service.block(1L, 99L)).isInstanceOf(ResourceNotFoundException.class);
    }
}