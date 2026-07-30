package seedu.unienable.model.classes;

import java.time.LocalDate;
import java.util.logging.Level;
import java.util.logging.Logger;

import seedu.unienable.model.enums.ActivityCategory;
import seedu.unienable.model.enums.CompletionStatus;
import seedu.unienable.model.enums.ScheduleType;

/** Base type for all itinerary activities, holding the fields shared by every scheduling type. */
public abstract class Activity {
    private static final Logger logger = Logger.getLogger(Activity.class.getName());

    private final int id;
    private String description;
    private ActivityCategory category;
    private LocalDate date;
    private EnergyRating energyRating;
    private SensoryRating sensoryRating;
    private String topic;
    private String note;
    private CompletionStatus status;

    /**
     * Creates an Activity with the fields shared by every scheduling type.
     *
     * @param id stable activity ID
     * @param description activity description
     * @param category fixed top-level category
     * @param date activity date
     * @param energyRating energy-demand rating
     * @param sensoryRating sensory-load rating
     * @param topic optional topic name, or null if none
     * @param note optional preparation note, or null if none
     */
    protected Activity(int id, String description, ActivityCategory category, LocalDate date,
            EnergyRating energyRating, SensoryRating sensoryRating, String topic, String note) {
        this.id = id;
        this.description = description;
        this.category = category;
        this.date = date;
        this.energyRating = energyRating;
        this.sensoryRating = sensoryRating;
        this.topic = topic;
        this.note = note;
        this.status = CompletionStatus.INCOMPLETE;
    }

    /** Returns whether this activity has a confirmed time or a flexible window. */
    public abstract ScheduleType getScheduleType();

    public int getId() {
        return id;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        logger.log(Level.INFO, "Updating description for activity [" + id + "] from '"
                + this.description + "' to '" + description + "'.");
        this.description = description;
    }

    public ActivityCategory getCategory() {
        return category;
    }

    public void setCategory(ActivityCategory category) {
        logger.log(Level.INFO, "Updating category for activity [" + id + "] from '"
                + this.category + "' to '" + category + "'.");
        this.category = category;
    }

    public LocalDate getDate() {
        return date;
    }

    public void setDate(LocalDate date) {
        logger.log(Level.INFO, "Updating date for activity [" + id + "] from '"
                + this.date + "' to '" + date + "'.");
        this.date = date;
    }

    public EnergyRating getEnergyRating() {
        return energyRating;
    }

    public void setEnergyRating(EnergyRating energyRating) {
        logger.log(Level.INFO, "Updating energy rating for activity [" + id + "] from '"
                + this.energyRating + "' to '" + energyRating + "'.");
        this.energyRating = energyRating;
    }

    public SensoryRating getSensoryRating() {
        return sensoryRating;
    }

    public void setSensoryRating(SensoryRating sensoryRating) {
        logger.log(Level.INFO, "Updating sensory rating for activity [" + id + "] from '"
                + this.sensoryRating + "' to '" + sensoryRating + "'.");
        this.sensoryRating = sensoryRating;
    }

    public String getTopic() {
        return topic;
    }

    public void setTopic(String topic) {
        logger.log(Level.INFO, "Updating topic for activity [" + id + "] from '"
                + this.topic + "' to '" + topic + "'.");
        this.topic = topic;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        logger.log(Level.INFO, "Updating note for activity [" + id + "] from '"
                + this.note + "' to '" + note + "'.");
        this.note = note;
    }

    public CompletionStatus getStatus() {
        return status;
    }

    public boolean isComplete() {
        return status == CompletionStatus.COMPLETE;
    }

    /** Marks this activity as completed. */
    public void mark() {
        logger.log(Level.INFO, "Marking activity [" + id + "] as complete.");
        status = CompletionStatus.COMPLETE;
    }

    /** Marks this activity as incomplete. */
    public void unmark() {
        logger.log(Level.INFO, "Marking activity [" + id + "] as incomplete.");
        status = CompletionStatus.INCOMPLETE;
    }
}
