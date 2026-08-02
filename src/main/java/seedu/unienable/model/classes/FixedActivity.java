package seedu.unienable.model.classes;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.logging.Level;
import java.util.logging.Logger;

import seedu.unienable.model.enums.ActivityCategory;
import seedu.unienable.model.enums.ScheduleType;

/** An activity with a confirmed start and end time. */
public class FixedActivity extends Activity {
    private static final Logger logger = Logger.getLogger(FixedActivity.class.getName());

    private LocalTime startTime;
    private LocalTime endTime;

    /**
     * Creates a FixedActivity.
     *
     * @param id stable activity ID
     * @param description activity description
     * @param category fixed top-level category
     * @param date activity date
     * @param startTime confirmed start time
     * @param endTime confirmed end time
     * @param energyRating energy-demand rating
     * @param sensoryRating sensory-load rating
     * @param topic optional topic name, or null if none
     * @param note optional preparation note, or null if none
     */
    public FixedActivity(int id, String description, ActivityCategory category, LocalDate date,
            LocalTime startTime, LocalTime endTime, EnergyRating energyRating, SensoryRating sensoryRating,
            String topic, String note) {
        super(id, description, category, date, energyRating, sensoryRating, topic, note);
        assert endTime.isAfter(startTime)
                : "FixedActivity constructed with end time not after start time - every caller "
                        + "(parsers, storage) must validate this before constructing";
        this.startTime = startTime;
        this.endTime = endTime;
    }

    /** Returns the confirmed start time. */
    public LocalTime getStartTime() {
        return startTime;
    }

    /** Sets the confirmed start time. */
    public void setStartTime(LocalTime startTime) {
        logger.log(Level.INFO, "Updating start time for activity [" + getId() + "] from '"
                + this.startTime + "' to '" + startTime + "'.");
        this.startTime = startTime;
    }

    /** Returns the confirmed end time. */
    public LocalTime getEndTime() {
        return endTime;
    }

    /** Sets the confirmed end time. */
    public void setEndTime(LocalTime endTime) {
        logger.log(Level.INFO, "Updating end time for activity [" + getId() + "] from '"
                + this.endTime + "' to '" + endTime + "'.");
        this.endTime = endTime;
    }

    @Override
    public ScheduleType getScheduleType() {
        return ScheduleType.FIXED;
    }
}
