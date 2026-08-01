package seedu.unienable.logic.graph;

import java.util.List;

/**
 * An immutable shortest-path result from {@link AccessibilityGraph#getShortestPath}: the ordered
 * facility names visited from source to destination (inclusive of both), and the total distance
 * in metres.
 */
public final class GraphPath {
    private final List<String> facilityNames;
    private final int totalDistanceInMetres;

    /**
     * Creates a GraphPath.
     *
     * @param facilityNames the facility names visited, in travel order, source to destination
     * @param totalDistanceInMetres the path's total distance in metres
     */
    public GraphPath(List<String> facilityNames, int totalDistanceInMetres) {
        this.facilityNames = List.copyOf(facilityNames);
        this.totalDistanceInMetres = totalDistanceInMetres;
    }

    /** Returns the facility names visited, in travel order, source to destination. */
    public List<String> getFacilityNames() {
        return facilityNames;
    }

    /** Returns the path's total distance in metres. */
    public int getTotalDistanceInMetres() {
        return totalDistanceInMetres;
    }
}
