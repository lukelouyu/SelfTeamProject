package seedu.unienable.logic;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.LocalDate;
import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;

class RelativeDateResolverTest {
    @Test
    public void today_returnsNowsDate() {
        assertEquals(LocalDate.of(2026, 8, 19),
                RelativeDateResolver.today(LocalDateTime.of(2026, 8, 19, 23, 59)));
    }

    @Test
    public void tomorrow_returnsDayAfterNowsDate() {
        assertEquals(LocalDate.of(2026, 8, 20),
                RelativeDateResolver.tomorrow(LocalDateTime.of(2026, 8, 19, 23, 59)));
    }

    @Test
    public void tomorrow_monthRollover_returnsFirstOfNextMonth() {
        assertEquals(LocalDate.of(2026, 9, 1),
                RelativeDateResolver.tomorrow(LocalDateTime.of(2026, 8, 31, 12, 0)));
    }

    @Test
    public void tomorrow_yearRollover_returnsFirstOfNextYear() {
        assertEquals(LocalDate.of(2027, 1, 1),
                RelativeDateResolver.tomorrow(LocalDateTime.of(2026, 12, 31, 12, 0)));
    }

    @Test
    public void mondayOfThisWeek_onSunday_returnsPrecedingMonday() {
        // Sunday 2026-08-16 belongs to the week starting Monday 2026-08-10.
        assertEquals(LocalDate.of(2026, 8, 10),
                RelativeDateResolver.mondayOfThisWeek(LocalDateTime.of(2026, 8, 16, 0, 0)));
    }

    @Test
    public void mondayOfThisWeek_onMonday_returnsSameDate() {
        assertEquals(LocalDate.of(2026, 8, 10),
                RelativeDateResolver.mondayOfThisWeek(LocalDateTime.of(2026, 8, 10, 0, 0)));
    }

    @Test
    public void mondayOfNextWeek_isExactlySevenDaysAfterMondayOfThisWeek() {
        LocalDateTime now = LocalDateTime.of(2026, 8, 13, 15, 30);
        assertEquals(RelativeDateResolver.mondayOfThisWeek(now).plusDays(7),
                RelativeDateResolver.mondayOfNextWeek(now));
    }

    @Test
    public void mondayOfNextWeek_yearBoundary_rollsIntoNextYear() {
        // 2026-12-31 is a Thursday in the week starting Monday 2026-12-28; next week starts
        // 2027-01-04.
        assertEquals(LocalDate.of(2027, 1, 4),
                RelativeDateResolver.mondayOfNextWeek(LocalDateTime.of(2026, 12, 31, 9, 0)));
    }

    @Test
    public void mondayOfWeekContaining_matchesMondayOfThisWeekForSameDate() {
        LocalDate date = LocalDate.of(2026, 8, 14);
        assertEquals(RelativeDateResolver.mondayOfThisWeek(date.atStartOfDay()),
                RelativeDateResolver.mondayOfWeekContaining(date));
    }
}
