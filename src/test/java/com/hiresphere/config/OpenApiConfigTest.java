package com.hiresphere.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class OpenApiConfigTest {

    private final OpenAPI api = new OpenApiConfig().hireSphereOpenApi();

    @Test
    void shouldDescribeTheApi() {
        assertThat(api.getInfo().getTitle()).isEqualTo("HireSphere API");
        assertThat(api.getInfo().getVersion()).isEqualTo("v1");
    }

    @Test
    void shouldDeclareABearerJwtSecurityScheme() {
        SecurityScheme scheme = api.getComponents().getSecuritySchemes().get("bearerAuth");

        assertThat(scheme.getType()).isEqualTo(SecurityScheme.Type.HTTP);
        assertThat(scheme.getScheme()).isEqualTo("bearer");
        assertThat(scheme.getBearerFormat()).isEqualTo("JWT");
        assertThat(api.getSecurity()).hasSize(1);
    }
}