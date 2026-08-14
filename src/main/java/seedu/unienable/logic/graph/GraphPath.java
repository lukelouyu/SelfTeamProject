package seedu.unienable.logic.graph;

import java.util.List;

import seedu.unienable.accessibility.classes.Connection;

/**
 * An immutable shortest-path result from {@link AccessibilityGraph#getShortestPath}: the ordered
 * facility names visited from source to destination (inclusive of both), the exact connection
 * Dijkstra selected for each consecutive hop, and the total distance in metres.
 *
 * <p>{@code connections} always has exactly {@code facilityNames.size() - 1} entries: one per
 * consecutive facility pair, in travel order. It is populated by the same relaxation step that
 * decides {@code totalDistanceInMetres}, so - unlike re-deriving a connection afterwards by
 * looking up any connection between two facility names - it can never disagree with the distance
 * actually summed, even when multiple parallel connections exist between the same two facilities.
 *
 * <p>The total is held as {@code long} rather than {@code int}: each individual connection
 * distance is a valid {@code int}, but a multi-hop route's cumulative distance can exceed {@code
 * Integer.MAX_VALUE}, so only the running total needs the wider type.
 */
public final class GraphPath {
    private final List<String> facilityNames;
    private final List<Connection> connections;
    private final long totalDistanceInMetres;

    /**
     * Creates a GraphPath.
     *
     * @param facilityNames the facility names visited, in travel order, source to destination
     * @param connections the exact connection used for each consecutive hop, in travel order;
     *     must have exactly {@code facilityNames.size() - 1} entries
     * @param totalDistanceInMetres the path's total distance in metres
     */
    public GraphPath(List<String> facilityNames, List<Connection> connections, long totalDistanceInMetres) {
        this.facilityNames = List.copyOf(facilityNames);
        this.connections = List.copyOf(connections);
        this.totalDistanceInMetres = totalDistanceInMetres;
    }

    /** Returns the facility names visited, in travel order, source to destination. */
    public List<String> getFacilityNames() {
        return facilityNames;
    }

    /**
     * Returns the exact connection Dijkstra selected for each consecutive hop, in travel order -
     * one entry per consecutive facility pair in {@link #getFacilityNames()}.
     */
    public List<Connection> getConnections() {
        return connections;
    }

    /** Returns the path's total distance in metres. */
    public long getTotalDistanceInMetres() {
        return totalDistanceInMetres;
    }
}
