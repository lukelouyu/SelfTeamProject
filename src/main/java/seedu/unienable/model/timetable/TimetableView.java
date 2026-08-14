package seedu.unienable.model.timetable;

import java.util.List;

/** Immutable calculated timetable result. */
public final class TimetableView {
    private final TimetablePeriod period;
    private final List<TimetableEntry> scheduledEntries;
    private final List<TimetableEntry> unscheduledFlexibleEntries;

    /**
     * Creates a timetable result from already ordered display entries.
     *
     * @param period selected period
     * @param scheduledEntries ordered scheduled entries
     * @param unscheduledFlexibleEntries ordered unscheduled flexible entries
     */
    public TimetableView(TimetablePeriod period, List<TimetableEntry> scheduledEntries,
            List<TimetableEntry> unscheduledFlexibleEntries) {
        this.period = period;
        this.scheduledEntries = List.copyOf(scheduledEntries);
        this.unscheduledFlexibleEntries = List.copyOf(unscheduledFlexibleEntries);
    }

    /** Returns the selected period. */
    public TimetablePeriod getPeriod() {
        return period;
    }

    /** Returns the immutable ordered scheduled-entry list. */
    public List<TimetableEntry> getScheduledEntries() {
        return scheduledEntries;
    }

    /** Returns the immutable ordered unscheduled-flexible list. */
    public List<TimetableEntry> getUnscheduledFlexibleEntries() {
        return unscheduledFlexibleEntries;
    }

    /** Returns whether any scheduled entry (fixed or adopted flexible) is marked as overlapping. */
    public boolean hasOverlaps() {
        return scheduledEntries.stream().anyMatch(TimetableEntry::isOverlapping);
    }

    /** Returns whether the period contains no fixed or flexible entry. */
    public boolean isEmpty() {
        return scheduledEntries.isEmpty() && unscheduledFlexibleEntries.isEmpty();
    }
}
