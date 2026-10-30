package com.hiresphere.common.util;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

public final class CountUtil {

    private CountUtil() {
    }

    public static <E extends Enum<E>> Map<E, Long> toEnumMap(Class<E> type, List<Object[]> rows) {
        Map<E, Long> map = new EnumMap<>(type);
        for (E constant : type.getEnumConstants()) {
            map.put(constant, 0L);
        }
        for (Object[] row : rows) {
            map.put(type.cast(row[0]), ((Number) row[1]).longValue());
        }
        return map;
    }
}