package seedu.unienable.parser.accessibility;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import seedu.unienable.accessibility.classes.Facility;
import seedu.unienable.ui.accessibility.AccessibilityDisclaimer;
import seedu.unienable.command.CommandResult;
import seedu.unienable.exception.InvalidCommandException;
import seedu.unienable.exception.MissingInputException;
import seedu.unienable.logic.FacilityManager;
import seedu.unienable.storage.Storage;

class FacilityCommandParserTest {
    @TempDir
    Path tempDir;

    private final FacilityCommandParser parser = new FacilityCommandParser();

    @Test
    public void parseList_takesNoArguments_listsEveryFacility() throws Exception {
        FacilityManager manager = new FacilityManager(List.of(new Facility("F05", "COM3", null, List.of())));

        CommandResult result = parser.parseList(manager, "").execute();

        assertTrue(result.getFeedback().contains("COM3"));
    }

    @Test
    public void parseList_trailingArguments_throwsInvalidCommandException() {
        // Regression test: "facility list" is documented as taking no arguments, but trailing
        // text was previously silently ignored rather than rejected.
        FacilityManager manager = new FacilityManager(List.of());

        assertThrows(InvalidCommandException.class, () -> parser.parseList(manager, "ignored-text"));
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

    @Test
    public void parseFind_unrecognisedLeadingToken_throwsInvalidCommandException() {
        // Regression test for RC05 (v1.0 RC retest, 2026-08-01).
        FacilityManager manager = new FacilityManager(List.of());

        assertThrows(InvalidCommandException.class,
                () -> parser.parseFind(manager, "ignored/yes type/LIFT"));
    }

    @Test
    public void parseValidate_takesNoArguments_returnsFacilityValidateCommand() throws Exception {
        Files.write(tempDir.resolve("facilities.txt"), List.of("FACILITY|F01|AS1|Block 1"));
        Storage storage = new Storage(tempDir);

        CommandResult result = parser.parseValidate(storage, "").execute();

        assertEquals("facilities.txt: no issues found.", result.getFeedback());
    }

    @Test
    public void parseValidate_trailingArguments_throwsInvalidCommandException() {
        Storage storage = new Storage(tempDir);

        assertThrows(InvalidCommandException.class, () -> parser.parseValidate(storage, "ignored-text"));
    }

    @Test
    public void parseFind_duplicateTypeMarker_throwsInvalidCommandException() {
        // Bug F regression: "facility find type/LIFT type/RAMP" previously let the first "type/"
        // silently absorb the second as literal text instead of being rejected.
        FacilityManager manager = new FacilityManager(List.of());

        InvalidCommandException exception = assertThrows(InvalidCommandException.class,
                () -> parser.parseFind(manager, "type/LIFT type/RAMP"));

        assertTrue(exception.getMessage().contains("Duplicate option \"type/\""));
    }

    @Test
    public void parseFind_duplicateStatusMarker_throwsInvalidCommandException() {
        FacilityManager manager = new FacilityManager(List.of());

        InvalidCommandException exception = assertThrows(InvalidCommandException.class,
                () -> parser.parseFind(manager, "type/LIFT status/YES status/NO"));

        assertTrue(exception.getMessage().contains("Duplicate option \"status/\""));
    }
}
