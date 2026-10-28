package com.hiresphere.auth;

import com.hiresphere.auth.service.LoginAttemptService;
import com.hiresphere.common.exception.AppException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class LoginAttemptServiceTest {

    @Test
    void shouldBlockAfterFiveFailures() {
        LoginAttemptService service = new LoginAttemptService();
        String key = "1.1.1.1|user@test.com";

        for (int i = 0; i < 5; i++) {
            service.recordFailure(key);
        }

        assertThatThrownBy(() -> service.checkNotBlocked(key)).isInstanceOf(AppException.class);
    }

    @Test
    void shouldAllowAfterReset() {
        LoginAttemptService service = new LoginAttemptService();
        String key = "1.1.1.1|user@test.com";
        for (int i = 0; i < 5; i++) {
            service.recordFailure(key);
        }

        service.reset(key);

        assertThatCode(() -> service.checkNotBlocked(key)).doesNotThrowAnyException();
    }
}