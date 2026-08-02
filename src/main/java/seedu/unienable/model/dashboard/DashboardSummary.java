package seedu.unienable.model.dashboard;

import java.util.EnumMap;
import java.util.Map;
import java.util.OptionalInt;

import seedu.unienable.model.enums.ActivityCategory;

/**
 * An immutable, fully-calculated dashboard result for one {@link DashboardPeriod}. Carries every
 * metric the default and detail views need; the formatter decides which to display.
 */
public final class DashboardSummary {
    private final DashboardPeriod period;
    private final int totalActivityCount;
    private final long plannedWorkloadMinutes;
    private final long nominalBufferMinutes;
    private final long overloadMinutes;
    private final RatingSummary energy;
    private final RatingSummary sensory;
    private final int eligibleCount;
    private final int completedEligibleCount;
    private final OptionalInt completionPercentage;
    private final int fixedCount;
    private final int flexibleCount;
    private final Map<ActivityCategory, Integer> categoryCounts;

    /**
     * Creates a DashboardSummary.
     *
     * @param period the period this summary was calculated over
     * @param totalActivityCount the number of activities included in the period (fixed + flexible)
     * @param plannedWorkloadMinutes clipped fixed durations plus full flexible durations, summed
     * @param nominalBufferMinutes {@code max(0, capacity - workload)}
     * @param overloadMinutes {@code max(0, workload - capacity)}
     * @param energy the energy-rating summary over included activities
     * @param sensory the sensory-rating summary over included activities
     * @param eligibleCount completion-eligible activities (completed + incomplete eligible)
     * @param completedEligibleCount completion-eligible activities marked complete
     * @param completionPercentage the rounded completion percentage, empty if eligibleCount is 0
     * @param fixedCount included fixed activities
     * @param flexibleCount included flexible activities
     * @param categoryCounts included-activity counts per category; must contain every
     *     {@link ActivityCategory} (zero-filled where none), defensively copied
     */
    public DashboardSummary(DashboardPeriod period, int totalActivityCount, long plannedWorkloadMinutes,
            long nominalBufferMinutes, long overloadMinutes, RatingSummary energy, RatingSummary sensory,
            int eligibleCount, int completedEligibleCount, OptionalInt completionPercentage, int fixedCount,
            int flexibleCount, Map<ActivityCategory, Integer> categoryCounts) {
        this.period = period;
        this.totalActivityCount = totalActivityCount;
        this.plannedWorkloadMinutes = plannedWorkloadMinutes;
        this.nominalBufferMinutes = nominalBufferMinutes;
        this.overloadMinutes = overloadMinutes;
        this.energy = energy;
        this.sensory = sensory;
        this.eligibleCount = eligibleCount;
        this.completedEligibleCount = completedEligibleCount;
        this.completionPercentage = completionPercentage;
        this.fixedCount = fixedCount;
        this.flexibleCount = flexibleCount;
        this.categoryCounts = Map.copyOf(new EnumMap<>(categoryCounts));
    }

    /** Returns the period this summary was calculated over. */
    public DashboardPeriod getPeriod() {
        return period;
    }

    /** Returns the number of activities included in the period. */
    public int getTotalActivityCount() {
        return totalActivityCount;
    }

    /** Returns the planned workload in minutes. */
    public long getPlannedWorkloadMinutes() {
        return plannedWorkloadMinutes;
    }

    /** Returns the nominal buffer in minutes ({@code max(0, capacity - workload)}). */
    public long getNominalBufferMinutes() {
        return nominalBufferMinutes;
    }

    /** Returns the overload in minutes ({@code max(0, workload - capacity)}); 0 if not overloaded. */
    public long getOverloadMinutes() {
        return overloadMinutes;
    }

    /** Returns whether planned workload exceeds the period's capacity. */
    public boolean isOverloaded() {
        return overloadMinutes > 0;
    }

    /** Returns the energy-rating summary over included activities. */
    public RatingSummary getEnergy() {
        return energy;
    }

    /** Returns the sensory-rating summary over included activities. */
    public RatingSummary getSensory() {
        return sensory;
    }

    /** Returns the completion-eligible activity count (completed + incomplete eligible). */
    public int getEligibleCount() {
        return eligibleCount;
    }

    /** Returns the completion-eligible activity count marked complete. */
    public int getCompletedEligibleCount() {
        return completedEligibleCount;
    }

    /** Returns the rounded completion percentage, empty if no activity is completion-eligible yet. */
    public OptionalInt getCompletionPercentage() {
        return completionPercentage;
    }

    /** Returns the number of included fixed activities. */
    public int getFixedCount() {
        return fixedCount;
    }

    /** Returns the number of included flexible activities. */
    public int getFlexibleCount() {
        return flexibleCount;
    }

    /** Returns included-activity counts per category; every {@link ActivityCategory} is present. */
    public Map<ActivityCategory, Integer> getCategoryCounts() {
        return categoryCounts;
    }
}
