package seedu.unienable.logic.graph;

import java.util.List;

/**
 * An immutable shortest-path result from {@link AccessibilityGraph#getShortestPath}: the ordered
 * facility names visited from source to destination (inclusive of both), and the total distance
 * in metres.
 *
 * <p>The total is held as {@code long} rather than {@code int}: each individual connection
 * distance is a valid {@code int}, but a multi-hop route's cumulative distance can exceed {@code
 * Integer.MAX_VALUE}, so only the running total needs the wider type.
 */
public final class GraphPath {
    private final List<String> facilityNames;
    private final long totalDistanceInMetres;

    /**
     * Creates a GraphPath.
     *
     * @param facilityNames the facility names visited, in travel order, source to destination
     * @param totalDistanceInMetres the path's total distance in metres
     */
    public GraphPath(List<String> facilityNames, long totalDistanceInMetres) {
        this.facilityNames = List.copyOf(facilityNames);
        this.totalDistanceInMetres = totalDistanceInMetres;
    }

    /** Returns the facility names visited, in travel order, source to destination. */
    public List<String> getFacilityNames() {
        return facilityNames;
    }

    /** Returns the path's total distance in metres. */
    public long getTotalDistanceInMetres() {
        return totalDistanceInMetres;
    }
}
