package seedu.unienable.logic.recur;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

import org.junit.jupiter.api.Test;

import seedu.unienable.exception.DuplicateActivityException;
import seedu.unienable.exception.InvalidActivityException;
import seedu.unienable.logic.ActivityManager;
import seedu.unienable.model.classes.EnergyRating;
import seedu.unienable.model.classes.FixedActivity;
import seedu.unienable.model.classes.SensoryRating;
import seedu.unienable.model.enums.ActivityCategory;
import seedu.unienable.model.recur.RecurrencePlan;
import seedu.unienable.testutil.recur.RecurrenceTestData;

class RecurrencePlannerTest {
    private final RecurrencePlanner planner = new RecurrencePlanner();

    @Test
    public void plan_cs2113FullSemester_handlesRecessAndNoClassDay() throws Exception {
        ActivityManager manager = new ActivityManager();
        FixedActivity source = RecurrenceTestData.cs2113Lecture(manager.getNextId());
        manager.add(source);

        RecurrencePlan plan = planner.plan(source,
                List.of(1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13),
                RecurrenceTestData.calendar(), manager, RecurrenceTestData.NOW);

        assertEquals(11, plan.getOccurrencesToCreate().size());
        assertEquals(LocalDate.of(2026, 9, 18),
                plan.getOccurrencesToCreate().get(4).getActivity().getDate());
        assertEquals(LocalDate.of(2026, 10, 2),
                plan.getOccurrencesToCreate().get(5).getActivity().getDate());
        assertEquals(2, plan.getSkippedOccurrences().size());
        assertTrue(plan.getSkippedOccurrences().get(1).getReason().contains("Well-Being Day"));
    }

    @Test
    public void plan_cg3201SparseScreenshotWeeks_generatesThreeExactDates() throws Exception {
        ActivityManager manager = new ActivityManager();
        FixedActivity source = RecurrenceTestData.cg3201Lab(manager.getNextId());
        manager.add(source);

        RecurrencePlan plan = planner.plan(source, List.of(3, 7, 9, 11),
                RecurrenceTestData.calendar(), manager, RecurrenceTestData.NOW);

        assertEquals(List.of(LocalDate.of(2027, 3, 2), LocalDate.of(2027, 3, 16),
                        LocalDate.of(2027, 3, 30)),
                plan.getOccurrencesToCreate().stream()
                        .map(item -> item.getActivity().getDate()).toList());
    }

    @Test
    public void plan_sourceWeekOmitted_rejectsBeforeMutation() throws Exception {
        ActivityManager manager = new ActivityManager();
        FixedActivity source = RecurrenceTestData.cg3201Lab(manager.getNextId());
        manager.add(source);

        assertThrows(InvalidActivityException.class, () -> planner.plan(source,
                List.of(7, 9, 11), RecurrenceTestData.calendar(), manager, RecurrenceTestData.NOW));
        assertEquals(1, manager.size());
        assertEquals(2, manager.getNextId());
    }

    @Test
    public void plan_weekMissingFromTextData_rejectsWithoutGuessing() throws Exception {
        ActivityManager manager = new ActivityManager();
        FixedActivity source = RecurrenceTestData.cs2113Lecture(manager.getNextId());
        manager.add(source);

        assertThrows(InvalidActivityException.class, () -> planner.plan(source,
                List.of(1, 14), RecurrenceTestData.calendar(), manager, RecurrenceTestData.NOW));
    }

    @Test
    public void plan_oneTargetOverlapsExistingActivity_rejectsWholePlan() throws Exception {
        ActivityManager manager = new ActivityManager();
        FixedActivity source = RecurrenceTestData.cs2113Lecture(manager.getNextId());
        manager.add(source);
        manager.add(new FixedActivity(manager.getNextId(), "Consultation",
                ActivityCategory.ACADEMIC, LocalDate.of(2026, 8, 21), LocalTime.of(17, 0),
                LocalTime.of(19, 0), EnergyRating.of(2), SensoryRating.of(2), null, null));

        DuplicateActivityException exception = assertThrows(DuplicateActivityException.class,
                () -> planner.plan(source, List.of(1, 2, 3), RecurrenceTestData.calendar(), manager,
                        RecurrenceTestData.NOW));

        // Atomicity: the whole plan is rejected before any mutation - not just the one conflicting
        // week - and the message identifies exactly which teaching week and calendar date failed.
        assertTrue(exception.getMessage().contains("Week 2"));
        assertTrue(exception.getMessage().contains("2026-08-21"));
        assertEquals(2, manager.size());
        assertEquals(3, manager.getNextId());
    }

    @Test
    public void plan_conflictInFinalRequestedWeek_stillRejectsWholePlanWithNoPartialAdditions()
            throws Exception {
        ActivityManager manager = new ActivityManager();
        FixedActivity source = RecurrenceTestData.cs2113Lecture(manager.getNextId());
        manager.add(source);
        // Week 13 target date for the CS2113 Friday lecture is 2026-11-13.
        manager.add(new FixedActivity(manager.getNextId(), "Blocking event",
                ActivityCategory.ACADEMIC, LocalDate.of(2026, 11, 13), LocalTime.of(16, 30),
                LocalTime.of(17, 0), EnergyRating.of(2), SensoryRating.of(2), null, null));

        DuplicateActivityException exception = assertThrows(DuplicateActivityException.class,
                () -> planner.plan(source, List.of(1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13),
                        RecurrenceTestData.calendar(), manager, RecurrenceTestData.NOW));

        assertTrue(exception.getMessage().contains("Week 13"));
        assertTrue(exception.getMessage().contains("2026-11-13"));
        // Every earlier week (1-12) would have succeeded in isolation, proving this is a
        // last-candidate failure, not a first-candidate one - and still nothing was added.
        assertEquals(2, manager.size());
    }

    @Test
    public void plan_cg3201TwoWeekdaySessions_recurIndependentlyWithoutConflict() throws Exception {
        // SR-09 (approved recurrence prompt): the same course description family (CG3201)
        // recurring on two different weekdays must not interfere with each other - each series
        // is planned, and can be added, independently.
        ActivityManager manager = new ActivityManager();
        FixedActivity lab = RecurrenceTestData.cg3201Lab(manager.getNextId());
        manager.add(lab);
        FixedActivity tutorial = new FixedActivity(manager.getNextId(), "CG3201 TUT [01]",
                ActivityCategory.ACADEMIC, LocalDate.of(2027, 1, 28), LocalTime.of(9, 0), LocalTime.of(10, 0),
                EnergyRating.of(3), SensoryRating.of(3), "CG3201", null);
        manager.add(tutorial);

        RecurrencePlan labPlan = planner.plan(lab, List.of(3, 7, 9, 11), RecurrenceTestData.calendar(), manager,
                RecurrenceTestData.NOW);
        manager.addAllAtomically(labPlan.getOccurrencesToCreate().stream()
                .map(RecurrencePlan.PlannedOccurrence::getActivity).toList());
        RecurrencePlan tutorialPlan = planner.plan(tutorial, List.of(3, 7, 9, 11),
                RecurrenceTestData.calendar(), manager, RecurrenceTestData.NOW);

        assertEquals(List.of(LocalDate.of(2027, 3, 2), LocalDate.of(2027, 3, 16), LocalDate.of(2027, 3, 30)),
                labPlan.getOccurrencesToCreate().stream().map(item -> item.getActivity().getDate()).toList());
        assertEquals(List.of(LocalDate.of(2027, 3, 4), LocalDate.of(2027, 3, 18), LocalDate.of(2027, 4, 1)),
                tutorialPlan.getOccurrencesToCreate().stream().map(item -> item.getActivity().getDate()).toList());
    }

    @Test
    public void plan_nextIdNearIntegerMaxValue_throwsInvalidActivityExceptionInsteadOfWrappingCandidateId()
            throws Exception {
        // plan() computes each candidate's ID as nextId + toCreate.size(); with nextId already at
        // Integer.MAX_VALUE, the second occurrence's ID would silently wrap into a negative,
        // already-invalid candidate ID instead of failing predictably. The rejection must surface
        // as a domain exception (an anticipated capacity limit), not a raw ArithmeticException
        // (which the application boundary would otherwise report as an "unexpected internal
        // error" instead of a clean validation message).
        ActivityManager manager = new ActivityManager();
        FixedActivity source = RecurrenceTestData.cs2113Lecture(Integer.MAX_VALUE - 1);
        manager.loadAll(List.of(source));
        assertEquals(Integer.MAX_VALUE, manager.getNextId());

        InvalidActivityException exception = assertThrows(InvalidActivityException.class, () -> planner.plan(source,
                List.of(1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13), RecurrenceTestData.calendar(), manager,
                RecurrenceTestData.NOW));

        assertTrue(exception.getMessage().contains("not enough activity IDs remain"));
    }

    @Test
    public void plan_completedSource_copiesFieldsButNewOccurrenceIsIncomplete() throws Exception {
        ActivityManager manager = new ActivityManager();
        FixedActivity source = RecurrenceTestData.cs2113Lecture(manager.getNextId());
        source.mark();
        manager.add(source);

        FixedActivity generated = planner.plan(source, List.of(1, 2),
                RecurrenceTestData.calendar(), manager, RecurrenceTestData.NOW)
                .getOccurrencesToCreate().get(0).getActivity();

        assertEquals(source.getDescription(), generated.getDescription());
        assertEquals(source.getTopic(), generated.getTopic());
        assertEquals(source.getNote(), generated.getNote());
        assertEquals(source.getEnergyRating(), generated.getEnergyRating());
        assertEquals(source.getSensoryRating(), generated.getSensoryRating());
        assertFalse(generated.isComplete());
    }

    // Bug D regression coverage: recur must reuse add/edit's "not in the past" philosophy and
    // reject the whole plan atomically - nothing created - the moment any requested week would
    // resolve to a new occurrence dated at or before "now".

    @Test
    public void recur_occurrenceBeforeToday_rejectedAtomically() throws Exception {
        // CS2113 lecture: source Week 1 = 2026-08-14, Week 2 = 2026-08-21, Week 3 = 2026-08-28.
        // "now" is set after Week 2's date but before Week 3's, so Week 2 alone is the offending
        // occurrence - the error must name it specifically.
        ActivityManager manager = new ActivityManager();
        FixedActivity source = RecurrenceTestData.cs2113Lecture(manager.getNextId());
        manager.add(source);
        LocalDateTime now = LocalDateTime.of(2026, 8, 25, 9, 0);

        InvalidActivityException exception = assertThrows(InvalidActivityException.class,
                () -> planner.plan(source, List.of(1, 2, 3), RecurrenceTestData.calendar(), manager, now));

        assertTrue(exception.getMessage().contains("Week 2"));
        assertTrue(exception.getMessage().contains("2026-08-21"));
        assertTrue(exception.getMessage().contains("already passed"));
    }

    @Test
    public void recur_todayFixedOccurrenceAlreadyStarted_rejected() throws Exception {
        // Week 2's target date (2026-08-21) is "today" relative to now, and its inherited start
        // time (16:00, from the CS2113 lecture fixture) has already started at "now" - the same
        // same-day-time-of-day rule add/edit already enforce must reject this too.
        ActivityManager manager = new ActivityManager();
        FixedActivity source = RecurrenceTestData.cs2113Lecture(manager.getNextId());
        manager.add(source);
        LocalDateTime now = LocalDateTime.of(2026, 8, 21, 16, 0);

        InvalidActivityException exception = assertThrows(InvalidActivityException.class,
                () -> planner.plan(source, List.of(1, 2), RecurrenceTestData.calendar(), manager, now));

        assertTrue(exception.getMessage().contains("Week 2"));
        assertTrue(exception.getMessage().contains("2026-08-21"));
        assertTrue(exception.getMessage().contains("already passed today"));
    }

    @Test
    public void recur_invalidOccurrence_createsNothing() throws Exception {
        // Atomicity: a mixed plan with both valid-future weeks (1, 3) and one past week (2) must
        // reject the entire plan before confirmation - nothing gets added, not even the valid
        // future weeks either side of the offending one.
        ActivityManager manager = new ActivityManager();
        FixedActivity source = RecurrenceTestData.cs2113Lecture(manager.getNextId());
        manager.add(source);
        LocalDateTime now = LocalDateTime.of(2026, 8, 25, 9, 0);
        int nextIdBefore = manager.getNextId();

        assertThrows(InvalidActivityException.class,
                () -> planner.plan(source, List.of(1, 2, 3), RecurrenceTestData.calendar(), manager, now));

        assertEquals(1, manager.size());
        assertEquals(nextIdBefore, manager.getNextId());
    }

    @Test
    public void recur_allFutureOccurrences_stillSucceeds() throws Exception {
        // Regression guard: the new past-occurrence check must not reject a plan whose every
        // requested week resolves to a genuinely future date.
        ActivityManager manager = new ActivityManager();
        FixedActivity source = RecurrenceTestData.cs2113Lecture(manager.getNextId());
        manager.add(source);
        LocalDateTime now = LocalDateTime.of(2026, 8, 1, 0, 0);

        RecurrencePlan plan = planner.plan(source, List.of(1, 2, 3), RecurrenceTestData.calendar(), manager, now);

        assertEquals(2, plan.getOccurrencesToCreate().size());
        assertEquals(LocalDate.of(2026, 8, 21),
                plan.getOccurrencesToCreate().get(0).getActivity().getDate());
        assertEquals(LocalDate.of(2026, 8, 28),
                plan.getOccurrencesToCreate().get(1).getActivity().getDate());
    }
}
