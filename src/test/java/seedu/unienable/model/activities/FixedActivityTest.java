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

class FixedActivityTest {
    private static FixedActivity newFixedActivity() throws Exception {
        return new FixedActivity(12, "CG3207 lecture", ActivityCategory.ACADEMIC,
                LocalDate.of(2026, 8, 15), LocalTime.of(9, 0), LocalTime.of(11, 0),
                EnergyRating.of(4), SensoryRating.of(3), "CG3207", "Bring laptop");
    }

    @Test
    public void constructor_setsTimingFields() throws Exception {
        FixedActivity activity = newFixedActivity();

        assertEquals(LocalTime.of(9, 0), activity.getStartTime());
        assertEquals(LocalTime.of(11, 0), activity.getEndTime());
    }

    @Test
    public void getScheduleType_returnsFixed() throws Exception {
        FixedActivity activity = newFixedActivity();

        assertEquals(ScheduleType.FIXED, activity.getScheduleType());
    }

    @Test
    public void setStartTime_updatesStartTime() throws Exception {
        FixedActivity activity = newFixedActivity();

        activity.setStartTime(LocalTime.of(10, 0));

        assertEquals(LocalTime.of(10, 0), activity.getStartTime());
    }

    @Test
    public void setEndTime_updatesEndTime() throws Exception {
        FixedActivity activity = newFixedActivity();

        activity.setEndTime(LocalTime.of(12, 0));

        assertEquals(LocalTime.of(12, 0), activity.getEndTime());
    }

    @Test
    public void isInstanceOfActivity_inheritsSharedBehaviour() throws Exception {
        FixedActivity activity = newFixedActivity();

        assertTrue(activity instanceof Activity);

        activity.mark();

        assertTrue(activity.isComplete());
    }
}
