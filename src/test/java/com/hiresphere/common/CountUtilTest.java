package com.hiresphere.common;

import com.hiresphere.common.util.CountUtil;
import com.hiresphere.user.entity.Role;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class CountUtilTest {

    @Test
    void shouldMapRowsAndZeroFillMissingEnumValues() {
        List<Object[]> rows = new ArrayList<>();
        rows.add(new Object[]{Role.CANDIDATE, 5L});

        Map<Role, Long> counts = CountUtil.toEnumMap(Role.class, rows);

        assertThat(counts).hasSize(Role.values().length);
        assertThat(counts).containsEntry(Role.CANDIDATE, 5L)
                .containsEntry(Role.RECRUITER, 0L)
                .containsEntry(Role.ADMIN, 0L);
    }

    @Test
    void shouldAcceptIntegerCountsToo() {
        List<Object[]> rows = new ArrayList<>();
        rows.add(new Object[]{Role.ADMIN, 2});

        assertThat(CountUtil.toEnumMap(Role.class, rows)).containsEntry(Role.ADMIN, 2L);
    }
}