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
 * An immutable, weighted, undirected graph over the facility/connection reference dataset, built
 * once at construction time from an already-loaded {@link FacilityManager} and
 * {@link ConnectionManager}. Provides Dijkstra-based shortest-path lookups by total distance.
 *
 * <p>This is preparatory v2.0 infrastructure: v1.0's {@code FacilityManager} and
 * {@code ConnectionManager} stay completely independent read-only lookups with no cross-reference
 * between them, exactly as before. This class is an add-on that only <em>reads</em> from both -
 * neither manager is modified, and this class is not referenced by any v1.0 command or by
 * {@code CommandDispatcher}/{@code ApplicationRunner}. v2.0's {@code route} command will be the
 * first caller of {@link #getShortestPath}.
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
        Map<String, String> names = new HashMap<>();
        Map<String, List<Edge>> graph = new HashMap<>();
        for (Facility facility : facilityManager.list()) {
            String key = facility.getName().toLowerCase(Locale.ROOT);
            names.put(key, facility.getName());
            graph.put(key, new ArrayList<>());
        }
        for (Connection connection : connectionManager.list()) {
            addDirectedEdge(graph, connection.getFrom(), connection.getTo(), connection.getDistanceInMetres());
            addDirectedEdge(graph, connection.getTo(), connection.getFrom(), connection.getDistanceInMetres());
        }
        Map<String, List<Edge>> frozenGraph = new HashMap<>();
        for (Map.Entry<String, List<Edge>> entry : graph.entrySet()) {
            frozenGraph.put(entry.getKey(), List.copyOf(entry.getValue()));
        }
        this.adjacency = Map.copyOf(frozenGraph);
        this.canonicalNames = Map.copyOf(names);
    }

    private void addDirectedEdge(Map<String, List<Edge>> graph, String from, String to, int distanceInMetres) {
        String key = from.toLowerCase(Locale.ROOT);
        graph.get(key).add(new Edge(to.toLowerCase(Locale.ROOT), distanceInMetres));
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

        Map<String, Integer> bestDistance = new HashMap<>();
        Map<String, String> previous = new HashMap<>();
        Set<String> settled = new HashSet<>();
        PriorityQueue<QueueEntry> queue = new PriorityQueue<>();

        bestDistance.put(fromKey, 0);
        queue.add(new QueueEntry(fromKey, 0));

        while (!queue.isEmpty()) {
            QueueEntry current = queue.poll();
            if (!settled.add(current.key)) {
                continue;
            }
            if (current.key.equals(toKey)) {
                break;
            }
            for (Edge edge : adjacency.get(current.key)) {
                int candidate = current.distanceInMetres + edge.distanceInMetres;
                Integer known = bestDistance.get(edge.toKey);
                if (known == null || candidate < known) {
                    bestDistance.put(edge.toKey, candidate);
                    previous.put(edge.toKey, current.key);
                    queue.add(new QueueEntry(edge.toKey, candidate));
                }
            }
        }

        Integer totalDistance = bestDistance.get(toKey);
        if (totalDistance == null) {
            return null;
        }
        return new GraphPath(reconstructPath(toKey, previous), totalDistance);
    }

    private List<String> reconstructPath(String toKey, Map<String, String> previous) {
        List<String> path = new ArrayList<>();
        String step = toKey;
        while (step != null) {
            path.add(canonicalNames.get(step));
            step = previous.get(step);
        }
        Collections.reverse(path);
        return path;
    }

    private String requireKnownFacility(String name) throws InvalidIndexException {
        String key = name.toLowerCase(Locale.ROOT);
        if (!adjacency.containsKey(key)) {
            throw new InvalidIndexException("Facility \"" + name + "\" is not recognised.");
        }
        return key;
    }

    /** One directed edge to a neighbouring facility (by lower-cased name) and its distance. */
    private static final class Edge {
        private final String toKey;
        private final int distanceInMetres;

        private Edge(String toKey, int distanceInMetres) {
            this.toKey = toKey;
            this.distanceInMetres = distanceInMetres;
        }
    }

    /**
     * One Dijkstra frontier entry: a facility (by lower-cased name) and the distance found when
     * this entry was created. Distance is fixed at construction, never mutated in place, so the
     * priority queue's heap invariant stays valid even though a facility may be pushed more than
     * once with different distances as shorter paths are discovered ("lazy deletion" Dijkstra).
     */
    private static final class QueueEntry implements Comparable<QueueEntry> {
        private final String key;
        private final int distanceInMetres;

        private QueueEntry(String key, int distanceInMetres) {
            this.key = key;
            this.distanceInMetres = distanceInMetres;
        }

        @Override
        public int compareTo(QueueEntry other) {
            return Integer.compare(distanceInMetres, other.distanceInMetres);
        }
    }
}
