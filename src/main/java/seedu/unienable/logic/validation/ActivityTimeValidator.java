package seedu.unienable.logic.validation;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

import seedu.unienable.exception.InvalidDateTimeException;

/**
 * Neutral business-rule validation for activity date/time values, shared by both {@code parser}
 * (which needs it while accepting new user input) and {@code command} (which needs to re-check the
 * same rule immediately before execution, e.g. after a confirmation prompt). Living outside both
 * packages avoids the alternative of one depending on the other purely to reach this rule.
 */
public final class ActivityTimeValidator {
    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm");

    private ActivityTimeValidator() {
    }

    /**
     * Rejects an activity start time that has already passed or is exactly now, when date is
     * today. Used for an activity's start time (add's from/earliest, or edit's
     * date/from/earliest when actively supplied, both at parse time and again immediately before
     * execution) - not for read-only filters (list/find date/) or for previously-saved activities
     * being left alone, both of which must continue to accept a genuinely past date/time.
     *
     * @param time the already-resolved start time to check
     * @param date the activity's resolved date
     * @param now the current date and time, threaded down from the application's single
     *     {@code now} seam rather than read via a new {@code LocalDateTime.now()} call
     * @throws InvalidDateTimeException if date is today and time is not strictly after now's
     *     time-of-day
     */
    public static void requireNotPastIfToday(LocalTime time, LocalDate date, LocalDateTime now)
            throws InvalidDateTimeException {
        if (date.equals(now.toLocalDate()) && !time.isAfter(now.toLocalTime())) {
            throw new InvalidDateTimeException("activity start time has passed. Please enter a start time after "
                    + TIME_FORMAT.format(now.toLocalTime()) + ".");
        }
    }
}
