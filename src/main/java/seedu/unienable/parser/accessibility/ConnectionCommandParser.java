package seedu.unienable.parser.accessibility;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import seedu.unienable.accessibility.enums.AccessibilityStatus;
import seedu.unienable.accessibility.enums.ShelterStatus;
import seedu.unienable.accessibility.enums.TraversalType;
import seedu.unienable.command.accessibility.ConnectionFindCommand;
import seedu.unienable.command.accessibility.ConnectionListCommand;
import seedu.unienable.command.accessibility.ConnectionViewCommand;
import seedu.unienable.exception.InvalidCommandException;
import seedu.unienable.exception.MissingInputException;
import seedu.unienable.logic.ConnectionManager;
import seedu.unienable.parser.common.FieldParser;

/** Parses connection-related commands (connection list, connection view, connection find) into Command objects. */
public class ConnectionCommandParser {
    private static final String[] FIND_MARKERS = { "from/", "to/", "type/", "status/", "shelter/" };

    /**
     * Builds a ConnectionListCommand. "connection list" takes no arguments.
     *
     * @param connectionManager the manager the resulting command will read from
     * @return the parsed ConnectionListCommand
     */
    public ConnectionListCommand parseList(ConnectionManager connectionManager) {
        return new ConnectionListCommand(connectionManager);
    }

    /**
     * Parses a connection view command's argument text into a ConnectionViewCommand.
     *
     * @param connectionManager the manager the resulting command will read from
     * @param args the text after the "connection view" command words: the connection ID
     * @return the parsed ConnectionViewCommand
     * @throws MissingInputException if no ID is supplied
     * @throws InvalidCommandException if the supplied ID is not a whole number
     */
    public ConnectionViewCommand parseView(ConnectionManager connectionManager, String args)
            throws MissingInputException, InvalidCommandException {
        String trimmed = args.trim();
        if (trimmed.isEmpty()) {
            throw new MissingInputException("a connection ID is required.");
        }
        try {
            return new ConnectionViewCommand(connectionManager, Integer.parseInt(trimmed));
        } catch (NumberFormatException e) {
            throw new InvalidCommandException("connection ID must be a whole number.");
        }
    }

    /**
     * Parses a connection find command's argument text into a ConnectionFindCommand. Every field
     * is optional and may appear in any order, but at least one is required.
     *
     * @param connectionManager the manager the resulting command will read from
     * @param args the text after the "connection find" command words
     * @return the parsed ConnectionFindCommand
     * @throws MissingInputException if no filter is supplied
     * @throws InvalidCommandException if type, status, or shelter is invalid
     */
    public ConnectionFindCommand parseFind(ConnectionManager connectionManager, String args)
            throws MissingInputException, InvalidCommandException {
        Map<String, String> fields = extractPresentFields(args, FIND_MARKERS);
        String from = blankToNull(fields.get("from/"));
        String to = blankToNull(fields.get("to/"));
        if (from == null && to == null && !fields.containsKey("type/") && !fields.containsKey("status/")
                && !fields.containsKey("shelter/")) {
            // A whitespace-only from/ or to/ does not count as a supplied filter, the same way it
            // does not count as a stored value anywhere else: "connection find from/   " with
            // nothing else must still be rejected rather than matching every connection.
            throw new MissingInputException("at least one filter is required.");
        }
        TraversalType type = fields.containsKey("type/") ? parseType(fields.get("type/")) : null;
        AccessibilityStatus status = fields.containsKey("status/") ? parseStatus(fields.get("status/")) : null;
        ShelterStatus shelter = fields.containsKey("shelter/") ? parseShelter(fields.get("shelter/")) : null;
        return new ConnectionFindCommand(connectionManager, from, to, type, status, shelter);
    }

    private TraversalType parseType(String text) throws InvalidCommandException {
        try {
            return TraversalType.valueOf(text.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new InvalidCommandException("type must be one of RAMP, SHELTERED_RAMP, LIFT, PATH, OTHER.");
        }
    }

    private AccessibilityStatus parseStatus(String text) throws InvalidCommandException {
        try {
            return AccessibilityStatus.valueOf(text.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new InvalidCommandException("status must be YES, NO, or UNKNOWN.");
        }
    }

    private ShelterStatus parseShelter(String text) throws InvalidCommandException {
        try {
            return ShelterStatus.valueOf(text.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new InvalidCommandException("shelter must be YES, NO, or UNKNOWN.");
        }
    }

    /**
     * Normalises a whitespace-only optional filter value (e.g. "from/   ") to null, so it is
     * treated as the filter being omitted entirely rather than a literal empty endpoint name that
     * can never match a real connection.
     *
     * @param value an already-trimmed field value, or null if the field was not present
     * @return value, or null if value is null or empty
     */
    private String blankToNull(String value) {
        return value == null || value.isEmpty() ? null : value;
    }

    private Map<String, String> extractPresentFields(String text, String... markers) {
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
}
