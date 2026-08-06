package seedu.unienable.model.classes;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;

import seedu.unienable.model.enums.ActivityCategory;
import seedu.unienable.model.enums.ScheduleType;

/** An activity that may be scheduled anywhere within an allowed time window. */
public class FlexibleActivity extends Activity {
    private static final Logger logger = Logger.getLogger(FlexibleActivity.class.getName());

    private LocalTime earliestStart;
    private LocalTime latestEnd;
    private int durationMinutes;
    private LocalTime adoptedStartTime;

    /**
     * Creates a FlexibleActivity.
     *
     * @param id stable activity ID
     * @param description activity description
     * @param category fixed top-level category
     * @param date activity date
     * @param earliestStart earliest allowed start time
     * @param latestEnd latest allowed end time
     * @param durationMinutes required duration in minutes
     * @param energyRating energy-demand rating
     * @param sensoryRating sensory-load rating
     * @param topic optional topic name, or null if none
     * @param note optional preparation note, or null if none
     */
    public FlexibleActivity(int id, String description, ActivityCategory category, LocalDate date,
            LocalTime earliestStart, LocalTime latestEnd, int durationMinutes,
            EnergyRating energyRating, SensoryRating sensoryRating, String topic, String note) {
        this(id, description, category, date, earliestStart, latestEnd, durationMinutes,
                energyRating, sensoryRating, topic, note, null);
    }

    /**
     * Creates a FlexibleActivity with an optional adopted placement.
     *
     * @param adoptedStartTime adopted scheduled start time, or null if the activity is still
     *     unscheduled
     */
    public FlexibleActivity(int id, String description, ActivityCategory category, LocalDate date,
            LocalTime earliestStart, LocalTime latestEnd, int durationMinutes,
            EnergyRating energyRating, SensoryRating sensoryRating, String topic, String note,
            LocalTime adoptedStartTime) {
        super(id, description, category, date, energyRating, sensoryRating, topic, note);
        requireValidWindow(earliestStart, latestEnd, durationMinutes);
        this.earliestStart = earliestStart;
        this.latestEnd = latestEnd;
        this.durationMinutes = durationMinutes;
        requireValidAdoptedPlacement(earliestStart, latestEnd, durationMinutes, adoptedStartTime);
        this.adoptedStartTime = adoptedStartTime;
    }

    /** Returns the earliest allowed start time. */
    public LocalTime getEarliestStart() {
        return earliestStart;
    }

    /**
     * Sets the earliest allowed start if the complete resulting window remains valid.
     *
     * @param earliestStart proposed earliest start
     * @throws IllegalArgumentException if the duration or adopted placement would no longer fit
     */
    public void setEarliestStart(LocalTime earliestStart) {
        updateWindow(earliestStart, latestEnd, durationMinutes);
    }

    /** Returns the latest allowed end time. */
    public LocalTime getLatestEnd() {
        return latestEnd;
    }

    /**
     * Sets the latest allowed end if the complete resulting window remains valid.
     *
     * @param latestEnd proposed latest end
     * @throws IllegalArgumentException if the duration or adopted placement would no longer fit
     */
    public void setLatestEnd(LocalTime latestEnd) {
        updateWindow(earliestStart, latestEnd, durationMinutes);
    }

    /** Returns the required duration in minutes. */
    public int getDurationMinutes() {
        return durationMinutes;
    }

    /**
     * Sets the duration if it and any adopted placement still fit the current window.
     *
     * @param durationMinutes proposed positive duration in minutes
     * @throws IllegalArgumentException if the resulting state would be invalid
     */
    public void setDurationMinutes(int durationMinutes) {
        updateWindow(earliestStart, latestEnd, durationMinutes);
    }

    /**
     * Atomically replaces the flexible window and duration after validating the full proposed
     * state, including any existing adopted placement.
     *
     * @param earliestStart proposed earliest start
     * @param latestEnd proposed latest end
     * @param durationMinutes proposed positive duration in minutes
     * @throws IllegalArgumentException if the window, duration, or adopted placement is invalid
     */
    public void updateWindow(LocalTime earliestStart, LocalTime latestEnd, int durationMinutes) {
        requireValidWindow(earliestStart, latestEnd, durationMinutes);
        requireValidAdoptedPlacement(earliestStart, latestEnd, durationMinutes, adoptedStartTime);
        logger.log(Level.INFO, "Updated flexible window for activity [" + getId() + "].");
        this.earliestStart = earliestStart;
        this.latestEnd = latestEnd;
        this.durationMinutes = durationMinutes;
    }

    /** Returns whether this activity has an adopted scheduled placement. */
    public boolean hasAdoptedPlacement() {
        return adoptedStartTime != null;
    }

    /** Returns the adopted scheduled start time, or null if still unscheduled. */
    public LocalTime getAdoptedStartTime() {
        return adoptedStartTime;
    }

    /** Returns the adopted scheduled end time, or null if still unscheduled. */
    public LocalTime getAdoptedEndTime() {
        return adoptedStartTime == null ? null : adoptedStartTime.plusMinutes(durationMinutes);
    }

    /** Sets the adopted placement after validating it still fits the flexible window. */
    public void setAdoptedStartTime(LocalTime adoptedStartTime) {
        requireValidAdoptedPlacement(earliestStart, latestEnd, durationMinutes, adoptedStartTime);
        logger.log(Level.INFO, "Updated adopted schedule for activity [" + getId() + "].");
        this.adoptedStartTime = adoptedStartTime;
    }

    /**
     * Returns whether the proposed non-null adopted start would fit the current flexible window.
     *
     * @param proposedStart proposed adopted start
     * @return true if the placement would fit
     */
    public boolean canAdoptAt(LocalTime proposedStart) {
        if (proposedStart == null) {
            return false;
        }
        return !proposedStart.isBefore(earliestStart)
                && !proposedStart.isAfter(latestEnd)
                && Duration.between(proposedStart, latestEnd).toMinutes() >= durationMinutes;
    }

    /** Clears any adopted placement and returns this activity to an unscheduled flexible state. */
    public void clearAdoptedPlacement() {
        logger.log(Level.INFO, "Cleared adopted schedule for activity [" + getId() + "].");
        this.adoptedStartTime = null;
    }

    private static void requireValidAdoptedPlacement(LocalTime earliestStart, LocalTime latestEnd,
            int durationMinutes, LocalTime adoptedStartTime) {
        if (adoptedStartTime == null) {
            return;
        }
        if (adoptedStartTime.isBefore(earliestStart)
                || adoptedStartTime.isAfter(latestEnd)
                || Duration.between(adoptedStartTime, latestEnd).toMinutes() < durationMinutes) {
            throw new IllegalArgumentException("adopted placement must fit inside the flexible window");
        }
    }

    private static void requireValidWindow(LocalTime earliestStart, LocalTime latestEnd,
            int durationMinutes) {
        Objects.requireNonNull(earliestStart, "earliest start must not be null");
        Objects.requireNonNull(latestEnd, "latest end must not be null");
        if (!latestEnd.isAfter(earliestStart)) {
            throw new IllegalArgumentException("latest end must be after earliest start");
        }
        if (durationMinutes <= 0) {
            throw new IllegalArgumentException("duration must be positive");
        }
        if (durationMinutes > Duration.between(earliestStart, latestEnd).toMinutes()) {
            throw new IllegalArgumentException("duration must fit inside the flexible window");
        }
    }

    @Override
    public ScheduleType getScheduleType() {
        return ScheduleType.FLEXIBLE;
    }
}
