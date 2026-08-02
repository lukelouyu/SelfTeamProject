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

    /** Returns the stable activity ID. */
    public int getId() {
        return id;
    }

    /** Returns the activity description. */
    public String getDescription() {
        return description;
    }

    /** Sets the activity description. */
    public void setDescription(String description) {
        logFieldUpdate("description");
        this.description = description;
    }

    /** Returns the fixed top-level category. */
    public ActivityCategory getCategory() {
        return category;
    }

    /** Sets the fixed top-level category. */
    public void setCategory(ActivityCategory category) {
        logFieldUpdate("category");
        this.category = category;
    }

    /** Returns the activity date. */
    public LocalDate getDate() {
        return date;
    }

    /** Sets the activity date. */
    public void setDate(LocalDate date) {
        logFieldUpdate("date");
        this.date = date;
    }

    /** Returns the energy-demand rating. */
    public EnergyRating getEnergyRating() {
        return energyRating;
    }

    /** Sets the energy-demand rating. */
    public void setEnergyRating(EnergyRating energyRating) {
        logFieldUpdate("energy rating");
        this.energyRating = energyRating;
    }

    /** Returns the sensory-load rating. */
    public SensoryRating getSensoryRating() {
        return sensoryRating;
    }

    /** Sets the sensory-load rating. */
    public void setSensoryRating(SensoryRating sensoryRating) {
        logFieldUpdate("sensory rating");
        this.sensoryRating = sensoryRating;
    }

    /** Returns the optional topic name, or null if none. */
    public String getTopic() {
        return topic;
    }

    /** Sets the optional topic name, or null to clear it. */
    public void setTopic(String topic) {
        logFieldUpdate("topic");
        this.topic = topic;
    }

    /** Returns the optional preparation note, or null if none. */
    public String getNote() {
        return note;
    }

    /** Sets the optional preparation note, or null to clear it. */
    public void setNote(String note) {
        logFieldUpdate("note");
        this.note = note;
    }

    /**
     * Logs that a field on this activity was updated, identifying it by ID and field name only -
     * not the old/new values, which may be arbitrary user-entered text (description, note).
     *
     * @param fieldName the name of the field that changed
     */
    private void logFieldUpdate(String fieldName) {
        logger.log(Level.INFO, "Updated " + fieldName + " for activity [" + id + "].");
    }

    /** Returns the completion status. */
    public CompletionStatus getStatus() {
        return status;
    }

    /** Returns whether this activity is marked complete. */
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
