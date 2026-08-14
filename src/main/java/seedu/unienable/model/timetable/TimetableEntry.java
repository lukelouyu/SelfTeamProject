package seedu.unienable.model.timetable;

import java.time.LocalDate;
import java.time.LocalTime;

import seedu.unienable.model.enums.ActivityCategory;

/** Immutable projection of one activity for timetable display. */
public final class TimetableEntry {
    private final int id;
    private final String description;
    private final LocalDate date;
    private final LocalTime startTime;
    private final LocalTime endTime;
    private final int durationMinutes;
    private final TimetableEntryType type;
    private final boolean overlapping;
    private final boolean complete;
    private final ActivityCategory category;
    private final int energyRating;
    private final int sensoryRating;
    private final String topic;
    private final String note;

    /**
     * Creates one immutable timetable display entry.
     *
     * @param id permanent activity ID
     * @param description activity description
     * @param date activity date
     * @param startTime scheduled start (fixed or adopted flexible) or unscheduled-flexible-window start
     * @param endTime scheduled end (fixed or adopted flexible) or unscheduled-flexible-window end
     * @param durationMinutes required flexible duration, or zero for scheduled entries
     * @param type display-entry type
     * @param overlapping whether this scheduled entry overlaps another scheduled entry
     * @param complete whether the source activity is complete
     * @param category activity category
     * @param energyRating energy-demand rating value
     * @param sensoryRating sensory-load rating value
     * @param topic optional topic
     * @param note optional note
     */
    public TimetableEntry(int id, String description, LocalDate date, LocalTime startTime,
            LocalTime endTime, int durationMinutes, TimetableEntryType type, boolean overlapping,
            boolean complete, ActivityCategory category, int energyRating, int sensoryRating,
            String topic, String note) {
        this.id = id;
        this.description = description;
        this.date = date;
        this.startTime = startTime;
        this.endTime = endTime;
        this.durationMinutes = durationMinutes;
        this.type = type;
        this.overlapping = overlapping;
        this.complete = complete;
        this.category = category;
        this.energyRating = energyRating;
        this.sensoryRating = sensoryRating;
        this.topic = topic;
        this.note = note;
    }

    /** Returns the permanent activity ID. */
    public int getId() {
        return id;
    }

    /** Returns the activity description. */
    public String getDescription() {
        return description;
    }

    /** Returns the activity date. */
    public LocalDate getDate() {
        return date;
    }

    /** Returns the scheduled start (fixed or adopted flexible) or unscheduled-flexible-window start. */
    public LocalTime getStartTime() {
        return startTime;
    }

    /** Returns the scheduled end (fixed or adopted flexible) or unscheduled-flexible-window end. */
    public LocalTime getEndTime() {
        return endTime;
    }

    /** Returns the required flexible duration, or zero for scheduled entries. */
    public int getDurationMinutes() {
        return durationMinutes;
    }

    /** Returns the display-entry type. */
    public TimetableEntryType getType() {
        return type;
    }

    /** Returns whether this scheduled entry overlaps another scheduled entry. */
    public boolean isOverlapping() {
        return overlapping;
    }

    /** Returns whether the source activity is complete. */
    public boolean isComplete() {
        return complete;
    }

    /** Returns the activity category. */
    public ActivityCategory getCategory() {
        return category;
    }

    /** Returns the energy-demand rating value. */
    public int getEnergyRating() {
        return energyRating;
    }

    /** Returns the sensory-load rating value. */
    public int getSensoryRating() {
        return sensoryRating;
    }

    /** Returns the optional topic, or null. */
    public String getTopic() {
        return topic;
    }

    /** Returns the optional note, or null. */
    public String getNote() {
        return note;
    }
}
