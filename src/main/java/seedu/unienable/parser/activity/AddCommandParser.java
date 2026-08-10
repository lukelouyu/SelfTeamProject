package seedu.unienable.parser.activity;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

import seedu.unienable.command.activity.crud.AddCommand;
import seedu.unienable.exception.InvalidActivityException;
import seedu.unienable.exception.InvalidCommandException;
import seedu.unienable.exception.InvalidDateTimeException;
import seedu.unienable.exception.InvalidIndexException;
import seedu.unienable.exception.MissingInputException;
import seedu.unienable.logic.ActivityManager;
import seedu.unienable.logic.TopicManager;
import seedu.unienable.model.classes.EnergyRating;
import seedu.unienable.model.classes.FixedActivity;
import seedu.unienable.model.classes.FlexibleActivity;
import seedu.unienable.model.classes.SensoryRating;
import seedu.unienable.model.enums.ActivityCategory;
import seedu.unienable.parser.common.DateTimeParser;
import seedu.unienable.parser.common.FieldParser;
import seedu.unienable.parser.common.RatingParser;

/** Parses the add command's argument text into an AddCommand. */
class AddCommandParser {
    /**
     * Parses an add command's argument text into an AddCommand. Fields must appear in the order
     * documented in the User Guide.
     *
     * @param activityManager the manager the resulting command will add to
     * @param topicManager the manager used to validate that a supplied topic/ already exists
     *     under the activity's category
     * @param now the current date and time, used to reject a date/ earlier than today and, for
     *     today, a start time at or before now
     * @param args the text after the "add" command word
     * @return the parsed AddCommand
     * @throws MissingInputException if a required field is missing
     * @throws InvalidActivityException if a field value fails validation
     * @throws InvalidDateTimeException if the date is malformed, does not exist, or is before
     *     today; or a time value is invalid, or (for today) the start time is at or before now
     * @throws InvalidCommandException if type is neither FIXED nor FLEXIBLE
     * @throws InvalidIndexException if topic/ does not exist under the category
     */
    AddCommand parse(ActivityManager activityManager, TopicManager topicManager, LocalDateTime now,
            String args)
            throws MissingInputException, InvalidActivityException, InvalidDateTimeException,
            InvalidCommandException, InvalidIndexException {
        FieldParser.rejectUnrecognisedLeadingText(args, ActivityCommandParser.ALL_ACTIVITY_MARKERS);
        FieldParser.rejectDuplicateMarkers(args, ActivityCommandParser.ALL_ACTIVITY_MARKERS);
        String description = requireField(args, "n/", "c/", "description", "category");
        FieldParser.validateNoDelimiter(description, "description");
        ActivityCategory category = FieldParser.parseCategory(requireField(args, "c/", "date/", "category", "date"));
        LocalDate date = DateTimeParser.parseNotBeforeDate(requireField(args, "date/", "type/", "date", "type"),
                now.toLocalDate());
        String typeEndMarker = firstPresentMarker(args, "type/", "from/", "earliest/");
        String type = FieldParser.requireField(args, "type/", typeEndMarker, "type");

        int id = activityManager.getNextId();
        if ("FIXED".equalsIgnoreCase(type)) {
            return new AddCommand(activityManager,
                    parseFixed(args, id, description, category, date, now, topicManager));
        }
        if ("FLEXIBLE".equalsIgnoreCase(type)) {
            return new AddCommand(activityManager,
                    parseFlexible(args, id, description, category, date, now, topicManager));
        }
        throw new InvalidCommandException("type must be FIXED or FLEXIBLE.");
    }

    private FixedActivity parseFixed(String args, int id, String description, ActivityCategory category,
            LocalDate date, LocalDateTime now, TopicManager topicManager)
            throws MissingInputException, InvalidActivityException, InvalidDateTimeException, InvalidIndexException {
        LocalTime start = DateTimeParser.parseNotBeforeNow(
                requireField(args, "from/", "to/", "from", "to"), date, now);
        LocalTime end = DateTimeParser.parseTime(requireField(args, "to/", "energy/", "to", "energy"));
        if (!end.isAfter(start)) {
            throw new InvalidActivityException("end time must be later than start time.");
        }
        CommonTail tail = parseCommonTail(args, "energy/");
        ActivityCommandParser.validateTopicExists(topicManager, category, tail.topic);
        return new FixedActivity(id, description, category, date, start, end, tail.energy, tail.sensory,
                tail.topic, tail.note);
    }

    private FlexibleActivity parseFlexible(String args, int id, String description, ActivityCategory category,
            LocalDate date, LocalDateTime now, TopicManager topicManager)
            throws MissingInputException, InvalidActivityException, InvalidDateTimeException, InvalidIndexException {
        LocalTime earliestStart = DateTimeParser.parseNotBeforeNow(
                requireField(args, "earliest/", "latest/", "earliest", "latest"), date, now);
        LocalTime latestEnd = DateTimeParser.parseTime(requireField(args, "latest/", "dur/", "latest", "dur"));
        if (!latestEnd.isAfter(earliestStart)) {
            throw new InvalidActivityException("latest end time must be after earliest start time.");
        }
        int durationMinutes = ActivityCommandParser.parsePositiveInt(
                requireField(args, "dur/", "energy/", "dur", "energy"), "dur");
        ActivityCommandParser.validateDurationFitsWindow(earliestStart, latestEnd, durationMinutes);
        CommonTail tail = parseCommonTail(args, "energy/");
        ActivityCommandParser.validateTopicExists(topicManager, category, tail.topic);
        return new FlexibleActivity(id, description, category, date, earliestStart, latestEnd, durationMinutes,
                tail.energy, tail.sensory, tail.topic, tail.note);
    }

    private CommonTail parseCommonTail(String args, String energyMarker)
            throws MissingInputException, InvalidActivityException {
        EnergyRating energy = RatingParser.parseEnergyRating(
                requireField(args, energyMarker, "sensory/", "energy", "sensory"));

        String sensoryEndMarker = firstPresentMarker(args, "sensory/", "topic/", "note/");
        SensoryRating sensory = RatingParser.parseSensoryRating(
                FieldParser.requireField(args, "sensory/", sensoryEndMarker, "sensory"));

        String topic = null;
        if (FieldParser.indexOfMarker(args, "topic/", 0) != -1) {
            String topicEndMarker = firstPresentMarker(args, "topic/", "note/");
            topic = ActivityCommandParser.blankToNull(FieldParser.extractField(args, "topic/", topicEndMarker));
        }
        String note = ActivityCommandParser.blankToNull(FieldParser.extractField(args, "note/", null));
        if (topic != null) {
            FieldParser.validateNoDelimiter(topic, "topic");
        }
        if (note != null) {
            FieldParser.validateNoDelimiter(note, "note");
        }
        return new CommonTail(energy, sensory, topic, note);
    }

    /**
     * Like {@link FieldParser#requireField(String, String, String, String)}, but for a start
     * marker whose end marker is itself always a required field at a statically-known position in
     * the grammar (as opposed to a marker whose end boundary can legitimately vary or be absent,
     * e.g. an optional trailing field). Verifies endMarker is actually present before extracting
     * startMarker's value.
     *
     * <p>Without this, a genuinely missing endMarker lets startMarker's extraction silently
     * swallow the rest of the command text - {@link FieldParser#extractField} reads to the end of
     * input whenever its end marker isn't found - producing a misleading validation failure on
     * startMarker's now-oversized value instead of correctly reporting that endMarker itself is
     * missing.
     *
     * @param args the full argument text
     * @param startMarker the marker just before the value
     * @param endMarker the marker required to end the value
     * @param fieldName startMarker's field name, used if startMarker itself is missing
     * @param endMarkerFieldName endMarker's field name, used if endMarker is missing
     * @return the trimmed value between startMarker and endMarker
     * @throws MissingInputException if endMarker is absent, or startMarker is absent/blank
     */
    private String requireField(String args, String startMarker, String endMarker, String fieldName,
            String endMarkerFieldName) throws MissingInputException {
        if (FieldParser.indexOfMarker(args, endMarker, 0) == -1) {
            throw new MissingInputException(endMarkerFieldName + " is required.");
        }
        return FieldParser.requireField(args, startMarker, endMarker, fieldName);
    }

    private String firstPresentMarker(String text, String afterMarker, String... candidates) {
        int searchFrom = FieldParser.indexOfMarker(text, afterMarker, 0);
        if (searchFrom == -1) {
            return null;
        }
        searchFrom += afterMarker.length();
        String best = null;
        int bestIndex = -1;
        for (String candidate : candidates) {
            int index = FieldParser.indexOfMarker(text, candidate, searchFrom);
            if (index != -1 && (bestIndex == -1 || index < bestIndex)) {
                bestIndex = index;
                best = candidate;
            }
        }
        return best;
    }

    private static final class CommonTail {
        private final EnergyRating energy;
        private final SensoryRating sensory;
        private final String topic;
        private final String note;

        private CommonTail(EnergyRating energy, SensoryRating sensory, String topic, String note) {
            this.energy = energy;
            this.sensory = sensory;
            this.topic = topic;
            this.note = note;
        }
    }
}
