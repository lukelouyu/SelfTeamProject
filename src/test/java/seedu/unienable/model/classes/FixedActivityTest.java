package seedu.unienable.model.classes;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;
import java.time.LocalTime;

import org.junit.jupiter.api.Test;

import seedu.unienable.model.enums.ActivityCategory;
import seedu.unienable.model.enums.ScheduleType;

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

    @Test
    public void constructor_endTimeNotAfterStartTime_throwsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class, () -> new FixedActivity(1, "desc", ActivityCategory.ACADEMIC,
                LocalDate.of(2026, 8, 15), LocalTime.of(11, 0), LocalTime.of(9, 0),
                EnergyRating.of(1), SensoryRating.of(1), null, null));
    }

    @Test
    public void setStartTime_afterEnd_rejectsWithoutChangingTiming() throws Exception {
        FixedActivity activity = newFixedActivity();

        assertThrows(IllegalArgumentException.class,
                () -> activity.setStartTime(LocalTime.of(12, 0)));

        assertEquals(LocalTime.of(9, 0), activity.getStartTime());
        assertEquals(LocalTime.of(11, 0), activity.getEndTime());
    }

    @Test
    public void updateTiming_validPair_updatesBothTimesAtomically() throws Exception {
        FixedActivity activity = newFixedActivity();

        activity.updateTiming(LocalTime.of(12, 0), LocalTime.of(14, 0));

        assertEquals(LocalTime.of(12, 0), activity.getStartTime());
        assertEquals(LocalTime.of(14, 0), activity.getEndTime());
    }
}
