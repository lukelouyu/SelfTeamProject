package seedu.unienable.logic.graph;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import seedu.unienable.accessibility.classes.Connection;
import seedu.unienable.accessibility.classes.Facility;
import seedu.unienable.accessibility.enums.AccessibilityStatus;
import seedu.unienable.accessibility.enums.ShelterStatus;
import seedu.unienable.accessibility.enums.TraversalType;
import seedu.unienable.exception.InvalidIndexException;
import seedu.unienable.logic.ConnectionManager;
import seedu.unienable.logic.FacilityManager;
import seedu.unienable.storage.ConnectionStorage;
import seedu.unienable.storage.FacilityStorage;
import seedu.unienable.storage.LoadResult;

/**
 * Covers {@link AccessibilityGraph} both against a small synthetic dataset (for exact control over
 * edge cases) and against the real bundled NUS FASS facilities.txt/connections.txt dataset (per
 * the technical-debt-hardening requirement to verify construction and pathfinding against the
 * known dataset).
 */
class AccessibilityGraphTest {
    private static Facility facility(String name) {
        return new Facility(name, name, null, List.of());
    }

    private static Connection connection(int id, String from, String to, int distanceInMetres) {
        return new Connection(id, from, to, distanceInMetres, AccessibilityStatus.YES, TraversalType.PATH,
                ShelterStatus.YES, null, null);
    }

    private static AccessibilityGraph syntheticGraph() {
        // A -- 10 -- B -- 10 -- C, with a direct A -- 5 -- C shortcut; D is fully isolated.
        FacilityManager facilityManager = new FacilityManager(
                List.of(facility("A"), facility("B"), facility("C"), facility("D")));
        ConnectionManager connectionManager = new ConnectionManager(List.of(
                connection(1, "A", "B", 10),
                connection(2, "B", "C", 10),
                connection(3, "A", "C", 5)));
        return new AccessibilityGraph(facilityManager, connectionManager);
    }

    private static Path resourcePath(String fileName) throws URISyntaxException {
        URL url = AccessibilityGraphTest.class.getClassLoader().getResource(fileName);
        return Paths.get(url.toURI());
    }

    private static AccessibilityGraph bundledDatasetGraph() throws Exception {
        LoadResult<Facility> facilities = new FacilityStorage().load(resourcePath("facilities.txt"));
        LoadResult<Connection> connections = new ConnectionStorage().load(resourcePath("connections.txt"));
        return new AccessibilityGraph(new FacilityManager(facilities.getRecords()),
                new ConnectionManager(connections.getRecords()));
    }

    @Test
    public void getShortestPath_shorterDirectEdgeBeatsLongerTwoHopPath_returnsDirectEdge() throws Exception {
        GraphPath path = syntheticGraph().getShortestPath("A", "C");

        assertEquals(List.of("A", "C"), path.getFacilityNames());
        assertEquals(5, path.getTotalDistanceInMetres());
    }

    @Test
    public void getShortestPath_sameFacility_returnsSingleNodeZeroDistancePath() throws Exception {
        GraphPath path = syntheticGraph().getShortestPath("A", "A");

        assertEquals(List.of("A"), path.getFacilityNames());
        assertEquals(0, path.getTotalDistanceInMetres());
    }

    @Test
    public void getShortestPath_isolatedFacility_returnsNull() throws Exception {
        assertNull(syntheticGraph().getShortestPath("A", "D"));
    }

    @Test
    public void getShortestPath_caseInsensitiveNames_resolvesToCanonicalCase() throws Exception {
        GraphPath path = syntheticGraph().getShortestPath("a", "c");

        assertEquals(List.of("A", "C"), path.getFacilityNames());
    }

    @Test
    public void getShortestPath_unknownFromFacility_throwsInvalidIndexException() {
        assertThrows(InvalidIndexException.class, () -> syntheticGraph().getShortestPath("Z", "A"));
    }

    @Test
    public void getShortestPath_unknownToFacility_throwsInvalidIndexException() {
        assertThrows(InvalidIndexException.class, () -> syntheticGraph().getShortestPath("A", "Z"));
    }

    @Test
    public void bundledDataset_directConnection_isPreferredOverLongerRoute() throws Exception {
        // AS6-AS8 is a direct 45m edge; the alternative via CLB is 70 + 55 = 125m.
        GraphPath path = bundledDatasetGraph().getShortestPath("AS6", "AS8");

        assertEquals(List.of("AS6", "AS8"), path.getFacilityNames());
        assertEquals(45, path.getTotalDistanceInMetres());
    }

    @Test
    public void bundledDataset_clbToAs3_routesThroughTheOnlyBarrierFreeLinkAtAs1As2() throws Exception {
        // AS2/AS3 are only reachable from the rest of FASS via the AS1-AS2 connection (see the
        // connections.txt note on connection 7), so the shortest path must pass through it:
        // CLB -70-> AS6 -90-> AS1 -130-> AS2 -35-> AS3 = 325m.
        GraphPath path = bundledDatasetGraph().getShortestPath("CLB", "AS3");

        assertEquals(List.of("CLB", "AS6", "AS1", "AS2", "AS3"), path.getFacilityNames());
        assertEquals(325, path.getTotalDistanceInMetres());
    }

    @Test
    public void getShortestPath_cumulativeDistanceExceedsIntegerMaxValue_choosesTrueShortestPath()
            throws Exception {
        // Two-hop path A-B-C sums to 2_400_000_000, which overflows a plain 32-bit int
        // (wraps to a large negative number), while the direct A-C edge is a smaller,
        // still-valid single distance of 2_000_000_000. A correct long-based accumulator must
        // still prefer the direct edge; a buggy int accumulator would wrongly prefer the
        // "cheaper-looking" overflowed two-hop path instead.
        FacilityManager facilityManager = new FacilityManager(
                List.of(facility("A"), facility("B"), facility("C")));
        ConnectionManager connectionManager = new ConnectionManager(List.of(
                connection(1, "A", "B", 1_200_000_000),
                connection(2, "B", "C", 1_200_000_000),
                connection(3, "A", "C", 2_000_000_000)));
        AccessibilityGraph graph = new AccessibilityGraph(facilityManager, connectionManager);

        GraphPath path = graph.getShortestPath("A", "C");

        assertEquals(List.of("A", "C"), path.getFacilityNames());
        assertEquals(2_000_000_000L, path.getTotalDistanceInMetres());
    }

    @Test
    public void getShortestPath_multiHopAboveIntegerMaxValue_reportsExactUnwrappedTotal() throws Exception {
        // A-B-C-D, each hop 1_000_000_000, total 3_000_000_000 - comfortably above
        // Integer.MAX_VALUE (2_147_483_647) with no shorter alternative available.
        FacilityManager facilityManager = new FacilityManager(
                List.of(facility("A"), facility("B"), facility("C"), facility("D")));
        ConnectionManager connectionManager = new ConnectionManager(List.of(
                connection(1, "A", "B", 1_000_000_000),
                connection(2, "B", "C", 1_000_000_000),
                connection(3, "C", "D", 1_000_000_000)));
        AccessibilityGraph graph = new AccessibilityGraph(facilityManager, connectionManager);

        GraphPath path = graph.getShortestPath("A", "D");

        assertEquals(List.of("A", "B", "C", "D"), path.getFacilityNames());
        assertEquals(3_000_000_000L, path.getTotalDistanceInMetres());
    }

    @Test
    public void reconstructPath_cyclicPredecessorMap_throwsInsteadOfLoopingIndefinitely() throws Exception {
        AccessibilityGraph graph = syntheticGraph();
        Map<String, String> cyclicPrevious = new HashMap<>();
        cyclicPrevious.put("a", "b");
        cyclicPrevious.put("b", "a");

        assertThrows(IllegalStateException.class,
                () -> graph.reconstructPath("a", 0L, cyclicPrevious, Map.of()));
    }

    @Test
    public void getShortestPath_parallelEdges_retainsExactChosenConnection() throws Exception {
        // Bug B regression: A-B has two parallel connections, a longer 100m one (id 1) and a
        // shorter 50m one (id 2). Dijkstra must choose the 50m connection, and GraphPath must
        // retain that exact Connection object - not merely "some connection between A and B",
        // which would be ambiguous between the two.
        FacilityManager facilityManager = new FacilityManager(List.of(facility("A"), facility("B")));
        Connection longer = new Connection(1, "A", "B", 100, AccessibilityStatus.YES, TraversalType.PATH,
                ShelterStatus.YES, null, "Long path");
        Connection shorter = new Connection(2, "A", "B", 50, AccessibilityStatus.YES, TraversalType.RAMP,
                ShelterStatus.NO, null, "Short ramp");
        ConnectionManager connectionManager = new ConnectionManager(List.of(longer, shorter));
        AccessibilityGraph graph = new AccessibilityGraph(facilityManager, connectionManager);

        GraphPath path = graph.getShortestPath("A", "B");

        assertEquals(50L, path.getTotalDistanceInMetres());
        assertEquals(List.of(shorter), path.getConnections());
    }

    @Test
    public void getShortestPath_parallelEdgesOppositeLoadOrder_stillRetainsShorterConnection() throws Exception {
        // Same as above but with the shorter connection listed (and therefore loaded) first, to
        // confirm the fix picks the connection Dijkstra actually used rather than always
        // preferring one particular load-order position.
        FacilityManager facilityManager = new FacilityManager(List.of(facility("A"), facility("B")));
        Connection shorter = new Connection(1, "A", "B", 50, AccessibilityStatus.YES, TraversalType.RAMP,
                ShelterStatus.NO, null, "Short ramp");
        Connection longer = new Connection(2, "A", "B", 100, AccessibilityStatus.YES, TraversalType.PATH,
                ShelterStatus.YES, null, "Long path");
        ConnectionManager connectionManager = new ConnectionManager(List.of(shorter, longer));
        AccessibilityGraph graph = new AccessibilityGraph(facilityManager, connectionManager);

        GraphPath path = graph.getShortestPath("A", "B");

        assertEquals(50L, path.getTotalDistanceInMetres());
        assertEquals(List.of(shorter), path.getConnections());
    }

    @Test
    public void bundledDataset_everyFacilityReachesEveryOtherFacility() throws Exception {
        AccessibilityGraph graph = bundledDatasetGraph();
        LoadResult<Facility> facilities = new FacilityStorage().load(resourcePath("facilities.txt"));

        for (Facility from : facilities.getRecords()) {
            for (Facility to : facilities.getRecords()) {
                assertTrue(graph.getShortestPath(from.getName(), to.getName()) != null,
                        "expected a path from " + from.getName() + " to " + to.getName());
            }
        }
    }
}
