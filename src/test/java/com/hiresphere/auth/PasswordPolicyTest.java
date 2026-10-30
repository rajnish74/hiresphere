package com.hiresphere.auth;

import com.hiresphere.auth.service.PasswordPolicy;
import com.hiresphere.common.exception.BadRequestException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PasswordPolicyTest {

    @Test
    void commonPasswordsShouldBeRejectedRegardlessOfCase() {
        assertThat(PasswordPolicy.violation("PASSWORD", "a@test.com")).isPresent();
        assertThat(PasswordPolicy.violation("Password1", "a@test.com")).isPresent();
        assertThat(PasswordPolicy.violation("12345678", "a@test.com")).isPresent();
    }

    @Test
    void repetitivePasswordsShouldBeRejected() {
        assertThat(PasswordPolicy.violation("aaaaaaaa", "a@test.com")).isPresent();
        assertThat(PasswordPolicy.violation("abababab", "a@test.com")).isPresent();
    }

    @Test
    void passwordContainingTheEmailNameShouldBeRejected() {
        assertThat(PasswordPolicy.violation("Asha@2026xyz", "asha@test.com")).isPresent();
        assertThat(PasswordPolicy.violation("MyASHAsecret9", "asha@test.com")).isPresent();
    }

    @Test
    void passwordContainingTheFullEmailShouldBeRejected() {
        assertThat(PasswordPolicy.violation("xasha@test.comx", "asha@test.com")).isPresent();
    }

    @Test
    void veryShortEmailNameShouldNotCauseFalsePositives() {
        // "tom" sirf 3 akshar ka hai, har doosre password mein ittefaq se aa sakta hai
        assertThat(PasswordPolicy.violation("Tom#Sky2026", "tom@test.com")).isEmpty();
    }

    @Test
    void strongUnrelatedPasswordShouldBeAccepted() {
        assertThat(PasswordPolicy.violation("Blue-Tiger-Runs-42", "asha@test.com")).isEmpty();
    }

    @Test
    void missingEmailShouldOnlySkipThePersonalInfoCheck() {
        assertThat(PasswordPolicy.violation("Blue-Tiger-Runs-42", null)).isEmpty();
        assertThat(PasswordPolicy.violation("password", null)).isPresent();
    }

    @Test
    void requireShouldThrowBadRequestForAWeakPassword() {
        assertThatThrownBy(() -> PasswordPolicy.require("password", "a@test.com"))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("too common");

        PasswordPolicy.require("Blue-Tiger-Runs-42", "a@test.com");   // strong ho to exception nahi
    }
}