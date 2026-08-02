package seedu.unienable.model.timetable;

import java.time.LocalDate;

/** Immutable inclusive date range selected for one timetable view. */
public final class TimetablePeriod {
    private final String label;
    private final LocalDate startDate;
    private final LocalDate endDate;
    private final boolean weekly;

    /**
     * Creates a selected timetable period.
     *
     * @param label user-facing period label
     * @param startDate inclusive first date
     * @param endDate inclusive last date
     * @param weekly whether this is a seven-day weekly view
     */
    public TimetablePeriod(String label, LocalDate startDate, LocalDate endDate, boolean weekly) {
        assert !endDate.isBefore(startDate) : "timetable period end must not precede its start";
        this.label = label;
        this.startDate = startDate;
        this.endDate = endDate;
        this.weekly = weekly;
    }

    /** Returns the user-facing period label. */
    public String getLabel() {
        return label;
    }

    /** Returns the inclusive first date. */
    public LocalDate getStartDate() {
        return startDate;
    }

    /** Returns the inclusive last date. */
    public LocalDate getEndDate() {
        return endDate;
    }

    /** Returns whether this period is a Monday-Sunday weekly view. */
    public boolean isWeekly() {
        return weekly;
    }

    /** Returns whether the given date lies inside this inclusive period. */
    public boolean contains(LocalDate date) {
        return !date.isBefore(startDate) && !date.isAfter(endDate);
    }
}
