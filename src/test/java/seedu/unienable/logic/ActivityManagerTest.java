package seedu.unienable.logic;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import org.junit.jupiter.api.Test;

import seedu.unienable.exception.DuplicateActivityException;
import seedu.unienable.exception.InvalidIndexException;
import seedu.unienable.model.classes.Activity;
import seedu.unienable.model.classes.EnergyRating;
import seedu.unienable.model.classes.FixedActivity;
import seedu.unienable.model.classes.FlexibleActivity;
import seedu.unienable.model.classes.SensoryRating;
import seedu.unienable.model.enums.ActivityCategory;
import seedu.unienable.model.enums.ActivityOrder;
import seedu.unienable.model.enums.CompletionStatus;

class ActivityManagerTest {
    private static FixedActivity newFixedActivity(int id) throws Exception {
        return new FixedActivity(id, "CG3207 lecture", ActivityCategory.ACADEMIC,
                LocalDate.of(2026, 8, 15), LocalTime.of(9, 0), LocalTime.of(11, 0),
                EnergyRating.of(4), SensoryRating.of(3), null, null);
    }

    private static FixedActivity newFixedActivity(int id, String description, LocalTime start, LocalTime end)
            throws Exception {
        return new FixedActivity(id, description, ActivityCategory.ACADEMIC,
                LocalDate.of(2026, 8, 15), start, end, EnergyRating.of(4), SensoryRating.of(3), null, null);
    }

    private static FlexibleActivity newFlexibleActivity(int id, LocalTime earliestStart, LocalTime latestEnd)
            throws Exception {
        return new FlexibleActivity(id, "Finish assignment 1", ActivityCategory.ACADEMIC,
                LocalDate.of(2026, 8, 15), earliestStart, latestEnd, 90,
                EnergyRating.of(5), SensoryRating.of(2), null, null);
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

    @Test
    public void add_exactDuplicateFixedActivity_throwsDuplicateActivityException() throws Exception {
        ActivityManager manager = new ActivityManager();
        manager.add(newFixedActivity(manager.getNextId()));

        DuplicateActivityException exception = assertThrows(DuplicateActivityException.class,
                () -> manager.add(newFixedActivity(manager.getNextId())));
        assertEquals("An identical activity already exists.", exception.getMessage());
    }

    @Test
    public void add_overlappingFixedActivity_throwsDuplicateActivityExceptionWithGuideWording() throws Exception {
        ActivityManager manager = new ActivityManager();
        manager.add(newFixedActivity(manager.getNextId(), "CG3207 lecture", LocalTime.of(9, 0), LocalTime.of(11, 0)));

        DuplicateActivityException exception = assertThrows(DuplicateActivityException.class,
                () -> manager.add(newFixedActivity(manager.getNextId(), "Consultation",
                        LocalTime.of(10, 30), LocalTime.of(11, 30))));
        assertEquals("This timing overlaps activity [1], CG3207 lecture (09:00–11:00).", exception.getMessage());
    }

    @Test
    public void add_nonOverlappingFixedActivitiesSameDate_bothSucceed() throws Exception {
        ActivityManager manager = new ActivityManager();
        manager.add(newFixedActivity(manager.getNextId(), "Morning lecture", LocalTime.of(9, 0), LocalTime.of(11, 0)));
        manager.add(newFixedActivity(manager.getNextId(), "Afternoon briefing",
                LocalTime.of(14, 0), LocalTime.of(15, 0)));

        assertEquals(2, manager.size());
    }

    @Test
    public void add_exactDuplicateFlexibleActivity_throwsDuplicateActivityException() throws Exception {
        ActivityManager manager = new ActivityManager();
        manager.add(newFlexibleActivity(manager.getNextId(), LocalTime.of(10, 0), LocalTime.of(18, 0)));

        assertThrows(DuplicateActivityException.class,
                () -> manager.add(newFlexibleActivity(manager.getNextId(), LocalTime.of(10, 0),
                        LocalTime.of(18, 0))));
    }

    @Test
    public void add_overlappingFlexibleActivities_bothSucceed() throws Exception {
        ActivityManager manager = new ActivityManager();
        manager.add(newFlexibleActivity(manager.getNextId(), LocalTime.of(10, 0), LocalTime.of(18, 0)));

        manager.add(newFlexibleActivity(manager.getNextId(), LocalTime.of(11, 0), LocalTime.of(19, 0)));

        assertEquals(2, manager.size());
    }

    @Test
    public void delete_existingId_removesActivity() throws Exception {
        ActivityManager manager = new ActivityManager();
        manager.add(newFixedActivity(manager.getNextId()));

        manager.delete(1);

        assertEquals(0, manager.size());
        assertThrows(InvalidIndexException.class, () -> manager.getById(1));
    }

    @Test
    public void delete_doesNotChangeOtherActivityIds() throws Exception {
        ActivityManager manager = new ActivityManager();
        manager.add(newFixedActivity(manager.getNextId(), "First", LocalTime.of(9, 0), LocalTime.of(10, 0)));
        manager.add(newFixedActivity(manager.getNextId(), "Second", LocalTime.of(11, 0), LocalTime.of(12, 0)));

        manager.delete(1);

        assertEquals(2, manager.getById(2).getId());
    }

    @Test
    public void delete_unknownId_throwsInvalidIndexException() {
        ActivityManager manager = new ActivityManager();

        assertThrows(InvalidIndexException.class, () -> manager.delete(999));
    }

    @Test
    public void mark_existingId_marksActivityComplete() throws Exception {
        ActivityManager manager = new ActivityManager();
        manager.add(newFixedActivity(manager.getNextId()));

        Activity marked = manager.mark(1);

        assertTrue(marked.isComplete());
        assertTrue(manager.getById(1).isComplete());
    }

    @Test
    public void mark_alreadyComplete_isAllowed() throws Exception {
        ActivityManager manager = new ActivityManager();
        manager.add(newFixedActivity(manager.getNextId()));
        manager.mark(1);

        manager.mark(1);

        assertTrue(manager.getById(1).isComplete());
    }

    @Test
    public void unmark_afterMark_marksActivityIncomplete() throws Exception {
        ActivityManager manager = new ActivityManager();
        manager.add(newFixedActivity(manager.getNextId()));
        manager.mark(1);

        Activity unmarked = manager.unmark(1);

        assertFalse(unmarked.isComplete());
    }

    @Test
    public void mark_unknownId_throwsInvalidIndexException() {
        ActivityManager manager = new ActivityManager();

        assertThrows(InvalidIndexException.class, () -> manager.mark(999));
    }

    @Test
    public void list_withNoFilters_returnsEveryActivity() throws Exception {
        ActivityManager manager = new ActivityManager();
        manager.add(newFixedActivity(manager.getNextId(), "First", LocalTime.of(9, 0), LocalTime.of(10, 0)));
        manager.add(newFixedActivity(manager.getNextId(), "Second", LocalTime.of(11, 0), LocalTime.of(12, 0)));

        List<Activity> result = manager.list(new ActivityFilter(null, null, null, null), ActivityOrder.INPUT);

        assertEquals(2, result.size());
    }

    @Test
    public void list_withFilter_returnsOnlyMatchingActivities() throws Exception {
        ActivityManager manager = new ActivityManager();
        manager.add(newFixedActivity(manager.getNextId(), "First", LocalTime.of(9, 0), LocalTime.of(10, 0)));
        manager.add(newFixedActivity(manager.getNextId(), "Second", LocalTime.of(11, 0), LocalTime.of(12, 0)));
        manager.mark(1);

        List<Activity> result = manager.list(
                new ActivityFilter(CompletionStatus.COMPLETE, null, null, null), ActivityOrder.INPUT);

        assertEquals(1, result.size());
        assertEquals(1, result.get(0).getId());
    }

    @Test
    public void getDefaultOrder_startsAsChronological() {
        assertEquals(ActivityOrder.CHRONOLOGICAL, new ActivityManager().getDefaultOrder());
    }

    @Test
    public void list_inputOrder_preservesAddedSequence() throws Exception {
        ActivityManager manager = new ActivityManager();
        manager.add(newFixedActivity(manager.getNextId(), "Later time", LocalTime.of(14, 0), LocalTime.of(15, 0)));
        manager.add(newFixedActivity(manager.getNextId(), "Earlier time", LocalTime.of(9, 0), LocalTime.of(10, 0)));

        List<Activity> result = manager.list(new ActivityFilter(null, null, null, null), ActivityOrder.INPUT);

        assertEquals(1, result.get(0).getId());
        assertEquals(2, result.get(1).getId());
    }

    @Test
    public void list_timeOrder_sortsByStartTimeThenId() throws Exception {
        ActivityManager manager = new ActivityManager();
        manager.add(newFixedActivity(manager.getNextId(), "Later time", LocalTime.of(14, 0), LocalTime.of(15, 0)));
        manager.add(newFixedActivity(manager.getNextId(), "Earlier time", LocalTime.of(9, 0), LocalTime.of(10, 0)));

        List<Activity> result = manager.list(new ActivityFilter(null, null, null, null), ActivityOrder.TIME);

        assertEquals(2, result.get(0).getId());
        assertEquals(1, result.get(1).getId());
    }

    @Test
    public void list_chronologicalOrder_sortsByDateThenTime() throws Exception {
        ActivityManager manager = new ActivityManager();
        FixedActivity laterDate = new FixedActivity(manager.getNextId(), "Later date", ActivityCategory.ACADEMIC,
                LocalDate.of(2026, 8, 16), LocalTime.of(9, 0), LocalTime.of(10, 0),
                EnergyRating.of(3), SensoryRating.of(2), null, null);
        manager.add(laterDate);
        manager.add(newFixedActivity(manager.getNextId(), "Earlier date", LocalTime.of(14, 0), LocalTime.of(15, 0)));

        List<Activity> result = manager.list(new ActivityFilter(null, null, null, null),
                ActivityOrder.CHRONOLOGICAL);

        assertEquals(2, result.get(0).getId());
        assertEquals(1, result.get(1).getId());
    }

    @Test
    public void list_nullOrder_usesSavedDefault() throws Exception {
        ActivityManager manager = new ActivityManager();
        manager.add(newFixedActivity(manager.getNextId(), "Later time", LocalTime.of(14, 0), LocalTime.of(15, 0)));
        manager.add(newFixedActivity(manager.getNextId(), "Earlier time", LocalTime.of(9, 0), LocalTime.of(10, 0)));
        manager.setDefaultOrder(ActivityOrder.TIME);

        List<Activity> result = manager.list(new ActivityFilter(null, null, null, null), null);

        assertEquals(2, result.get(0).getId());
        assertEquals(1, result.get(1).getId());
    }
}
