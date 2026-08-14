package seedu.unienable.parser.accessibility;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import seedu.unienable.accessibility.classes.Connection;
import seedu.unienable.accessibility.enums.AccessibilityStatus;
import seedu.unienable.accessibility.enums.ShelterStatus;
import seedu.unienable.accessibility.enums.TraversalType;
import seedu.unienable.command.CommandResult;
import seedu.unienable.exception.InvalidCommandException;
import seedu.unienable.exception.MissingInputException;
import seedu.unienable.logic.ConnectionManager;
import seedu.unienable.storage.Storage;

class ConnectionCommandParserTest {
    @TempDir
    Path tempDir;

    private final ConnectionCommandParser parser = new ConnectionCommandParser();

    private static Connection newConnection(int id, String from, String to) {
        return new Connection(id, from, to, 80, AccessibilityStatus.YES, TraversalType.PATH,
                ShelterStatus.YES, null, null);
    }

    @Test
    public void parseList_takesNoArguments_listsEveryConnection() throws Exception {
        ConnectionManager manager = new ConnectionManager(List.of(newConnection(12, "COM3", "COM1")));

        CommandResult result = parser.parseList(manager, "").execute();

        assertTrue(result.getFeedback().contains("COM3 <-> COM1"));
    }

    @Test
    public void parseList_trailingArguments_throwsInvalidCommandException() {
        // Regression test: "connection list" is documented as taking no arguments, but trailing
        // text was previously silently ignored rather than rejected.
        ConnectionManager manager = new ConnectionManager(List.of());

        assertThrows(InvalidCommandException.class, () -> parser.parseList(manager, "ignored-text"));
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
    public void parseFind_whitespaceOnlyArgs_throwsMissingInputException() {
        ConnectionManager manager = new ConnectionManager(List.of());

        assertThrows(MissingInputException.class, () -> parser.parseFind(manager, "   "));
    }

    @Test
    public void parseFind_unrecognisedLeadingToken_throwsInvalidCommandException() {
        // Regression test for RC05 (v1.0 RC retest, 2026-08-01).
        ConnectionManager manager = new ConnectionManager(List.of());

        assertThrows(InvalidCommandException.class,
                () -> parser.parseFind(manager, "ignored/yes from/AS6"));
    }

    @Test
    public void parseFind_whitespaceOnlyFromAlone_throwsMissingInputException() {
        // Regression test: a blank from/ does not count as a supplied filter, so
        // "connection find from/   " with nothing else must still be rejected rather than
        // silently matching every connection.
        ConnectionManager manager = new ConnectionManager(List.of());

        assertThrows(MissingInputException.class, () -> parser.parseFind(manager, "from/   "));
    }

    @Test
    public void parseFind_whitespaceOnlyFromWithOtherFilter_ignoresFromUsesOtherFilter() throws Exception {
        Connection match = newConnection(12, "COM3", "COM1");
        Connection nonMatch = newConnection(13, "COM1", "AS6");
        ConnectionManager manager = new ConnectionManager(List.of(match, nonMatch));

        CommandResult result = parser.parseFind(manager, "from/    to/COM1").execute();

        String feedback = result.getFeedback();
        assertTrue(feedback.contains("[12]"));
        assertTrue(feedback.contains("[13]"));
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

    @Test
    public void parseValidate_takesNoArguments_returnsConnectionValidateCommand() throws Exception {
        Files.write(tempDir.resolve("facilities.txt"), List.of("FACILITY|F01|AS1|Block 1",
                "FACILITY|F02|AS2|Block 2"));
        Files.write(tempDir.resolve("connections.txt"), List.of("CONNECTION|1|AS1|AS2|50|YES|PATH|YES"));
        Storage storage = new Storage(tempDir);

        CommandResult result = parser.parseValidate(storage, "").execute();

        assertEquals("connections.txt: no issues found.", result.getFeedback());
    }

    @Test
    public void parseValidate_trailingArguments_throwsInvalidCommandException() {
        Storage storage = new Storage(tempDir);

        assertThrows(InvalidCommandException.class, () -> parser.parseValidate(storage, "ignored-text"));
    }

    @Test
    public void parseFind_duplicateFromMarker_throwsInvalidCommandException() {
        // Bug F regression: "connection find from/AS6 from/AS8" previously let the first "from/"
        // silently absorb the second as literal text, then matched no connection at all, instead
        // of being rejected as a repeated field.
        ConnectionManager manager = new ConnectionManager(List.of());

        InvalidCommandException exception = assertThrows(InvalidCommandException.class,
                () -> parser.parseFind(manager, "from/AS6 from/AS8"));

        assertTrue(exception.getMessage().contains("Duplicate option \"from/\""));
    }

    @Test
    public void parseFind_duplicateTypeMarker_throwsInvalidCommandException() {
        ConnectionManager manager = new ConnectionManager(List.of());

        InvalidCommandException exception = assertThrows(InvalidCommandException.class,
                () -> parser.parseFind(manager, "type/RAMP type/LIFT"));

        assertTrue(exception.getMessage().contains("Duplicate option \"type/\""));
    }
}
