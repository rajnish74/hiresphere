package com.hiresphere.auth;

import com.hiresphere.auth.service.JwtService;
import com.hiresphere.config.JwtProperties;
import com.hiresphere.user.entity.Role;
import com.hiresphere.user.entity.User;
import com.nimbusds.jose.jwk.source.ImmutableSecret;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.*;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

class JwtServiceTest {

    private static final String SECRET = "unit-test-secret-key-with-at-least-32-bytes!!";

    @Test
    void generatedTokenShouldContainSubjectAndRole() {
        SecretKey key = new SecretKeySpec(SECRET.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
        JwtEncoder encoder = new NimbusJwtEncoder(new ImmutableSecret<>(key));
        JwtDecoder decoder = NimbusJwtDecoder.withSecretKey(key).macAlgorithm(MacAlgorithm.HS256).build();
        JwtService service = new JwtService(encoder, new JwtProperties(SECRET, 15, "hiresphere"));

        User user = new User();
        user.setId(42L);
        user.setEmail("rec@hiresphere.com");
        user.setRole(Role.RECRUITER);

        Jwt jwt = decoder.decode(service.generateAccessToken(user));

        assertThat(jwt.getSubject()).isEqualTo("42");
        assertThat(jwt.<String>getClaim("role")).isEqualTo("RECRUITER");
        assertThat(jwt.getIssuer().toString()).isEqualTo("hiresphere");
    }
}