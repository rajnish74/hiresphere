package com.hiresphere.ai.json;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class JsonSchemasTest {

    @Test
    @SuppressWarnings("unchecked")
    void anObjectShouldListItsPropertiesAndRequireAllOfThem() {
        Map<String, Object> schema = JsonSchemas.object(Map.of(
                "skills", JsonSchemas.array(JsonSchemas.string("one skill"), 60),
                "level", JsonSchemas.stringEnum("seniority", "JUNIOR", "SENIOR"),
                "years", JsonSchemas.integer("years of experience")));

        assertThat(schema).containsEntry("type", "object");
        assertThat((List<String>) schema.get("required")).containsExactlyInAnyOrder("skills", "level", "years");
        Map<String, Object> properties = (Map<String, Object>) schema.get("properties");
        assertThat(properties).containsOnlyKeys("skills", "level", "years");

        Map<String, Object> skills = (Map<String, Object>) properties.get("skills");
        assertThat(skills).containsEntry("type", "array").containsEntry("maxItems", 60);
        assertThat((Map<String, Object>) skills.get("items")).containsEntry("type", "string");

        Map<String, Object> level = (Map<String, Object>) properties.get("level");
        assertThat((List<String>) level.get("enum")).containsExactly("JUNIOR", "SENIOR");
        assertThat((Map<String, Object>) properties.get("years")).containsEntry("type", "integer");
    }
}