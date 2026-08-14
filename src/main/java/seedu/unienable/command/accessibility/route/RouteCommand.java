package seedu.unienable.command.accessibility.route;

import seedu.unienable.command.ReadOnlyCommand;
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
public class RouteCommand extends ReadOnlyCommand {
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
        // GraphPath already carries the exact connection Dijkstra selected for each hop - no
        // separate re-lookup against ConnectionManager is needed (or safe: a pair of facilities
        // can have multiple parallel connections, and re-deriving "some connection between these
        // two names" afterwards has no way to know which one the shortest-path total came from).
        return new CommandResult(RouteFormatter.formatRoute(path, path.getConnections()));
    }
}
