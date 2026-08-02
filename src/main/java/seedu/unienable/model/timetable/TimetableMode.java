package seedu.unienable.model.timetable;

/** Output mode selected for one timetable request. */
public enum TimetableMode {
    /** Full day-grouped output with empty-day placeholders. */
    NORMAL,
    /** Narrow weekly output that omits empty days and the legend. */
    COMPACT,
    /** Full output plus stored activity metadata. */
    DETAIL
}
