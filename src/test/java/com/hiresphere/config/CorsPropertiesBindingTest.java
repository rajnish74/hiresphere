package com.hiresphere.config;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

class CorsPropertiesBindingTest {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withUserConfiguration(CorsConfig.class);

    @Test
    void aCommaSeparatedValueShouldBecomeAList() {
        runner.withPropertyValues("app.cors.allowed-origins=http://localhost:5173, http://localhost:3000")
                .run(ctx -> assertThat(ctx.getBean(CorsProperties.class).allowedOrigins())
                        .containsExactly("http://localhost:5173", "http://localhost:3000"));
    }

    @Test
    void anIndexedListShouldBindToo() {
        runner.withPropertyValues("app.cors.allowed-origins[0]=http://a.example.com",
                        "app.cors.allowed-origins[1]=http://b.example.com")
                .run(ctx -> assertThat(ctx.getBean(CorsProperties.class).allowedOrigins())
                        .containsExactly("http://a.example.com", "http://b.example.com"));
    }

    @Test
    void aMissingPropertyShouldMeanNoOriginsNotAFailure() {
        runner.run(ctx -> assertThat(ctx.getBean(CorsProperties.class).allowedOrigins()).isEmpty());
    }
}