package seedu.unienable.parser.common;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;
import java.util.regex.Pattern;

import seedu.unienable.exception.InvalidDateTimeException;

/**
 * Parses date and time text in the formats documented in the User Guide (yyyy-MM-dd, HH:mm).
 *
 * <p>Both formatters use {@link ResolverStyle#STRICT} (the default {@code DateTimeFormatter}
 * resolver style, SMART, silently normalises out-of-range values instead of rejecting them, e.g.
 * "2026-02-30" would become 2026-02-28 and "24:00" would become 00:00). The date pattern uses
 * "uuuu" (proleptic year) rather than "yyyy" (year-of-era), since strict resolving requires a
 * year field that unambiguously identifies the year without an era.
 */
public class DateTimeParser {
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("uuuu-MM-dd")
            .withResolverStyle(ResolverStyle.STRICT);
    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm")
            .withResolverStyle(ResolverStyle.STRICT);
    private static final Pattern DATE_SHAPE = Pattern.compile("\\d{4}-\\d{2}-\\d{2}");

    /**
     * Parses a date string in yyyy-MM-dd format, distinguishing text that does not even match the
     * expected shape (e.g. "2026-08:15") from text that matches the shape but names a calendar
     * date that does not exist (e.g. "2026-02-30", "2027-02-29"). Both are strictly rejected -
     * see the class Javadoc - but with different, specific messages rather than one generic
     * "wrong format" message covering both.
     *
     * @param date the date text to parse
     * @return the parsed date
     * @throws InvalidDateTimeException if date does not match yyyy-MM-dd shape, or matches the
     *     shape but names a calendar date that does not exist
     */
    public static LocalDate parseDate(String date) throws InvalidDateTimeException {
        String trimmed = date.trim();
        if (!DATE_SHAPE.matcher(trimmed).matches()) {
            throw new InvalidDateTimeException("date must be in yyyy-MM-dd format.");
        }
        try {
            return LocalDate.parse(trimmed, DATE_FORMAT);
        } catch (DateTimeParseException e) {
            throw new InvalidDateTimeException(
                    "date does not exist. Please enter a valid calendar date in yyyy-MM-dd format.");
        }
    }

    /**
     * Parses a date string exactly like {@link #parseDate}, additionally rejecting a
     * well-formed, calendar-valid date that is earlier than today. Used for activity date fields
     * a user is actively supplying (add, edit) - not for read-only filters (list/find date/) or
     * for restoring previously-saved activities from storage, both of which must continue to
     * accept a genuinely past date.
     *
     * @param date the date text to parse
     * @param today the current local date, threaded down from the application's single
     *     {@code now} seam rather than read via a new {@code LocalDate.now()} call
     * @return the parsed date
     * @throws InvalidDateTimeException if date is malformed, does not exist, or is before today
     */
    public static LocalDate parseNotBeforeDate(String date, LocalDate today) throws InvalidDateTimeException {
        LocalDate parsed = parseDate(date);
        if (parsed.isBefore(today)) {
            throw new InvalidDateTimeException("date has passed. Please enter a date from " + today
                    + " onwards.");
        }
        return parsed;
    }

    /**
     * Parses a time string in 24-hour HH:mm format.
     *
     * @param time the time text to parse
     * @return the parsed time
     * @throws InvalidDateTimeException if time is not a valid HH:mm time
     */
    public static LocalTime parseTime(String time) throws InvalidDateTimeException {
        try {
            return LocalTime.parse(time.trim(), TIME_FORMAT);
        } catch (DateTimeParseException e) {
            throw new InvalidDateTimeException("time must be in 24-hour HH:mm format.");
        }
    }

    /**
     * Rejects an activity start time that has already passed or is exactly now, when date is
     * today - a well-formed date/time combination that {@link #parseNotBeforeDate} alone cannot
     * catch, since it only looks at the date. Used for an activity's start time (add's
     * from/earliest, or edit's date/from/earliest when actively supplied) - not for read-only
     * filters (list/find date/) or for previously-saved activities being left alone, both of
     * which must continue to accept a genuinely past date/time.
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

    /**
     * Parses a time string exactly like {@link #parseTime}, additionally applying
     * {@link #requireNotPastIfToday} to the result.
     *
     * @param time the time text to parse
     * @param date the activity's resolved date
     * @param now the current date and time
     * @return the parsed time
     * @throws InvalidDateTimeException if time is malformed, or date is today and time is not
     *     strictly after now's time-of-day
     */
    public static LocalTime parseNotBeforeNow(String time, LocalDate date, LocalDateTime now)
            throws InvalidDateTimeException {
        LocalTime parsed = parseTime(time);
        requireNotPastIfToday(parsed, date, now);
        return parsed;
    }
}
