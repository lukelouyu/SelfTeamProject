package seedu.unienable.model.dashboard;

/**
 * An immutable summary of one rating dimension (energy or sensory) over a set of included
 * activities: total, high-rating count, and - only meaningful when at least one activity
 * contributed - average and highest. Reused identically for both energy and sensory, since the
 * two are structurally identical metrics over different ratings.
 */
public final class RatingSummary {
    private final int total;
    private final int highCount;
    private final boolean hasData;
    private final double average;
    private final int highest;
    private final int[] distribution;

    /**
     * Creates a RatingSummary.
     *
     * @param total the sum of every included activity's rating for this dimension
     * @param highCount the number of included activities whose rating meets the high-rating
     *     threshold
     * @param hasData whether at least one activity contributed - when false, {@code average} and
     *     {@code highest} are not meaningful and must not be displayed as real values
     * @param average the average rating, rounded to one decimal place (half-up); meaningless if
     *     {@code hasData} is false
     * @param highest the highest included rating; meaningless if {@code hasData} is false
     * @param distribution the count of included activities at each rating 1-5, indexed
     *     {@code distribution[rating - 1]}; defensively copied
     */
    public RatingSummary(int total, int highCount, boolean hasData, double average, int highest,
            int[] distribution) {
        this.total = total;
        this.highCount = highCount;
        this.hasData = hasData;
        this.average = average;
        this.highest = highest;
        this.distribution = distribution.clone();
    }

    /** Returns the sum of every included activity's rating for this dimension. */
    public int getTotal() {
        return total;
    }

    /** Returns the number of included activities whose rating meets the high-rating threshold. */
    public int getHighCount() {
        return highCount;
    }

    /** Returns whether at least one activity contributed to this summary. */
    public boolean hasData() {
        return hasData;
    }

    /**
     * Returns the average rating, rounded to one decimal place (half-up). Meaningless if
     * {@link #hasData()} is false.
     */
    public double getAverage() {
        return average;
    }

    /** Returns the highest included rating. Meaningless if {@link #hasData()} is false. */
    public int getHighest() {
        return highest;
    }

    /**
     * Returns the count of included activities at each rating 1-5, indexed
     * {@code [rating - 1]}, as an unmodifiable copy.
     */
    public int[] getDistribution() {
        return distribution.clone();
    }
}
