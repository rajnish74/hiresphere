package com.hiresphere.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    static final String BEARER = "bearerAuth";

    @Bean
    public OpenAPI hireSphereOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("HireSphere API")
                        .version("v1")
                        .description("""
                                Hiring platform API: candidates, recruiters, admin.

                                To try protected endpoints: call POST /api/v1/auth/login, copy the accessToken, \
                                click Authorize and paste it (without the word Bearer). Access tokens last 15 minutes."""))
                .components(new Components().addSecuritySchemes(BEARER, new SecurityScheme()
                        .type(SecurityScheme.Type.HTTP)
                        .scheme("bearer")
                        .bearerFormat("JWT")))
                .addSecurityItem(new SecurityRequirement().addList(BEARER));
    }
}