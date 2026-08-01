package seedu.unienable.logic.graph;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

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
