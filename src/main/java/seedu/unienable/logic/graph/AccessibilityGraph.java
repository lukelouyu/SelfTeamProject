package seedu.unienable.logic.graph;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Set;

import seedu.unienable.accessibility.classes.Connection;
import seedu.unienable.accessibility.classes.Facility;
import seedu.unienable.exception.InvalidIndexException;
import seedu.unienable.logic.ConnectionManager;
import seedu.unienable.logic.FacilityManager;

/**
 * An immutable, weighted, undirected graph over a facility/connection reference dataset, built
 * once at construction time. Provides Dijkstra-based shortest-path lookups by total distance.
 *
 * <p>This class applies no accessibility-status or other filtering policy of its own - it builds
 * an edge for every connection it is given. v1.0's {@code FacilityManager}/{@code
 * ConnectionManager} stay completely independent read-only lookups with no cross-reference between
 * them; this class only <em>reads</em> from whatever facility/connection lists it is constructed
 * with, never modifying either. v2.0's {@code route} command is the first caller of {@link
 * #getShortestPath}, via {@code logic.route.AccessibleRouteGraphFactory}, which builds a graph
 * over a YES-only-filtered connection list rather than every connection - that filtering is
 * route-specific policy and deliberately lives in the factory, not here.
 */
public final class AccessibilityGraph {
    private final Map<String, List<Edge>> adjacency;
    private final Map<String, String> canonicalNames;

    /**
     * Builds the graph from every facility (as a node) and every connection (as a two-way,
     * distance-weighted edge) currently held by the given managers.
     *
     * @param facilityManager the facility reference data to build nodes from
     * @param connectionManager the connection reference data to build edges from
     */
    public AccessibilityGraph(FacilityManager facilityManager, ConnectionManager connectionManager) {
        this(facilityManager.list(), connectionManager.list());
    }

    /**
     * Builds the graph directly from facility/connection lists rather than manager objects, so a
     * caller that has already filtered or otherwise derived a connection list (e.g. v2.0's
     * {@code route} command, which needs only confirmed-accessible connections) can build a graph
     * over exactly that list without constructing a manager purely to hold it. This constructor
     * applies no filtering or policy of its own - every given connection becomes an edge, exactly
     * like the manager-based constructor.
     *
     * @param facilities the facility reference data to build nodes from
     * @param connections the connection reference data to build edges from
     */
    public AccessibilityGraph(List<Facility> facilities, List<Connection> connections) {
        Map<String, String> names = new HashMap<>();
        Map<String, List<Edge>> graph = new HashMap<>();
        for (Facility facility : facilities) {
            String key = facility.getName().toLowerCase(Locale.ROOT);
            names.put(key, facility.getName());
            graph.put(key, new ArrayList<>());
        }
        for (Connection connection : connections) {
            addDirectedEdge(graph, connection.getFrom(), connection.getTo(), connection);
            addDirectedEdge(graph, connection.getTo(), connection.getFrom(), connection);
        }
        Map<String, List<Edge>> frozenGraph = new HashMap<>();
        for (Map.Entry<String, List<Edge>> entry : graph.entrySet()) {
            frozenGraph.put(entry.getKey(), List.copyOf(entry.getValue()));
        }
        this.adjacency = Map.copyOf(frozenGraph);
        this.canonicalNames = Map.copyOf(names);
    }

    private void addDirectedEdge(Map<String, List<Edge>> graph, String from, String to, Connection connection) {
        String key = from.toLowerCase(Locale.ROOT);
        graph.get(key).add(new Edge(to.toLowerCase(Locale.ROOT), connection));
    }

    /**
     * Finds the shortest path between two facilities by total distance, using Dijkstra's
     * algorithm (valid since every stored connection distance is a positive whole number - see
     * {@code ConnectionStorage}'s load-time validation).
     *
     * @param from the source facility name (case-insensitive)
     * @param to the destination facility name (case-insensitive)
     * @return the shortest path and its total distance, or null if no path connects them
     * @throws InvalidIndexException if either name is not a known facility
     */
    public GraphPath getShortestPath(String from, String to) throws InvalidIndexException {
        String fromKey = requireKnownFacility(from);
        String toKey = requireKnownFacility(to);

        Map<String, Long> bestDistance = new HashMap<>();
        Map<String, String> previous = new HashMap<>();
        Map<String, Connection> viaConnection = new HashMap<>();
        Set<String> settled = new HashSet<>();
        PriorityQueue<QueueEntry> queue = new PriorityQueue<>();

        bestDistance.put(fromKey, 0L);
        queue.add(new QueueEntry(fromKey, 0L));

        while (!queue.isEmpty()) {
            QueueEntry current = queue.poll();
            if (!settled.add(current.key)) {
                continue;
            }
            if (current.key.equals(toKey)) {
                break;
            }
            for (Edge edge : adjacency.get(current.key)) {
                // Individual connection distances stay int (bounded by ConnectionStorage's
                // load-time validation), but a route may cross many edges, so the running total
                // is accumulated as long to avoid silently wrapping past Integer.MAX_VALUE.
                long candidate = current.distanceInMetres + (long) edge.connection.getDistanceInMetres();
                Long known = bestDistance.get(edge.toKey);
                if (known == null || candidate < known) {
                    bestDistance.put(edge.toKey, candidate);
                    previous.put(edge.toKey, current.key);
                    // Recorded in the same relaxation step that decides bestDistance/previous, so
                    // it can never disagree with the distance actually summed - unlike
                    // re-deriving "some connection between these two facility names" afterwards,
                    // which is ambiguous whenever parallel connections exist between the same pair.
                    viaConnection.put(edge.toKey, edge.connection);
                    queue.add(new QueueEntry(edge.toKey, candidate));
                }
            }
        }

        Long totalDistance = bestDistance.get(toKey);
        if (totalDistance == null) {
            return null;
        }
        return reconstructPath(toKey, totalDistance, previous, viaConnection);
    }

    // Package-private (rather than private) so AccessibilityGraphTest can exercise the cycle
    // guard directly with a deliberately malformed "previous" map, without needing a corrupted
    // Dijkstra run to reach it.
    GraphPath reconstructPath(String toKey, long totalDistanceInMetres, Map<String, String> previous,
            Map<String, Connection> viaConnection) {
        List<String> facilityNames = new ArrayList<>();
        List<Connection> connections = new ArrayList<>();
        Set<String> visited = new HashSet<>();
        String step = toKey;
        while (step != null) {
            if (!visited.add(step)) {
                throw new IllegalStateException(
                        "Cycle detected while reconstructing route path at facility \"" + step + "\".");
            }
            facilityNames.add(canonicalNames.get(step));
            Connection connection = viaConnection.get(step);
            if (connection != null) {
                connections.add(connection);
            }
            step = previous.get(step);
        }
        Collections.reverse(facilityNames);
        Collections.reverse(connections);
        return new GraphPath(facilityNames, connections, totalDistanceInMetres);
    }

    private String requireKnownFacility(String name) throws InvalidIndexException {
        String key = name.toLowerCase(Locale.ROOT);
        if (!adjacency.containsKey(key)) {
            throw new InvalidIndexException("Facility \"" + name + "\" is not recognised.");
        }
        return key;
    }

    /**
     * One directed edge to a neighbouring facility (by lower-cased name), carrying the exact
     * {@link Connection} it was built from so a winning relaxation can record precisely which
     * connection produced it - not just the distance - even when parallel connections exist
     * between the same two facilities.
     */
    private static final class Edge {
        private final String toKey;
        private final Connection connection;

        private Edge(String toKey, Connection connection) {
            this.toKey = toKey;
            this.connection = connection;
        }
    }

    /**
     * One Dijkstra frontier entry: a facility (by lower-cased name) and the cumulative distance
     * found when this entry was created. Distance is fixed at construction, never mutated in
     * place, so the priority queue's heap invariant stays valid even though a facility may be
     * pushed more than once with different distances as shorter paths are discovered ("lazy
     * deletion" Dijkstra). Held as {@code long} since a route's cumulative distance can exceed
     * {@code Integer.MAX_VALUE} even though every individual edge distance is a valid {@code int}.
     */
    private static final class QueueEntry implements Comparable<QueueEntry> {
        private final String key;
        private final long distanceInMetres;

        private QueueEntry(String key, long distanceInMetres) {
            this.key = key;
            this.distanceInMetres = distanceInMetres;
        }

        @Override
        public int compareTo(QueueEntry other) {
            return Long.compare(distanceInMetres, other.distanceInMetres);
        }
    }
}
