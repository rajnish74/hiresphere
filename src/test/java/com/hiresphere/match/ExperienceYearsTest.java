package com.hiresphere.match;

import com.hiresphere.match.ExperienceYears.Period;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ExperienceYearsTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 11, 12);

    private static LocalDate d(String iso) {
        return LocalDate.parse(iso);
    }

    private static Period p(String start, String end) {
        return new Period(d(start), end == null ? null : d(end));
    }

    private Integer years(Period... periods) {
        return ExperienceYears.totalYears(List.of(periods), TODAY);
    }

    @Test
    void aSinglePeriodShouldGiveItsLengthInYears() {
        assertThat(years(p("2019-01-01", "2023-01-01"))).isEqualTo(4);
    }

    @Test
    void shouldRoundToTheNearestYearNotDownAndNotUp() {
        assertThat(years(p("2020-01-01", "2022-04-01"))).isEqualTo(2);   // 2 saal 3 mahine
        assertThat(years(p("2020-01-01", "2022-09-01"))).isEqualTo(3);   // 2 saal 8 mahine
        assertThat(years(p("2024-01-01", "2024-03-01"))).isZero();       // 2 mahine
    }

    @Test
    void overlappingJobsShouldBeCountedOnlyOnce() {
        // 2 saal + 2 saal, beech mein 1 saal ka overlap: kul 3 saal, 4 nahi
        assertThat(years(p("2019-01-01", "2021-01-01"), p("2020-01-01", "2022-01-01"))).isEqualTo(3);
    }

    @Test
    void aPeriodInsideAnotherShouldAddNothing() {
        assertThat(years(p("2018-01-01", "2024-01-01"), p("2020-01-01", "2021-01-01"))).isEqualTo(6);
    }

    @Test
    void periodsThatJustTouchShouldAddUpWithoutDoubleCounting() {
        assertThat(years(p("2019-01-01", "2021-01-01"), p("2021-01-01", "2023-01-01"))).isEqualTo(4);
    }

    @Test
    void aGapBetweenJobsShouldNotCount() {
        assertThat(years(p("2018-01-01", "2019-01-01"), p("2020-01-01", "2021-01-01"))).isEqualTo(2);
    }

    @Test
    void theOrderOfTheEntriesShouldNotMatter() {
        assertThat(years(p("2020-01-01", "2022-01-01"), p("2018-01-01", "2020-06-01")))
                .isEqualTo(years(p("2018-01-01", "2020-06-01"), p("2020-01-01", "2022-01-01")));
    }

    @Test
    void aCurrentJobWithoutAnEndDateShouldRunUntilToday() {
        assertThat(years(p("2023-11-12", null))).isEqualTo(3);
    }

    @Test
    void anEndDateInTheFutureShouldBeCutAtToday() {
        assertThat(years(p("2025-11-12", "2030-01-01"))).isEqualTo(1);
    }

    @Test
    void entriesThatStartInTheFutureOrEndBeforeTheyStartShouldBeIgnored() {
        assertThat(years(p("2027-01-01", null), p("2022-01-01", "2021-01-01"), p("2020-01-01", "2021-01-01"))).isEqualTo(1);
    }

    @Test
    void noUsableEntriesShouldMeanUnknownNotZero() {
        assertThat(ExperienceYears.totalYears(List.of(), TODAY)).isNull();
        assertThat(ExperienceYears.totalYears(null, TODAY)).isNull();
        assertThat(ExperienceYears.totalYears(List.of(p("2030-01-01", null)), TODAY)).isNull();
        assertThat(ExperienceYears.totalYears(Arrays.asList(null, new Period(null, d("2020-01-01"))), TODAY)).isNull();
        assertThat(ExperienceYears.totalYears(List.of(p("2020-01-01", "2021-01-01")), null)).isNull();
    }

    @Test
    void usableEntriesShouldStillCountWhenOthersAreBroken() {
        assertThat(ExperienceYears.totalYears(Arrays.asList(null, p("2022-01-01", "2023-01-01")), TODAY)).isEqualTo(1);
    }
}