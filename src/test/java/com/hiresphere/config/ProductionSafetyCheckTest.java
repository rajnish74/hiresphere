package com.hiresphere.config;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ProductionSafetyCheckTest {

    @Test
    void theDevelopmentSecretMustBeRejected() {
        assertThatThrownBy(() -> ProductionSafetyCheck.validateJwtSecret(ProductionSafetyCheck.DEV_SECRET))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("development JWT secret");
    }

    @Test
    void aShortSecretMustBeRejected() {
        assertThatThrownBy(() -> ProductionSafetyCheck.validateJwtSecret("too-short"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("at least 32 bytes");
    }

    @Test
    void aMissingSecretMustBeRejected() {
        assertThatThrownBy(() -> ProductionSafetyCheck.validateJwtSecret(null))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void aLongRandomSecretShouldBeAccepted() {
        assertThatCode(() -> ProductionSafetyCheck.validateJwtSecret("q8Zk2mN5vR7xT1bY4cW9eH3jL6pS0aD8fG2hK5nM7rU="))
                .doesNotThrowAnyException();
    }
}