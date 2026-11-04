package com.hiresphere.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.media.IntegerSchema;
import io.swagger.v3.oas.models.media.ObjectSchema;
import io.swagger.v3.oas.models.media.StringSchema;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class OpenApiTypesConfigTest {

    @Test
    void responseSchemasShouldMarkEveryPropertyRequiredAndRequestsShouldBeLeftAlone() {
        OpenAPI api = new OpenAPI().components(new Components()
                .addSchemas("UserResponse", new ObjectSchema()
                        .addProperty("id", new IntegerSchema())
                        .addProperty("phone", new StringSchema()))
                .addSchemas("LoginRequest", new ObjectSchema().addProperty("email", new StringSchema())));

        new OpenApiTypesConfig().responsesAreFullyPopulated().customise(api);

        assertThat(api.getComponents().getSchemas().get("UserResponse").getRequired())
                .containsExactlyInAnyOrder("id", "phone");
        assertThat(api.getComponents().getSchemas().get("LoginRequest").getRequired()).isNull();
    }

    @Test
    void anOpenApiDocumentWithoutSchemasShouldNotFail() {
        new OpenApiTypesConfig().responsesAreFullyPopulated().customise(new OpenAPI());
    }
}