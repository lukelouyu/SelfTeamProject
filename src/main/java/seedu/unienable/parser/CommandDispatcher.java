package seedu.unienable.parser;

import java.time.LocalDateTime;

import seedu.unienable.command.Command;
import seedu.unienable.command.ExitCommand;
import seedu.unienable.command.GuideCommand;
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
import seedu.unienable.parser.common.Parser;
import seedu.unienable.parser.topic.TopicCommandParser;

/** Routes one full line of user input to the right parser, returning the Command it produces. */
public class CommandDispatcher {
    private final ActivityManager activityManager;
    private final TopicManager topicManager;
    private final FacilityManager facilityManager;
    private final ConnectionManager connectionManager;

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
     */
    public CommandDispatcher(ActivityManager activityManager, TopicManager topicManager,
            FacilityManager facilityManager, ConnectionManager connectionManager) {
        this.activityManager = activityManager;
        this.topicManager = topicManager;
        this.facilityManager = facilityManager;
        this.connectionManager = connectionManager;
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
     */
    public Command dispatch(String input, LocalDateTime now)
            throws MissingInputException, InvalidActivityException, InvalidCommandException,
            InvalidDateTimeException, InvalidIndexException {
        String commandWord = Parser.getCommandWord(input);
        String args = Parser.getArguments(input);

        switch (commandWord) {
        case "add":
            return activityCommandParser.parseAdd(activityManager, args);
        case "list":
            return activityCommandParser.parseList(activityManager, args);
        case "view":
            return activityCommandParser.parseView(activityManager, args);
        case "find":
            return activityCommandParser.parseFind(activityManager, args);
        case "edit":
            return activityCommandParser.parseEdit(activityManager, args);
        case "delete":
            return activityCommandParser.parseDelete(activityManager, args);
        case "mark":
            return activityCommandParser.parseMark(activityManager, args);
        case "unmark":
            return activityCommandParser.parseUnmark(activityManager, args);
        case "next":
            return activityCommandParser.parseNext(activityManager, now);
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
        case "bye":
            return new ExitCommand();
        default:
            throw new InvalidCommandException("Unknown command \"" + commandWord + "\". Enter guide for help.");
        }
    }

    private Command dispatchTopic(String args)
            throws MissingInputException, InvalidActivityException, InvalidCommandException {
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
            return facilityCommandParser.parseList(facilityManager);
        case "view":
            return facilityCommandParser.parseView(facilityManager, parts[1]);
        case "find":
            return facilityCommandParser.parseFind(facilityManager, parts[1]);
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
            return connectionCommandParser.parseList(connectionManager);
        case "view":
            return connectionCommandParser.parseView(connectionManager, parts[1]);
        case "find":
            return connectionCommandParser.parseFind(connectionManager, parts[1]);
        default:
            throw new InvalidCommandException("Unknown connection command \"" + parts[0] + "\". Enter guide "
                    + "for help.");
        }
    }

    /**
     * Splits "SUBCOMMAND rest of args" into its two parts. Blank input maps to an empty
     * sub-command, so a missing sub-command falls through to each dispatcher's default case
     * rather than throwing here.
     */
    private String[] splitSubCommand(String args) {
        String trimmed = args.trim();
        String[] parts = trimmed.split("\\s+", 2);
        return new String[] { parts[0], parts.length > 1 ? parts[1] : "" };
    }
}
