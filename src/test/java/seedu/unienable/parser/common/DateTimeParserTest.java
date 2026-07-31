package seedu.unienable.parser.common;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.LocalDate;
import java.time.LocalTime;

import org.junit.jupiter.api.Test;

import seedu.unienable.exception.InvalidDateTimeException;

class DateTimeParserTest {
    @Test
    public void parseDate_validDate_returnsLocalDate() throws InvalidDateTimeException {
        assertEquals(LocalDate.of(2026, 8, 15), DateTimeParser.parseDate("2026-08-15"));
    }

    @Test
    public void parseDate_invalidFormat_throwsInvalidDateTimeException() {
        InvalidDateTimeException exception = assertThrows(InvalidDateTimeException.class,
                () -> DateTimeParser.parseDate("15-08-2026"));
        assertEquals("date must be in yyyy-MM-dd format.", exception.getMessage());
    }

    @Test
    public void parseTime_validTime_returnsLocalTime() throws InvalidDateTimeException {
        assertEquals(LocalTime.of(9, 30), DateTimeParser.parseTime("09:30"));
    }

    @Test
    public void parseTime_invalidFormat_throwsInvalidDateTimeException() {
        InvalidDateTimeException exception = assertThrows(InvalidDateTimeException.class,
                () -> DateTimeParser.parseTime("9:30am"));
        assertEquals("time must be in 24-hour HH:mm format.", exception.getMessage());
    }

    @Test
    public void parseDate_nonExistentCalendarDate_throwsInvalidDateTimeException() {
        // Regression test: DateTimeFormatter's default SMART resolver style silently normalises
        // an out-of-range day-of-month instead of rejecting it, so "2026-02-30" previously became
        // 2026-02-28 rather than being rejected.
        assertThrows(InvalidDateTimeException.class, () -> DateTimeParser.parseDate("2026-02-30"));
    }

    @Test
    public void parseDate_dayThirtyOneInThirtyDayMonth_throwsInvalidDateTimeException() {
        assertThrows(InvalidDateTimeException.class, () -> DateTimeParser.parseDate("2026-04-31"));
    }

    @Test
    public void parseDate_leapDayInNonLeapYear_throwsInvalidDateTimeException() {
        assertThrows(InvalidDateTimeException.class, () -> DateTimeParser.parseDate("2026-02-29"));
    }

    @Test
    public void parseDate_leapDayInLeapYear_returnsLocalDate() throws InvalidDateTimeException {
        assertEquals(LocalDate.of(2028, 2, 29), DateTimeParser.parseDate("2028-02-29"));
    }

    @Test
    public void parseTime_twentyFourHundredHours_throwsInvalidDateTimeException() {
        // Regression test: the SMART resolver previously accepted "24:00" and silently normalised
        // it to 00:00 instead of rejecting it, since the documented HH:mm range is 00:00-23:59.
        assertThrows(InvalidDateTimeException.class, () -> DateTimeParser.parseTime("24:00"));
    }

    @Test
    public void parseTime_hourTwentyFive_throwsInvalidDateTimeException() {
        assertThrows(InvalidDateTimeException.class, () -> DateTimeParser.parseTime("25:00"));
    }

    @Test
    public void parseTime_minuteSixty_throwsInvalidDateTimeException() {
        assertThrows(InvalidDateTimeException.class, () -> DateTimeParser.parseTime("23:60"));
    }

    @Test
    public void parseTime_midnight_returnsLocalTime() throws InvalidDateTimeException {
        assertEquals(LocalTime.of(0, 0), DateTimeParser.parseTime("00:00"));
    }

    @Test
    public void parseTime_lastMinuteOfDay_returnsLocalTime() throws InvalidDateTimeException {
        assertEquals(LocalTime.of(23, 59), DateTimeParser.parseTime("23:59"));
    }
}
