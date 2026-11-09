package com.hiresphere.ai;

import java.util.function.Supplier;
import java.util.regex.Pattern;


public final class AiCallContext {

    public static final String UNSPECIFIED = "UNSPECIFIED";
    private static final Pattern FEATURE = Pattern.compile("[A-Z][A-Z0-9_]{1,39}");
    private static final ThreadLocal<Context> CURRENT = new ThreadLocal<>();

    public record Context(String feature, Long userId) {
    }

    private AiCallContext() {
    }

    public static Context current() {
        Context context = CURRENT.get();
        return context != null ? context : new Context(UNSPECIFIED, null);
    }

    public static <T> T with(String feature, Long userId, Supplier<T> action) {
        if (feature == null || !FEATURE.matcher(feature).matches()) {
            throw new IllegalArgumentException("feature must be UPPER_SNAKE_CASE and at most 40 characters");
        }
        Context previous = CURRENT.get();
        CURRENT.set(new Context(feature, userId));
        try {
            return action.get();
        } finally {
            if (previous == null) {
                CURRENT.remove();
            } else {
                CURRENT.set(previous);
            }
        }
    }
}