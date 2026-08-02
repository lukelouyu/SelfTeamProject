package seedu.unienable.model.timetable;

import java.util.List;

/** Immutable calculated timetable result. */
public final class TimetableView {
    private final TimetablePeriod period;
    private final List<TimetableEntry> fixedEntries;
    private final List<TimetableEntry> unscheduledFlexibleEntries;

    /** Creates a timetable result from already ordered display entries. */
    public TimetableView(TimetablePeriod period, List<TimetableEntry> fixedEntries,
            List<TimetableEntry> unscheduledFlexibleEntries) {
        this.period = period;
        this.fixedEntries = List.copyOf(fixedEntries);
        this.unscheduledFlexibleEntries = List.copyOf(unscheduledFlexibleEntries);
    }

    public TimetablePeriod getPeriod() {
        return period;
    }

    public List<TimetableEntry> getFixedEntries() {
        return fixedEntries;
    }

    public List<TimetableEntry> getUnscheduledFlexibleEntries() {
        return unscheduledFlexibleEntries;
    }

    public boolean hasOverlaps() {
        return fixedEntries.stream().anyMatch(TimetableEntry::isOverlapping);
    }

    public boolean isEmpty() {
        return fixedEntries.isEmpty() && unscheduledFlexibleEntries.isEmpty();
    }
}
