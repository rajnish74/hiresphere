package com.hiresphere.config;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = CorsConfig.class)
class CorsYmlDefaultsTest {

    @Autowired
    private CorsProperties props;

    @Test
    void theDefaultDevConfigShouldAllowTheViteDevServer() {
        assertThat(props.allowedOrigins()).contains("http://localhost:5173");
    }
}