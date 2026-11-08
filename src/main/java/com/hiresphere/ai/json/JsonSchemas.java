package com.hiresphere.ai.json;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class JsonSchemas {

    private JsonSchemas() {
    }

    public static Map<String, Object> string(String description) {
        Map<String, Object> schema = new LinkedHashMap<>();
        schema.put("type", "string");
        schema.put("description", description);
        return schema;
    }

    public static Map<String, Object> stringEnum(String description, String... values) {
        Map<String, Object> schema = string(description);
        schema.put("enum", List.of(values));
        return schema;
    }

    public static Map<String, Object> integer(String description) {
        Map<String, Object> schema = new LinkedHashMap<>();
        schema.put("type", "integer");
        schema.put("description", description);
        return schema;
    }

    public static Map<String, Object> array(Map<String, Object> items, int maxItems) {
        Map<String, Object> schema = new LinkedHashMap<>();
        schema.put("type", "array");
        schema.put("items", items);
        schema.put("maxItems", maxItems);
        return schema;
    }

    public static Map<String, Object> object(Map<String, Map<String, Object>> properties) {
        Map<String, Object> schema = new LinkedHashMap<>();
        schema.put("type", "object");
        schema.put("properties", new LinkedHashMap<>(properties));
        schema.put("required", new ArrayList<>(properties.keySet()));
        return schema;
    }
}