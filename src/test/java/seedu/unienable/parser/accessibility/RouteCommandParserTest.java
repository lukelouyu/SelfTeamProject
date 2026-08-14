package seedu.unienable.parser.accessibility;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

import seedu.unienable.accessibility.classes.Facility;
import seedu.unienable.command.accessibility.route.RouteCommand;
import seedu.unienable.exception.InvalidCommandException;
import seedu.unienable.exception.MissingInputException;
import seedu.unienable.logic.ConnectionManager;
import seedu.unienable.logic.FacilityManager;

class RouteCommandParserTest {
    private final RouteCommandParser parser = new RouteCommandParser();
    private final FacilityManager facilityManager = new FacilityManager(
            List.of(new Facility("A", "AS6", null, List.of())));
    private final ConnectionManager connectionManager = new ConnectionManager(List.of());

    @Test
    public void parse_normalOrder_returnsRouteCommand() {
        RouteCommand command = assertDoesNotThrow(
                () -> parser.parse(facilityManager, connectionManager, "from/AS6 to/AS8"));

        assertNotNull(command);
    }

    @Test
    public void parse_markersInReverseOrder_stillParsesCorrectly() {
        assertDoesNotThrow(() -> parser.parse(facilityManager, connectionManager, "to/AS8 from/AS6"));
    }

    @Test
    public void parse_missingFrom_throwsMissingInputException() {
        MissingInputException exception = assertThrows(MissingInputException.class,
                () -> parser.parse(facilityManager, connectionManager, "to/AS8"));

        assertTrue(exception.getMessage().contains("source facility"));
    }

    @Test
    public void parse_missingTo_throwsMissingInputException() {
        MissingInputException exception = assertThrows(MissingInputException.class,
                () -> parser.parse(facilityManager, connectionManager, "from/AS6"));

        assertTrue(exception.getMessage().contains("destination facility"));
    }

    @Test
    public void parse_blankFromValue_throwsMissingInputException() {
        assertThrows(MissingInputException.class,
                () -> parser.parse(facilityManager, connectionManager, "from/   to/AS8"));
    }

    @Test
    public void parse_noArguments_throwsMissingInputException() {
        assertThrows(MissingInputException.class, () -> parser.parse(facilityManager, connectionManager, ""));
    }

    @Test
    public void parse_unrecognisedLeadingText_throwsInvalidCommandException() {
        assertThrows(InvalidCommandException.class,
                () -> parser.parse(facilityManager, connectionManager, "bogus from/AS6 to/AS8"));
    }

    @Test
    public void parse_duplicateFromMarker_throwsInvalidCommandException() {
        // Bug F regression: "route from/AS6 from/AS8 to/AS9" previously let the first "from/"
        // silently absorb the second as literal text instead of being rejected.
        InvalidCommandException exception = assertThrows(InvalidCommandException.class,
                () -> parser.parse(facilityManager, connectionManager, "from/AS6 from/AS8 to/AS9"));

        assertTrue(exception.getMessage().contains("Duplicate option \"from/\""));
    }

    @Test
    public void parse_duplicateToMarker_throwsInvalidCommandException() {
        InvalidCommandException exception = assertThrows(InvalidCommandException.class,
                () -> parser.parse(facilityManager, connectionManager, "from/AS6 to/AS8 to/AS9"));

        assertTrue(exception.getMessage().contains("Duplicate option \"to/\""));
    }
}
