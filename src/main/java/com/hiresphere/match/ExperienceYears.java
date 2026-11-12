package com.hiresphere.match;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;

public final class ExperienceYears {

    public record Period(LocalDate start, LocalDate end) {
    }

    private static final double DAYS_PER_YEAR = 365.25;

    private ExperienceYears() {
    }


    public static Integer totalYears(Collection<Period> periods, LocalDate today) {
        if (periods == null || today == null) {
            return null;
        }
        List<long[]> ranges = new ArrayList<>();
        for (Period p : periods) {
            if (p == null || p.start() == null || p.start().isAfter(today)) {
                continue;
            }
            LocalDate end = p.end() == null || p.end().isAfter(today) ? today : p.end();
            if (end.isBefore(p.start())) {
                continue;
            }
            ranges.add(new long[]{p.start().toEpochDay(), end.toEpochDay()});
        }
        if (ranges.isEmpty()) {
            return null;
        }

        ranges.sort(Comparator.comparingLong(r -> r[0]));
        long total = 0;
        long currentStart = ranges.get(0)[0];
        long currentEnd = ranges.get(0)[1];
        for (int i = 1; i < ranges.size(); i++) {
            long[] next = ranges.get(i);
            if (next[0] <= currentEnd) {
                currentEnd = Math.max(currentEnd, next[1]);
            } else {
                total += currentEnd - currentStart;
                currentStart = next[0];
                currentEnd = next[1];
            }
        }
        total += currentEnd - currentStart;
        return (int) Math.round(total / DAYS_PER_YEAR);
    }
}