package seedu.unienable.command.accessibility;

import seedu.unienable.command.Command;
import seedu.unienable.command.CommandResult;

import java.util.ArrayList;
import java.util.List;

import seedu.unienable.accessibility.classes.Connection;
import seedu.unienable.accessibility.enums.AccessibilityStatus;
import seedu.unienable.accessibility.enums.ShelterStatus;
import seedu.unienable.accessibility.enums.TraversalType;
import seedu.unienable.logic.ConnectionManager;

/**
 * Finds connections matching every supplied filter (from/to/type/status/shelter), combined with
 * AND. Since every stored connection is two-way, from/ and to/ each match either endpoint rather
 * than a specific direction.
 */
public class ConnectionFindCommand extends Command {
    private final ConnectionManager connectionManager;
    private final String from;
    private final String to;
    private final TraversalType type;
    private final AccessibilityStatus status;
    private final ShelterStatus shelter;

    public ConnectionFindCommand(ConnectionManager connectionManager, String from, String to, TraversalType type,
            AccessibilityStatus status, ShelterStatus shelter) {
        this.connectionManager = connectionManager;
        this.from = from;
        this.to = to;
        this.type = type;
        this.status = status;
        this.shelter = shelter;
    }

    @Override
    public CommandResult execute() {
        List<Connection> matches = new ArrayList<>();
        for (Connection connection : connectionManager.list()) {
            if (matches(connection)) {
                matches.add(connection);
            }
        }
        return new CommandResult(formatResult(matches));
    }

    private boolean matches(Connection connection) {
        return matchesEndpoint(connection, from)
                && matchesEndpoint(connection, to)
                && (type == null || connection.getType() == type)
                && (status == null || connection.getAccessibility() == status)
                && (shelter == null || connection.getShelter() == shelter);
    }

    private boolean matchesEndpoint(Connection connection, String facilityName) {
        return facilityName == null
                || connection.getFrom().equalsIgnoreCase(facilityName)
                || connection.getTo().equalsIgnoreCase(facilityName);
    }

    private String formatResult(List<Connection> connections) {
        if (connections.isEmpty()) {
            return "No connections found.\n\n" + AccessibilityDisclaimer.TEXT;
        }
        StringBuilder result = new StringBuilder("Found ").append(connections.size())
                .append(connections.size() == 1 ? " connection:" : " connections:");
        for (Connection connection : connections) {
            result.append('\n').append(formatConnectionLines(connection));
        }
        result.append("\n\n").append(AccessibilityDisclaimer.TEXT);
        return result.toString();
    }

    private String formatConnectionLines(Connection connection) {
        return "[" + connection.getId() + "] " + connection.getFrom() + " <-> " + connection.getTo()
                + " | " + connection.getDistanceInMetres() + " m"
                + " | ACCESSIBLE " + connection.getAccessibility()
                + " | " + connection.getType()
                + "\n    Shelter: " + connection.getShelter();
    }
}
