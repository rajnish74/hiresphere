package com.hiresphere.config;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.env.Environment;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("prod")
@TestPropertySource(properties = "app.jwt.secret=prod-test-secret-0123456789-abcdefghijklmnop")
class ProdProfileContextTest {

    @Autowired
    private Environment env;

    @Test
    void swaggerShouldBeOffByDefaultInProd() {
        assertThat(env.getProperty("springdoc.swagger-ui.enabled", Boolean.class)).isFalse();
        assertThat(env.getProperty("springdoc.api-docs.enabled", Boolean.class)).isFalse();
    }

    @Test
    void shouldShutDownGracefully() {
        assertThat(env.getProperty("server.shutdown")).isEqualTo("graceful");
    }

    @Test
    void healthShouldNotDependOnTheMailServer() {
        assertThat(env.getProperty("management.health.mail.enabled", Boolean.class)).isFalse();
    }

    @Test
    void shouldTrustTheProxyForTheClientIp() {
        assertThat(env.getProperty("server.forward-headers-strategy")).isEqualTo("native");
    }
}