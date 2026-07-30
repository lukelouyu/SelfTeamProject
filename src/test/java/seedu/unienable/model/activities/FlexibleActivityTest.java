package seedu.unienable.model.activities;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;
import java.time.LocalTime;

import org.junit.jupiter.api.Test;

import seedu.unienable.model.Activity;
import seedu.unienable.model.ActivityCategory;
import seedu.unienable.model.EnergyRating;
import seedu.unienable.model.ScheduleType;
import seedu.unienable.model.SensoryRating;

class FlexibleActivityTest {
    private static FlexibleActivity newFlexibleActivity() throws Exception {
        return new FlexibleActivity(13, "Finish assignment 1", ActivityCategory.ACADEMIC,
                LocalDate.of(2026, 8, 15), LocalTime.of(10, 0), LocalTime.of(18, 0), 90,
                EnergyRating.of(5), SensoryRating.of(2), "CG3207", null);
    }

    @Test
    public void constructor_setsWindowAndDurationFields() throws Exception {
        FlexibleActivity activity = newFlexibleActivity();

        assertEquals(LocalTime.of(10, 0), activity.getEarliestStart());
        assertEquals(LocalTime.of(18, 0), activity.getLatestEnd());
        assertEquals(90, activity.getDurationMinutes());
    }

    @Test
    public void getScheduleType_returnsFlexible() throws Exception {
        FlexibleActivity activity = newFlexibleActivity();

        assertEquals(ScheduleType.FLEXIBLE, activity.getScheduleType());
    }

    @Test
    public void setEarliestStart_updatesEarliestStart() throws Exception {
        FlexibleActivity activity = newFlexibleActivity();

        activity.setEarliestStart(LocalTime.of(11, 0));

        assertEquals(LocalTime.of(11, 0), activity.getEarliestStart());
    }

    @Test
    public void setLatestEnd_updatesLatestEnd() throws Exception {
        FlexibleActivity activity = newFlexibleActivity();

        activity.setLatestEnd(LocalTime.of(19, 0));

        assertEquals(LocalTime.of(19, 0), activity.getLatestEnd());
    }

    @Test
    public void setDurationMinutes_updatesDurationMinutes() throws Exception {
        FlexibleActivity activity = newFlexibleActivity();

        activity.setDurationMinutes(60);

        assertEquals(60, activity.getDurationMinutes());
    }

    @Test
    public void isInstanceOfActivity_inheritsSharedBehaviour() throws Exception {
        FlexibleActivity activity = newFlexibleActivity();

        assertTrue(activity instanceof Activity);

        activity.mark();

        assertTrue(activity.isComplete());
    }
}
