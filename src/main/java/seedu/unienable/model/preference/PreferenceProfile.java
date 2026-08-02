package seedu.unienable.model.preference;

import java.time.LocalTime;
import java.util.Objects;

/** Immutable global planning preferences consumed by the future daily recommender. */
public final class PreferenceProfile {
    /** Largest accepted minimum buffer, in minutes. */
    public static final int MAXIMUM_BUFFER_MINUTES = 1440;

    private static final PreferenceProfile DEFAULT_PROFILE = new PreferenceProfile(
            LocalTime.of(8, 0), LocalTime.of(20, 0), 15, TomatoSuggestion.OFF);

    private final LocalTime preferredStart;
    private final LocalTime preferredEnd;
    private final int minimumBufferMinutes;
    private final TomatoSuggestion tomatoSuggestion;

    private PreferenceProfile(LocalTime preferredStart, LocalTime preferredEnd,
            int minimumBufferMinutes, TomatoSuggestion tomatoSuggestion) {
        this.preferredStart = Objects.requireNonNull(preferredStart);
        this.preferredEnd = Objects.requireNonNull(preferredEnd);
        this.tomatoSuggestion = Objects.requireNonNull(tomatoSuggestion);
        if (!preferredStart.isBefore(preferredEnd)) {
            throw new IllegalArgumentException("preferred start must be before preferred end.");
        }
        if (minimumBufferMinutes < 0 || minimumBufferMinutes > MAXIMUM_BUFFER_MINUTES) {
            throw new IllegalArgumentException("minimum buffer must be from 0 to "
                    + MAXIMUM_BUFFER_MINUTES + " minutes.");
        }
        this.minimumBufferMinutes = minimumBufferMinutes;
    }

    /**
     * Returns the single authoritative documented default profile.
     *
     * @return the immutable default profile
     */
    public static PreferenceProfile defaults() {
        return DEFAULT_PROFILE;
    }

    /**
     * Creates a validated complete profile.
     *
     * @param preferredStart preferred start of the daily planning range
     * @param preferredEnd preferred exclusive end of the daily planning range
     * @param minimumBufferMinutes requested buffer from 0 to 1440 minutes
     * @param tomatoSuggestion whether advisory Tomato suggestions are enabled
     * @return a new immutable profile
     * @throws IllegalArgumentException if the range or buffer is invalid
     */
    public static PreferenceProfile of(LocalTime preferredStart, LocalTime preferredEnd,
            int minimumBufferMinutes, TomatoSuggestion tomatoSuggestion) {
        return new PreferenceProfile(preferredStart, preferredEnd, minimumBufferMinutes,
                tomatoSuggestion);
    }

    /**
     * Returns the preferred inclusive daily start boundary.
     *
     * @return the preferred start
     */
    public LocalTime getPreferredStart() {
        return preferredStart;
    }

    /**
     * Returns the preferred exclusive daily end boundary.
     *
     * @return the preferred end
     */
    public LocalTime getPreferredEnd() {
        return preferredEnd;
    }

    /**
     * Returns the requested minimum buffer between planned activities, in minutes.
     *
     * @return the minimum buffer in minutes
     */
    public int getMinimumBufferMinutes() {
        return minimumBufferMinutes;
    }

    /**
     * Returns whether advisory Tomato/Pomodoro suggestions are enabled.
     *
     * @return the suggestion flag
     */
    public TomatoSuggestion getTomatoSuggestion() {
        return tomatoSuggestion;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof PreferenceProfile)) {
            return false;
        }
        PreferenceProfile that = (PreferenceProfile) other;
        return minimumBufferMinutes == that.minimumBufferMinutes
                && preferredStart.equals(that.preferredStart)
                && preferredEnd.equals(that.preferredEnd)
                && tomatoSuggestion == that.tomatoSuggestion;
    }

    @Override
    public int hashCode() {
        return Objects.hash(preferredStart, preferredEnd, minimumBufferMinutes, tomatoSuggestion);
    }
}
