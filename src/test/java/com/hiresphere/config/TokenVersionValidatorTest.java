package com.hiresphere.config;

import com.hiresphere.user.service.TokenVersionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.oauth2.jwt.Jwt;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TokenVersionValidatorTest {

    @Mock
    private TokenVersionService tokenVersionService;

    private TokenVersionValidator validator;

    @BeforeEach
    void setUp() {
        validator = new TokenVersionValidator(tokenVersionService);
    }

    private Jwt jwt(String subject, Object version) {
        Jwt.Builder builder = Jwt.withTokenValue("token").header("alg", "HS256").subject(subject);
        if (version != null) {
            builder.claim("ver", version);
        }
        return builder.build();
    }

    @Test
    void currentVersionShouldPass() {
        when(tokenVersionService.isCurrent(7L, 3L)).thenReturn(true);

        assertThat(validator.validate(jwt("7", 3L)).hasErrors()).isFalse();
    }

    @Test
    void staleVersionShouldFail() {
        when(tokenVersionService.isCurrent(7L, 2L)).thenReturn(false);

        assertThat(validator.validate(jwt("7", 2L)).hasErrors()).isTrue();
    }

    @Test
    void tokenWithoutVersionClaimShouldBeTreatedAsVersionZero() {
        when(tokenVersionService.isCurrent(7L, 0L)).thenReturn(true);

        assertThat(validator.validate(jwt("7", null)).hasErrors()).isFalse();
    }

    @Test
    void nonNumericSubjectShouldFailWithoutTouchingTheDatabase() {
        assertThat(validator.validate(jwt("abc", 1L)).hasErrors()).isTrue();

        verifyNoInteractions(tokenVersionService);
    }
}