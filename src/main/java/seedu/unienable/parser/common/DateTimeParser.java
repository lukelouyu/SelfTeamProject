package seedu.unienable.parser.common;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;

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

    /**
     * Parses a date string in yyyy-MM-dd format.
     *
     * @param date the date text to parse
     * @return the parsed date
     * @throws InvalidDateTimeException if date is not a valid yyyy-MM-dd date
     */
    public static LocalDate parseDate(String date) throws InvalidDateTimeException {
        try {
            return LocalDate.parse(date.trim(), DATE_FORMAT);
        } catch (DateTimeParseException e) {
            throw new InvalidDateTimeException("date must be in yyyy-MM-dd format.");
        }
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
}
