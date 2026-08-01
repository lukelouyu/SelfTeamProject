package seedu.unienable.logic.graph;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

class GraphPathTest {
    @Test
    public void getters_returnConstructorValues() {
        GraphPath path = new GraphPath(List.of("AS6", "AS8"), 45);

        assertEquals(List.of("AS6", "AS8"), path.getFacilityNames());
        assertEquals(45, path.getTotalDistanceInMetres());
    }

    @Test
    public void getFacilityNames_isUnmodifiable() {
        GraphPath path = new GraphPath(List.of("AS6", "AS8"), 45);

        assertThrows(UnsupportedOperationException.class, () -> path.getFacilityNames().add("AS1"));
    }

    @Test
    public void constructor_defensivelyCopiesInputList() {
        List<String> names = new ArrayList<>(List.of("AS6", "AS8"));
        GraphPath path = new GraphPath(names, 45);

        names.add("AS1");

        assertEquals(List.of("AS6", "AS8"), path.getFacilityNames());
    }
}
