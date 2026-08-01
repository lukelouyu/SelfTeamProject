package seedu.unienable.parser;

import java.time.LocalDateTime;
import java.util.Locale;

import seedu.unienable.command.Command;
import seedu.unienable.command.general.ExitCommand;
import seedu.unienable.command.general.GuideCommand;
import seedu.unienable.command.general.ResetCommand;
import seedu.unienable.exception.DuplicateActivityException;
import seedu.unienable.exception.InvalidActivityException;
import seedu.unienable.exception.InvalidCommandException;
import seedu.unienable.exception.InvalidDateTimeException;
import seedu.unienable.exception.InvalidIndexException;
import seedu.unienable.exception.MissingInputException;
import seedu.unienable.logic.ActivityManager;
import seedu.unienable.logic.ConnectionManager;
import seedu.unienable.logic.FacilityManager;
import seedu.unienable.logic.TopicManager;
import seedu.unienable.parser.activity.ActivityCommandParser;
import seedu.unienable.parser.accessibility.ConnectionCommandParser;
import seedu.unienable.parser.accessibility.FacilityCommandParser;
import seedu.unienable.parser.common.Parser;
import seedu.unienable.parser.topic.TopicCommandParser;
import seedu.unienable.storage.Storage;

/** Routes one full line of user input to the right parser, returning the Command it produces. */
public class CommandDispatcher {
    private final ActivityManager activityManager;
    private final TopicManager topicManager;
    private final FacilityManager facilityManager;
    private final ConnectionManager connectionManager;
    private final Storage storage;

    private final ActivityCommandParser activityCommandParser = new ActivityCommandParser();
    private final TopicCommandParser topicCommandParser = new TopicCommandParser();
    private final FacilityCommandParser facilityCommandParser = new FacilityCommandParser();
    private final ConnectionCommandParser connectionCommandParser = new ConnectionCommandParser();

    /**
     * Creates a CommandDispatcher over the given managers.
     *
     * @param activityManager the manager activity commands act on
     * @param topicManager the manager topic commands act on
     * @param facilityManager the manager facility commands read from
     * @param connectionManager the manager connection commands read from
     * @param storage the storage coordinator "facility validate"/"connection validate" re-read
     *     the raw data files through
     */
    public CommandDispatcher(ActivityManager activityManager, TopicManager topicManager,
            FacilityManager facilityManager, ConnectionManager connectionManager, Storage storage) {
        this.activityManager = activityManager;
        this.topicManager = topicManager;
        this.facilityManager = facilityManager;
        this.connectionManager = connectionManager;
        this.storage = storage;
    }

    /**
     * Parses one full line of user input into a Command.
     *
     * @param input the full raw user input
     * @param now the current date and time, used by commands like "next" that depend on it
     * @return the parsed Command
     * @throws MissingInputException if a required field is missing
     * @throws InvalidActivityException if an activity-domain field (e.g. category) is invalid
     * @throws InvalidCommandException if the command word is unrecognised, or a field value is
     *     invalid
     * @throws InvalidDateTimeException if a date or time value is invalid
     * @throws InvalidIndexException if a referenced stable ID does not exist
     * @throws DuplicateActivityException if an edit would exactly duplicate or overlap another
     *     activity, or a topic rename/delete conflicts with existing topics or usage
     */
    public Command dispatch(String input, LocalDateTime now)
            throws MissingInputException, InvalidActivityException, InvalidCommandException,
            InvalidDateTimeException, InvalidIndexException, DuplicateActivityException {
        String commandWord = Parser.getCommandWord(input);
        String args = Parser.getArguments(input);

        switch (commandWord) {
        case "add":
            return activityCommandParser.parseAdd(activityManager, topicManager, now, args);
        case "list":
            return activityCommandParser.parseList(activityManager, now, args);
        case "view":
            return activityCommandParser.parseView(activityManager, args);
        case "find":
            return activityCommandParser.parseFind(activityManager, args);
        case "edit":
            return activityCommandParser.parseEdit(activityManager, topicManager, now, args);
        case "delete":
            return activityCommandParser.parseDelete(activityManager, args);
        case "mark":
            return activityCommandParser.parseMark(activityManager, args);
        case "unmark":
            return activityCommandParser.parseUnmark(activityManager, args);
        case "next":
            return activityCommandParser.parseNext(activityManager, now, args);
        case "order":
            return activityCommandParser.parseOrder(activityManager, args);
        case "topic":
            return dispatchTopic(args);
        case "facility":
            return dispatchFacility(args);
        case "connection":
            return dispatchConnection(args);
        case "guide":
            return new GuideCommand(args.isEmpty() ? null : args);
        case "reset":
            return dispatchReset(args);
        case "bye":
            requireNoArguments("bye", args);
            return new ExitCommand();
        case "1": case "2": case "3": case "4": case "5":
        case "6": case "7": case "8": case "9": case "10": case "11":
            // A bare menu number entered right after "guide" displays the main menu, matching
            // its own "Enter a number from 1 to 11." instruction; see GuideCommand's Javadoc.
            return new GuideCommand(commandWord);
        default:
            throw new InvalidCommandException("Unknown command \"" + commandWord + "\". Enter guide for help.");
        }
    }

    /**
     * Parses "reset all" - the only accepted form. A missing, misspelled, or trailing argument is
     * rejected clearly rather than silently accepted or ignored.
     *
     * @param args the text after the "reset" command word
     * @return the parsed ResetCommand
     * @throws MissingInputException if no argument is supplied
     * @throws InvalidCommandException if the argument is anything other than exactly "all"
     */
    private Command dispatchReset(String args) throws MissingInputException, InvalidCommandException {
        String trimmed = args.trim();
        if (trimmed.isEmpty()) {
            throw new MissingInputException("reset requires \"all\".");
        }
        if (!"all".equalsIgnoreCase(trimmed)) {
            throw new InvalidCommandException("reset only accepts \"all\".");
        }
        return new ResetCommand(activityManager, topicManager);
    }

    private Command dispatchTopic(String args)
            throws MissingInputException, InvalidActivityException, InvalidCommandException, InvalidIndexException,
            DuplicateActivityException {
        String[] parts = splitSubCommand(args);
        switch (parts[0]) {
        case "add":
            return topicCommandParser.parseAdd(topicManager, parts[1]);
        case "list":
            return topicCommandParser.parseList(topicManager, parts[1]);
        case "rename":
            return topicCommandParser.parseRename(topicManager, parts[1]);
        case "delete":
            return topicCommandParser.parseDelete(topicManager, parts[1]);
        default:
            throw new InvalidCommandException("Unknown topic command \"" + parts[0] + "\". Enter guide topic "
                    + "for help.");
        }
    }

    private Command dispatchFacility(String args)
            throws MissingInputException, InvalidCommandException {
        String[] parts = splitSubCommand(args);
        switch (parts[0]) {
        case "list":
            return facilityCommandParser.parseList(facilityManager, parts[1]);
        case "view":
            return facilityCommandParser.parseView(facilityManager, parts[1]);
        case "find":
            return facilityCommandParser.parseFind(facilityManager, parts[1]);
        case "validate":
            return facilityCommandParser.parseValidate(storage, parts[1]);
        default:
            throw new InvalidCommandException("Unknown facility command \"" + parts[0] + "\". Enter guide "
                    + "facility for help.");
        }
    }

    private Command dispatchConnection(String args)
            throws MissingInputException, InvalidCommandException {
        String[] parts = splitSubCommand(args);
        switch (parts[0]) {
        case "list":
            return connectionCommandParser.parseList(connectionManager, parts[1]);
        case "view":
            return connectionCommandParser.parseView(connectionManager, parts[1]);
        case "find":
            return connectionCommandParser.parseFind(connectionManager, parts[1]);
        case "validate":
            return connectionCommandParser.parseValidate(storage, parts[1]);
        default:
            throw new InvalidCommandException("Unknown connection command \"" + parts[0] + "\". Enter guide "
                    + "for help.");
        }
    }

    /**
     * Splits "SUBCOMMAND rest of args" into its two parts. Blank input maps to an empty
     * sub-command, so a missing sub-command falls through to each dispatcher's default case
     * rather than throwing here. The sub-command word is lower-cased (matching the top-level
     * command word's own case-insensitivity, per {@link Parser#getCommandWord}) so "TOPIC ADD",
     * "FaCiLiTy FiNd", etc. resolve the same as their lower-case form; the rest of the argument
     * text is left in its original case since it may contain free text.
     */
    private String[] splitSubCommand(String args) {
        String trimmed = args.trim();
        String[] parts = trimmed.split("\\s+", 2);
        return new String[] { parts[0].toLowerCase(Locale.ROOT), parts.length > 1 ? parts[1] : "" };
    }

    /**
     * Rejects any non-blank text for a command documented as taking no arguments at all, so a
     * typo'd trailing word (e.g. "bye now") does not silently execute as if it were never typed.
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
}
