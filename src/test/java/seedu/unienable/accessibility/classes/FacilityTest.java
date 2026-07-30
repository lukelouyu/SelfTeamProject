package seedu.unienable.accessibility.classes;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;

import org.junit.jupiter.api.Test;

import seedu.unienable.accessibility.enums.AccessibilityStatus;

class FacilityTest {
    @Test
    public void getters_returnConstructorValues() {
        FacilityFeature lift = new FacilityFeature(FacilityFeature.Type.LIFT, AccessibilityStatus.YES,
                "Near the level 1 lobby");
        Facility facility = new Facility("F05", "COM3", "Engineering building", List.of(lift));

        assertEquals("F05", facility.getId());
        assertEquals("COM3", facility.getName());
        assertEquals("Engineering building", facility.getDescription());
        assertEquals(1, facility.getFeatures().size());
        assertEquals(lift, facility.getFeatures().get(0));
    }

    @Test
    public void getDescription_allowsNull() {
        Facility facility = new Facility("F05", "COM3", null, List.of());

        assertNull(facility.getDescription());
    }

    @Test
    public void getFeatures_isUnmodifiable() {
        Facility facility = new Facility("F05", "COM3", null, List.of());

        assertThrows(UnsupportedOperationException.class, () ->
                facility.getFeatures().add(
                        new FacilityFeature(FacilityFeature.Type.OTHER, AccessibilityStatus.UNKNOWN, null)));
    }
}
