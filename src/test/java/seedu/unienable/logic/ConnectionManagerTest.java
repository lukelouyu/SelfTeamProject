package seedu.unienable.logic;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;

import org.junit.jupiter.api.Test;

import seedu.unienable.accessibility.classes.Connection;
import seedu.unienable.accessibility.enums.AccessibilityStatus;
import seedu.unienable.accessibility.enums.ShelterStatus;
import seedu.unienable.accessibility.enums.TraversalType;
import seedu.unienable.exception.InvalidIndexException;

class ConnectionManagerTest {
    private static Connection newConnection(int id, String from, String to) {
        return new Connection(id, from, to, 80, AccessibilityStatus.YES, TraversalType.SHELTERED_RAMP,
                ShelterStatus.YES, null, null);
    }

    @Test
    public void list_returnsConnectionsInLoadOrder() {
        Connection first = newConnection(12, "COM3", "COM1");
        Connection second = newConnection(13, "COM1", "AS6");
        ConnectionManager manager = new ConnectionManager(List.of(first, second));

        assertEquals(List.of(first, second), manager.list());
    }

    @Test
    public void findById_existingId_returnsConnection() throws Exception {
        Connection connection = newConnection(12, "COM3", "COM1");
        ConnectionManager manager = new ConnectionManager(List.of(connection));

        assertEquals(connection, manager.findById(12));
    }

    @Test
    public void findById_unknownId_throwsInvalidIndexException() {
        ConnectionManager manager = new ConnectionManager(List.of());

        assertThrows(InvalidIndexException.class, () -> manager.findById(999));
    }
}
