package seedu.unienable.command;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;

import org.junit.jupiter.api.Test;

import seedu.unienable.accessibility.classes.Facility;
import seedu.unienable.accessibility.classes.FacilityFeature;
import seedu.unienable.accessibility.enums.AccessibilityStatus;
import seedu.unienable.logic.FacilityManager;

class FacilityFindCommandTest {
    @Test
    public void execute_yesStatus_listsMatchingFacilitiesWithPaddedNotes() {
        Facility clbLevel3 = new Facility("F02", "CLB LEVEL 3", null, List.of(
                new FacilityFeature(FacilityFeature.Type.LIFT, AccessibilityStatus.YES, "Near the entrance")));
        Facility com3 = new Facility("F05", "COM3", null, List.of(
                new FacilityFeature(FacilityFeature.Type.LIFT, AccessibilityStatus.YES, "Near the level 1 lobby")));
        FacilityManager facilityManager = new FacilityManager(List.of(clbLevel3, com3));

        CommandResult result = new FacilityFindCommand(facilityManager, FacilityFeature.Type.LIFT,
                AccessibilityStatus.YES).execute();

        String com3Line = "[F05] COM3" + " ".repeat(7) + " | Near the level 1 lobby";
        assertEquals("Facilities where LIFT is YES:\n"
                + "[F02] CLB LEVEL 3 | Near the entrance\n"
                + com3Line, result.getFeedback());
    }

    @Test
    public void execute_facilityWithNoNotesForMatchedFeature_omitsPipe() {
        Facility facility = new Facility("F04", "COM1", null,
                List.of(new FacilityFeature(FacilityFeature.Type.REST_POINT, AccessibilityStatus.NO, null)));
        FacilityManager facilityManager = new FacilityManager(List.of(facility));

        CommandResult result = new FacilityFindCommand(facilityManager, FacilityFeature.Type.REST_POINT,
                AccessibilityStatus.NO).execute();

        assertEquals("Facilities where REST_POINT is NO:\n[F04] COM1", result.getFeedback());
    }

    @Test
    public void execute_unknownStatus_appendsExplanatoryFooter() {
        Facility facility = new Facility("F01", "AS6", null,
                List.of(new FacilityFeature(FacilityFeature.Type.REST_POINT, AccessibilityStatus.UNKNOWN, null)));
        FacilityManager facilityManager = new FacilityManager(List.of(facility));

        CommandResult result = new FacilityFindCommand(facilityManager, FacilityFeature.Type.REST_POINT,
                AccessibilityStatus.UNKNOWN).execute();

        assertEquals("Facilities where REST_POINT is UNKNOWN:\n"
                + "[F01] AS6\n"
                + "\n"
                + "UNKNOWN means the local dataset does not confirm the feature.", result.getFeedback());
    }

    @Test
    public void execute_noMatches_showsHeaderOnly() {
        FacilityManager facilityManager = new FacilityManager(List.of());

        CommandResult result = new FacilityFindCommand(facilityManager, FacilityFeature.Type.LIFT,
                AccessibilityStatus.YES).execute();

        assertEquals("Facilities where LIFT is YES:", result.getFeedback());
    }
}
