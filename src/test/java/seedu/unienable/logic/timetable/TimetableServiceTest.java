package seedu.unienable.logic.timetable;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

import org.junit.jupiter.api.Test;

import seedu.unienable.logic.ActivityManager;
import seedu.unienable.model.classes.Activity;
import seedu.unienable.model.classes.EnergyRating;
import seedu.unienable.model.classes.FixedActivity;
import seedu.unienable.model.classes.FlexibleActivity;
import seedu.unienable.model.classes.SensoryRating;
import seedu.unienable.model.enums.ActivityCategory;
import seedu.unienable.model.timetable.TimetableEntry;
import seedu.unienable.model.timetable.TimetableEntryType;
import seedu.unienable.model.timetable.TimetablePeriod;
import seedu.unienable.model.timetable.TimetableView;

class TimetableServiceTest {
    private static final LocalDate MONDAY = LocalDate.of(2026, 8, 17);

    private static FixedActivity fixed(int id, LocalDate date, int startHour, int startMinute,
            int endHour, int endMinute) throws Exception {
        return new FixedActivity(id, "Fixed " + id, ActivityCategory.ACADEMIC, date,
                LocalTime.of(startHour, startMinute), LocalTime.of(endHour, endMinute),
                EnergyRating.of(2), SensoryRating.of(3), null, null);
    }

    private static FlexibleActivity flexible(int id, LocalDate date, int startHour, int endHour)
            throws Exception {
        return new FlexibleActivity(id, "Flexible " + id, ActivityCategory.OTHERS, date,
                LocalTime.of(startHour, 0), LocalTime.of(endHour, 0), 60,
                EnergyRating.of(1), SensoryRating.of(2), null, null);
    }

    @Test
    public void resolveDay_exactInclusiveDate() {
        TimetablePeriod period = TimetableService.resolveDay(MONDAY);

        assertEquals(MONDAY, period.getStartDate());
        assertEquals(MONDAY, period.getEndDate());
        assertFalse(period.isWeekly());
    }

    @Test
    public void resolveWeek_midweek_resolvesContainingMondaySunday() {
        TimetablePeriod period = TimetableService.resolveWeek(LocalDate.of(2026, 8, 19));

        assertEquals(MONDAY, period.getStartDate());
        assertEquals(LocalDate.of(2026, 8, 23), period.getEndDate());
    }

    @Test
    public void resolveWeek_sundayAndYearBoundary_resolveCorrectly() {
        TimetablePeriod sunday = TimetableService.resolveWeek(LocalDate.of(2026, 8, 23));
        TimetablePeriod newYear = TimetableService.resolveWeek(LocalDate.of(2027, 1, 1));

        assertEquals(MONDAY, sunday.getStartDate());
        assertEquals(LocalDate.of(2026, 12, 28), newYear.getStartDate());
        assertEquals(LocalDate.of(2027, 1, 3), newYear.getEndDate());
    }

    @Test
    public void resolveThisWeek_usesInjectedNow() {
        TimetablePeriod period = TimetableService.resolveThisWeek(
                LocalDateTime.of(2026, 8, 23, 23, 59));

        assertEquals(MONDAY, period.getStartDate());
        assertEquals("This week", period.getLabel());
    }

    @Test
    public void resolveNextWeek_isOneWeekAfterResolveThisWeek() {
        TimetablePeriod thisWeek = TimetableService.resolveThisWeek(LocalDateTime.of(2026, 8, 23, 23, 59));
        TimetablePeriod nextWeek = TimetableService.resolveNextWeek(LocalDateTime.of(2026, 8, 23, 23, 59));

        assertEquals(MONDAY.plusDays(7), nextWeek.getStartDate());
        assertEquals(thisWeek.getEndDate().plusDays(1), nextWeek.getStartDate());
        assertEquals("Next week", nextWeek.getLabel());
    }

    @Test
    public void build_empty_returnsEmptyView() {
        TimetableView view = TimetableService.build(List.of(), TimetableService.resolveWeek(MONDAY));

        assertTrue(view.isEmpty());
        assertFalse(view.hasOverlaps());
    }

    @Test
    public void build_filtersOutsidePeriodAndIncludesWeekend() throws Exception {
        List<Activity> activities = List.of(
                fixed(1, MONDAY.minusDays(1), 9, 0, 10, 0),
                fixed(2, MONDAY, 9, 0, 10, 0),
                fixed(3, MONDAY.plusDays(6), 9, 0, 10, 0),
                fixed(4, MONDAY.plusDays(7), 9, 0, 10, 0));

        TimetableView view = TimetableService.build(activities, TimetableService.resolveWeek(MONDAY));

        assertEquals(List.of(2, 3), view.getFixedEntries().stream()
                .map(TimetableEntry::getId).toList());
    }

    @Test
    public void build_sortsByDateStartThenPermanentId() throws Exception {
        List<Activity> activities = List.of(
                fixed(9, MONDAY.plusDays(1), 8, 0, 9, 0),
                fixed(4, MONDAY, 10, 0, 11, 0),
                fixed(3, MONDAY, 9, 0, 10, 0),
                fixed(2, MONDAY, 9, 0, 9, 30));

        TimetableView view = TimetableService.build(activities, TimetableService.resolveWeek(MONDAY));

        assertEquals(List.of(2, 3, 4, 9), view.getFixedEntries().stream()
                .map(TimetableEntry::getId).toList());
    }

    @Test
    public void build_identicalStartsRetainsEveryActivityAndMarksOverlap() throws Exception {
        List<Activity> activities = List.of(
                fixed(2, MONDAY, 9, 0, 10, 0),
                fixed(1, MONDAY, 9, 0, 11, 0));

        TimetableView view = TimetableService.build(activities, TimetableService.resolveDay(MONDAY));

        assertEquals(List.of(1, 2), view.getFixedEntries().stream()
                .map(TimetableEntry::getId).toList());
        assertTrue(view.getFixedEntries().stream().allMatch(TimetableEntry::isOverlapping));
    }

    @Test
    public void build_nestedOverlap_marksEveryParticipant() throws Exception {
        List<Activity> activities = List.of(
                fixed(1, MONDAY, 9, 0, 13, 0),
                fixed(2, MONDAY, 10, 0, 11, 0),
                fixed(3, MONDAY, 12, 0, 14, 0));

        TimetableView view = TimetableService.build(activities, TimetableService.resolveDay(MONDAY));

        assertTrue(view.hasOverlaps());
        assertTrue(view.getFixedEntries().stream().allMatch(TimetableEntry::isOverlapping));
    }

    @Test
    public void build_adjacentActivitiesAreNotOverlaps() throws Exception {
        List<Activity> activities = List.of(
                fixed(1, MONDAY, 9, 0, 10, 0),
                fixed(2, MONDAY, 10, 0, 11, 0));

        TimetableView view = TimetableService.build(activities, TimetableService.resolveDay(MONDAY));

        assertFalse(view.hasOverlaps());
    }

    @Test
    public void build_sameTimesDifferentDatesAreNotOverlaps() throws Exception {
        List<Activity> activities = List.of(
                fixed(1, MONDAY, 9, 0, 10, 0),
                fixed(2, MONDAY.plusDays(1), 9, 0, 10, 0));

        TimetableView view = TimetableService.build(activities, TimetableService.resolveWeek(MONDAY));

        assertFalse(view.hasOverlaps());
    }

    @Test
    public void build_flexibleActivitiesRemainSeparateAndSorted() throws Exception {
        List<Activity> activities = List.of(
                flexible(3, MONDAY.plusDays(1), 9, 12),
                flexible(2, MONDAY, 10, 15),
                flexible(1, MONDAY, 10, 14));

        TimetableView view = TimetableService.build(activities, TimetableService.resolveWeek(MONDAY));

        assertTrue(view.getFixedEntries().isEmpty());
        assertEquals(List.of(1, 2, 3), view.getUnscheduledFlexibleEntries().stream()
                .map(TimetableEntry::getId).toList());
    }

    @Test
    public void build_adoptedFlexibleAppearsAsScheduledRecommendedEntry() throws Exception {
        FlexibleActivity flexible = flexible(1, MONDAY, 9, 12);
        flexible.setAdoptedStartTime(LocalTime.of(10, 0));

        TimetableView view = TimetableService.build(List.of(flexible), TimetableService.resolveDay(MONDAY));

        assertEquals(1, view.getFixedEntries().size());
        assertTrue(view.getUnscheduledFlexibleEntries().isEmpty());
        assertEquals(TimetableEntryType.ADOPTED_FLEXIBLE, view.getFixedEntries().get(0).getType());
        assertEquals(LocalTime.of(10, 0), view.getFixedEntries().get(0).getStartTime());
        assertEquals(LocalTime.of(11, 0), view.getFixedEntries().get(0).getEndTime());
    }

    @Test
    public void build_createsImmutableProjectionIndependentOfSourceMutation() throws Exception {
        FixedActivity activity = fixed(1, MONDAY, 9, 0, 10, 0);
        TimetableView view = TimetableService.build(List.of(activity),
                TimetableService.resolveDay(MONDAY));

        activity.setDescription("Changed later");

        assertEquals("Fixed 1", view.getFixedEntries().get(0).getDescription());
        assertThrows(UnsupportedOperationException.class,
                () -> view.getFixedEntries().add(view.getFixedEntries().get(0)));
    }

    @Test
    public void buildFromManager_neverMutatesManager() throws Exception {
        ActivityManager manager = new ActivityManager();
        FixedActivity activity = fixed(1, MONDAY, 9, 0, 10, 0);
        manager.loadAll(List.of(activity));
        int nextIdBefore = manager.getNextId();

        TimetableService.build(manager, TimetableService.resolveDay(MONDAY));

        assertEquals(1, manager.size());
        assertEquals(nextIdBefore, manager.getNextId());
        assertFalse(activity.isComplete());
    }
}
