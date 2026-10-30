package com.hiresphere.common.util;

import java.util.Locale;

public final class SearchUtil {

    private SearchUtil() {
    }

    public static String likePattern(String input) {
        String escaped = input.trim().toLowerCase(Locale.ROOT)
                .replace("\\", "\\\\")
                .replace("%", "\\%")
                .replace("_", "\\_");
        return "%" + escaped + "%";
    }
}