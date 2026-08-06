package seedu.unienable.logic;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.TemporalAdjusters;

/**
 * Resolves the shared relative-date vocabulary ({@code today}, {@code tomorrow}, {@code this
 * week}, {@code next week}) against one injected current date/time, so every date-aware command
 * parser and service applies exactly the same today/tomorrow/week-boundary rules instead of each
 * recomputing its own copy of the same {@link TemporalAdjusters#previousOrSame} logic.
 *
 * <p>{@code this week} and {@code next week} are always the Monday-Sunday week containing (or
 * immediately following) the date {@link #today} resolves to - never a rolling seven-day window
 * measured from {@code now}'s time of day. This class only resolves dates; recognising the
 * keywords in a command's argument text, and deciding which selectors a given command accepts,
 * remains each parser's own responsibility, since that grammar (surrounding markers, combination
 * rules, trailing-text handling) genuinely differs per command.
 */
public final class RelativeDateResolver {
    private RelativeDateResolver() {
    }

    /** Returns the calendar date now resolves to. */
    public static LocalDate today(LocalDateTime now) {
        return now.toLocalDate();
    }

    /** Returns the calendar date immediately after the one now resolves to. */
    public static LocalDate tomorrow(LocalDateTime now) {
        return now.toLocalDate().plusDays(1);
    }

    /** Returns the Monday of the Monday-Sunday week containing date. */
    public static LocalDate mondayOfWeekContaining(LocalDate date) {
        return date.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
    }

    /** Returns the Monday of the Monday-Sunday week containing now's date. */
    public static LocalDate mondayOfThisWeek(LocalDateTime now) {
        return mondayOfWeekContaining(now.toLocalDate());
    }

    /** Returns the Monday of the Monday-Sunday week immediately following {@link #mondayOfThisWeek}. */
    public static LocalDate mondayOfNextWeek(LocalDateTime now) {
        return mondayOfThisWeek(now).plusDays(7);
    }
}
