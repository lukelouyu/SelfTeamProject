package seedu.unienable.parser.activity;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Locale;

import seedu.unienable.command.Command;
import seedu.unienable.command.activity.crud.AddCommand;
import seedu.unienable.command.activity.crud.DeleteCommand;
import seedu.unienable.command.activity.crud.EditCommand;
import seedu.unienable.command.activity.crud.ViewCommand;
import seedu.unienable.command.activity.general.FindCommand;
import seedu.unienable.command.activity.general.ListCommand;
import seedu.unienable.command.activity.general.MarkCommand;
import seedu.unienable.command.activity.general.NextCommand;
import seedu.unienable.command.activity.general.OrderSetCommand;
import seedu.unienable.command.activity.general.OrderViewCommand;
import seedu.unienable.command.activity.general.UnmarkCommand;
import seedu.unienable.exception.DuplicateActivityException;
import seedu.unienable.exception.InvalidActivityException;
import seedu.unienable.exception.InvalidCommandException;
import seedu.unienable.exception.InvalidDateTimeException;
import seedu.unienable.exception.InvalidIndexException;
import seedu.unienable.exception.MissingInputException;
import seedu.unienable.logic.ActivityManager;
import seedu.unienable.logic.TopicManager;
import seedu.unienable.model.enums.ActivityCategory;
import seedu.unienable.model.enums.ActivityOrder;
import seedu.unienable.parser.common.FieldParser;

/**
 * Routes activity-related commands (add, list, find, edit, delete, mark, unmark, next, order) to
 * the command-specific parser that owns each one's grammar, and directly handles the commands
 * whose "grammar" is just a bare activity ID or no arguments at all. Also holds the small value
 * parsers and checks genuinely shared by more than one of those command-specific parsers, so
 * those rules can't silently drift apart between commands.
 */
public class ActivityCommandParser {
    /** Every field marker an activity can have; used to reject unrecognised text in add/edit. */
    static final String[] ALL_ACTIVITY_MARKERS = {
        "n/", "c/", "date/", "type/", "from/", "to/", "earliest/", "latest/", "dur/",
        "energy/", "sensory/", "topic/", "note/"
    };

    private final AddCommandParser addCommandParser = new AddCommandParser();
    private final EditCommandParser editCommandParser = new EditCommandParser();
    private final ListCommandParser listCommandParser = new ListCommandParser();
    private final FindCommandParser findCommandParser = new FindCommandParser();

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
    public AddCommand parseAdd(ActivityManager activityManager, TopicManager topicManager, LocalDateTime now,
            String args)
            throws MissingInputException, InvalidActivityException, InvalidDateTimeException,
            InvalidCommandException, InvalidIndexException {
        return addCommandParser.parse(activityManager, topicManager, now, args);
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
     * and may appear in any order; an optional relative-date phrase ("today", "tomorrow", "this
     * week", "next week", or "overdue") may additionally appear at the very start of the text,
     * before any markers.
     *
     * @param activityManager the manager the resulting command will read from
     * @param now the current date and time, used to resolve "today"/"tomorrow"/"this week"/
     *     "next week"/"overdue"
     * @param args the text after the "list" command word
     * @return the parsed ListCommand
     * @throws InvalidActivityException if the category is invalid
     * @throws InvalidCommandException if status or order is invalid, a relative-date phrase is
     *     unrecognised, a relative-date phrase is combined with date/ or another relative-date
     *     phrase, status/ is combined with overdue, or unrecognised text follows a relative-date
     *     phrase
     * @throws InvalidDateTimeException if the date is invalid
     */
    public ListCommand parseList(ActivityManager activityManager, LocalDateTime now, String args)
            throws InvalidActivityException, InvalidCommandException, InvalidDateTimeException {
        return listCommandParser.parse(activityManager, now, args);
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
     * @throws InvalidCommandException if order is invalid, or k/ contains more than two words
     * @throws InvalidDateTimeException if the date is invalid
     */
    public FindCommand parseFind(ActivityManager activityManager, String args)
            throws MissingInputException, InvalidActivityException, InvalidCommandException,
            InvalidDateTimeException {
        return findCommandParser.parse(activityManager, args);
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
        FieldParser.requireNoArguments("next", args);
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
            FieldParser.requireNoArguments("order view", parts.length > 1 ? parts[1] : "");
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
    public EditCommand parseEdit(ActivityManager activityManager, TopicManager topicManager, LocalDateTime now,
            String args)
            throws MissingInputException, InvalidCommandException, InvalidIndexException, InvalidActivityException,
            InvalidDateTimeException, DuplicateActivityException {
        return editCommandParser.parse(activityManager, topicManager, now, args);
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

    static int parsePositiveInt(String text, String fieldName) throws InvalidActivityException {
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
    static void validateDurationFitsWindow(LocalTime earliestStart, LocalTime latestEnd, int durationMinutes)
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
    static String blankToNull(String value) {
        return value == null || value.isEmpty() ? null : value;
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
    static void validateTopicExists(TopicManager topicManager, ActivityCategory category, String topic)
            throws InvalidIndexException {
        if (topic != null && !topicManager.exists(category, topic)) {
            throw new InvalidIndexException("Topic \"" + topic + "\" does not exist under " + category
                    + ". Create it first with \"topic add\", supply a different topic/, or clear it with topic/.");
        }
    }

    static ActivityOrder parseActivityOrder(String text) throws InvalidCommandException {
        try {
            return ActivityOrder.valueOf(text.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw new InvalidCommandException("order must be input, time, or chronological.");
        }
    }
}
