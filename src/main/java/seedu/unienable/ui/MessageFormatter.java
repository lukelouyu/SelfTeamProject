package seedu.unienable.ui;

import seedu.unienable.model.classes.Activity;
import seedu.unienable.model.classes.FixedActivity;
import seedu.unienable.model.classes.FlexibleActivity;

/** Formats Activity data into the display shapes used across commands. */
public class MessageFormatter {
    private static final int LABEL_WIDTH = 12;

    /**
     * Formats every stored field of an activity, matching the "view ID" command's layout.
     *
     * @param activity the activity to format
     * @return the formatted, field-per-line block
     */
    public static String formatView(Activity activity) {
        StringBuilder result = new StringBuilder();
        result.append("Activity [").append(activity.getId()).append("]\n");
        appendField(result, "Description", activity.getDescription());
        appendField(result, "Status", activity.isComplete() ? "Complete" : "Incomplete");
        appendField(result, "Type", activity.getScheduleType());
        appendField(result, "Date", activity.getDate());
        appendTimingFields(result, activity);
        appendField(result, "Category", activity.getCategory());
        if (activity.getTopic() != null) {
            appendField(result, "Topic", activity.getTopic());
        }
        appendField(result, "Energy", activity.getEnergyRating());
        result.append(labelPrefix("Sensory")).append(activity.getSensoryRating());
        if (activity.getNote() != null) {
            result.append('\n');
            result.append(labelPrefix("Note")).append(activity.getNote());
        }
        return result.toString();
    }

    private static void appendTimingFields(StringBuilder result, Activity activity) {
        if (activity instanceof FixedActivity) {
            FixedActivity fixed = (FixedActivity) activity;
            appendField(result, "Start", fixed.getStartTime());
            appendField(result, "End", fixed.getEndTime());
            return;
        }
        FlexibleActivity flexible = (FlexibleActivity) activity;
        appendField(result, "Earliest", flexible.getEarliestStart());
        appendField(result, "Latest", flexible.getLatestEnd());
        appendField(result, "Duration", flexible.getDurationMinutes() + " min");
    }

    private static void appendField(StringBuilder result, String label, Object value) {
        result.append(labelPrefix(label)).append(value).append('\n');
    }

    private static String labelPrefix(String label) {
        StringBuilder padded = new StringBuilder(label);
        while (padded.length() < LABEL_WIDTH) {
            padded.append(' ');
        }
        return padded.append(": ").toString();
    }
}
