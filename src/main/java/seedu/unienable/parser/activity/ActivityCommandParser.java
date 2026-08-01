package seedu.unienable.parser.activity;

import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import seedu.unienable.command.activity.AddCommand;
import seedu.unienable.command.Command;
import seedu.unienable.command.activity.DeleteCommand;
import seedu.unienable.command.activity.EditCommand;
import seedu.unienable.command.activity.FindCommand;
import seedu.unienable.command.activity.ListCommand;
import seedu.unienable.command.activity.MarkCommand;
import seedu.unienable.command.activity.NextCommand;
import seedu.unienable.command.activity.OrderSetCommand;
import seedu.unienable.command.activity.OrderViewCommand;
import seedu.unienable.command.activity.UnmarkCommand;
import seedu.unienable.command.activity.ViewCommand;
import seedu.unienable.exception.DuplicateActivityException;
import seedu.unienable.exception.InvalidActivityException;
import seedu.unienable.exception.InvalidCommandException;
import seedu.unienable.exception.InvalidDateTimeException;
import seedu.unienable.exception.InvalidIndexException;
import seedu.unienable.exception.MissingInputException;
import seedu.unienable.logic.ActivityFilter;
import seedu.unienable.logic.ActivityManager;
import seedu.unienable.logic.TopicManager;
import seedu.unienable.model.classes.Activity;
import seedu.unienable.model.classes.EnergyRating;
import seedu.unienable.model.classes.FixedActivity;
import seedu.unienable.model.classes.FlexibleActivity;
import seedu.unienable.model.classes.SensoryRating;
import seedu.unienable.model.enums.ActivityCategory;
import seedu.unienable.model.enums.ActivityOrder;
import seedu.unienable.model.enums.CompletionStatus;
import seedu.unienable.model.enums.ScheduleType;
import seedu.unienable.parser.common.DateTimeParser;
import seedu.unienable.parser.common.FieldParser;
import seedu.unienable.parser.common.RatingParser;

/** Parses activity-related commands (add, list, find, edit, delete, mark, unmark, next) into Command objects. */
public class ActivityCommandParser {
    private static final String[] EDIT_MARKERS = {
        "n/", "c/", "date/", "type/", "from/", "to/", "earliest/", "latest/", "dur/",
        "energy/", "sensory/", "topic/", "note/"
    };
    private static final String[] LIST_MARKERS = { "view/", "status/", "c/", "topic/", "date/", "order/" };
    private static final String[] FIND_MARKERS = { "k/", "c/", "topic/", "date/", "order/" };

    /**
     * Parses an add command's argument text into an AddCommand. Fields must appear in the order
     * documented in the User Guide.
     *
     * @param activityManager the manager the resulting command will add to
     * @param topicManager the manager used to validate that a supplied topic/ already exists
     *     under the activity's category
     * @param args the text after the "add" command word
     * @return the parsed AddCommand
     * @throws MissingInputException if a required field is missing
     * @throws InvalidActivityException if a field value fails validation
     * @throws InvalidDateTimeException if the date or a time value is invalid
     * @throws InvalidCommandException if type is neither FIXED nor FLEXIBLE
     * @throws InvalidIndexException if topic/ does not exist under the category
     */
    public AddCommand parseAdd(ActivityManager activityManager, TopicManager topicManager, String args)
            throws MissingInputException, InvalidActivityException, InvalidDateTimeException,
            InvalidCommandException, InvalidIndexException {
        rejectUnrecognisedLeadingText(args, EDIT_MARKERS);
        String description = requireField(args, "n/", "c/", "description");
        validateNoDelimiter(description, "description");
        ActivityCategory category = parseCategory(requireField(args, "c/", "date/", "category"));
        LocalDate date = DateTimeParser.parseDate(requireField(args, "date/", "type/", "date"));
        String typeEndMarker = firstPresentMarker(args, "type/", "from/", "earliest/");
        String type = requireField(args, "type/", typeEndMarker, "type");

        int id = activityManager.getNextId();
        if ("FIXED".equalsIgnoreCase(type)) {
            return new AddCommand(activityManager,
                    parseFixed(args, id, description, category, date, topicManager));
        }
        if ("FLEXIBLE".equalsIgnoreCase(type)) {
            return new AddCommand(activityManager,
                    parseFlexible(args, id, description, category, date, topicManager));
        }
        throw new InvalidCommandException("type must be FIXED or FLEXIBLE.");
    }

    private FixedActivity parseFixed(String args, int id, String description, ActivityCategory category,
            LocalDate date, TopicManager topicManager)
            throws MissingInputException, InvalidActivityException, InvalidDateTimeException, InvalidIndexException {
        LocalTime start = DateTimeParser.parseTime(requireField(args, "from/", "to/", "from"));
        LocalTime end = DateTimeParser.parseTime(requireField(args, "to/", "energy/", "to"));
        if (!end.isAfter(start)) {
            throw new InvalidActivityException("end time must be later than start time.");
        }
        CommonTail tail = parseCommonTail(args, "energy/");
        validateTopicExists(topicManager, category, tail.topic);
        return new FixedActivity(id, description, category, date, start, end, tail.energy, tail.sensory,
                tail.topic, tail.note);
    }

    private FlexibleActivity parseFlexible(String args, int id, String description, ActivityCategory category,
            LocalDate date, TopicManager topicManager)
            throws MissingInputException, InvalidActivityException, InvalidDateTimeException, InvalidIndexException {
        LocalTime earliestStart = DateTimeParser.parseTime(requireField(args, "earliest/", "latest/", "earliest"));
        LocalTime latestEnd = DateTimeParser.parseTime(requireField(args, "latest/", "dur/", "latest"));
        if (!latestEnd.isAfter(earliestStart)) {
            throw new InvalidActivityException("latest end time must be after earliest start time.");
        }
        int durationMinutes = parsePositiveInt(requireField(args, "dur/", "energy/", "dur"), "dur");
        validateDurationFitsWindow(earliestStart, latestEnd, durationMinutes);
        CommonTail tail = parseCommonTail(args, "energy/");
        validateTopicExists(topicManager, category, tail.topic);
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
        if (FieldParser.indexOfMarker(args, "topic/", 0) != -1) {
            String topicEndMarker = firstPresentMarker(args, "topic/", "note/");
            topic = blankToNull(FieldParser.extractField(args, "topic/", topicEndMarker));
        }
        String note = blankToNull(FieldParser.extractField(args, "note/", null));
        if (topic != null) {
            validateNoDelimiter(topic, "topic");
        }
        if (note != null) {
            validateNoDelimiter(note, "note");
        }
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
     * Parses a list command's argument text into a ListCommand. Every marker field is optional
     * and may appear in any order; an optional relative-date phrase ("today", "tomorrow", or
     * "this week") may additionally appear at the very start of the text, before any markers.
     *
     * @param activityManager the manager the resulting command will read from
     * @param now the current date and time, used to resolve "today"/"tomorrow"/"this week"
     * @param args the text after the "list" command word
     * @return the parsed ListCommand
     * @throws InvalidActivityException if the category is invalid
     * @throws InvalidCommandException if status or order is invalid, a relative-date phrase is
     *     unrecognised, a relative-date phrase is combined with date/ or another relative-date
     *     phrase, or unrecognised text follows a relative-date phrase
     * @throws InvalidDateTimeException if the date is invalid
     */
    public ListCommand parseList(ActivityManager activityManager, LocalDateTime now, String args)
            throws InvalidActivityException, InvalidCommandException, InvalidDateTimeException {
        RelativeDateAndRemainder parsed = extractRelativeDate(now, args);
        Map<String, String> fields = extractPresentFields(parsed.remainder, LIST_MARKERS);
        if (parsed.hasRelativeDate() && fields.containsKey("date/")) {
            throw new InvalidCommandException(
                    "date/ cannot be combined with today, tomorrow, or this week.");
        }

        boolean detail = parseViewMode(fields.get("view/"));
        CompletionStatus status = parseStatus(fields.get("status/"));
        ActivityCategory category = fields.containsKey("c/") ? parseCategory(fields.get("c/")) : null;
        String topic = blankToNull(fields.get("topic/"));
        LocalDate date = fields.containsKey("date/") ? DateTimeParser.parseDate(fields.get("date/")) : parsed.date;
        ActivityOrder order = fields.containsKey("order/") ? parseActivityOrder(fields.get("order/")) : null;

        ActivityFilter filter = new ActivityFilter(status, category, topic, date, parsed.dateFrom, parsed.dateTo);
        return new ListCommand(activityManager, filter, order, detail);
    }

    /**
     * Consumes an optional leading relative-date phrase ("today", "tomorrow", or "this week")
     * from a list command's argument text, resolving it against now. Text that already starts
     * with a recognised list marker is left untouched (no relative-date phrase is possible
     * there), preserving every existing marker-only usage exactly as before.
     *
     * @param now the current date and time
     * @param args the full text after the "list" command word
     * @return the resolved relative date (if any) plus the remaining text to parse as markers
     * @throws InvalidCommandException if the leading text is not blank, does not start with a
     *     known marker, and is not a recognised relative-date phrase; or if a relative-date
     *     phrase is followed by unrecognised text
     */
    private RelativeDateAndRemainder extractRelativeDate(LocalDateTime now, String args)
            throws InvalidCommandException {
        String trimmed = args.trim();
        if (trimmed.isEmpty() || startsWithKnownMarker(trimmed)) {
            return new RelativeDateAndRemainder(null, null, null, trimmed);
        }

        String[] words = trimmed.split("\\s+", 3);
        LocalDate today = now.toLocalDate();
        RelativeDateAndRemainder result;
        if ("today".equalsIgnoreCase(words[0])) {
            result = new RelativeDateAndRemainder(today, null, null, remainderAfter(trimmed, words, 1));
        } else if ("tomorrow".equalsIgnoreCase(words[0])) {
            result = new RelativeDateAndRemainder(today.plusDays(1), null, null, remainderAfter(trimmed, words, 1));
        } else if ("this".equalsIgnoreCase(words[0])) {
            if (words.length < 2 || !"week".equalsIgnoreCase(words[1])) {
                throw new InvalidCommandException(
                        "Unknown list option \"this " + (words.length > 1 ? words[1] : "")
                                + "\"; only \"this week\" is supported.");
            }
            LocalDate monday = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
            LocalDate sunday = today.with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY));
            result = new RelativeDateAndRemainder(null, monday, sunday, remainderAfter(trimmed, words, 2));
        } else {
            throw new InvalidCommandException("Unknown list option \"" + words[0] + "\".");
        }

        if (!result.remainder.isEmpty() && !startsWithKnownMarker(result.remainder)) {
            throw new InvalidCommandException(
                    "Unknown list option \"" + result.remainder.split("\\s+", 2)[0] + "\".");
        }
        return result;
    }

    private boolean startsWithKnownMarker(String text) {
        for (String marker : LIST_MARKERS) {
            if (FieldParser.indexOfMarker(text, marker, 0) == 0) {
                return true;
            }
        }
        return false;
    }

    private String remainderAfter(String trimmed, String[] words, int wordCount) {
        int consumed = 0;
        for (int i = 0; i < wordCount; i++) {
            consumed = trimmed.indexOf(words[i], consumed) + words[i].length();
        }
        return trimmed.substring(consumed).trim();
    }

    /** Carries a resolved relative date (exact or range) plus the marker text left to parse. */
    private static final class RelativeDateAndRemainder {
        private final LocalDate date;
        private final LocalDate dateFrom;
        private final LocalDate dateTo;
        private final String remainder;

        private RelativeDateAndRemainder(LocalDate date, LocalDate dateFrom, LocalDate dateTo, String remainder) {
            this.date = date;
            this.dateFrom = dateFrom;
            this.dateTo = dateTo;
            this.remainder = remainder;
        }

        private boolean hasRelativeDate() {
            return date != null || dateFrom != null;
        }
    }

    /**
     * Parses list's optional view/ marker into a detail/concise flag. Unlike status/ (which
     * accepts null to mean "all"), a present-but-unrecognised view/ value is rejected rather than
     * silently falling back to concise, so a typo like "view/nonsense" is not mistaken for a
     * request that simply happens to match the concise default.
     *
     * @param text the raw view/ value, or null if not supplied
     * @return true for "detail", false if not supplied or "concise"
     * @throws InvalidCommandException if text is supplied and is neither "concise" nor "detail"
     */
    private boolean parseViewMode(String text) throws InvalidCommandException {
        if (text == null || "concise".equalsIgnoreCase(text)) {
            return false;
        }
        if ("detail".equalsIgnoreCase(text)) {
            return true;
        }
        throw new InvalidCommandException("view must be concise or detail.");
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
            return ActivityOrder.valueOf(text.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw new InvalidCommandException("order must be input, time, or chronological.");
        }
    }

    /**
     * Parses a find command's argument text into a FindCommand. At least one keyword or filter is
     * required; find has no view/ option and always uses concise formatting.
     *
     * @param activityManager the manager the resulting command will read from
     * @param args the text after the "find" command word
     * @return the parsed FindCommand
     * @throws MissingInputException if neither a keyword nor a filter is supplied
     * @throws InvalidActivityException if the category is invalid
     * @throws InvalidCommandException if order is invalid
     * @throws InvalidDateTimeException if the date is invalid
     */
    public FindCommand parseFind(ActivityManager activityManager, String args)
            throws MissingInputException, InvalidActivityException, InvalidCommandException,
            InvalidDateTimeException {
        rejectUnrecognisedLeadingText(args, FIND_MARKERS);
        Map<String, String> fields = extractPresentFields(args, FIND_MARKERS);
        if (!hasKeywordOrFilter(fields)) {
            throw new MissingInputException("at least one keyword or filter is required.");
        }

        String rawKeywords = blankToNull(fields.get("k/"));
        List<String> keywords = rawKeywords != null
                ? Arrays.asList(rawKeywords.split("\\s+"))
                : List.of();
        ActivityCategory category = fields.containsKey("c/") ? parseCategory(fields.get("c/")) : null;
        String topic = blankToNull(fields.get("topic/"));
        LocalDate date = fields.containsKey("date/") ? DateTimeParser.parseDate(fields.get("date/")) : null;
        ActivityOrder order = fields.containsKey("order/") ? parseActivityOrder(fields.get("order/")) : null;

        return new FindCommand(activityManager, keywords, new ActivityFilter(null, category, topic, date),
                order, false);
    }

    /**
     * Checks whether a find command's extracted fields include at least one real keyword or
     * filter. order/ alone does not count: it is a display-ordering directive, not something to
     * search by, so "find order/time" with nothing else must still be rejected. A whitespace-only
     * topic/ also does not count, the same way it does not count as a stored topic anywhere else:
     * "find topic/   " with nothing else must still be rejected rather than matching everything.
     * A whitespace-only k/ does not count either, for the same reason: without this check, the
     * blank keyword survives as a single empty-string token that every activity's fields trivially
     * "contain," so "find k/   " with nothing else would silently match every activity instead of
     * being rejected.
     *
     * @param fields the fields extracted from a find command's argument text
     * @return true if at least one of a non-blank k/, c/, a non-blank topic/, or date/ is present
     */
    private boolean hasKeywordOrFilter(Map<String, String> fields) {
        return blankToNull(fields.get("k/")) != null || fields.containsKey("c/")
                || blankToNull(fields.get("topic/")) != null || fields.containsKey("date/");
    }

    /**
     * Builds a NextCommand. "next" takes no arguments.
     *
     * @param activityManager the manager the resulting command will read from
     * @param now the current date and time
     * @param args the text after the "next" command word, expected to be empty
     * @return the parsed NextCommand
     * @throws InvalidCommandException if args is not empty
     */
    public NextCommand parseNext(ActivityManager activityManager, LocalDateTime now, String args)
            throws InvalidCommandException {
        requireNoArguments("next", args);
        return new NextCommand(activityManager, now);
    }

    /**
     * Parses an order command's argument text into an OrderViewCommand or OrderSetCommand.
     *
     * @param activityManager the manager the resulting command will read from or update
     * @param args the text after the "order" command word: "view" or "set ORDER"
     * @return the parsed command
     * @throws MissingInputException if no sub-command, or no order value after "set", is supplied
     * @throws InvalidCommandException if the sub-command isn't "view"/"set", or the order value
     *     isn't one of input/time/chronological
     */
    public Command parseOrder(ActivityManager activityManager, String args)
            throws MissingInputException, InvalidCommandException {
        String trimmed = args.trim();
        if (trimmed.isEmpty()) {
            throw new MissingInputException("order requires \"view\" or \"set ORDER\".");
        }
        String[] parts = trimmed.split("\\s+", 2);
        String subCommand = parts[0];
        if ("view".equalsIgnoreCase(subCommand)) {
            requireNoArguments("order view", parts.length > 1 ? parts[1] : "");
            return new OrderViewCommand(activityManager);
        }
        if ("set".equalsIgnoreCase(subCommand)) {
            if (parts.length < 2 || parts[1].isBlank()) {
                throw new MissingInputException("order set requires input, time, or chronological.");
            }
            return new OrderSetCommand(activityManager, parseActivityOrder(parts[1].trim()));
        }
        throw new InvalidCommandException("order requires \"view\" or \"set ORDER\".");
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
            return ActivityCategory.valueOf(text.toUpperCase(Locale.ROOT));
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

    /**
     * Checks that a flexible activity's duration fits inside its allowed window, per the User
     * Guide's documented rule ("the duration must fit inside the allowed window").
     *
     * @param earliestStart the earliest allowed start time
     * @param latestEnd the latest allowed end time
     * @param durationMinutes the required duration in minutes
     * @throws InvalidActivityException if durationMinutes exceeds the window from
     *     earliestStart to latestEnd
     */
    private void validateDurationFitsWindow(LocalTime earliestStart, LocalTime latestEnd, int durationMinutes)
            throws InvalidActivityException {
        long windowMinutes = Duration.between(earliestStart, latestEnd).toMinutes();
        if (durationMinutes > windowMinutes) {
            throw new InvalidActivityException("dur must fit inside the earliest/latest window ("
                    + windowMinutes + " min available).");
        }
    }

    /**
     * Normalises a whitespace-only optional field value (e.g. "topic/   ") to null, so it is
     * treated the same as the field being omitted entirely rather than stored as an empty string.
     *
     * @param value an already-trimmed field value, or null if the field was not present
     * @return value, or null if value is null or empty
     */
    private String blankToNull(String value) {
        return value == null || value.isEmpty() ? null : value;
    }

    /**
     * Rejects a value that contains the storage delimiter '|', since activities.txt cannot
     * escape it (see ActivityStorage). Catching this here, before the activity is built, avoids
     * the alternative: the activity is accepted and reported as added, then permanently fails to
     * persist because Storage.save() rejects the same value on every later save.
     *
     * @param value the already-extracted field value
     * @param fieldName the field name to use in the error message
     * @throws InvalidActivityException if value contains '|'
     */
    private void validateNoDelimiter(String value, String fieldName) throws InvalidActivityException {
        if (value.contains("|")) {
            throw new InvalidActivityException(fieldName + " must not contain the '|' character.");
        }
    }

    /**
     * Rejects a non-null topic that does not exist under the given category, preserving the
     * "topics are one-level groupings inside a fixed category" invariant. Without this check, an
     * activity's topic/ field was just an unvalidated string: an add could reference a topic that
     * was never created, and an edit that changed category could silently strand an existing
     * topic outside the category it is registered under.
     *
     * @param topicManager the manager to check the topic against
     * @param category the activity's resulting category
     * @param topic the activity's resulting topic, or null if it has none
     * @throws InvalidIndexException if topic is non-null and does not exist under category
     */
    private void validateTopicExists(TopicManager topicManager, ActivityCategory category, String topic)
            throws InvalidIndexException {
        if (topic != null && !topicManager.exists(category, topic)) {
            throw new InvalidIndexException("Topic \"" + topic + "\" does not exist under " + category
                    + ". Create it first with \"topic add\", supply a different topic/, or clear it with topic/.");
        }
    }

    /**
     * Rejects any non-blank text for a command documented as taking no arguments at all, so a
     * typo'd trailing word (e.g. "next tomorrow") does not silently execute as if it were never
     * typed.
     *
     * @param commandName the command name to use in the error message
     * @param args the text after the command word
     * @throws InvalidCommandException if args is not blank
     */
    private void requireNoArguments(String commandName, String args) throws InvalidCommandException {
        if (!args.trim().isEmpty()) {
            throw new InvalidCommandException("\"" + commandName + "\" does not take any arguments.");
        }
    }

    /**
     * Rejects text that appears before the first marker this command recognises (or, if none of
     * the given markers appear at all, any non-blank text at all). Without this check, such text
     * is invisible to marker-based extraction and would be silently discarded rather than
     * reported as a specific, correctable mistake - e.g. "add ignored/yes n/Lecture ..." would
     * otherwise mutate data despite the unrecognised "ignored/" text.
     *
     * @param args the full argument text
     * @param markers every marker this command recognises
     * @throws InvalidCommandException if unrecognised leading text is present
     */
    private void rejectUnrecognisedLeadingText(String args, String... markers) throws InvalidCommandException {
        String leading = FieldParser.leadingUnrecognisedText(args, markers);
        if (!leading.isEmpty()) {
            throw new InvalidCommandException("Unknown option \"" + leading + "\".");
        }
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
            if (FieldParser.indexOfMarker(text, marker, 0) != -1) {
                present.add(marker);
            }
        }
        present.sort(Comparator.comparingInt(marker -> FieldParser.indexOfMarker(text, marker, 0)));

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
     * @param topicManager the manager used to validate that the activity's resulting topic
     *     (carried over or newly supplied) exists under its resulting category
     * @param args the text after the "edit" command word, starting with the activity ID
     * @return the parsed EditCommand
     * @throws MissingInputException if no ID, no fields, or a required new-type timing field is
     *     missing
     * @throws InvalidCommandException if the ID is not a whole number, or type is neither FIXED
     *     nor FLEXIBLE
     * @throws InvalidIndexException if no activity has that ID, or the resulting topic does not
     *     exist under the resulting category
     * @throws InvalidActivityException if a field value fails validation
     * @throws InvalidDateTimeException if a date or time value is invalid
     * @throws DuplicateActivityException if the resulting activity exactly duplicates another,
     *     or (for a FixedActivity) overlaps another fixed activity on the same date
     */
    public EditCommand parseEdit(ActivityManager activityManager, TopicManager topicManager, String args)
            throws MissingInputException, InvalidCommandException, InvalidIndexException, InvalidActivityException,
            InvalidDateTimeException, DuplicateActivityException {
        String[] parts = args.trim().split("\\s+", 2);
        int id = parseEditId(parts[0]);
        String fieldsText = parts.length > 1 ? parts[1] : "";

        rejectUnrecognisedLeadingText(fieldsText, EDIT_MARKERS);
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
        String topic = fields.containsKey("topic/") ? blankToNull(fields.get("topic/")) : old.getTopic();
        String note = fields.containsKey("note/") ? blankToNull(fields.get("note/")) : old.getNote();

        validateNoDelimiter(description, "description");
        if (topic != null) {
            validateNoDelimiter(topic, "topic");
        }
        if (note != null) {
            validateNoDelimiter(note, "note");
        }
        validateTopicExists(topicManager, category, topic);

        ScheduleType oldType = old.getScheduleType();
        ScheduleType newType = fields.containsKey("type/") ? parseScheduleType(fields.get("type/")) : oldType;
        boolean typeChanged = newType != oldType;

        Activity newActivity = newType == ScheduleType.FIXED
                ? buildFixed(id, description, category, date, energy, sensory, topic, note, fields, old, typeChanged)
                : buildFlexible(id, description, category, date, energy, sensory, topic, note, fields, old,
                        typeChanged);
        activityManager.checkNoConflicts(newActivity, id);

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
        validateDurationFitsWindow(earliestStart, latestEnd, durationMinutes);
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
