package seedu.unienable.parser.dashboard;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;

import seedu.unienable.command.dashboard.DashboardCommand;
import seedu.unienable.exception.InvalidCommandException;
import seedu.unienable.exception.InvalidDateTimeException;
import seedu.unienable.exception.MissingInputException;
import seedu.unienable.logic.ActivityManager;

class DashboardCommandParserTest {
    private static final LocalDateTime NOW = LocalDateTime.of(2026, 8, 17, 10, 0);

    private final DashboardCommandParser parser = new DashboardCommandParser();
    private final ActivityManager activityManager = new ActivityManager();

    @Test
    public void parse_today_succeeds() {
        DashboardCommand command = assertDoesNotThrow(() -> parser.parse(activityManager, NOW, "today"));

        assertNotNull(command);
    }

    @Test
    public void parse_todayDetail_succeeds() {
        assertDoesNotThrow(() -> parser.parse(activityManager, NOW, "today detail"));
    }

    @Test
    public void parse_tomorrow_succeeds() {
        assertDoesNotThrow(() -> parser.parse(activityManager, NOW, "tomorrow"));
    }

    @Test
    public void parse_tomorrowDetail_succeeds() {
        assertDoesNotThrow(() -> parser.parse(activityManager, NOW, "tomorrow detail"));
    }

    @Test
    public void parse_dateWithValue_succeeds() {
        assertDoesNotThrow(() -> parser.parse(activityManager, NOW, "date/2026-08-20"));
    }

    @Test
    public void parse_dateWithValueDetail_succeeds() {
        assertDoesNotThrow(() -> parser.parse(activityManager, NOW, "date/2026-08-20 detail"));
    }

    @Test
    public void parse_dayWithValue_succeeds() {
        assertDoesNotThrow(() -> parser.parse(activityManager, NOW, "day/2026-08-20"));
    }

    @Test
    public void parse_dayWithValueDetail_succeeds() {
        assertDoesNotThrow(() -> parser.parse(activityManager, NOW, "day/2026-08-20 detail"));
    }

    @Test
    public void parse_emptyDayValue_throwsMissingInputException() {
        assertThrows(MissingInputException.class, () -> parser.parse(activityManager, NOW, "day/"));
    }

    @Test
    public void parse_thisWeek_succeeds() {
        assertDoesNotThrow(() -> parser.parse(activityManager, NOW, "this week"));
    }

    @Test
    public void parse_thisWeekDetail_succeeds() {
        assertDoesNotThrow(() -> parser.parse(activityManager, NOW, "this week detail"));
    }

    @Test
    public void parse_nextWeek_succeeds() {
        assertDoesNotThrow(() -> parser.parse(activityManager, NOW, "next week"));
    }

    @Test
    public void parse_nextWeekDetail_succeeds() {
        assertDoesNotThrow(() -> parser.parse(activityManager, NOW, "next week detail"));
    }

    @Test
    public void parse_nextWithoutWeek_throwsInvalidCommandException() {
        assertThrows(InvalidCommandException.class, () -> parser.parse(activityManager, NOW, "next"));
    }

    @Test
    public void parse_nextWeekExtraTrailingText_throwsInvalidCommandException() {
        assertThrows(InvalidCommandException.class,
                () -> parser.parse(activityManager, NOW, "next week extra"));
    }

    @Test
    public void parse_bareDashboard_throwsMissingInputException() {
        assertThrows(MissingInputException.class, () -> parser.parse(activityManager, NOW, ""));
    }

    @Test
    public void parse_emptyDateValue_throwsMissingInputException() {
        assertThrows(MissingInputException.class, () -> parser.parse(activityManager, NOW, "date/"));
    }

    @Test
    public void parse_calendarInvalidDate_throwsInvalidDateTimeException() {
        assertThrows(InvalidDateTimeException.class, () -> parser.parse(activityManager, NOW, "date/2026-02-30"));
    }

    @Test
    public void parse_bareDateNoMarker_throwsInvalidCommandException() {
        assertThrows(InvalidCommandException.class, () -> parser.parse(activityManager, NOW, "2026-08-15"));
    }

    @Test
    public void parse_unknownSelectorWeek_throwsInvalidCommandException() {
        assertThrows(InvalidCommandException.class, () -> parser.parse(activityManager, NOW, "week"));
    }

    @Test
    public void parse_unknownSelector_throwsInvalidCommandException() {
        assertThrows(InvalidCommandException.class, () -> parser.parse(activityManager, NOW, "unknown"));
    }

    @Test
    public void parse_thisWithoutWeek_throwsInvalidCommandException() {
        assertThrows(InvalidCommandException.class, () -> parser.parse(activityManager, NOW, "this"));
    }

    @Test
    public void parse_duplicateDetail_throwsInvalidCommandException() {
        assertThrows(InvalidCommandException.class, () -> parser.parse(activityManager, NOW, "today detail detail"));
    }

    @Test
    public void parse_tomorrowExtraTrailingText_throwsInvalidCommandException() {
        assertThrows(InvalidCommandException.class, () -> parser.parse(activityManager, NOW, "tomorrow extra"));
    }

    @Test
    public void parse_dateWithUnexpectedTrailingText_throwsInvalidCommandException() {
        assertThrows(InvalidCommandException.class,
                () -> parser.parse(activityManager, NOW, "date/2026-08-15 unexpected"));
    }

    @Test
    public void parse_caseInsensitiveSelectorAndDetailKeyword_succeeds() {
        assertDoesNotThrow(() -> parser.parse(activityManager, NOW, "TODAY DETAIL"));
        assertDoesNotThrow(() -> parser.parse(activityManager, NOW, "This Week Detail"));
        assertDoesNotThrow(() -> parser.parse(activityManager, NOW, "Next Week Detail"));
        assertDoesNotThrow(() -> parser.parse(activityManager, NOW, "DATE/2026-08-20"));
        assertDoesNotThrow(() -> parser.parse(activityManager, NOW, "DAY/2026-08-20"));
    }

    @Test
    public void parse_everyRejectCase_neverTouchesActivityManager() {
        String[] rejectedInputs = { "", "date/", "day/", "date/2026-02-30", "2026-08-15", "week", "unknown",
            "today detail detail", "tomorrow extra", "date/2026-08-15 unexpected", "next", "next week extra" };
        for (String input : rejectedInputs) {
            try {
                parser.parse(activityManager, NOW, input);
            } catch (Exception expected) {
                // Rejection is expected for every input in this list; only the manager-untouched
                // guarantee is under test here.
            }
        }

        assertEquals(0, activityManager.size());
    }
}
