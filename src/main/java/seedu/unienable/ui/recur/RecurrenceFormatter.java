package seedu.unienable.ui.recur;

import java.util.stream.Collectors;

import seedu.unienable.model.classes.FixedActivity;
import seedu.unienable.model.recur.AcademicWeek;
import seedu.unienable.model.recur.RecurrencePlan;
import seedu.unienable.model.recur.RecurrencePlan.PlannedOccurrence;
import seedu.unienable.model.recur.RecurrencePlan.SkippedOccurrence;

/** Formats recurrence previews and results without embedding calendar-specific data. */
public final class RecurrenceFormatter {
    private RecurrenceFormatter() {
    }

    /** Returns the y/n preview shown before any occurrence is created. */
    public static String formatPreview(RecurrencePlan plan) {
        FixedActivity source = plan.getSource();
        AcademicWeek sourceWeek = plan.getSourceWeek();
        StringBuilder output = new StringBuilder();
        output.append("Create recurring sessions from activity [")
                .append(source.getId()).append("]?\n\n")
                .append("Source       : ").append(source.getDescription()).append('\n')
                .append("Calendar     : ").append(sourceWeek.getAcademicYear()).append(' ')
                .append(sourceWeek.getSemester()).append('\n')
                .append("Day and time : ").append(source.getDate().getDayOfWeek()).append(", ")
                .append(source.getStartTime()).append(" -> ").append(source.getEndTime()).append('\n')
                .append("Weeks        : ").append(formatWeeks(plan)).append("\n\n")
                .append("To create:\n");
        for (PlannedOccurrence occurrence : plan.getOccurrencesToCreate()) {
            output.append(formatPlanned(occurrence)).append('\n');
        }
        appendSkipped(output, plan);
        int count = plan.getOccurrencesToCreate().size();
        output.append("\n\n").append(count).append(count == 1
                ? " new activity will be created.\n" : " new activities will be created.\n")
                .append("Continue? (y/n)");
        return output.toString();
    }

    /** Returns feedback when every requested occurrence is already present or skipped. */
    public static String formatNothingToCreate(RecurrencePlan plan) {
        StringBuilder output = new StringBuilder("No new recurring sessions to create.");
        appendSkipped(output, plan);
        return output.toString();
    }

    /** Returns success feedback after the full plan has been added. */
    public static String formatSuccess(RecurrencePlan plan) {
        int count = plan.getOccurrencesToCreate().size();
        StringBuilder output = new StringBuilder("Created ").append(count)
                .append(count == 1 ? " recurring session" : " recurring sessions")
                .append(" from activity [").append(plan.getSource().getId()).append("]:");
        for (PlannedOccurrence occurrence : plan.getOccurrencesToCreate()) {
            output.append('\n').append(formatPlanned(occurrence));
        }
        return output.toString();
    }

    private static String formatWeeks(RecurrencePlan plan) {
        return plan.getRequestedWeeks().stream().map(String::valueOf)
                .collect(Collectors.joining(", "));
    }

    private static String formatPlanned(PlannedOccurrence occurrence) {
        FixedActivity activity = occurrence.getActivity();
        return "Week " + occurrence.getWeekNumber() + " | " + activity.getDate()
                + " | Activity [" + activity.getId() + "]";
    }

    private static void appendSkipped(StringBuilder output, RecurrencePlan plan) {
        if (plan.getSkippedOccurrences().isEmpty()) {
            return;
        }
        if (output.length() > 0 && output.charAt(output.length() - 1) == '\n') {
            output.append("\nSkipped:\n");
        } else {
            output.append("\n\nSkipped:\n");
        }
        for (SkippedOccurrence occurrence : plan.getSkippedOccurrences()) {
            output.append("Week ").append(occurrence.getWeekNumber()).append(" | ")
                    .append(occurrence.getDate()).append(" | ")
                    .append(occurrence.getReason()).append('\n');
        }
        output.setLength(output.length() - 1);
    }
}
