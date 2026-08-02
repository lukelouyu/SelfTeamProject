package seedu.unienable.logic.route;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import seedu.unienable.accessibility.classes.Connection;
import seedu.unienable.accessibility.classes.Facility;
import seedu.unienable.accessibility.enums.AccessibilityStatus;
import seedu.unienable.accessibility.enums.ShelterStatus;
import seedu.unienable.accessibility.enums.TraversalType;
import seedu.unienable.logic.ConnectionManager;
import seedu.unienable.logic.FacilityManager;
import seedu.unienable.logic.graph.AccessibilityGraph;
import seedu.unienable.logic.graph.GraphPath;
import seedu.unienable.storage.ConnectionStorage;
import seedu.unienable.storage.FacilityStorage;
import seedu.unienable.storage.LoadResult;

/**
 * Covers {@link AccessibleRouteGraphFactory} against small synthetic fixtures only - no real
 * bundled NUS FASS dataset - per the approved route test constraints.
 */
class AccessibleRouteGraphFactoryTest {
    private static Facility facility(String name) {
        return new Facility(name, name, null, List.of());
    }

    private static Connection connection(int id, String from, String to, int distanceInMetres,
            AccessibilityStatus status) {
        return new Connection(id, from, to, distanceInMetres, status, TraversalType.PATH, ShelterStatus.YES, null,
                null);
    }

    @Test
    public void build_excludesNoAndUnknownConnections_onlyYesEdgesUsable() throws Exception {
        FacilityManager facilities = new FacilityManager(List.of(facility("A"), facility("B"), facility("C")));
        ConnectionManager connections = new ConnectionManager(List.of(
                connection(1, "A", "C", 1, AccessibilityStatus.NO),
                connection(2, "A", "B", 2, AccessibilityStatus.UNKNOWN),
                connection(3, "B", "C", 5, AccessibilityStatus.YES)));

        AccessibilityGraph graph = AccessibleRouteGraphFactory.build(facilities, connections);

        // The only path from A to C in the raw dataset goes through the NO and UNKNOWN edges;
        // both must be excluded, so A is unreachable from C in the filtered graph.
        assertNull(graph.getShortestPath("A", "C"));
        // B-C (YES) remains usable on its own.
        assertEquals(5, graph.getShortestPath("B", "C").getTotalDistanceInMetres());
    }

    @Test
    public void build_shorterMultiEdgePathBeatsLongerDirectEdge() throws Exception {
        FacilityManager facilities = new FacilityManager(List.of(facility("A"), facility("B"), facility("C")));
        ConnectionManager connections = new ConnectionManager(List.of(
                connection(1, "A", "B", 10, AccessibilityStatus.YES),
                connection(2, "B", "C", 10, AccessibilityStatus.YES),
                connection(3, "A", "C", 25, AccessibilityStatus.YES)));

        GraphPath path = AccessibleRouteGraphFactory.build(facilities, connections).getShortestPath("A", "C");

        assertEquals(List.of("A", "B", "C"), path.getFacilityNames());
        assertEquals(20, path.getTotalDistanceInMetres());
    }

    @Test
    public void build_fewerEdgesButLongerDistance_losesToMoreEdgesShorterDistance() throws Exception {
        // Proves Dijkstra (distance-optimal) rather than BFS (hop-count-optimal): the 1-edge direct
        // route is far longer than the 3-edge chain, so the chain must win.
        FacilityManager facilities = new FacilityManager(
                List.of(facility("A"), facility("B"), facility("C"), facility("D")));
        ConnectionManager connections = new ConnectionManager(List.of(
                connection(1, "A", "C", 100, AccessibilityStatus.YES),
                connection(2, "A", "B", 10, AccessibilityStatus.YES),
                connection(3, "B", "D", 10, AccessibilityStatus.YES),
                connection(4, "D", "C", 10, AccessibilityStatus.YES)));

        GraphPath path = AccessibleRouteGraphFactory.build(facilities, connections).getShortestPath("A", "C");

        assertEquals(List.of("A", "B", "D", "C"), path.getFacilityNames());
        assertEquals(30, path.getTotalDistanceInMetres());
    }

    @Test
    public void build_equalDistanceAlternatePaths_deterministicAcrossRepeatedCalls() throws Exception {
        FacilityManager facilities = new FacilityManager(
                List.of(facility("A"), facility("B"), facility("C"), facility("D")));
        ConnectionManager connections = new ConnectionManager(List.of(
                connection(1, "A", "B", 10, AccessibilityStatus.YES),
                connection(2, "B", "C", 10, AccessibilityStatus.YES),
                connection(3, "A", "D", 10, AccessibilityStatus.YES),
                connection(4, "D", "C", 10, AccessibilityStatus.YES)));

        AccessibilityGraph graph = AccessibleRouteGraphFactory.build(facilities, connections);
        GraphPath first = graph.getShortestPath("A", "C");
        GraphPath second = graph.getShortestPath("A", "C");

        assertEquals(20, first.getTotalDistanceInMetres());
        assertEquals(first.getFacilityNames(), second.getFacilityNames());
    }

    @Test
    public void build_malformedNegativeDistanceLines_areSkippedNotCrashing(@TempDir Path tempDir) throws Exception {
        Path facilitiesFile = tempDir.resolve("facilities.txt");
        Path connectionsFile = tempDir.resolve("connections.txt");
        Files.writeString(facilitiesFile, "FACILITY|F1|A|\nFACILITY|F2|B|\nFACILITY|F3|C|\n");
        Files.writeString(connectionsFile,
                "CONNECTION|1|A|B|10|YES|PATH|YES\n"
                        + "CONNECTION|2|B|C|-5|YES|PATH|YES\n"
                        + "not a connection line at all\n"
                        + "CONNECTION|3|B|C|15|YES|PATH|YES\n");

        LoadResult<Facility> facilityLoad = new FacilityStorage().load(facilitiesFile);
        LoadResult<Connection> connectionLoad = new ConnectionStorage().load(connectionsFile,
                facilityLoad.getRecords());

        assertEquals(2, connectionLoad.getWarnings().size());
        assertEquals(2, connectionLoad.getRecords().size());

        FacilityManager facilities = new FacilityManager(facilityLoad.getRecords());
        ConnectionManager connections = new ConnectionManager(connectionLoad.getRecords());
        GraphPath path = assertDoesNotThrow(
                () -> AccessibleRouteGraphFactory.build(facilities, connections).getShortestPath("A", "C"));

        assertEquals(25, path.getTotalDistanceInMetres());
    }

    @Test
    public void build_duplicateConnectionBetweenSamePair_secondSkippedAtLoadNotGraph(@TempDir Path tempDir)
            throws Exception {
        Path facilitiesFile = tempDir.resolve("facilities.txt");
        Path connectionsFile = tempDir.resolve("connections.txt");
        Files.writeString(facilitiesFile, "FACILITY|F1|A|\nFACILITY|F2|B|\n");
        Files.writeString(connectionsFile,
                "CONNECTION|1|A|B|10|YES|PATH|YES\n"
                        + "CONNECTION|1|A|B|99|YES|PATH|YES\n");

        LoadResult<Facility> facilityLoad = new FacilityStorage().load(facilitiesFile);
        LoadResult<Connection> connectionLoad = new ConnectionStorage().load(connectionsFile,
                facilityLoad.getRecords());

        assertEquals(1, connectionLoad.getWarnings().size());
        assertEquals(1, connectionLoad.getRecords().size());

        FacilityManager facilities = new FacilityManager(facilityLoad.getRecords());
        ConnectionManager connections = new ConnectionManager(connectionLoad.getRecords());
        GraphPath path = AccessibleRouteGraphFactory.build(facilities, connections).getShortestPath("A", "B");

        assertEquals(10, path.getTotalDistanceInMetres());
    }

    @Test
    public void build_isolatedSingleFacilityNoConnections_buildsWithoutError() {
        FacilityManager facilities = new FacilityManager(List.of(facility("A")));
        ConnectionManager connections = new ConnectionManager(List.of());

        GraphPath path = assertDoesNotThrow(
                () -> AccessibleRouteGraphFactory.build(facilities, connections).getShortestPath("A", "A"));

        assertEquals(List.of("A"), path.getFacilityNames());
        assertEquals(0, path.getTotalDistanceInMetres());
    }
}
