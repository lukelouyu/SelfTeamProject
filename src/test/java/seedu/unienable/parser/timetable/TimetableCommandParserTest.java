package seedu.unienable.parser.timetable;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;

import seedu.unienable.exception.InvalidCommandException;
import seedu.unienable.exception.InvalidDateTimeException;
import seedu.unienable.exception.MissingInputException;
import seedu.unienable.logic.ActivityManager;

class TimetableCommandParserTest {
    private static final LocalDateTime NOW = LocalDateTime.of(2026, 8, 19, 10, 0);

    private final TimetableCommandParser parser = new TimetableCommandParser();
    private final ActivityManager activityManager = new ActivityManager();

    @Test
    public void parse_weekAnyDate_succeeds() {
        assertDoesNotThrow(() -> parser.parse(activityManager, NOW, "week/2026-08-19"));
    }

    @Test
    public void parse_weekCompact_succeeds() {
        assertDoesNotThrow(() -> parser.parse(activityManager, NOW, "week/2026-08-19 compact"));
    }

    @Test
    public void parse_weekDetail_succeeds() {
        assertDoesNotThrow(() -> parser.parse(activityManager, NOW, "week/2026-08-19 detail"));
    }

    @Test
    public void parse_thisWeekEveryMode_succeeds() {
        assertDoesNotThrow(() -> parser.parse(activityManager, NOW, "this week"));
        assertDoesNotThrow(() -> parser.parse(activityManager, NOW, "this week compact"));
        assertDoesNotThrow(() -> parser.parse(activityManager, NOW, "this week detail"));
    }

    @Test
    public void parse_dayEveryMode_succeeds() {
        assertDoesNotThrow(() -> parser.parse(activityManager, NOW, "day/2026-08-19"));
        assertDoesNotThrow(() -> parser.parse(activityManager, NOW, "day/2026-08-19 detail"));
    }

    @Test
    public void parse_caseInsensitiveKeywords_succeeds() {
        assertDoesNotThrow(() -> parser.parse(activityManager, NOW, "WEEK/2026-08-19 COMPACT"));
        assertDoesNotThrow(() -> parser.parse(activityManager, NOW, "This Week Detail"));
        assertDoesNotThrow(() -> parser.parse(activityManager, NOW, "DAY/2026-08-19 DETAIL"));
        assertDoesNotThrow(() -> parser.parse(activityManager, NOW, "TODAY DETAIL"));
        assertDoesNotThrow(() -> parser.parse(activityManager, NOW, "Tomorrow"));
        assertDoesNotThrow(() -> parser.parse(activityManager, NOW, "Next Week Detail"));
    }

    @Test
    public void parse_bareCommand_throwsMissingInputException() {
        assertThrows(MissingInputException.class, () -> parser.parse(activityManager, NOW, ""));
    }

    @Test
    public void parse_emptyMarkerValues_throwMissingInputException() {
        assertThrows(MissingInputException.class,
                () -> parser.parse(activityManager, NOW, "week/"));
        assertThrows(MissingInputException.class,
                () -> parser.parse(activityManager, NOW, "day/"));
    }

    @Test
    public void parse_invalidCalendarDate_throwsInvalidDateTimeException() {
        assertThrows(InvalidDateTimeException.class,
                () -> parser.parse(activityManager, NOW, "day/2026-02-30"));
    }

    @Test
    public void parse_wrongDateShape_throwsInvalidDateTimeException() {
        assertThrows(InvalidDateTimeException.class,
                () -> parser.parse(activityManager, NOW, "week/19-08-2026"));
    }

    @Test
    public void parse_unknownSelector_throwsInvalidCommandException() {
        assertThrows(InvalidCommandException.class,
                () -> parser.parse(activityManager, NOW, "yesterday"));
    }

    @Test
    public void parse_todayEveryMode_succeeds() {
        assertDoesNotThrow(() -> parser.parse(activityManager, NOW, "today"));
        assertDoesNotThrow(() -> parser.parse(activityManager, NOW, "today detail"));
    }

    @Test
    public void parse_tomorrowEveryMode_succeeds() {
        assertDoesNotThrow(() -> parser.parse(activityManager, NOW, "tomorrow"));
        assertDoesNotThrow(() -> parser.parse(activityManager, NOW, "tomorrow detail"));
    }

    @Test
    public void parse_today_resolvesToTodaysDateSameAsDayMarker() throws Exception {
        String viaMarker = parser.parse(activityManager, NOW, "day/2026-08-19").execute().getFeedback();
        String viaKeyword = parser.parse(activityManager, NOW, "today").execute().getFeedback();

        assertEquals(viaMarker, viaKeyword);
        assertTrue(viaKeyword.contains("Period: 2026-08-19"));
    }

    @Test
    public void parse_tomorrow_resolvesToTomorrowsDateSameAsDayMarker() throws Exception {
        String viaMarker = parser.parse(activityManager, NOW, "day/2026-08-20").execute().getFeedback();
        String viaKeyword = parser.parse(activityManager, NOW, "tomorrow").execute().getFeedback();

        assertEquals(viaMarker, viaKeyword);
        assertTrue(viaKeyword.contains("Period: 2026-08-20"));
    }

    @Test
    public void parse_nextWeekEveryMode_succeeds() {
        assertDoesNotThrow(() -> parser.parse(activityManager, NOW, "next week"));
        assertDoesNotThrow(() -> parser.parse(activityManager, NOW, "next week compact"));
        assertDoesNotThrow(() -> parser.parse(activityManager, NOW, "next week detail"));
    }

    @Test
    public void parse_nextWithoutWeek_throwsInvalidCommandException() {
        assertThrows(InvalidCommandException.class,
                () -> parser.parse(activityManager, NOW, "next"));
    }

    @Test
    public void parse_thisWithoutWeek_throwsInvalidCommandException() {
        assertThrows(InvalidCommandException.class,
                () -> parser.parse(activityManager, NOW, "this"));
    }

    @Test
    public void parse_todayCompact_throwsInvalidCommandException() {
        assertThrows(InvalidCommandException.class,
                () -> parser.parse(activityManager, NOW, "today compact"));
    }

    @Test
    public void parse_dayCompact_throwsInvalidCommandException() {
        assertThrows(InvalidCommandException.class,
                () -> parser.parse(activityManager, NOW, "day/2026-08-19 compact"));
    }

    @Test
    public void parse_unknownMode_throwsInvalidCommandException() {
        assertThrows(InvalidCommandException.class,
                () -> parser.parse(activityManager, NOW, "week/2026-08-19 wide"));
    }

    @Test
    public void parse_duplicateOrMixedModes_throwInvalidCommandException() {
        assertThrows(InvalidCommandException.class,
                () -> parser.parse(activityManager, NOW, "this week detail detail"));
        assertThrows(InvalidCommandException.class,
                () -> parser.parse(activityManager, NOW, "this week compact detail"));
    }

    @Test
    public void parse_rejectionsNeverTouchManager() {
        String[] rejected = { "", "week/", "day/", "yesterday", "this", "next", "today compact",
            "day/2026-08-19 compact", "week/2026-08-19 wide", "this week compact detail" };
        for (String input : rejected) {
            try {
                parser.parse(activityManager, NOW, input);
            } catch (Exception expected) {
                // Only the no-mutation guarantee is under test for this rejection table.
            }
        }
        assertEquals(0, activityManager.size());
    }
}
