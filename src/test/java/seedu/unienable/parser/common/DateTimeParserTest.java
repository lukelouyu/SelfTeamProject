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
}
