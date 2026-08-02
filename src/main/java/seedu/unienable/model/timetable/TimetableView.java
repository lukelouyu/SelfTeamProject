package seedu.unienable.model.timetable;

import java.util.List;

/** Immutable calculated timetable result. */
public final class TimetableView {
    private final TimetablePeriod period;
    private final List<TimetableEntry> fixedEntries;
    private final List<TimetableEntry> unscheduledFlexibleEntries;

    /**
     * Creates a timetable result from already ordered display entries.
     *
     * @param period selected period
     * @param fixedEntries ordered fixed entries
     * @param unscheduledFlexibleEntries ordered unscheduled flexible entries
     */
    public TimetableView(TimetablePeriod period, List<TimetableEntry> fixedEntries,
            List<TimetableEntry> unscheduledFlexibleEntries) {
        this.period = period;
        this.fixedEntries = List.copyOf(fixedEntries);
        this.unscheduledFlexibleEntries = List.copyOf(unscheduledFlexibleEntries);
    }

    /** Returns the selected period. */
    public TimetablePeriod getPeriod() {
        return period;
    }

    /** Returns the immutable ordered fixed-entry list. */
    public List<TimetableEntry> getFixedEntries() {
        return fixedEntries;
    }

    /** Returns the immutable ordered unscheduled-flexible list. */
    public List<TimetableEntry> getUnscheduledFlexibleEntries() {
        return unscheduledFlexibleEntries;
    }

    /** Returns whether any fixed entry is marked as overlapping. */
    public boolean hasOverlaps() {
        return fixedEntries.stream().anyMatch(TimetableEntry::isOverlapping);
    }

    /** Returns whether the period contains no fixed or flexible entry. */
    public boolean isEmpty() {
        return fixedEntries.isEmpty() && unscheduledFlexibleEntries.isEmpty();
    }
}
