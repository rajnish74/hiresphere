package com.hiresphere.ai.prompt;

import org.springframework.core.io.ClassPathResource;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class PromptTemplate {

    private static final Pattern PLACEHOLDER = Pattern.compile("\\{\\{([A-Za-z0-9_]+)}}");
    private static final Map<String, PromptTemplate> CACHE = new ConcurrentHashMap<>();

    private final String name;
    private final String text;
    private final Set<String> placeholders;

    private PromptTemplate(String name, String text) {
        this.name = name;
        this.text = text;
        Set<String> found = new LinkedHashSet<>();
        Matcher m = PLACEHOLDER.matcher(text);
        while (m.find()) {
            found.add(m.group(1));
        }
        this.placeholders = Set.copyOf(found);
    }

    public static PromptTemplate of(String name, String text) {
        return new PromptTemplate(name, text);
    }

    /** `prompts/<name>.txt` classpath se. Naam mein path ka hissa nahi ho sakta. */
    public static PromptTemplate load(String name) {
        if (name == null || !name.matches("[a-z0-9.-]+") || name.contains("..")) {
            throw new IllegalArgumentException("Invalid prompt template name: " + name);
        }
        return CACHE.computeIfAbsent(name, PromptTemplate::read);
    }

    private static PromptTemplate read(String name) {
        ClassPathResource resource = new ClassPathResource("prompts/" + name + ".txt");
        if (!resource.exists()) {
            throw new IllegalArgumentException("Unknown prompt template: " + name);
        }
        try (InputStream in = resource.getInputStream()) {
            return new PromptTemplate(name, new String(in.readAllBytes(), StandardCharsets.UTF_8));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public Set<String> placeholders() {
        return placeholders;
    }

    public String render(Map<String, String> values) {
        for (String needed : placeholders) {
            if (!values.containsKey(needed)) {
                throw new IllegalArgumentException("Prompt '" + name + "' needs a value for: " + needed);
            }
        }
        for (Map.Entry<String, String> entry : values.entrySet()) {
            if (!placeholders.contains(entry.getKey())) {
                throw new IllegalArgumentException("Prompt '" + name + "' has no placeholder: " + entry.getKey());
            }
            if (entry.getValue() == null) {
                throw new IllegalArgumentException("Value for '" + entry.getKey() + "' is null");
            }
        }
        Matcher m = PLACEHOLDER.matcher(text);
        StringBuilder out = new StringBuilder();
        while (m.find()) {
            m.appendReplacement(out, Matcher.quoteReplacement(values.get(m.group(1))));
        }
        m.appendTail(out);
        return out.toString().strip();
    }
}