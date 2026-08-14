package seedu.unienable.command.accessibility.route;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

import seedu.unienable.accessibility.classes.Connection;
import seedu.unienable.accessibility.classes.Facility;
import seedu.unienable.accessibility.enums.AccessibilityStatus;
import seedu.unienable.accessibility.enums.ShelterStatus;
import seedu.unienable.accessibility.enums.TraversalType;
import seedu.unienable.command.CommandResult;
import seedu.unienable.exception.InvalidIndexException;
import seedu.unienable.logic.ConnectionManager;
import seedu.unienable.logic.FacilityManager;

/** Covers {@link RouteCommand} against small synthetic fixtures - no real bundled dataset. */
class RouteCommandTest {
    private static Facility facility(String name) {
        return new Facility(name, name, null, List.of());
    }

    private static Connection connection(int id, String from, String to, int distanceInMetres,
            AccessibilityStatus status, TraversalType type, ShelterStatus shelter, String barrier, String notes) {
        return new Connection(id, from, to, distanceInMetres, status, type, shelter, barrier, notes);
    }

    private static FacilityManager threeFacilityChain() {
        return new FacilityManager(List.of(facility("A"), facility("B"), facility("C"), facility("D")));
    }

    private static ConnectionManager threeFacilityChainConnections() {
        return new ConnectionManager(List.of(
                connection(1, "A", "B", 10, AccessibilityStatus.YES, TraversalType.PATH, ShelterStatus.YES,
                        null, null),
                connection(2, "B", "C", 15, AccessibilityStatus.YES, TraversalType.SHELTERED_RAMP,
                        ShelterStatus.NO, "Steps at B", "Avoid peak hours"),
                connection(3, "A", "D", 5, AccessibilityStatus.NO, TraversalType.RAMP, ShelterStatus.UNKNOWN,
                        null, null)));
    }

    @Test
    public void execute_directRoute_showsSingleSegmentWithFullDetail() throws Exception {
        RouteCommand command = new RouteCommand(threeFacilityChain(), threeFacilityChainConnections(), "A", "B");

        String feedback = command.execute().getFeedback();

        assertTrue(feedback.startsWith("Route from A to B:"));
        assertTrue(feedback.contains("[1] A -> B | 10 m | PATH | Shelter: YES"));
        assertTrue(feedback.contains("Total distance: 10 m (1 segment)"));
    }

    @Test
    public void execute_multiEdgeRoute_showsEveryOrderedSegment() throws Exception {
        RouteCommand command = new RouteCommand(threeFacilityChain(), threeFacilityChainConnections(), "A", "C");

        String feedback = command.execute().getFeedback();

        assertTrue(feedback.contains("[1] A -> B | 10 m | PATH | Shelter: YES"));
        assertTrue(feedback.contains("[2] B -> C | 15 m | SHELTERED_RAMP | Shelter: NO | Barrier: Steps at B"
                + " | Notes: Avoid peak hours"));
        assertTrue(feedback.contains("Total distance: 25 m (2 segments)"));
    }

    @Test
    public void execute_segmentDirection_matchesTravelDirectionNotStoredConnectionOrder() throws Exception {
        FacilityManager facilities = new FacilityManager(List.of(facility("Y"), facility("Z")));
        ConnectionManager connections = new ConnectionManager(List.of(
                connection(1, "Z", "Y", 8, AccessibilityStatus.YES, TraversalType.PATH, ShelterStatus.YES,
                        null, null)));
        RouteCommand command = new RouteCommand(facilities, connections, "Y", "Z");

        String feedback = command.execute().getFeedback();

        assertTrue(feedback.contains("[1] Y -> Z | 8 m"));
        assertFalse(feedback.contains("Z -> Y"));
    }

    @Test
    public void execute_segmentWithoutBarrierOrNotes_omitsBoth() throws Exception {
        RouteCommand command = new RouteCommand(threeFacilityChain(), threeFacilityChainConnections(), "A", "B");

        String feedback = command.execute().getFeedback();

        assertFalse(feedback.contains("Barrier:"));
        assertFalse(feedback.contains("Notes:"));
    }

    @Test
    public void execute_sameKnownFacility_returnsZeroLengthSuccess() throws Exception {
        RouteCommand command = new RouteCommand(threeFacilityChain(), threeFacilityChainConnections(), "A", "A");

        String feedback = command.execute().getFeedback();

        assertTrue(feedback.startsWith("Route from A to A:"));
        assertTrue(feedback.contains("Source and destination are the same facility. No travel is required."));
        assertTrue(feedback.contains("Total distance: 0 m (0 segments)"));
    }

    @Test
    public void execute_unknownSourceFacility_throwsInvalidIndexException() {
        RouteCommand command = new RouteCommand(threeFacilityChain(), threeFacilityChainConnections(),
                "NoSuchPlace", "B");

        InvalidIndexException exception = assertThrows(InvalidIndexException.class, command::execute);

        assertTrue(exception.getMessage().contains("NoSuchPlace"));
    }

    @Test
    public void execute_unknownDestinationFacility_throwsInvalidIndexException() {
        RouteCommand command = new RouteCommand(threeFacilityChain(), threeFacilityChainConnections(), "A",
                "NoSuchPlace");

        InvalidIndexException exception = assertThrows(InvalidIndexException.class, command::execute);

        assertTrue(exception.getMessage().contains("NoSuchPlace"));
    }

    @Test
    public void execute_disconnectedKnownFacilities_showsNoSupportedRouteFallback() throws Exception {
        RouteCommand command = new RouteCommand(threeFacilityChain(), threeFacilityChainConnections(), "A", "D");

        String feedback = command.execute().getFeedback();

        assertTrue(feedback.startsWith("No supported accessible route was found between \"A\" and \"D\""));
        assertTrue(feedback.contains("Please ask people around you."));
        assertTrue(feedback.contains("We are still improving our database."));
        assertTrue(feedback.contains("This does not mean no real-world accessible route exists"));
    }

    @Test
    public void execute_onlyPathUsesNoStatusEdge_treatedAsDisconnected() throws Exception {
        // A-D exists only via connection 3, which is status NO - route must not use it even though
        // it is the only physical connection between them.
        RouteCommand command = new RouteCommand(threeFacilityChain(), threeFacilityChainConnections(), "D", "A");

        String feedback = command.execute().getFeedback();

        assertTrue(feedback.startsWith("No supported accessible route was found"));
    }

    @Test
    public void execute_caseInsensitiveFacilityNames_resolvesAndNormalizesOutput() throws Exception {
        RouteCommand command = new RouteCommand(threeFacilityChain(), threeFacilityChainConnections(), "a", "b");

        String feedback = command.execute().getFeedback();

        assertTrue(feedback.startsWith("Route from A to B:"));
    }

    @Test
    public void execute_output_neverContainsTimeEstimateOrGuaranteeLanguage() throws Exception {
        CommandResult result = new RouteCommand(threeFacilityChain(), threeFacilityChainConnections(), "A", "C")
                .execute();

        String feedback = result.getFeedback();
        assertFalse(feedback.contains("minutes"));
        assertFalse(feedback.contains("ETA"));
        assertTrue(feedback.contains("does not guarantee real-world accessibility"));
        assertTrue(feedback.contains("Sample local accessibility reference data."));
    }

    @Test
    public void execute_disconnectedRoute_showsAccessibilityDisclaimer() throws Exception {
        String feedback = new RouteCommand(threeFacilityChain(), threeFacilityChainConnections(), "A", "D")
                .execute().getFeedback();

        assertTrue(feedback.contains("Sample local accessibility reference data."));
    }

    @Test
    public void route_parallelEdges_displaysChosenEdgeAndMatchingDistance() throws Exception {
        // Bug B regression: two parallel accessible connections between the same facility pair -
        // a longer 100m PATH and a shorter 50m RAMP. The displayed segment must be the 50m RAMP
        // (the one Dijkstra actually used), and its distance must match the printed total exactly.
        FacilityManager facilities = new FacilityManager(List.of(facility("A"), facility("B")));
        ConnectionManager connections = new ConnectionManager(List.of(
                connection(1, "A", "B", 100, AccessibilityStatus.YES, TraversalType.PATH, ShelterStatus.YES,
                        null, "Long path"),
                connection(2, "A", "B", 50, AccessibilityStatus.YES, TraversalType.RAMP, ShelterStatus.NO,
                        null, "Short ramp")));
        RouteCommand command = new RouteCommand(facilities, connections, "A", "B");

        String feedback = command.execute().getFeedback();

        assertTrue(feedback.contains("[1] A -> B | 50 m | RAMP | Shelter: NO | Notes: Short ramp"));
        assertFalse(feedback.contains("100 m"));
        assertFalse(feedback.contains("Long path"));
        assertTrue(feedback.contains("Total distance: 50 m (1 segment)"));
    }

    @Test
    public void route_parallelEdgesShorterListedFirst_stillDisplaysShorterChosenEdge() throws Exception {
        // Same scenario with load order reversed, to confirm the fix isn't just "always the last
        // one loaded" or "always the first one loaded" but genuinely the one Dijkstra chose.
        FacilityManager facilities = new FacilityManager(List.of(facility("A"), facility("B")));
        ConnectionManager connections = new ConnectionManager(List.of(
                connection(1, "A", "B", 50, AccessibilityStatus.YES, TraversalType.RAMP, ShelterStatus.NO,
                        null, "Short ramp"),
                connection(2, "A", "B", 100, AccessibilityStatus.YES, TraversalType.PATH, ShelterStatus.YES,
                        null, "Long path")));
        RouteCommand command = new RouteCommand(facilities, connections, "A", "B");

        String feedback = command.execute().getFeedback();

        assertTrue(feedback.contains("[1] A -> B | 50 m | RAMP | Shelter: NO | Notes: Short ramp"));
        assertTrue(feedback.contains("Total distance: 50 m (1 segment)"));
    }
}
