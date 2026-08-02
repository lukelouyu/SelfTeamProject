package seedu.unienable.ui.accessibility;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;

import org.junit.jupiter.api.Test;

import seedu.unienable.accessibility.classes.Connection;
import seedu.unienable.accessibility.enums.AccessibilityStatus;
import seedu.unienable.accessibility.enums.ShelterStatus;
import seedu.unienable.accessibility.enums.TraversalType;
import seedu.unienable.logic.graph.GraphPath;

class RouteFormatterTest {
    private static final String LIMITATIONS = "This is a confirmed-accessible route based on local data only. "
            + "It does not estimate travel time and does not guarantee real-world accessibility at the time "
            + "of travel.";
    private static final String DISCLAIMER = "Sample local accessibility reference data. Distances are "
            + "estimates and may be incomplete. Please verify with current campus information when needed.";

    @Test
    public void formatRoute_singleSegment_exactText() {
        GraphPath path = new GraphPath(List.of("A", "B"), 10);
        Connection segment = new Connection(1, "A", "B", 10, AccessibilityStatus.YES, TraversalType.PATH,
                ShelterStatus.YES, null, null);

        String result = RouteFormatter.formatRoute(path, List.of(segment));

        assertEquals("Route from A to B:\n"
                + "[1] A -> B | 10 m | PATH | Shelter: YES\n"
                + "Total distance: 10 m (1 segment)\n\n"
                + LIMITATIONS + "\n\n" + DISCLAIMER, result);
    }

    @Test
    public void formatRoute_segmentWithBarrierAndNotes_includesBoth() {
        GraphPath path = new GraphPath(List.of("A", "B"), 10);
        Connection segment = new Connection(1, "A", "B", 10, AccessibilityStatus.YES, TraversalType.RAMP,
                ShelterStatus.NO, "Steps", "Avoid peak hours");

        String result = RouteFormatter.formatRoute(path, List.of(segment));

        assertEquals("Route from A to B:\n"
                + "[1] A -> B | 10 m | RAMP | Shelter: NO | Barrier: Steps | Notes: Avoid peak hours\n"
                + "Total distance: 10 m (1 segment)\n\n"
                + LIMITATIONS + "\n\n" + DISCLAIMER, result);
    }

    @Test
    public void formatRoute_zeroLengthSameFacility_exactText() {
        GraphPath path = new GraphPath(List.of("A"), 0);

        String result = RouteFormatter.formatRoute(path, List.of());

        assertEquals("Route from A to A:\n"
                + "Source and destination are the same facility. No travel is required.\n"
                + "Total distance: 0 m (0 segments)\n\n"
                + LIMITATIONS + "\n\n" + DISCLAIMER, result);
    }

    @Test
    public void formatNoRoute_exactText() {
        String result = RouteFormatter.formatNoRoute("A", "D");

        assertEquals("No supported accessible route was found between \"A\" and \"D\" in UniEnable's local "
                + "dataset.\n"
                + "This does not mean no real-world accessible route exists - only that one is not "
                + "confirmed here.\n"
                + "Please ask people around you.\n"
                + "We are still improving our database.\n\n"
                + DISCLAIMER, result);
    }
}
