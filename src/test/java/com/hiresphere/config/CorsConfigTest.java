package com.hiresphere.config;

import org.junit.jupiter.api.Test;
import org.springframework.web.cors.CorsConfiguration;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CorsConfigTest {

    private CorsConfiguration config(String... origins) {
        return CorsConfig.configuration(new CorsProperties(List.of(origins)));
    }

    @Test
    void listedOriginsShouldBeAllowedAndOthersRejected() {
        CorsConfiguration c = config("http://localhost:5173");

        assertThat(c.checkOrigin("http://localhost:5173")).isEqualTo("http://localhost:5173");
        assertThat(c.checkOrigin("http://evil.example.com")).isNull();
    }

    @Test
    void noConfiguredOriginsMeansNoCrossOriginAccess() {
        assertThat(config().checkOrigin("http://localhost:5173")).isNull();
    }

    @Test
    void wildcardOriginsMustBeRejected() {
        assertThatThrownBy(() -> new CorsProperties(List.of("*"))).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void blankEntriesShouldBeIgnored() {
        assertThat(new CorsProperties(List.of(" ", "http://localhost:5173 ")).allowedOrigins())
                .containsExactly("http://localhost:5173");
    }

    @Test
    void requestIdShouldBeReadableByTheBrowserAndCredentialsStayOff() {
        CorsConfiguration c = config("http://localhost:5173");

        assertThat(c.getExposedHeaders()).contains("X-Request-Id");
        assertThat(c.getAllowCredentials()).isFalse();
    }
}