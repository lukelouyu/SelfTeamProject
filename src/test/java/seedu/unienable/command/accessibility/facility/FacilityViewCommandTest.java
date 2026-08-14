package seedu.unienable.command.accessibility.facility;

import seedu.unienable.command.CommandResult;
import seedu.unienable.ui.accessibility.AccessibilityDisclaimer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;

import org.junit.jupiter.api.Test;

import seedu.unienable.accessibility.classes.Facility;
import seedu.unienable.accessibility.classes.FacilityFeature;
import seedu.unienable.accessibility.enums.AccessibilityStatus;
import seedu.unienable.exception.InvalidIndexException;
import seedu.unienable.logic.FacilityManager;

class FacilityViewCommandTest {
    @Test
    public void execute_equalWidthTypesAndStatuses_needsNoPadding() throws Exception {
        FacilityFeature lift = new FacilityFeature(FacilityFeature.Type.LIFT, AccessibilityStatus.YES, "Near lobby");
        FacilityFeature ramp = new FacilityFeature(FacilityFeature.Type.RAMP, AccessibilityStatus.NO, null);
        Facility facility = new Facility("F05", "COM3", null, List.of(lift, ramp));
        FacilityManager facilityManager = new FacilityManager(List.of(facility));

        CommandResult result = new FacilityViewCommand(facilityManager, "COM3").execute();

        assertEquals("Facility: COM3 [F05]\n"
                + "LIFT | YES | Near lobby\n"
                + "RAMP | NO\n"
                + "\n"
                + AccessibilityDisclaimer.TEXT, result.getFeedback());
    }

    @Test
    public void execute_unevenWidths_padsTypeAndStatusColumnsToTheLongestValue() throws Exception {
        FacilityFeature lift = new FacilityFeature(FacilityFeature.Type.LIFT,
                AccessibilityStatus.UNKNOWN, "loc");
        FacilityFeature washroom = new FacilityFeature(FacilityFeature.Type.ACCESSIBLE_WASHROOM,
                AccessibilityStatus.YES, null);
        Facility facility = new Facility("F05", "COM3", null, List.of(lift, washroom));
        FacilityManager facilityManager = new FacilityManager(List.of(facility));

        CommandResult result = new FacilityViewCommand(facilityManager, "COM3").execute();

        String expectedLiftLine = "LIFT" + " ".repeat(15) + " | UNKNOWN | loc";
        String expectedWashroomLine = "ACCESSIBLE_WASHROOM | YES";
        assertEquals("Facility: COM3 [F05]\n"
                + expectedLiftLine + "\n"
                + expectedWashroomLine + "\n"
                + "\n"
                + AccessibilityDisclaimer.TEXT, result.getFeedback());
    }

    @Test
    public void execute_caseInsensitiveName_findsFacility() throws Exception {
        Facility facility = new Facility("F05", "COM3", null, List.of());
        FacilityManager facilityManager = new FacilityManager(List.of(facility));

        CommandResult result = new FacilityViewCommand(facilityManager, "com3").execute();

        assertEquals("Facility: COM3 [F05]\n"
                + "\n"
                + AccessibilityDisclaimer.TEXT, result.getFeedback());
    }

    @Test
    public void execute_unknownFacility_throwsInvalidIndexException() {
        FacilityManager facilityManager = new FacilityManager(List.of());

        assertThrows(InvalidIndexException.class, () -> new FacilityViewCommand(facilityManager, "COM3").execute());
    }
}
