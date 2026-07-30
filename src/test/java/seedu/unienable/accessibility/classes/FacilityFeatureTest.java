package seedu.unienable.accessibility.classes;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

import seedu.unienable.accessibility.enums.AccessibilityStatus;

class FacilityFeatureTest {
    @Test
    public void getters_returnConstructorValues() {
        FacilityFeature feature = new FacilityFeature(FacilityFeature.Type.LIFT, AccessibilityStatus.YES,
                "Near the level 1 lobby");

        assertEquals(FacilityFeature.Type.LIFT, feature.getType());
        assertEquals(AccessibilityStatus.YES, feature.getStatus());
        assertEquals("Near the level 1 lobby", feature.getNotes());
    }

    @Test
    public void getNotes_allowsNull() {
        FacilityFeature feature = new FacilityFeature(FacilityFeature.Type.REST_POINT, AccessibilityStatus.NO, null);

        assertNull(feature.getNotes());
    }

    @Test
    public void type_values_matchDocumentedFeatureTypesInOrder() {
        assertArrayEquals(
                new FacilityFeature.Type[] {
                    FacilityFeature.Type.LIFT,
                    FacilityFeature.Type.RAMP,
                    FacilityFeature.Type.SHELTERED_RAMP,
                    FacilityFeature.Type.ACCESSIBLE_WASHROOM,
                    FacilityFeature.Type.STEP_FREE_ENTRANCE,
                    FacilityFeature.Type.REST_POINT,
                    FacilityFeature.Type.AUTOMATIC_DOOR,
                    FacilityFeature.Type.OTHER
                },
                FacilityFeature.Type.values());
    }
}
