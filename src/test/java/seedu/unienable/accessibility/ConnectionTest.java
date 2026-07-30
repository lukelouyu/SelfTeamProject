package seedu.unienable.accessibility;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

class ConnectionTest {
    @Test
    public void getters_returnConstructorValues() {
        Connection connection = new Connection(12, "COM3", "COM1", 80, AccessibilityStatus.YES,
                TraversalType.SHELTERED_RAMP, ShelterStatus.YES, null, "Gentle slope beside the main entrance");

        assertEquals(12, connection.getId());
        assertEquals("COM3", connection.getFrom());
        assertEquals("COM1", connection.getTo());
        assertEquals(80, connection.getDistanceInMetres());
        assertEquals(AccessibilityStatus.YES, connection.getAccessibility());
        assertEquals(TraversalType.SHELTERED_RAMP, connection.getType());
        assertEquals(ShelterStatus.YES, connection.getShelter());
        assertNull(connection.getKnownBarrier());
        assertEquals("Gentle slope beside the main entrance", connection.getNotes());
    }

    @Test
    public void getKnownBarrier_canBeRecorded() {
        Connection connection = new Connection(18, "AS2", "AS3", 60, AccessibilityStatus.NO,
                TraversalType.PATH, ShelterStatus.NO, "Flight of stairs", null);

        assertEquals("Flight of stairs", connection.getKnownBarrier());
        assertNull(connection.getNotes());
    }
}
