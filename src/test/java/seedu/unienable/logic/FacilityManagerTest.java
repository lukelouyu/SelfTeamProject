package seedu.unienable.logic;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

import seedu.unienable.accessibility.classes.Facility;
import seedu.unienable.accessibility.classes.FacilityFeature;
import seedu.unienable.accessibility.enums.AccessibilityStatus;
import seedu.unienable.exception.InvalidIndexException;

class FacilityManagerTest {
    @Test
    public void list_returnsFacilitiesInLoadOrder() {
        Facility com3 = new Facility("F05", "COM3", null, List.of());
        Facility com1 = new Facility("F04", "COM1", null, List.of());
        FacilityManager manager = new FacilityManager(List.of(com3, com1));

        assertEquals(List.of(com3, com1), manager.list());
    }

    @Test
    public void findByName_isCaseInsensitive() throws Exception {
        Facility com3 = new Facility("F05", "COM3", null, List.of());
        FacilityManager manager = new FacilityManager(List.of(com3));

        assertEquals(com3, manager.findByName("com3"));
    }

    @Test
    public void findByName_unknownName_throwsInvalidIndexException() {
        FacilityManager manager = new FacilityManager(List.of());

        assertThrows(InvalidIndexException.class, () -> manager.findByName("COM3"));
    }

    @Test
    public void findByFeature_matchingTypeAndStatus_returnsOnlyMatchingFacilities() {
        Facility withLift = new Facility("F02", "CLB LEVEL 3", null,
                List.of(new FacilityFeature(FacilityFeature.Type.LIFT, AccessibilityStatus.YES, "Near entrance")));
        Facility withoutLift = new Facility("F04", "COM1", null,
                List.of(new FacilityFeature(FacilityFeature.Type.LIFT, AccessibilityStatus.NO, null)));
        FacilityManager manager = new FacilityManager(List.of(withLift, withoutLift));

        assertEquals(List.of(withLift), manager.findByFeature(FacilityFeature.Type.LIFT, AccessibilityStatus.YES));
    }

    @Test
    public void findByFeature_noMatches_returnsEmptyList() {
        Facility facility = new Facility("F04", "COM1", null, List.of());
        FacilityManager manager = new FacilityManager(List.of(facility));

        assertTrue(manager.findByFeature(FacilityFeature.Type.LIFT, AccessibilityStatus.YES).isEmpty());
    }
}
