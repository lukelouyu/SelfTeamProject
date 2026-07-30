package seedu.unienable.parser;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import seedu.unienable.command.AddCommand;
import seedu.unienable.command.DeleteCommand;
import seedu.unienable.command.EditCommand;
import seedu.unienable.command.ListCommand;
import seedu.unienable.command.MarkCommand;
import seedu.unienable.command.UnmarkCommand;
import seedu.unienable.command.ViewCommand;
import seedu.unienable.exception.InvalidActivityException;
import seedu.unienable.exception.InvalidCommandException;
import seedu.unienable.exception.InvalidDateTimeException;
import seedu.unienable.exception.InvalidIndexException;
import seedu.unienable.exception.MissingInputException;
import seedu.unienable.logic.ActivityFilter;
import seedu.unienable.logic.ActivityManager;
import seedu.unienable.model.classes.Activity;
import seedu.unienable.model.classes.EnergyRating;
import seedu.unienable.model.classes.FixedActivity;
import seedu.unienable.model.classes.FlexibleActivity;
import seedu.unienable.model.classes.SensoryRating;
import seedu.unienable.model.enums.ActivityCategory;
import seedu.unienable.model.enums.ActivityOrder;
import seedu.unienable.model.enums.CompletionStatus;
import seedu.unienable.model.enums.ScheduleType;

/** Parses activity-related commands (add, list, find, edit, delete, mark, unmark, next) into Command objects. */
public class ActivityCommandParser {
    private static final String[] EDIT_MARKERS = {
        "n/", "c/", "date/", "type/", "from/", "to/", "earliest/", "latest/", "dur/",
        "energy/", "sensory/", "topic/", "note/"
    };
    private static final String[] LIST_MARKERS = { "view/", "status/", "c/", "topic/", "date/", "order/" };

    /**
     * Parses an add command's argument text into an AddCommand. Fields must appear in the order
     * documented in the User Guide.
     *
     * @param activityManager the manager the resulting command will add to
     * @param args the text after the "add" command word
     * @return the parsed AddCommand
     * @throws MissingInputException if a required field is missing
     * @throws InvalidActivityException if a field value fails validation
     * @throws InvalidDateTimeException if the date or a time value is invalid
     * @throws InvalidCommandException if type is neither FIXED nor FLEXIBLE
     */
    public AddCommand parseAdd(ActivityManager activityManager, String args)
            throws MissingInputException, InvalidActivityException, InvalidDateTimeException,
            InvalidCommandException {
        String description = requireField(args, "n/", "c/", "description");
        ActivityCategory category = parseCategory(requireField(args, "c/", "date/", "category"));
        LocalDate date = DateTimeParser.parseDate(requireField(args, "date/", "type/", "date"));
        String type = firstToken(requireField(args, "type/", null, "type"));

        int id = activityManager.getNextId();
        if ("FIXED".equalsIgnoreCase(type)) {
            return new AddCommand(activityManager, parseFixed(args, id, description, category, date));
        }
        if ("FLEXIBLE".equalsIgnoreCase(type)) {
            return new AddCommand(activityManager, parseFlexible(args, id, description, category, date));
        }
        throw new InvalidCommandException("type must be FIXED or FLEXIBLE.");
    }

    private FixedActivity parseFixed(String args, int id, String description, ActivityCategory category,
            LocalDate date) throws MissingInputException, InvalidActivityException, InvalidDateTimeException {
        LocalTime start = DateTimeParser.parseTime(requireField(args, "from/", "to/", "from"));
        LocalTime end = DateTimeParser.parseTime(requireField(args, "to/", "energy/", "to"));
        if (!end.isAfter(start)) {
            throw new InvalidActivityException("end time must be later than start time.");
        }
        CommonTail tail = parseCommonTail(args, "energy/");
        return new FixedActivity(id, description, category, date, start, end, tail.energy, tail.sensory,
                tail.topic, tail.note);
    }

    private FlexibleActivity parseFlexible(String args, int id, String description, ActivityCategory category,
            LocalDate date) throws MissingInputException, InvalidActivityException, InvalidDateTimeException {
        LocalTime earliestStart = DateTimeParser.parseTime(requireField(args, "earliest/", "latest/", "earliest"));
        LocalTime latestEnd = DateTimeParser.parseTime(requireField(args, "latest/", "dur/", "latest"));
        if (!latestEnd.isAfter(earliestStart)) {
            throw new InvalidActivityException("latest end time must be after earliest start time.");
        }
        int durationMinutes = parsePositiveInt(requireField(args, "dur/", "energy/", "dur"), "dur");
        CommonTail tail = parseCommonTail(args, "energy/");
        return new FlexibleActivity(id, description, category, date, earliestStart, latestEnd, durationMinutes,
                tail.energy, tail.sensory, tail.topic, tail.note);
    }

    private CommonTail parseCommonTail(String args, String energyMarker)
            throws MissingInputException, InvalidActivityException {
        EnergyRating energy = RatingParser.parseEnergyRating(requireField(args, energyMarker, "sensory/", "energy"));

        String sensoryEndMarker = firstPresentMarker(args, "sensory/", "topic/", "note/");
        SensoryRating sensory = RatingParser.parseSensoryRating(
                requireField(args, "sensory/", sensoryEndMarker, "sensory"));

        String topic = null;
        if (args.contains("topic/")) {
            String topicEndMarker = firstPresentMarker(args, "topic/", "note/");
            topic = FieldParser.extractField(args, "topic/", topicEndMarker);
        }
        String note = FieldParser.extractField(args, "note/", null);
        return new CommonTail(energy, sensory, topic, note);
    }

    /**
     * Parses a delete command's argument text into a DeleteCommand.
     *
     * @param activityManager the manager the resulting command will delete from
     * @param args the text after the "delete" command word
     * @return the parsed DeleteCommand
     * @throws MissingInputException if no ID is supplied
     * @throws InvalidCommandException if the supplied ID is not a whole number
     */
    public DeleteCommand parseDelete(ActivityManager activityManager, String args)
            throws MissingInputException, InvalidCommandException {
        return new DeleteCommand(activityManager, parseId(args));
    }

    /**
     * Parses a mark command's argument text into a MarkCommand.
     *
     * @param activityManager the manager the resulting command will mark in
     * @param args the text after the "mark" command word
     * @return the parsed MarkCommand
     * @throws MissingInputException if no ID is supplied
     * @throws InvalidCommandException if the supplied ID is not a whole number
     */
    public MarkCommand parseMark(ActivityManager activityManager, String args)
            throws MissingInputException, InvalidCommandException {
        return new MarkCommand(activityManager, parseId(args));
    }

    /**
     * Parses an unmark command's argument text into an UnmarkCommand.
     *
     * @param activityManager the manager the resulting command will unmark in
     * @param args the text after the "unmark" command word
     * @return the parsed UnmarkCommand
     * @throws MissingInputException if no ID is supplied
     * @throws InvalidCommandException if the supplied ID is not a whole number
     */
    public UnmarkCommand parseUnmark(ActivityManager activityManager, String args)
            throws MissingInputException, InvalidCommandException {
        return new UnmarkCommand(activityManager, parseId(args));
    }

    /**
     * Parses a view command's argument text into a ViewCommand.
     *
     * @param activityManager the manager the resulting command will read from
     * @param args the text after the "view" command word
     * @return the parsed ViewCommand
     * @throws MissingInputException if no ID is supplied
     * @throws InvalidCommandException if the supplied ID is not a whole number
     */
    public ViewCommand parseView(ActivityManager activityManager, String args)
            throws MissingInputException, InvalidCommandException {
        return new ViewCommand(activityManager, parseId(args));
    }

    /**
     * Parses a list command's argument text into a ListCommand. Every field is optional and may
     * appear in any order.
     *
     * @param activityManager the manager the resulting command will read from
     * @param args the text after the "list" command word
     * @return the parsed ListCommand
     * @throws InvalidActivityException if the category is invalid
     * @throws InvalidCommandException if status or order is invalid
     * @throws InvalidDateTimeException if the date is invalid
     */
    public ListCommand parseList(ActivityManager activityManager, String args)
            throws InvalidActivityException, InvalidCommandException, InvalidDateTimeException {
        Map<String, String> fields = extractPresentFields(args, LIST_MARKERS);

        boolean detail = "detail".equalsIgnoreCase(fields.get("view/"));
        CompletionStatus status = parseStatus(fields.get("status/"));
        ActivityCategory category = fields.containsKey("c/") ? parseCategory(fields.get("c/")) : null;
        String topic = fields.get("topic/");
        LocalDate date = fields.containsKey("date/") ? DateTimeParser.parseDate(fields.get("date/")) : null;
        ActivityOrder order = fields.containsKey("order/") ? parseActivityOrder(fields.get("order/")) : null;

        return new ListCommand(activityManager, new ActivityFilter(status, category, topic, date), order, detail);
    }

    private CompletionStatus parseStatus(String text) throws InvalidCommandException {
        if (text == null || "all".equalsIgnoreCase(text)) {
            return null;
        }
        if ("completed".equalsIgnoreCase(text)) {
            return CompletionStatus.COMPLETE;
        }
        if ("incomplete".equalsIgnoreCase(text)) {
            return CompletionStatus.INCOMPLETE;
        }
        throw new InvalidCommandException("status must be all, completed, or incomplete.");
    }

    private ActivityOrder parseActivityOrder(String text) throws InvalidCommandException {
        try {
            return ActivityOrder.valueOf(text.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new InvalidCommandException("order must be input, time, or chronological.");
        }
    }

    private int parseId(String args) throws MissingInputException, InvalidCommandException {
        String trimmed = args.trim();
        if (trimmed.isEmpty()) {
            throw new MissingInputException("an activity ID is required.");
        }
        try {
            return Integer.parseInt(trimmed);
        } catch (NumberFormatException e) {
            throw new InvalidCommandException("activity ID must be a whole number.");
        }
    }

    private String requireField(String args, String startMarker, String endMarker, String fieldName)
            throws MissingInputException {
        String value = FieldParser.extractField(args, startMarker, endMarker);
        if (value == null || value.isEmpty()) {
            throw new MissingInputException(fieldName + " is required.");
        }
        return value;
    }

    private ActivityCategory parseCategory(String text) throws InvalidActivityException {
        try {
            return ActivityCategory.valueOf(text.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new InvalidActivityException("category must be one of ACADEMIC, CCA, WORK_INTERNSHIP, OTHERS.");
        }
    }

    private int parsePositiveInt(String text, String fieldName) throws InvalidActivityException {
        try {
            int value = Integer.parseInt(text);
            if (value <= 0) {
                throw new NumberFormatException();
            }
            return value;
        } catch (NumberFormatException e) {
            throw new InvalidActivityException(fieldName + " must be a positive whole number of minutes.");
        }
    }

    private String firstToken(String text) {
        return text.trim().split("\\s+", 2)[0];
    }

    private String firstPresentMarker(String text, String afterMarker, String... candidates) {
        int searchFrom = text.indexOf(afterMarker);
        if (searchFrom == -1) {
            return null;
        }
        searchFrom += afterMarker.length();
        String best = null;
        int bestIndex = -1;
        for (String candidate : candidates) {
            int index = text.indexOf(candidate, searchFrom);
            if (index != -1 && (bestIndex == -1 || index < bestIndex)) {
                bestIndex = index;
                best = candidate;
            }
        }
        return best;
    }

    /**
     * Extracts every marker (from the given candidates) that is actually present in the text,
     * into a map of marker to its value, using each field's neighbouring present marker (by
     * position) as its end boundary. Unlike add's fixed field order, this supports an arbitrary
     * subset of markers in any order, as edit's format requires.
     *
     * @param text the text to extract fields from
     * @param markers every marker that could appear, e.g. "n/", "c/", "date/"
     * @return a map from each present marker to its trimmed value, in the order the markers
     *     appear in the text
     */
    Map<String, String> extractPresentFields(String text, String... markers) {
        List<String> present = new ArrayList<>();
        for (String marker : markers) {
            if (text.contains(marker)) {
                present.add(marker);
            }
        }
        present.sort(Comparator.comparingInt(text::indexOf));

        Map<String, String> result = new LinkedHashMap<>();
        for (int i = 0; i < present.size(); i++) {
            String marker = present.get(i);
            String endMarker = i + 1 < present.size() ? present.get(i + 1) : null;
            result.put(marker, FieldParser.extractField(text, marker, endMarker));
        }
        return result;
    }

    /**
     * Parses an edit command's argument text into an EditCommand. Any subset of the 13 editable
     * prefixes may be supplied, in any order; at least one is required. Changing type/ between
     * FIXED and FLEXIBLE requires supplying every timing field the new type needs, since a value
     * from the old type's timing fields cannot carry over.
     *
     * @param activityManager the manager holding the activity being edited
     * @param args the text after the "edit" command word, starting with the activity ID
     * @return the parsed EditCommand
     * @throws MissingInputException if no ID, no fields, or a required new-type timing field is
     *     missing
     * @throws InvalidCommandException if the ID is not a whole number, or type is neither FIXED
     *     nor FLEXIBLE
     * @throws InvalidIndexException if no activity has that ID
     * @throws InvalidActivityException if a field value fails validation
     * @throws InvalidDateTimeException if a date or time value is invalid
     */
    public EditCommand parseEdit(ActivityManager activityManager, String args)
            throws MissingInputException, InvalidCommandException, InvalidIndexException, InvalidActivityException,
            InvalidDateTimeException {
        String[] parts = args.trim().split("\\s+", 2);
        int id = parseEditId(parts[0]);
        String fieldsText = parts.length > 1 ? parts[1] : "";

        Map<String, String> fields = extractPresentFields(fieldsText, EDIT_MARKERS);
        if (fields.isEmpty()) {
            throw new MissingInputException("at least one field must be supplied.");
        }

        Activity old = activityManager.getById(id);
        String description = fields.getOrDefault("n/", old.getDescription());
        ActivityCategory category = fields.containsKey("c/") ? parseCategory(fields.get("c/")) : old.getCategory();
        LocalDate date = fields.containsKey("date/") ? DateTimeParser.parseDate(fields.get("date/")) : old.getDate();
        EnergyRating energy = fields.containsKey("energy/")
                ? RatingParser.parseEnergyRating(fields.get("energy/")) : old.getEnergyRating();
        SensoryRating sensory = fields.containsKey("sensory/")
                ? RatingParser.parseSensoryRating(fields.get("sensory/")) : old.getSensoryRating();
        String topic = fields.getOrDefault("topic/", old.getTopic());
        String note = fields.getOrDefault("note/", old.getNote());

        ScheduleType oldType = old.getScheduleType();
        ScheduleType newType = fields.containsKey("type/") ? parseScheduleType(fields.get("type/")) : oldType;
        boolean typeChanged = newType != oldType;

        Activity newActivity = newType == ScheduleType.FIXED
                ? buildFixed(id, description, category, date, energy, sensory, topic, note, fields, old, typeChanged)
                : buildFlexible(id, description, category, date, energy, sensory, topic, note, fields, old,
                        typeChanged);

        if (old.isComplete()) {
            newActivity.mark();
        }
        return new EditCommand(activityManager, id, newActivity);
    }

    private FixedActivity buildFixed(int id, String description, ActivityCategory category, LocalDate date,
            EnergyRating energy, SensoryRating sensory, String topic, String note, Map<String, String> fields,
            Activity old, boolean typeChanged)
            throws MissingInputException, InvalidActivityException, InvalidDateTimeException {
        LocalTime start = resolveRequiredTime(fields, "from/", typeChanged,
                typeChanged ? null : ((FixedActivity) old).getStartTime());
        LocalTime end = resolveRequiredTime(fields, "to/", typeChanged,
                typeChanged ? null : ((FixedActivity) old).getEndTime());
        if (!end.isAfter(start)) {
            throw new InvalidActivityException("end time must be later than start time.");
        }
        return new FixedActivity(id, description, category, date, start, end, energy, sensory, topic, note);
    }

    private FlexibleActivity buildFlexible(int id, String description, ActivityCategory category, LocalDate date,
            EnergyRating energy, SensoryRating sensory, String topic, String note, Map<String, String> fields,
            Activity old, boolean typeChanged)
            throws MissingInputException, InvalidActivityException, InvalidDateTimeException {
        LocalTime earliestStart = resolveRequiredTime(fields, "earliest/", typeChanged,
                typeChanged ? null : ((FlexibleActivity) old).getEarliestStart());
        LocalTime latestEnd = resolveRequiredTime(fields, "latest/", typeChanged,
                typeChanged ? null : ((FlexibleActivity) old).getLatestEnd());
        if (!latestEnd.isAfter(earliestStart)) {
            throw new InvalidActivityException("latest end time must be after earliest start time.");
        }
        int durationMinutes;
        if (fields.containsKey("dur/")) {
            durationMinutes = parsePositiveInt(fields.get("dur/"), "dur");
        } else if (typeChanged) {
            throw new MissingInputException("dur/ is required when changing type to FLEXIBLE.");
        } else {
            durationMinutes = ((FlexibleActivity) old).getDurationMinutes();
        }
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
            return ScheduleType.valueOf(text.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new InvalidCommandException("type must be FIXED or FLEXIBLE.");
        }
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
