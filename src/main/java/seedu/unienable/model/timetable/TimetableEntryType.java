package seedu.unienable.model.timetable;

/** Meaning of one immutable timetable display entry. */
public enum TimetableEntryType {
    /** Activity with confirmed start and end times. */
    FIXED,
    /** Flexible activity with an adopted scheduled placement. */
    ADOPTED_FLEXIBLE,
    /** Flexible activity shown without an invented placement. */
    UNSCHEDULED_FLEXIBLE
}
