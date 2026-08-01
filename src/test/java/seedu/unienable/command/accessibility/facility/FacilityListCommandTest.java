package seedu.unienable.command.accessibility.facility;

import seedu.unienable.command.CommandResult;
import seedu.unienable.command.accessibility.common.AccessibilityDisclaimer;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;

import org.junit.jupiter.api.Test;

import seedu.unienable.accessibility.classes.Facility;
import seedu.unienable.logic.FacilityManager;

class FacilityListCommandTest {
    @Test
    public void execute_multipleFacilities_listsEachIdAndNameWithDisclaimer() {
        FacilityManager facilityManager = new FacilityManager(List.of(
                new Facility("F01", "AS6", null, List.of()),
                new Facility("F02", "CLB LEVEL 3", null, List.of()),
                new Facility("F03", "CLB LEVEL 6", null, List.of()),
                new Facility("F04", "COM1", null, List.of()),
                new Facility("F05", "COM3", null, List.of()),
                new Facility("F06", "E4", null, List.of())));

        CommandResult result = new FacilityListCommand(facilityManager).execute();

        assertEquals("Known facilities in the local reference:\n"
                + "[F01] AS6\n"
                + "[F02] CLB LEVEL 3\n"
                + "[F03] CLB LEVEL 6\n"
                + "[F04] COM1\n"
                + "[F05] COM3\n"
                + "[F06] E4\n"
                + "\n"
                + AccessibilityDisclaimer.TEXT, result.getFeedback());
    }

    @Test
    public void execute_noFacilities_showsHeaderAndDisclaimerOnly() {
        FacilityManager facilityManager = new FacilityManager(List.of());

        CommandResult result = new FacilityListCommand(facilityManager).execute();

        assertEquals("Known facilities in the local reference:\n"
                + "\n"
                + AccessibilityDisclaimer.TEXT, result.getFeedback());
    }
}
