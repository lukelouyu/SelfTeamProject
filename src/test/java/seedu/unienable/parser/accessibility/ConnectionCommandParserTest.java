package seedu.unienable.parser.accessibility;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

import seedu.unienable.accessibility.classes.Connection;
import seedu.unienable.accessibility.enums.AccessibilityStatus;
import seedu.unienable.accessibility.enums.ShelterStatus;
import seedu.unienable.accessibility.enums.TraversalType;
import seedu.unienable.command.CommandResult;
import seedu.unienable.exception.InvalidCommandException;
import seedu.unienable.exception.MissingInputException;
import seedu.unienable.logic.ConnectionManager;

class ConnectionCommandParserTest {
    private final ConnectionCommandParser parser = new ConnectionCommandParser();

    private static Connection newConnection(int id, String from, String to) {
        return new Connection(id, from, to, 80, AccessibilityStatus.YES, TraversalType.PATH,
                ShelterStatus.YES, null, null);
    }

    @Test
    public void parseList_takesNoArguments_listsEveryConnection() {
        ConnectionManager manager = new ConnectionManager(List.of(newConnection(12, "COM3", "COM1")));

        CommandResult result = parser.parseList(manager).execute();

        assertTrue(result.getFeedback().contains("COM3 <-> COM1"));
    }

    @Test
    public void parseView_validId_findsConnection() throws Exception {
        ConnectionManager manager = new ConnectionManager(List.of(newConnection(12, "COM3", "COM1")));

        CommandResult result = parser.parseView(manager, "12").execute();

        assertTrue(result.getFeedback().contains("Connection [12]"));
    }

    @Test
    public void parseView_missingId_throwsMissingInputException() {
        ConnectionManager manager = new ConnectionManager(List.of());

        assertThrows(MissingInputException.class, () -> parser.parseView(manager, ""));
    }

    @Test
    public void parseView_nonNumericId_throwsInvalidCommandException() {
        ConnectionManager manager = new ConnectionManager(List.of());

        assertThrows(InvalidCommandException.class, () -> parser.parseView(manager, "twelve"));
    }

    @Test
    public void parseFind_singleFilter_findsMatchingConnections() throws Exception {
        ConnectionManager manager = new ConnectionManager(List.of(newConnection(12, "COM3", "COM1")));

        CommandResult result = parser.parseFind(manager, "from/COM3").execute();

        assertTrue(result.getFeedback().contains("COM3 <-> COM1"));
    }

    @Test
    public void parseFind_multipleFiltersInAnyOrder_combineWithAnd() throws Exception {
        Connection match = newConnection(12, "COM3", "COM1");
        Connection nonMatch = newConnection(13, "COM1", "AS6");
        ConnectionManager manager = new ConnectionManager(List.of(match, nonMatch));

        CommandResult result = parser.parseFind(manager, "to/COM1 from/COM3").execute();

        String feedback = result.getFeedback();
        assertTrue(feedback.contains("[12]"));
        assertTrue(!feedback.contains("[13]"));
    }

    @Test
    public void parseFind_noFilters_throwsMissingInputException() {
        ConnectionManager manager = new ConnectionManager(List.of());

        assertThrows(MissingInputException.class, () -> parser.parseFind(manager, ""));
    }

    @Test
    public void parseFind_invalidType_throwsInvalidCommandException() {
        ConnectionManager manager = new ConnectionManager(List.of());

        assertThrows(InvalidCommandException.class, () -> parser.parseFind(manager, "type/BOGUS"));
    }

    @Test
    public void parseFind_invalidStatus_throwsInvalidCommandException() {
        ConnectionManager manager = new ConnectionManager(List.of());

        assertThrows(InvalidCommandException.class, () -> parser.parseFind(manager, "status/BOGUS"));
    }

    @Test
    public void parseFind_invalidShelter_throwsInvalidCommandException() {
        ConnectionManager manager = new ConnectionManager(List.of());

        assertThrows(InvalidCommandException.class, () -> parser.parseFind(manager, "shelter/BOGUS"));
    }

    @Test
    public void parseFind_statusFilter_narrowsResults() throws Exception {
        Connection accessible = new Connection(1, "A", "B", 10, AccessibilityStatus.YES, TraversalType.PATH,
                ShelterStatus.YES, null, null);
        Connection inaccessible = new Connection(2, "C", "D", 10, AccessibilityStatus.NO, TraversalType.PATH,
                ShelterStatus.YES, null, null);
        ConnectionManager manager = new ConnectionManager(List.of(accessible, inaccessible));

        CommandResult result = parser.parseFind(manager, "status/NO").execute();

        String feedback = result.getFeedback();
        assertTrue(feedback.contains("[2]"));
        assertTrue(!feedback.contains("[1]"));
    }
}
