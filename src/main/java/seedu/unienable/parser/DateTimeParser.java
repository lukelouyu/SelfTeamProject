package seedu.unienable.parser;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

import seedu.unienable.exception.InvalidDateTimeException;

/** Parses date and time text in the formats documented in the User Guide (yyyy-MM-dd, HH:mm). */
public class DateTimeParser {
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm");

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
