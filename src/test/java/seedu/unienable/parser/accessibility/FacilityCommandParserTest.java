package seedu.unienable.parser.accessibility;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

import seedu.unienable.accessibility.classes.Facility;
import seedu.unienable.command.accessibility.AccessibilityDisclaimer;
import seedu.unienable.command.CommandResult;
import seedu.unienable.exception.InvalidCommandException;
import seedu.unienable.exception.MissingInputException;
import seedu.unienable.logic.FacilityManager;

class FacilityCommandParserTest {
    private final FacilityCommandParser parser = new FacilityCommandParser();

    @Test
    public void parseList_takesNoArguments_listsEveryFacility() {
        FacilityManager manager = new FacilityManager(List.of(new Facility("F05", "COM3", null, List.of())));

        CommandResult result = parser.parseList(manager).execute();

        assertTrue(result.getFeedback().contains("COM3"));
    }

    @Test
    public void parseView_validName_findsFacility() throws Exception {
        FacilityManager manager = new FacilityManager(List.of(new Facility("F05", "COM3", null, List.of())));

        CommandResult result = parser.parseView(manager, "COM3").execute();

        assertTrue(result.getFeedback().contains("Facility: COM3 [F05]"));
    }

    @Test
    public void parseView_blankArgs_throwsMissingInputException() {
        FacilityManager manager = new FacilityManager(List.of());

        assertThrows(MissingInputException.class, () -> parser.parseView(manager, "  "));
    }

    @Test
    public void parseFind_noStatusSupplied_defaultsToYes() throws Exception {
        FacilityManager manager = new FacilityManager(List.of());

        CommandResult result = parser.parseFind(manager, "type/LIFT").execute();

        assertEquals("Facilities where LIFT is YES:\n\n" + AccessibilityDisclaimer.TEXT, result.getFeedback());
    }

    @Test
    public void parseFind_explicitStatus_isUsed() throws Exception {
        FacilityManager manager = new FacilityManager(List.of());

        CommandResult result = parser.parseFind(manager, "type/REST_POINT status/UNKNOWN").execute();

        assertEquals("Facilities where REST_POINT is UNKNOWN:\n\n"
                + "UNKNOWN means the local dataset does not confirm the feature.\n\n"
                + AccessibilityDisclaimer.TEXT, result.getFeedback());
    }

    @Test
    public void parseFind_missingType_throwsMissingInputException() {
        FacilityManager manager = new FacilityManager(List.of());

        assertThrows(MissingInputException.class, () -> parser.parseFind(manager, "status/YES"));
    }

    @Test
    public void parseFind_invalidType_throwsInvalidCommandException() {
        FacilityManager manager = new FacilityManager(List.of());

        assertThrows(InvalidCommandException.class, () -> parser.parseFind(manager, "type/BOGUS"));
    }

    @Test
    public void parseFind_invalidStatus_throwsInvalidCommandException() {
        FacilityManager manager = new FacilityManager(List.of());

        assertThrows(InvalidCommandException.class,
                () -> parser.parseFind(manager, "type/LIFT status/BOGUS"));
    }
}
