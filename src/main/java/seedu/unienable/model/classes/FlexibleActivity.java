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

    @Override
    public ScheduleType getScheduleType() {
        return ScheduleType.FLEXIBLE;
    }
}
