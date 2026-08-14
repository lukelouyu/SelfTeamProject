package seedu.unienable.command.accessibility.connection;

import seedu.unienable.command.CommandResult;
import seedu.unienable.ui.accessibility.AccessibilityDisclaimer;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;

import org.junit.jupiter.api.Test;

import seedu.unienable.accessibility.classes.Connection;
import seedu.unienable.accessibility.enums.AccessibilityStatus;
import seedu.unienable.accessibility.enums.ShelterStatus;
import seedu.unienable.accessibility.enums.TraversalType;
import seedu.unienable.logic.ConnectionManager;

class ConnectionFindCommandTest {
    private static ConnectionManager sampleManager() {
        Connection as1As4 = new Connection(1, "AS1", "AS4", 60, AccessibilityStatus.YES,
                TraversalType.PATH, ShelterStatus.YES, null, null);
        Connection as4As7 = new Connection(2, "AS4", "AS7", 50, AccessibilityStatus.YES,
                TraversalType.PATH, ShelterStatus.YES, null, null);
        Connection as1As2 = new Connection(3, "AS1", "AS2", 130, AccessibilityStatus.YES,
                TraversalType.RAMP, ShelterStatus.NO, null, null);
        return new ConnectionManager(List.of(as1As4, as4As7, as1As2));
    }

    @Test
    public void execute_fromAndTypeFilters_combineWithAndAndMatchEitherEndpoint() {
        ConnectionManager manager = sampleManager();

        CommandResult result = new ConnectionFindCommand(manager, "AS4", null, TraversalType.PATH, null, null)
                .execute();

        assertEquals("Found 2 connections:\n"
                + "[1] AS1 <-> AS4 | 60 m | ACCESSIBLE YES | PATH\n"
                + "    Shelter: YES\n"
                + "[2] AS4 <-> AS7 | 50 m | ACCESSIBLE YES | PATH\n"
                + "    Shelter: YES\n"
                + "\n"
                + AccessibilityDisclaimer.TEXT, result.getFeedback());
    }

    @Test
    public void execute_noMatches_showsNoConnectionsFoundMessageWithDisclaimer() {
        ConnectionManager manager = sampleManager();

        CommandResult result = new ConnectionFindCommand(manager, null, null, TraversalType.LIFT, null, null)
                .execute();

        assertEquals("No connections found.\n\n" + AccessibilityDisclaimer.TEXT, result.getFeedback());
    }

    @Test
    public void execute_fromFilter_isCaseInsensitive() {
        ConnectionManager manager = sampleManager();

        CommandResult result = new ConnectionFindCommand(manager, "as1", null, null, null, null).execute();

        assertEquals("Found 2 connections:\n"
                + "[1] AS1 <-> AS4 | 60 m | ACCESSIBLE YES | PATH\n"
                + "    Shelter: YES\n"
                + "[3] AS1 <-> AS2 | 130 m | ACCESSIBLE YES | RAMP\n"
                + "    Shelter: NO\n"
                + "\n"
                + AccessibilityDisclaimer.TEXT, result.getFeedback());
    }

    @Test
    public void execute_unknownFacilityName_showsNoConnectionsFoundRatherThanError() {
        ConnectionManager manager = sampleManager();

        CommandResult result = new ConnectionFindCommand(manager, "AS99", null, null, null, null).execute();

        assertEquals("No connections found.\n\n" + AccessibilityDisclaimer.TEXT, result.getFeedback());
    }
}
