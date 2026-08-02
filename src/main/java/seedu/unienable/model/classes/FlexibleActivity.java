package seedu.unienable.model.classes;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
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
        assert latestEnd.isAfter(earliestStart)
                : "FlexibleActivity constructed with latest end not after earliest start - every "
                        + "caller (parsers, storage) must validate this before constructing";
        assert durationMinutes <= Duration.between(earliestStart, latestEnd).toMinutes()
                : "FlexibleActivity constructed with a duration that does not fit the "
                        + "earliest/latest window - every caller must validate this before constructing";
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

    /** Sets the earliest allowed start time. */
    public void setEarliestStart(LocalTime earliestStart) {
        logger.log(Level.INFO, "Updated earliest start for activity [" + getId() + "].");
        this.earliestStart = earliestStart;
    }

    /** Returns the latest allowed end time. */
    public LocalTime getLatestEnd() {
        return latestEnd;
    }

    /** Sets the latest allowed end time. */
    public void setLatestEnd(LocalTime latestEnd) {
        logger.log(Level.INFO, "Updated latest end for activity [" + getId() + "].");
        this.latestEnd = latestEnd;
    }

    /** Returns the required duration in minutes. */
    public int getDurationMinutes() {
        return durationMinutes;
    }

    /** Sets the required duration in minutes. */
    public void setDurationMinutes(int durationMinutes) {
        logger.log(Level.INFO, "Updated duration for activity [" + getId() + "].");
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
        LocalTime adoptedEndTime = adoptedStartTime.plusMinutes(durationMinutes);
        if (adoptedStartTime.isBefore(earliestStart) || adoptedEndTime.isAfter(latestEnd)) {
            throw new IllegalArgumentException("adopted placement must fit inside the flexible window");
        }
    }

    @Override
    public ScheduleType getScheduleType() {
        return ScheduleType.FLEXIBLE;
    }
}
