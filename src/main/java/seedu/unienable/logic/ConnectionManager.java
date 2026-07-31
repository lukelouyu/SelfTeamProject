package seedu.unienable.logic;

import java.util.List;

import seedu.unienable.accessibility.classes.Connection;
import seedu.unienable.exception.InvalidIndexException;

/**
 * Holds the loaded connection reference dataset in memory. Read-only: no in-app command adds,
 * edits, or deletes connection records.
 */
public class ConnectionManager {
    private final List<Connection> connections;

    /**
     * Creates a ConnectionManager over an immutable copy of the given connections.
     *
     * @param connections the loaded connection records, in load order
     */
    public ConnectionManager(List<Connection> connections) {
        this.connections = List.copyOf(connections);
    }

    /** Returns every known connection, in load order. */
    public List<Connection> list() {
        return connections;
    }

    /**
     * Finds a connection by its stable ID.
     *
     * @param id the connection ID to look up
     * @return the matching connection
     * @throws InvalidIndexException if no connection has that ID
     */
    public Connection findById(int id) throws InvalidIndexException {
        for (Connection connection : connections) {
            if (connection.getId() == id) {
                return connection;
            }
        }
        throw new InvalidIndexException("Connection [" + id + "] does not exist.");
    }
}
