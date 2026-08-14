package seedu.unienable.command.accessibility.connection;

import seedu.unienable.command.ReadOnlyCommand;
import seedu.unienable.command.CommandResult;
import seedu.unienable.ui.accessibility.AccessibilityDisclaimer;

import java.util.List;

import seedu.unienable.accessibility.classes.Connection;
import seedu.unienable.logic.ConnectionManager;

/** Lists every known connection's ID, endpoints, distance, accessibility, traversal type, and shelter status. */
public class ConnectionListCommand extends ReadOnlyCommand {
    private final ConnectionManager connectionManager;

    /**
     * Creates a ConnectionListCommand.
     *
     * @param connectionManager the manager to list connections from
     */
    public ConnectionListCommand(ConnectionManager connectionManager) {
        this.connectionManager = connectionManager;
    }

    @Override
    public CommandResult execute() {
        List<Connection> connections = connectionManager.list();
        StringBuilder result = new StringBuilder("Known two-way connections:");
        for (Connection connection : connections) {
            result.append('\n').append(formatConnectionLines(connection));
        }
        result.append("\n\n").append(AccessibilityDisclaimer.TEXT);
        return new CommandResult(result.toString());
    }

    private String formatConnectionLines(Connection connection) {
        return "[" + connection.getId() + "] " + connection.getFrom() + " <-> " + connection.getTo()
                + " | " + connection.getDistanceInMetres() + " m"
                + " | ACCESSIBLE " + connection.getAccessibility()
                + " | " + connection.getType()
                + "\n    Shelter: " + connection.getShelter();
    }
}
