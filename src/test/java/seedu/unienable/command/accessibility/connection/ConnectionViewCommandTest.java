package seedu.unienable.command.accessibility.connection;

import seedu.unienable.command.CommandResult;
import seedu.unienable.ui.accessibility.AccessibilityDisclaimer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

import seedu.unienable.accessibility.classes.Connection;
import seedu.unienable.accessibility.enums.AccessibilityStatus;
import seedu.unienable.accessibility.enums.ShelterStatus;
import seedu.unienable.accessibility.enums.TraversalType;
import seedu.unienable.exception.InvalidIndexException;
import seedu.unienable.logic.ConnectionManager;

class ConnectionViewCommandTest {
    @Test
    public void execute_matchesGuideFieldLayoutWithDisclaimer() throws Exception {
        Connection connection = new Connection(12, "COM3", "COM1", 80, AccessibilityStatus.YES,
                TraversalType.SHELTERED_RAMP, ShelterStatus.YES, null,
                "Gentle slope beside the main entrance");
        ConnectionManager connectionManager = new ConnectionManager(List.of(connection));

        CommandResult result = new ConnectionViewCommand(connectionManager, 12).execute();

        assertEquals("Connection [12]\n"
                + "From          : COM3\n"
                + "To            : COM1\n"
                + "Distance      : 80 m\n"
                + "Accessibility : YES\n"
                + "Type          : SHELTERED_RAMP\n"
                + "Shelter       : YES\n"
                + "Known barrier : None recorded\n"
                + "Notes         : Gentle slope beside the main entrance\n"
                + "\n"
                + AccessibilityDisclaimer.TEXT, result.getFeedback());
    }

    @Test
    public void execute_knownBarrierAndNotesPresent_showsBothVerbatim() throws Exception {
        Connection connection = new Connection(14, "AS6", "CLB LEVEL 3", 140, AccessibilityStatus.YES,
                TraversalType.RAMP, ShelterStatus.NO, "Uneven paving", "Slight incline");
        ConnectionManager connectionManager = new ConnectionManager(List.of(connection));

        CommandResult result = new ConnectionViewCommand(connectionManager, 14).execute();

        String feedback = result.getFeedback();
        assertTrue(feedback.contains("Known barrier : Uneven paving"));
        assertTrue(feedback.contains("Notes         : Slight incline"));
    }

    @Test
    public void execute_unknownId_throwsInvalidIndexException() {
        ConnectionManager connectionManager = new ConnectionManager(List.of());

        assertThrows(InvalidIndexException.class, () -> new ConnectionViewCommand(connectionManager, 999).execute());
    }
}
