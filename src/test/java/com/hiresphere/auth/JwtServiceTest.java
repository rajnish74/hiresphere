package com.hiresphere.auth;

import com.hiresphere.auth.service.JwtService;
import com.hiresphere.config.JwtProperties;
import com.hiresphere.user.entity.Role;
import com.hiresphere.user.entity.User;
import com.nimbusds.jose.jwk.source.ImmutableSecret;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.*;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtServiceTest {

    private static final String SECRET = "unit-test-secret-key-with-at-least-32-bytes!!";

    private JwtEncoder encoder;
    private JwtDecoder decoder;
    private JwtService service;

    @BeforeEach
    void setUp() {
        SecretKey key = new SecretKeySpec(SECRET.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
        encoder = new NimbusJwtEncoder(new ImmutableSecret<>(key));
        decoder = NimbusJwtDecoder.withSecretKey(key).macAlgorithm(MacAlgorithm.HS256).build();
        service = new JwtService(encoder, new JwtProperties(SECRET, 15, 7, "hiresphere"));
    }

    @Test
    void generatedTokenShouldContainSubjectAndRole() {
        User user = new User();
        user.setId(42L);
        user.setEmail("rec@hiresphere.com");
        user.setRole(Role.RECRUITER);

        Jwt jwt = decoder.decode(service.generateAccessToken(user));

        assertThat(jwt.getSubject()).isEqualTo("42");
        assertThat(jwt.<String>getClaim("role")).isEqualTo("RECRUITER");
        assertThat(jwt.getClaimAsString("iss")).isEqualTo("hiresphere");
    }

    @Test
    void expiredTokenShouldBeRejected() {
        Instant now = Instant.now();
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer("hiresphere")
                .subject("1")
                .issuedAt(now.minusSeconds(600))
                .expiresAt(now.minusSeconds(300))
                .build();
        String token = encoder.encode(
                JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(), claims)
        ).getTokenValue();

        assertThatThrownBy(() -> decoder.decode(token)).isInstanceOf(JwtException.class);
    }
}