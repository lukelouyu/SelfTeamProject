package seedu.unienable.ui.timetable;

import java.time.LocalDate;
import java.time.format.TextStyle;
import java.util.List;
import java.util.Locale;

import seedu.unienable.model.timetable.TimetableEntry;
import seedu.unienable.model.timetable.TimetableMode;
import seedu.unienable.model.timetable.TimetablePeriod;
import seedu.unienable.model.timetable.TimetableView;

/** Formats a calculated timetable as deterministic plain text. */
public final class TimetableFormatter {
    private TimetableFormatter() {
    }

    /**
     * Formats one timetable in the selected output mode.
     *
     * @param view immutable calculated timetable
     * @param mode selected output mode
     * @return deterministic plain-text timetable
     */
    public static String format(TimetableView view, TimetableMode mode) {
        StringBuilder result = new StringBuilder();
        TimetablePeriod period = view.getPeriod();
        result.append(period.isWeekly() ? "Weekly Timetable\n" : "Daily Timetable\n");
        result.append("Period: ").append(period.getLabel());

        if (view.hasOverlaps()) {
            result.append("\n\nWarning: overlapping fixed commitments are marked [OVERLAP].");
        }

        appendFixedEntries(result, view, mode);
        appendFlexibleEntries(result, view.getUnscheduledFlexibleEntries(), mode);

        if (mode != TimetableMode.COMPACT) {
            result.append("\n\n[F] Fixed activity | [U] Unscheduled flexible activity");
        }
        return result.toString();
    }

    private static void appendFixedEntries(StringBuilder result, TimetableView view,
            TimetableMode mode) {
        TimetablePeriod period = view.getPeriod();
        boolean wroteDay = false;
        for (LocalDate date = period.getStartDate(); !date.isAfter(period.getEndDate());
                date = date.plusDays(1)) {
            List<TimetableEntry> entries = entriesOn(view.getFixedEntries(), date);
            if (mode == TimetableMode.COMPACT && entries.isEmpty()) {
                continue;
            }
            result.append("\n\n").append(dayHeading(date));
            wroteDay = true;
            if (entries.isEmpty()) {
                result.append("\n  No fixed activities.");
                continue;
            }
            for (TimetableEntry entry : entries) {
                result.append('\n');
                appendFixedEntry(result, entry, mode);
            }
        }
        if (!wroteDay) {
            result.append("\n\nNo fixed activities in the selected period.");
        }
    }

    private static List<TimetableEntry> entriesOn(List<TimetableEntry> entries, LocalDate date) {
        return entries.stream().filter(entry -> entry.getDate().equals(date)).toList();
    }

    private static String dayHeading(LocalDate date) {
        String day = date.getDayOfWeek().getDisplayName(TextStyle.FULL, Locale.ENGLISH)
                .toUpperCase(Locale.ROOT);
        return day + " | " + date;
    }

    private static void appendFixedEntry(StringBuilder result, TimetableEntry entry,
            TimetableMode mode) {
        result.append("  ").append(entry.getStartTime()).append('-').append(entry.getEndTime())
                .append("  [F][").append(entry.getId()).append("] ")
                .append(entry.getDescription());
        if (entry.isOverlapping()) {
            result.append(" [OVERLAP]");
        }
        appendDetail(result, entry, mode);
    }

    private static void appendFlexibleEntries(StringBuilder result, List<TimetableEntry> entries,
            TimetableMode mode) {
        result.append("\n\nUNSCHEDULED FLEXIBLE ACTIVITIES");
        if (entries.isEmpty()) {
            result.append("\n  None in the selected period.");
            return;
        }
        for (TimetableEntry entry : entries) {
            result.append("\n  ").append(entry.getDate()).append(' ')
                    .append(entry.getStartTime()).append('-').append(entry.getEndTime())
                    .append("  [U][").append(entry.getId()).append("] ")
                    .append(entry.getDescription()).append(" (")
                    .append(entry.getDurationMinutes()).append(" min required)");
            appendDetail(result, entry, mode);
        }
    }

    private static void appendDetail(StringBuilder result, TimetableEntry entry,
            TimetableMode mode) {
        if (mode != TimetableMode.DETAIL) {
            return;
        }
        result.append("\n      Status: ").append(entry.isComplete() ? "Complete" : "Incomplete")
                .append(" | Category: ").append(entry.getCategory())
                .append(" | Energy: ").append(entry.getEnergyRating()).append("/5")
                .append(" | Sensory: ").append(entry.getSensoryRating()).append("/5");
        if (entry.getTopic() != null || entry.getNote() != null) {
            result.append("\n      Topic: ").append(orNone(entry.getTopic()))
                    .append(" | Note: ").append(orNone(entry.getNote()));
        }
    }

    private static String orNone(String value) {
        return value == null ? "None" : value;
    }
}
