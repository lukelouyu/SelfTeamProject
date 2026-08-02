package seedu.unienable.model.dashboard;

import java.time.Duration;
import java.time.LocalDateTime;

/**
 * An immutable, half-open time period a dashboard is calculated over: {@code [start, end)}.
 * Capacity is derived from the boundary duration rather than stored separately, so a single-day
 * period (1440 minutes) and a week period (10080 minutes) need no hardcoded per-period table.
 */
public final class DashboardPeriod {
    private final String label;
    private final LocalDateTime start;
    private final LocalDateTime end;

    /**
     * Creates a DashboardPeriod.
     *
     * @param label the display label, e.g. "Today", "Tomorrow", "This week", or an ISO date
     *     string for a {@code date/} selection
     * @param start the inclusive start of the period
     * @param end the exclusive end of the period
     */
    public DashboardPeriod(String label, LocalDateTime start, LocalDateTime end) {
        this.label = label;
        this.start = start;
        this.end = end;
    }

    /** Returns the display label. */
    public String getLabel() {
        return label;
    }

    /** Returns the inclusive start of the period. */
    public LocalDateTime getStart() {
        return start;
    }

    /** Returns the exclusive end of the period. */
    public LocalDateTime getEnd() {
        return end;
    }

    /** Returns the period's total capacity in minutes, derived from its own boundaries. */
    public long getCapacityMinutes() {
        return Duration.between(start, end).toMinutes();
    }
}
