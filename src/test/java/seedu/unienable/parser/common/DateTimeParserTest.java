package seedu.unienable.parser.common;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.LocalDate;
import java.time.LocalDateTime;
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
    public void parseDate_nonExistentCalendarDate_throwsWithNonExistentDateMessage() {
        // Regression test: DateTimeFormatter's default SMART resolver style silently normalises
        // an out-of-range day-of-month instead of rejecting it, so "2026-02-30" previously became
        // 2026-02-28 rather than being rejected.
        //
        // Also regression coverage for the v1.0 RC retest bug report: "2026-02-30" matches the
        // yyyy-MM-dd shape exactly, so it must not be reported with the generic "must be in
        // yyyy-MM-dd format" message (that message is misleading here - the shape is correct, the
        // calendar date just doesn't exist) or be described as a past date.
        InvalidDateTimeException exception = assertThrows(InvalidDateTimeException.class,
                () -> DateTimeParser.parseDate("2026-02-30"));
        assertEquals("date does not exist. Please enter a valid calendar date in yyyy-MM-dd format.",
                exception.getMessage());
    }

    @Test
    public void parseDate_dayThirtyOneInThirtyDayMonth_throwsWithNonExistentDateMessage() {
        InvalidDateTimeException exception = assertThrows(InvalidDateTimeException.class,
                () -> DateTimeParser.parseDate("2026-04-31"));
        assertEquals("date does not exist. Please enter a valid calendar date in yyyy-MM-dd format.",
                exception.getMessage());
    }

    @Test
    public void parseDate_leapDayInNonLeapYear_throwsWithNonExistentDateMessage() {
        // 2027 is not a leap year, so February 29 does not exist that year.
        InvalidDateTimeException exception = assertThrows(InvalidDateTimeException.class,
                () -> DateTimeParser.parseDate("2027-02-29"));
        assertEquals("date does not exist. Please enter a valid calendar date in yyyy-MM-dd format.",
                exception.getMessage());
    }

    @Test
    public void parseDate_leapDayInLeapYear_returnsLocalDate() throws InvalidDateTimeException {
        assertEquals(LocalDate.of(2028, 2, 29), DateTimeParser.parseDate("2028-02-29"));
    }

    @Test
    public void parseDate_wrongSeparator_throwsWithFormatMessageNotExistenceMessage() {
        // Regression coverage for the v1.0 RC retest bug report: text that does not even match
        // the yyyy-MM-dd shape must keep the original "wrong format" message, distinct from the
        // "does not exist" message used once the shape matches.
        InvalidDateTimeException exception = assertThrows(InvalidDateTimeException.class,
                () -> DateTimeParser.parseDate("2026-08:15"));
        assertEquals("date must be in yyyy-MM-dd format.", exception.getMessage());
    }

    @Test
    public void parseNotBeforeDate_pastDate_throwsWithPastDateMessageIncludingToday() {
        LocalDate today = LocalDate.of(2026, 8, 1);

        InvalidDateTimeException exception = assertThrows(InvalidDateTimeException.class,
                () -> DateTimeParser.parseNotBeforeDate("2026-07-31", today));
        assertEquals("date has passed. Please enter a date from 2026-08-01 onwards.", exception.getMessage());
    }

    @Test
    public void parseNotBeforeDate_today_isAccepted() throws InvalidDateTimeException {
        LocalDate today = LocalDate.of(2026, 8, 1);

        assertEquals(today, DateTimeParser.parseNotBeforeDate("2026-08-01", today));
    }

    @Test
    public void parseNotBeforeDate_futureDate_isAccepted() throws InvalidDateTimeException {
        LocalDate today = LocalDate.of(2026, 8, 1);

        assertEquals(LocalDate.of(2026, 8, 2), DateTimeParser.parseNotBeforeDate("2026-08-02", today));
    }

    @Test
    public void parseNotBeforeDate_futureLeapDate_isAccepted() throws InvalidDateTimeException {
        LocalDate today = LocalDate.of(2026, 8, 1);

        assertEquals(LocalDate.of(2028, 2, 29), DateTimeParser.parseNotBeforeDate("2028-02-29", today));
    }

    @Test
    public void parseNotBeforeDate_nonExistentDate_reportsNonExistentNotPast() {
        // Existence must be validated before comparing against today, so a nonexistent date is
        // never misreported as merely being in the past.
        LocalDate today = LocalDate.of(2026, 8, 1);

        InvalidDateTimeException exception = assertThrows(InvalidDateTimeException.class,
                () -> DateTimeParser.parseNotBeforeDate("2026-02-30", today));
        assertEquals("date does not exist. Please enter a valid calendar date in yyyy-MM-dd format.",
                exception.getMessage());
    }

    @Test
    public void parseNotBeforeDate_wrongFormat_reportsFormatNotPast() {
        LocalDate today = LocalDate.of(2026, 8, 1);

        InvalidDateTimeException exception = assertThrows(InvalidDateTimeException.class,
                () -> DateTimeParser.parseNotBeforeDate("2026-08:15", today));
        assertEquals("date must be in yyyy-MM-dd format.", exception.getMessage());
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

    @Test
    public void requireNotPastIfToday_futureDate_ignoresTimeEntirely() throws InvalidDateTimeException {
        LocalDateTime now = LocalDateTime.of(2026, 8, 1, 16, 0);

        DateTimeParser.requireNotPastIfToday(LocalTime.of(9, 0), LocalDate.of(2026, 8, 2), now);
    }

    @Test
    public void requireNotPastIfToday_todayTimeAfterNow_isAccepted() throws InvalidDateTimeException {
        LocalDateTime now = LocalDateTime.of(2026, 8, 1, 16, 0);

        DateTimeParser.requireNotPastIfToday(LocalTime.of(16, 1), LocalDate.of(2026, 8, 1), now);
    }

    @Test
    public void requireNotPastIfToday_todayTimeExactlyNow_throwsWithStartTimeMessage() {
        LocalDateTime now = LocalDateTime.of(2026, 8, 1, 16, 0);

        InvalidDateTimeException exception = assertThrows(InvalidDateTimeException.class,
                () -> DateTimeParser.requireNotPastIfToday(LocalTime.of(16, 0), LocalDate.of(2026, 8, 1), now));
        assertEquals("activity start time has passed. Please enter a start time after 16:00.",
                exception.getMessage());
    }

    @Test
    public void requireNotPastIfToday_todayTimeBeforeNow_throwsWithStartTimeMessage() {
        LocalDateTime now = LocalDateTime.of(2026, 8, 1, 16, 0);

        InvalidDateTimeException exception = assertThrows(InvalidDateTimeException.class,
                () -> DateTimeParser.requireNotPastIfToday(LocalTime.of(15, 59), LocalDate.of(2026, 8, 1), now));
        assertEquals("activity start time has passed. Please enter a start time after 16:00.",
                exception.getMessage());
    }

    @Test
    public void parseNotBeforeNow_validTimeAfterNow_returnsLocalTime() throws InvalidDateTimeException {
        LocalDateTime now = LocalDateTime.of(2026, 8, 1, 16, 0);

        assertEquals(LocalTime.of(16, 1),
                DateTimeParser.parseNotBeforeNow("16:01", LocalDate.of(2026, 8, 1), now));
    }

    @Test
    public void parseNotBeforeNow_malformedTime_reportsFormatNotPast() {
        LocalDateTime now = LocalDateTime.of(2026, 8, 1, 16, 0);

        InvalidDateTimeException exception = assertThrows(InvalidDateTimeException.class,
                () -> DateTimeParser.parseNotBeforeNow("4:30pm", LocalDate.of(2026, 8, 1), now));
        assertEquals("time must be in 24-hour HH:mm format.", exception.getMessage());
    }
}
