package seedu.unienable.logic.route;

import java.util.ArrayList;
import java.util.List;

import seedu.unienable.accessibility.classes.Connection;
import seedu.unienable.accessibility.enums.AccessibilityStatus;
import seedu.unienable.logic.ConnectionManager;
import seedu.unienable.logic.FacilityManager;
import seedu.unienable.logic.graph.AccessibilityGraph;

/**
 * Builds the {@link AccessibilityGraph} used by {@code route}: a graph over only those
 * connections whose recorded accessibility status is confirmed {@link AccessibilityStatus#YES}.
 * {@code NO} and {@code UNKNOWN} connections are excluded entirely, never selected even as a last
 * resort - this is {@code route}'s command-specific policy, kept out of the general-purpose
 * {@link AccessibilityGraph}. See {@code docs/tasks/v2/route/IMPLEMENTATION_NOTES.md} for why this
 * filtering lives here rather than in {@code AccessibilityGraph} itself, and why it does not
 * re-validate data that {@code FacilityStorage}/{@code ConnectionStorage} already guarantee is
 * well-formed by the time it reaches a loaded {@link FacilityManager}/{@link ConnectionManager}.
 */
public final class AccessibleRouteGraphFactory {
    private AccessibleRouteGraphFactory() {
    }

    /**
     * Builds a confirmed-accessible-only route graph from the given loaded reference data.
     *
     * @param facilityManager every known facility, used as graph nodes
     * @param connectionManager every known connection; only {@code YES}-status ones become edges
     * @return the filtered graph
     */
    public static AccessibilityGraph build(FacilityManager facilityManager, ConnectionManager connectionManager) {
        return new AccessibilityGraph(facilityManager.list(), filterConfirmedAccessible(connectionManager.list()));
    }

    /**
     * Returns only the connections whose accessibility status is confirmed {@code YES}, in their
     * original order. Exposed separately (not just used internally by {@link #build}) so a caller
     * that also needs to resolve which connection record backs a given graph segment - e.g.
     * {@code RouteCommand}, showing per-segment distance/type/barrier/notes/shelter - looks up
     * against the exact same eligible set the graph itself was built from.
     *
     * @param connections every known connection
     * @return the connections with {@code accessibility == YES}
     */
    public static List<Connection> filterConfirmedAccessible(List<Connection> connections) {
        List<Connection> accessible = new ArrayList<>();
        for (Connection connection : connections) {
            if (connection.getAccessibility() == AccessibilityStatus.YES) {
                accessible.add(connection);
            }
        }
        return accessible;
    }
}
