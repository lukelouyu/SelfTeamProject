package seedu.unienable.logic.graph;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import seedu.unienable.accessibility.classes.Connection;
import seedu.unienable.accessibility.enums.AccessibilityStatus;
import seedu.unienable.accessibility.enums.ShelterStatus;
import seedu.unienable.accessibility.enums.TraversalType;

class GraphPathTest {
    private static Connection connection(int id, String from, String to, int distanceInMetres) {
        return new Connection(id, from, to, distanceInMetres, AccessibilityStatus.YES, TraversalType.PATH,
                ShelterStatus.YES, null, null);
    }

    @Test
    public void getters_returnConstructorValues() {
        Connection connection = connection(1, "AS6", "AS8", 45);
        GraphPath path = new GraphPath(List.of("AS6", "AS8"), List.of(connection), 45);

        assertEquals(List.of("AS6", "AS8"), path.getFacilityNames());
        assertEquals(List.of(connection), path.getConnections());
        assertEquals(45, path.getTotalDistanceInMetres());
    }

    @Test
    public void getFacilityNames_isUnmodifiable() {
        GraphPath path = new GraphPath(List.of("AS6", "AS8"), List.of(connection(1, "AS6", "AS8", 45)), 45);

        assertThrows(UnsupportedOperationException.class, () -> path.getFacilityNames().add("AS1"));
    }

    @Test
    public void getConnections_isUnmodifiable() {
        GraphPath path = new GraphPath(List.of("AS6", "AS8"), List.of(connection(1, "AS6", "AS8", 45)), 45);

        assertThrows(UnsupportedOperationException.class,
                () -> path.getConnections().add(connection(2, "AS8", "AS9", 10)));
    }

    @Test
    public void constructor_defensivelyCopiesInputList() {
        List<String> names = new ArrayList<>(List.of("AS6", "AS8"));
        List<Connection> connections = new ArrayList<>(List.of(connection(1, "AS6", "AS8", 45)));
        GraphPath path = new GraphPath(names, connections, 45);

        names.add("AS1");
        connections.add(connection(2, "AS8", "AS9", 10));

        assertEquals(List.of("AS6", "AS8"), path.getFacilityNames());
        assertEquals(1, path.getConnections().size());
    }

    @Test
    public void getTotalDistanceInMetres_valueAboveIntegerMaxValue_preservedExactly() {
        long aboveIntMax = ((long) Integer.MAX_VALUE) + 1_000L;
        GraphPath path = new GraphPath(List.of("AS6", "AS8"), List.of(connection(1, "AS6", "AS8", 45)), aboveIntMax);

        assertEquals(aboveIntMax, path.getTotalDistanceInMetres());
    }
}
