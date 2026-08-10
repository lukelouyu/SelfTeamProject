package seedu.unienable.parser.activity;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Locale;
import java.util.Map;

import seedu.unienable.command.activity.crud.EditCommand;
import seedu.unienable.exception.DuplicateActivityException;
import seedu.unienable.exception.InvalidActivityException;
import seedu.unienable.exception.InvalidCommandException;
import seedu.unienable.exception.InvalidDateTimeException;
import seedu.unienable.exception.InvalidIndexException;
import seedu.unienable.exception.MissingInputException;
import seedu.unienable.logic.ActivityManager;
import seedu.unienable.logic.TopicManager;
import seedu.unienable.model.classes.Activity;
import seedu.unienable.model.classes.EnergyRating;
import seedu.unienable.model.classes.FixedActivity;
import seedu.unienable.model.classes.FlexibleActivity;
import seedu.unienable.model.classes.SensoryRating;
import seedu.unienable.model.enums.ActivityCategory;
import seedu.unienable.model.enums.ScheduleType;
import seedu.unienable.parser.common.DateTimeParser;
import seedu.unienable.parser.common.FieldParser;
import seedu.unienable.parser.common.RatingParser;

/** Parses the edit command's argument text into an EditCommand. */
class EditCommandParser {
    /**
     * Parses an edit command's argument text into an EditCommand. Any subset of the 13 editable
     * prefixes may be supplied, in any order; at least one is required. Changing type/ between
     * FIXED and FLEXIBLE requires supplying every timing field the new type needs, since a value
     * from the old type's timing fields cannot carry over.
     *
     * @param activityManager the manager holding the activity being edited
     * @param topicManager the manager used to validate that the activity's resulting topic
     *     (carried over or newly supplied) exists under its resulting category
     * @param now the current date and time, used to reject a supplied date/ earlier than today
     *     and, when date/ or the start-time marker is actively supplied and resolves to today, a
     *     start time at or before now
     * @param args the text after the "edit" command word, starting with the activity ID
     * @return the parsed EditCommand
     * @throws MissingInputException if no ID, no fields, or a required new-type timing field is
     *     missing
     * @throws InvalidCommandException if the ID is not a whole number, or type is neither FIXED
     *     nor FLEXIBLE
     * @throws InvalidIndexException if no activity has that ID, or the resulting topic does not
     *     exist under the resulting category
     * @throws InvalidActivityException if a field value fails validation
     * @throws InvalidDateTimeException if a supplied date is malformed, does not exist, or is
     *     before today; or a time value is invalid, or (when date/ or the start-time marker is
     *     actively supplied) the resulting start time is at or before now on today's date
     * @throws DuplicateActivityException if the resulting activity exactly duplicates another,
     *     or (for a FixedActivity) overlaps another fixed activity on the same date
     */
    EditCommand parse(ActivityManager activityManager, TopicManager topicManager, LocalDateTime now,
            String args)
            throws MissingInputException, InvalidCommandException, InvalidIndexException, InvalidActivityException,
            InvalidDateTimeException, DuplicateActivityException {
        String[] parts = args.trim().split("\\s+", 2);
        int id = parseEditId(parts[0]);
        String fieldsText = parts.length > 1 ? parts[1] : "";

        FieldParser.rejectUnrecognisedLeadingText(fieldsText, ActivityCommandParser.ALL_ACTIVITY_MARKERS);
        FieldParser.rejectDuplicateMarkers(fieldsText, ActivityCommandParser.ALL_ACTIVITY_MARKERS);
        Map<String, String> fields = FieldParser.extractPresentFields(fieldsText,
                ActivityCommandParser.ALL_ACTIVITY_MARKERS);
        if (fields.isEmpty()) {
            throw new MissingInputException("at least one field must be supplied.");
        }

        Activity old = activityManager.getById(id);
        String description = fields.getOrDefault("n/", old.getDescription());
        ActivityCategory category = fields.containsKey("c/")
                ? FieldParser.parseCategory(fields.get("c/")) : old.getCategory();
        LocalDate date = fields.containsKey("date/")
                ? DateTimeParser.parseNotBeforeDate(fields.get("date/"), now.toLocalDate()) : old.getDate();
        EnergyRating energy = fields.containsKey("energy/")
                ? RatingParser.parseEnergyRating(fields.get("energy/")) : old.getEnergyRating();
        SensoryRating sensory = fields.containsKey("sensory/")
                ? RatingParser.parseSensoryRating(fields.get("sensory/")) : old.getSensoryRating();
        String topic = fields.containsKey("topic/")
                ? ActivityCommandParser.blankToNull(fields.get("topic/")) : old.getTopic();
        String note = fields.containsKey("note/")
                ? ActivityCommandParser.blankToNull(fields.get("note/")) : old.getNote();

        FieldParser.validateNoDelimiter(description, "description");
        if (topic != null) {
            FieldParser.validateNoDelimiter(topic, "topic");
        }
        if (note != null) {
            FieldParser.validateNoDelimiter(note, "note");
        }
        ActivityCommandParser.validateTopicExists(topicManager, category, topic);

        ScheduleType oldType = old.getScheduleType();
        ScheduleType newType = fields.containsKey("type/") ? parseScheduleType(fields.get("type/")) : oldType;
        boolean typeChanged = newType != oldType;

        Activity newActivity = newType == ScheduleType.FIXED
                ? buildFixed(id, description, category, date, energy, sensory, topic, note, fields, old, typeChanged,
                        now)
                : buildFlexible(id, description, category, date, energy, sensory, topic, note, fields, old,
                        typeChanged, now);
        activityManager.checkNoConflicts(newActivity, id);

        if (old.isComplete()) {
            newActivity.mark();
        }
        return new EditCommand(activityManager, id, newActivity);
    }

    private FixedActivity buildFixed(int id, String description, ActivityCategory category, LocalDate date,
            EnergyRating energy, SensoryRating sensory, String topic, String note, Map<String, String> fields,
            Activity old, boolean typeChanged, LocalDateTime now)
            throws MissingInputException, InvalidActivityException, InvalidDateTimeException {
        LocalTime start = resolveRequiredTime(fields, "from/", typeChanged,
                typeChanged ? null : ((FixedActivity) old).getStartTime());
        LocalTime end = resolveRequiredTime(fields, "to/", typeChanged,
                typeChanged ? null : ((FixedActivity) old).getEndTime());
        if (!end.isAfter(start)) {
            throw new InvalidActivityException("end time must be later than start time.");
        }
        if (fields.containsKey("date/") || fields.containsKey("from/")) {
            DateTimeParser.requireNotPastIfToday(start, date, now);
        }
        return new FixedActivity(id, description, category, date, start, end, energy, sensory, topic, note);
    }

    private FlexibleActivity buildFlexible(int id, String description, ActivityCategory category, LocalDate date,
            EnergyRating energy, SensoryRating sensory, String topic, String note, Map<String, String> fields,
            Activity old, boolean typeChanged, LocalDateTime now)
            throws MissingInputException, InvalidActivityException, InvalidDateTimeException {
        LocalTime earliestStart = resolveRequiredTime(fields, "earliest/", typeChanged,
                typeChanged ? null : ((FlexibleActivity) old).getEarliestStart());
        LocalTime latestEnd = resolveRequiredTime(fields, "latest/", typeChanged,
                typeChanged ? null : ((FlexibleActivity) old).getLatestEnd());
        if (!latestEnd.isAfter(earliestStart)) {
            throw new InvalidActivityException("latest end time must be after earliest start time.");
        }
        if (fields.containsKey("date/") || fields.containsKey("earliest/")) {
            DateTimeParser.requireNotPastIfToday(earliestStart, date, now);
        }
        int durationMinutes;
        if (fields.containsKey("dur/")) {
            durationMinutes = ActivityCommandParser.parsePositiveInt(fields.get("dur/"), "dur");
        } else if (typeChanged) {
            throw new MissingInputException("dur/ is required when changing type to FLEXIBLE.");
        } else {
            durationMinutes = ((FlexibleActivity) old).getDurationMinutes();
        }
        ActivityCommandParser.validateDurationFitsWindow(earliestStart, latestEnd, durationMinutes);
        return new FlexibleActivity(id, description, category, date, earliestStart, latestEnd, durationMinutes,
                energy, sensory, topic, note);
    }

    private LocalTime resolveRequiredTime(Map<String, String> fields, String marker, boolean typeChanged,
            LocalTime fallbackIfSameType) throws MissingInputException, InvalidDateTimeException {
        if (fields.containsKey(marker)) {
            return DateTimeParser.parseTime(fields.get(marker));
        }
        if (typeChanged) {
            throw new MissingInputException(marker + " is required when changing type.");
        }
        return fallbackIfSameType;
    }

    private int parseEditId(String text) throws MissingInputException, InvalidCommandException {
        if (text.isEmpty()) {
            throw new MissingInputException("an activity ID is required.");
        }
        try {
            return Integer.parseInt(text);
        } catch (NumberFormatException e) {
            throw new InvalidCommandException("activity ID must be a whole number.");
        }
    }

    private ScheduleType parseScheduleType(String text) throws InvalidCommandException {
        try {
            return ScheduleType.valueOf(text.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw new InvalidCommandException("type must be FIXED or FLEXIBLE.");
        }
    }
}
