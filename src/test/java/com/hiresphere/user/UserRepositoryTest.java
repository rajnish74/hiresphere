package com.hiresphere.user;

import com.hiresphere.user.entity.Role;
import com.hiresphere.user.entity.User;
import com.hiresphere.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
class UserRepositoryTest {

    @Autowired
    private UserRepository userRepository;

    @Test
    void shouldSaveUserAndFindByEmail() {
        User user = new User();
        user.setFullName("Test User");
        user.setEmail("test@hiresphere.com");
        user.setPasswordHash("dummy-hash");
        user.setRole(Role.CANDIDATE);

        userRepository.saveAndFlush(user);

        var found = userRepository.findByEmail("test@hiresphere.com");

        assertThat(found).isPresent();
        assertThat(found.get().getCreatedAt()).isNotNull();
        assertThat(found.get().getVersion()).isNotNull();
        assertThat(found.get().isDeleted()).isFalse();
    }
}