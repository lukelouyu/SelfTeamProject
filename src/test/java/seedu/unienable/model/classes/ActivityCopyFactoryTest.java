package seedu.unienable.model.classes;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import org.junit.jupiter.api.Test;

import seedu.unienable.model.enums.ActivityCategory;

class ActivityCopyFactoryTest {
    @Test
    public void copyAll_preservesEveryFlexibleFieldAndCompletionState() throws Exception {
        FlexibleActivity original = new FlexibleActivity(7, "Study", ActivityCategory.ACADEMIC,
                LocalDate.of(2026, 8, 17), LocalTime.of(9, 0), LocalTime.of(14, 0), 90,
                EnergyRating.of(4), SensoryRating.of(3), "CS2113", "Bring notes", LocalTime.of(10, 0));
        original.mark();

        FlexibleActivity copy = (FlexibleActivity) ActivityCopyFactory.copyAll(List.of(original)).get(0);

        assertNotSame(original, copy);
        assertEquals(original.getId(), copy.getId());
        assertEquals(original.getDescription(), copy.getDescription());
        assertEquals(original.getCategory(), copy.getCategory());
        assertEquals(original.getDate(), copy.getDate());
        assertEquals(original.getEarliestStart(), copy.getEarliestStart());
        assertEquals(original.getLatestEnd(), copy.getLatestEnd());
        assertEquals(original.getDurationMinutes(), copy.getDurationMinutes());
        assertEquals(original.getEnergyRating(), copy.getEnergyRating());
        assertEquals(original.getSensoryRating(), copy.getSensoryRating());
        assertEquals(original.getTopic(), copy.getTopic());
        assertEquals(original.getNote(), copy.getNote());
        assertEquals(original.getAdoptedStartTime(), copy.getAdoptedStartTime());
        assertTrue(copy.isComplete());
    }

    @Test
    public void copyWithAdoptedStart_overridesPlacementWithoutMutatingOriginal() throws Exception {
        FlexibleActivity original = new FlexibleActivity(7, "Study", ActivityCategory.ACADEMIC,
                LocalDate.of(2026, 8, 17), LocalTime.of(9, 0), LocalTime.of(14, 0), 60,
                EnergyRating.of(4), SensoryRating.of(3), null, null);

        FlexibleActivity copy = (FlexibleActivity) ActivityCopyFactory.copyWithAdoptedStart(
                original, LocalTime.of(11, 0));

        assertEquals(LocalTime.of(11, 0), copy.getAdoptedStartTime());
        assertTrue(!original.hasAdoptedPlacement());
    }
}
