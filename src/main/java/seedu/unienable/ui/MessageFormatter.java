package seedu.unienable.ui;

import seedu.unienable.model.classes.Activity;
import seedu.unienable.model.classes.FixedActivity;
import seedu.unienable.model.classes.FlexibleActivity;
import seedu.unienable.model.enums.ScheduleType;

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

    /**
     * Formats one activity as a two-line concise entry, matching "list"/"find"'s default view.
     *
     * @param activity the activity to format
     * @return the formatted two-line entry
     */
    public static String formatConcise(Activity activity) {
        String statusSymbol = activity.isComplete() ? "X" : " ";
        String typeSymbol = activity.getScheduleType() == ScheduleType.FIXED ? "F" : "L";
        String topicSuffix = activity.getTopic() == null ? "" : " / " + activity.getTopic();
        StringBuilder result = new StringBuilder();
        result.append('[').append(activity.getId()).append("][").append(statusSymbol).append(']')
                .append('[').append(typeSymbol).append("] ").append(activity.getDate()).append(' ');
        if (activity instanceof FixedActivity) {
            FixedActivity fixed = (FixedActivity) activity;
            result.append(fixed.getStartTime()).append("–").append(fixed.getEndTime());
        } else {
            FlexibleActivity flexible = (FlexibleActivity) activity;
            result.append(flexible.getEarliestStart()).append("–").append(flexible.getLatestEnd());
        }
        result.append(" | ").append(activity.getDescription()).append("\n             ")
                .append(activity.getCategory()).append(topicSuffix);
        if (activity instanceof FlexibleActivity) {
            result.append(" | ").append(((FlexibleActivity) activity).getDurationMinutes()).append(" min");
        }
        result.append(" | E").append(activity.getEnergyRating().getValue())
                .append(" | S").append(activity.getSensoryRating().getValue());
        return result.toString();
    }

    /**
     * Formats one activity as a multi-line detail entry, matching "list view/detail"'s layout.
     *
     * @param activity the activity to format
     * @return the formatted detail entry
     */
    public static String formatDetail(Activity activity) {
        StringBuilder result = new StringBuilder();
        result.append('[').append(activity.getId()).append("] ").append(activity.getDescription()).append('\n');
        result.append("Status: ").append(activity.isComplete() ? "Complete" : "Incomplete")
                .append(" | Type: ").append(activity.getScheduleType())
                .append(" | Date: ").append(activity.getDate()).append('\n');
        if (activity instanceof FixedActivity) {
            FixedActivity fixed = (FixedActivity) activity;
            result.append("Time: ").append(fixed.getStartTime()).append("–").append(fixed.getEndTime())
                    .append(" | Category: ").append(fixed.getCategory());
            appendTopicSuffix(result, fixed.getTopic());
            result.append('\n');
        } else {
            FlexibleActivity flexible = (FlexibleActivity) activity;
            result.append("Window: ").append(flexible.getEarliestStart()).append("–")
                    .append(flexible.getLatestEnd()).append(" | Duration: ")
                    .append(flexible.getDurationMinutes()).append(" min\n");
            result.append("Category: ").append(flexible.getCategory());
            appendTopicSuffix(result, flexible.getTopic());
            result.append('\n');
        }
        result.append("Energy: ").append(activity.getEnergyRating()).append(" | Sensory: ")
                .append(activity.getSensoryRating()).append(" | Note: ")
                .append(activity.getNote() == null ? "None" : activity.getNote());
        return result.toString();
    }

    private static void appendTopicSuffix(StringBuilder result, String topic) {
        if (topic != null) {
            result.append(" | Topic: ").append(topic);
        }
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
