package seedu.unienable.command.accessibility;

import seedu.unienable.command.Command;
import seedu.unienable.command.CommandResult;

import seedu.unienable.accessibility.classes.Connection;
import seedu.unienable.exception.InvalidIndexException;
import seedu.unienable.logic.ConnectionManager;

/** Displays one connection's complete stored details. */
public class ConnectionViewCommand extends Command {
    private static final int LABEL_WIDTH = 14;

    private final ConnectionManager connectionManager;
    private final int id;

    public ConnectionViewCommand(ConnectionManager connectionManager, int id) {
        this.connectionManager = connectionManager;
        this.id = id;
    }

    @Override
    public CommandResult execute() throws InvalidIndexException {
        return new CommandResult(formatResult(connectionManager.findById(id)));
    }

    private String formatResult(Connection connection) {
        StringBuilder result = new StringBuilder("Connection [").append(connection.getId()).append("]\n");
        appendField(result, "From", connection.getFrom());
        appendField(result, "To", connection.getTo());
        appendField(result, "Distance", connection.getDistanceInMetres() + " m");
        appendField(result, "Accessibility", connection.getAccessibility());
        appendField(result, "Type", connection.getType());
        appendField(result, "Shelter", connection.getShelter());
        appendField(result, "Known barrier", orNoneRecorded(connection.getKnownBarrier()));
        result.append(labelPrefix("Notes")).append(orNoneRecorded(connection.getNotes()));
        result.append("\n\n").append(AccessibilityDisclaimer.TEXT);
        return result.toString();
    }

    private String orNoneRecorded(String value) {
        return value == null ? "None recorded" : value;
    }

    private void appendField(StringBuilder result, String label, Object value) {
        result.append(labelPrefix(label)).append(value).append('\n');
    }

    private String labelPrefix(String label) {
        StringBuilder padded = new StringBuilder(label);
        while (padded.length() < LABEL_WIDTH) {
            padded.append(' ');
        }
        return padded.append(": ").toString();
    }
}
