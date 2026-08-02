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

    public int getId() {
        return id;
    }

    public String getDescription() {
        return description;
    }

    public LocalDate getDate() {
        return date;
    }

    public LocalTime getStartTime() {
        return startTime;
    }

    public LocalTime getEndTime() {
        return endTime;
    }

    public int getDurationMinutes() {
        return durationMinutes;
    }

    public TimetableEntryType getType() {
        return type;
    }

    public boolean isOverlapping() {
        return overlapping;
    }

    public boolean isComplete() {
        return complete;
    }

    public ActivityCategory getCategory() {
        return category;
    }

    public int getEnergyRating() {
        return energyRating;
    }

    public int getSensoryRating() {
        return sensoryRating;
    }

    public String getTopic() {
        return topic;
    }

    public String getNote() {
        return note;
    }
}
