package seedu.unienable.ui.timetable;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import org.junit.jupiter.api.Test;

import seedu.unienable.model.enums.ActivityCategory;
import seedu.unienable.model.timetable.TimetableEntry;
import seedu.unienable.model.timetable.TimetableEntryType;
import seedu.unienable.model.timetable.TimetableMode;
import seedu.unienable.model.timetable.TimetablePeriod;
import seedu.unienable.model.timetable.TimetableView;

class TimetableFormatterTest {
    private static final LocalDate MONDAY = LocalDate.of(2026, 8, 17);

    private static TimetableEntry fixed(int id, LocalDate date, int startHour, int endHour,
            boolean overlapping) {
        return new TimetableEntry(id, "Lecture " + id, date, LocalTime.of(startHour, 0),
                LocalTime.of(endHour, 0), 0, TimetableEntryType.FIXED, overlapping, false,
                ActivityCategory.ACADEMIC, 2, 3, "CS2113", "Bring laptop");
    }

    private static TimetableEntry flexible(int id, LocalDate date) {
        return new TimetableEntry(id, "Study", date, LocalTime.of(13, 0), LocalTime.of(17, 0),
                90, TimetableEntryType.UNSCHEDULED_FLEXIBLE, false, true,
                ActivityCategory.OTHERS, 3, 4, null, null);
    }

    @Test
    public void format_dayDefault_exactText() {
        TimetablePeriod period = new TimetablePeriod(MONDAY.toString(), MONDAY, MONDAY, false);
        TimetableView view = new TimetableView(period, List.of(fixed(1, MONDAY, 9, 11, false)),
                List.of(flexible(2, MONDAY)));

        String result = TimetableFormatter.format(view, TimetableMode.NORMAL);

        assertEquals("Daily Timetable\n"
                + "Period: 2026-08-17\n\n"
                + "MONDAY | 2026-08-17\n"
                + "  09:00-11:00  [F][1] Lecture 1\n\n"
                + "UNSCHEDULED FLEXIBLE ACTIVITIES\n"
                + "  2026-08-17 13:00-17:00  [U][2] Study (90 min required)\n\n"
                + "[F] Fixed activity | [U] Unscheduled flexible activity", result);
    }

    @Test
    public void format_weekNormal_includesAllSevenDaysAndEmptyPlaceholders() {
        TimetablePeriod period = new TimetablePeriod("This week", MONDAY, MONDAY.plusDays(6), true);
        TimetableView view = new TimetableView(period,
                List.of(fixed(1, MONDAY, 9, 11, false)), List.of());

        String result = TimetableFormatter.format(view, TimetableMode.NORMAL);

        assertTrue(result.contains("MONDAY | 2026-08-17"));
        assertTrue(result.contains("SUNDAY | 2026-08-23"));
        assertEquals(6, countOccurrences(result, "No fixed activities."));
    }

    @Test
    public void format_weekCompact_omitsEmptyDaysLegendAndPlaceholder() {
        TimetablePeriod period = new TimetablePeriod("This week", MONDAY, MONDAY.plusDays(6), true);
        TimetableView view = new TimetableView(period,
                List.of(fixed(1, MONDAY.plusDays(2), 9, 11, false)), List.of());

        String result = TimetableFormatter.format(view, TimetableMode.COMPACT);

        assertTrue(result.contains("WEDNESDAY | 2026-08-19"));
        assertFalse(result.contains("MONDAY |"));
        assertFalse(result.contains("No fixed activities."));
        assertFalse(result.contains("[F] Fixed activity |"));
    }

    @Test
    public void format_emptyCompact_showsHelpfulMessages() {
        TimetablePeriod period = new TimetablePeriod("This week", MONDAY, MONDAY.plusDays(6), true);
        TimetableView view = new TimetableView(period, List.of(), List.of());

        String result = TimetableFormatter.format(view, TimetableMode.COMPACT);

        assertTrue(result.contains("No fixed activities in the selected period."));
        assertTrue(result.contains("UNSCHEDULED FLEXIBLE ACTIVITIES\n  None"));
    }

    @Test
    public void format_detail_includesEveryStoredMetadataField() {
        TimetablePeriod period = new TimetablePeriod(MONDAY.toString(), MONDAY, MONDAY, false);
        TimetableView view = new TimetableView(period,
                List.of(fixed(1, MONDAY, 9, 11, false)), List.of(flexible(2, MONDAY)));

        String result = TimetableFormatter.format(view, TimetableMode.DETAIL);

        assertTrue(result.contains("Status: Incomplete | Category: ACADEMIC"
                + " | Energy: 2/5 | Sensory: 3/5"));
        assertTrue(result.contains("Topic: CS2113 | Note: Bring laptop"));
        assertTrue(result.contains("Status: Complete | Category: OTHERS"
                + " | Energy: 3/5 | Sensory: 4/5"));
    }

    @Test
    public void format_overlaps_marksEveryEntryAndShowsWarning() {
        TimetablePeriod period = new TimetablePeriod(MONDAY.toString(), MONDAY, MONDAY, false);
        TimetableView view = new TimetableView(period,
                List.of(fixed(1, MONDAY, 9, 11, true), fixed(2, MONDAY, 10, 12, true)),
                List.of());

        String result = TimetableFormatter.format(view, TimetableMode.NORMAL);

        assertTrue(result.contains("Warning: overlapping fixed commitments"));
        assertEquals(3, countOccurrences(result, "[OVERLAP]"));
    }

    @Test
    public void format_plainText_containsNoAnsiEscape() {
        TimetablePeriod period = new TimetablePeriod(MONDAY.toString(), MONDAY, MONDAY, false);
        TimetableView view = new TimetableView(period,
                List.of(fixed(1, MONDAY, 9, 11, false)), List.of());

        String result = TimetableFormatter.format(view, TimetableMode.NORMAL);

        assertFalse(result.contains("\u001B"));
    }

    private static int countOccurrences(String text, String needle) {
        return (text.length() - text.replace(needle, "").length()) / needle.length();
    }
}
