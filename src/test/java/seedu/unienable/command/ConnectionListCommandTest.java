package seedu.unienable.command;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;

import org.junit.jupiter.api.Test;

import seedu.unienable.accessibility.classes.Connection;
import seedu.unienable.accessibility.enums.AccessibilityStatus;
import seedu.unienable.accessibility.enums.ShelterStatus;
import seedu.unienable.accessibility.enums.TraversalType;
import seedu.unienable.logic.ConnectionManager;

class ConnectionListCommandTest {
    @Test
    public void execute_multipleConnections_formatsIdEndpointsDistanceAccessibilityTypeAndShelter() {
        Connection first = new Connection(12, "COM3", "COM1", 80, AccessibilityStatus.YES,
                TraversalType.SHELTERED_RAMP, ShelterStatus.YES, null, null);
        Connection second = new Connection(15, "CLB LEVEL 3", "CLB LEVEL 6", 40, AccessibilityStatus.YES,
                TraversalType.LIFT, ShelterStatus.YES, null, null);
        ConnectionManager connectionManager = new ConnectionManager(List.of(first, second));

        CommandResult result = new ConnectionListCommand(connectionManager).execute();

        assertEquals("Known two-way connections:\n"
                + "[12] COM3 <-> COM1 | 80 m | ACCESSIBLE YES | SHELTERED_RAMP\n"
                + "    Shelter: YES\n"
                + "[15] CLB LEVEL 3 <-> CLB LEVEL 6 | 40 m | ACCESSIBLE YES | LIFT\n"
                + "    Shelter: YES", result.getFeedback());
    }

    @Test
    public void execute_noConnections_showsHeaderOnly() {
        ConnectionManager connectionManager = new ConnectionManager(List.of());

        CommandResult result = new ConnectionListCommand(connectionManager).execute();

        assertEquals("Known two-way connections:", result.getFeedback());
    }
}
