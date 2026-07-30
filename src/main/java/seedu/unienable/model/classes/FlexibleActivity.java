package seedu.unienable.model.classes;

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
        this.earliestStart = earliestStart;
        this.latestEnd = latestEnd;
        this.durationMinutes = durationMinutes;
    }

    public LocalTime getEarliestStart() {
        return earliestStart;
    }

    public void setEarliestStart(LocalTime earliestStart) {
        logger.log(Level.INFO, "Updating earliest start for activity [" + getId() + "] from '"
                + this.earliestStart + "' to '" + earliestStart + "'.");
        this.earliestStart = earliestStart;
    }

    public LocalTime getLatestEnd() {
        return latestEnd;
    }

    public void setLatestEnd(LocalTime latestEnd) {
        logger.log(Level.INFO, "Updating latest end for activity [" + getId() + "] from '"
                + this.latestEnd + "' to '" + latestEnd + "'.");
        this.latestEnd = latestEnd;
    }

    public int getDurationMinutes() {
        return durationMinutes;
    }

    public void setDurationMinutes(int durationMinutes) {
        logger.log(Level.INFO, "Updating duration for activity [" + getId() + "] from '"
                + this.durationMinutes + "' to '" + durationMinutes + "'.");
        this.durationMinutes = durationMinutes;
    }

    @Override
    public ScheduleType getScheduleType() {
        return ScheduleType.FLEXIBLE;
    }
}
