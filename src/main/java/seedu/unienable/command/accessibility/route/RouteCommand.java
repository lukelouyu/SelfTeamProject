package seedu.unienable.command.accessibility.route;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import seedu.unienable.accessibility.classes.Connection;
import seedu.unienable.command.Command;
import seedu.unienable.command.CommandResult;
import seedu.unienable.exception.InvalidIndexException;
import seedu.unienable.logic.ConnectionManager;
import seedu.unienable.logic.FacilityManager;
import seedu.unienable.logic.graph.AccessibilityGraph;
import seedu.unienable.logic.graph.GraphPath;
import seedu.unienable.logic.route.AccessibleRouteGraphFactory;
import seedu.unienable.ui.accessibility.RouteFormatter;

/**
 * Finds the shortest confirmed-accessible route between two known facilities, by distance, using
 * only connections whose accessibility status is confirmed {@code YES}. Read-only: never adds,
 * edits, or deletes a facility or connection record.
 *
 * <p>Source and destination naming the same known facility is a successful zero-length result
 * (see {@code docs/tasks/v2/route/ACCEPTANCE_CRITERIA.md}, AC9), not an error. Two known
 * facilities with no confirmed-accessible path between them get the documented no-route fallback
 * rather than an exception (AC11); only an unrecognised facility name is an error (AC10).
 */
public class RouteCommand extends Command {
    private final FacilityManager facilityManager;
    private final ConnectionManager connectionManager;
    private final String from;
    private final String to;

    /**
     * Creates a RouteCommand.
     *
     * @param facilityManager the facility reference data to route over
     * @param connectionManager the connection reference data to route over
     * @param from the source facility name (case-insensitive)
     * @param to the destination facility name (case-insensitive)
     */
    public RouteCommand(FacilityManager facilityManager, ConnectionManager connectionManager, String from,
            String to) {
        this.facilityManager = facilityManager;
        this.connectionManager = connectionManager;
        this.from = from;
        this.to = to;
    }

    @Override
    public CommandResult execute() throws InvalidIndexException {
        AccessibilityGraph graph = AccessibleRouteGraphFactory.build(facilityManager, connectionManager);
        GraphPath path = graph.getShortestPath(from, to);
        if (path == null) {
            return new CommandResult(RouteFormatter.formatNoRoute(
                    facilityManager.findByName(from).getName(), facilityManager.findByName(to).getName()));
        }
        List<Connection> accessible = AccessibleRouteGraphFactory.filterConfirmedAccessible(connectionManager.list());
        return new CommandResult(RouteFormatter.formatRoute(path, resolveSegments(path, accessible)));
    }

    /**
     * Resolves the connection record backing each consecutive pair in the path's facility chain,
     * in travel order. Every connection is stored two-way, so a pair may match a connection whose
     * own stored {@code from}/{@code to} is in either order; displayed segment direction always
     * follows the path's own travel direction, not the stored record's order.
     *
     * @param path the shortest path found, over the same accessible-only edge set as {@code accessible}
     * @param accessible the confirmed-accessible connections the path was computed over
     * @return one connection per consecutive facility pair in {@code path}
     */
    private List<Connection> resolveSegments(GraphPath path, List<Connection> accessible) {
        List<String> facilityNames = path.getFacilityNames();
        List<Connection> segments = new ArrayList<>();
        for (int i = 0; i + 1 < facilityNames.size(); i++) {
            segments.add(findConnectionBetween(facilityNames.get(i), facilityNames.get(i + 1), accessible));
        }
        return segments;
    }

    private Connection findConnectionBetween(String facilityA, String facilityB, List<Connection> accessible) {
        for (Connection connection : accessible) {
            if (connects(connection, facilityA, facilityB)) {
                return connection;
            }
        }
        throw new IllegalStateException("no accessible connection found between \"" + facilityA + "\" and \""
                + facilityB + "\" - the graph these facilities came from must have been built from this exact "
                + "connection list");
    }

    private boolean connects(Connection connection, String facilityA, String facilityB) {
        return (equalsIgnoreCase(connection.getFrom(), facilityA) && equalsIgnoreCase(connection.getTo(), facilityB))
                || (equalsIgnoreCase(connection.getFrom(), facilityB) && equalsIgnoreCase(connection.getTo(),
                        facilityA));
    }

    private boolean equalsIgnoreCase(String a, String b) {
        return a.toLowerCase(Locale.ROOT).equals(b.toLowerCase(Locale.ROOT));
    }
}
