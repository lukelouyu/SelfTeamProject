package seedu.unienable.logic;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.LocalDate;
import java.time.LocalTime;

import org.junit.jupiter.api.Test;

import seedu.unienable.exception.InvalidIndexException;
import seedu.unienable.model.classes.EnergyRating;
import seedu.unienable.model.classes.FixedActivity;
import seedu.unienable.model.classes.SensoryRating;
import seedu.unienable.model.enums.ActivityCategory;

class ActivityManagerTest {
    private static FixedActivity newFixedActivity(int id) throws Exception {
        return new FixedActivity(id, "CG3207 lecture", ActivityCategory.ACADEMIC,
                LocalDate.of(2026, 8, 15), LocalTime.of(9, 0), LocalTime.of(11, 0),
                EnergyRating.of(4), SensoryRating.of(3), null, null);
    }

    @Test
    public void getNextId_startsAtOneAndIncrementsOnlyOnAdd() throws Exception {
        ActivityManager manager = new ActivityManager();

        assertEquals(1, manager.getNextId());
        assertEquals(1, manager.getNextId());

        manager.add(newFixedActivity(manager.getNextId()));

        assertEquals(2, manager.getNextId());
    }

    @Test
    public void getById_existingId_returnsActivity() throws Exception {
        ActivityManager manager = new ActivityManager();
        manager.add(newFixedActivity(manager.getNextId()));

        assertEquals(1, manager.getById(1).getId());
    }

    @Test
    public void getById_unknownId_throwsInvalidIndexException() {
        ActivityManager manager = new ActivityManager();

        InvalidIndexException exception = assertThrows(InvalidIndexException.class, () -> manager.getById(999));
        assertEquals("Activity [999] does not exist.", exception.getMessage());
    }

    @Test
    public void getAll_isUnmodifiable() throws Exception {
        ActivityManager manager = new ActivityManager();
        manager.add(newFixedActivity(manager.getNextId()));

        assertThrows(UnsupportedOperationException.class,
                () -> manager.getAll().add(newFixedActivity(99)));
    }

    @Test
    public void size_reflectsNumberOfActivities() throws Exception {
        ActivityManager manager = new ActivityManager();

        assertEquals(0, manager.size());

        manager.add(newFixedActivity(manager.getNextId()));

        assertEquals(1, manager.size());
    }
}
