package seedu.unienable.logic.recur;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;
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
                RecurrenceTestData.calendar(), manager);

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
                RecurrenceTestData.calendar(), manager);

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
                List.of(7, 9, 11), RecurrenceTestData.calendar(), manager));
        assertEquals(1, manager.size());
        assertEquals(2, manager.getNextId());
    }

    @Test
    public void plan_weekMissingFromTextData_rejectsWithoutGuessing() throws Exception {
        ActivityManager manager = new ActivityManager();
        FixedActivity source = RecurrenceTestData.cs2113Lecture(manager.getNextId());
        manager.add(source);

        assertThrows(InvalidActivityException.class, () -> planner.plan(source,
                List.of(1, 14), RecurrenceTestData.calendar(), manager));
    }

    @Test
    public void plan_oneTargetOverlapsExistingActivity_rejectsWholePlan() throws Exception {
        ActivityManager manager = new ActivityManager();
        FixedActivity source = RecurrenceTestData.cs2113Lecture(manager.getNextId());
        manager.add(source);
        manager.add(new FixedActivity(manager.getNextId(), "Consultation",
                ActivityCategory.ACADEMIC, LocalDate.of(2026, 8, 21), LocalTime.of(17, 0),
                LocalTime.of(19, 0), EnergyRating.of(2), SensoryRating.of(2), null, null));

        assertThrows(DuplicateActivityException.class, () -> planner.plan(source,
                List.of(1, 2, 3), RecurrenceTestData.calendar(), manager));
        assertEquals(2, manager.size());
        assertEquals(3, manager.getNextId());
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

        RecurrencePlan labPlan = planner.plan(lab, List.of(3, 7, 9, 11), RecurrenceTestData.calendar(), manager);
        manager.addAllAtomically(labPlan.getOccurrencesToCreate().stream()
                .map(RecurrencePlan.PlannedOccurrence::getActivity).toList());
        RecurrencePlan tutorialPlan = planner.plan(tutorial, List.of(3, 7, 9, 11),
                RecurrenceTestData.calendar(), manager);

        assertEquals(List.of(LocalDate.of(2027, 3, 2), LocalDate.of(2027, 3, 16), LocalDate.of(2027, 3, 30)),
                labPlan.getOccurrencesToCreate().stream().map(item -> item.getActivity().getDate()).toList());
        assertEquals(List.of(LocalDate.of(2027, 3, 4), LocalDate.of(2027, 3, 18), LocalDate.of(2027, 4, 1)),
                tutorialPlan.getOccurrencesToCreate().stream().map(item -> item.getActivity().getDate()).toList());
    }

    @Test
    public void plan_completedSource_copiesFieldsButNewOccurrenceIsIncomplete() throws Exception {
        ActivityManager manager = new ActivityManager();
        FixedActivity source = RecurrenceTestData.cs2113Lecture(manager.getNextId());
        source.mark();
        manager.add(source);

        FixedActivity generated = planner.plan(source, List.of(1, 2),
                RecurrenceTestData.calendar(), manager)
                .getOccurrencesToCreate().get(0).getActivity();

        assertEquals(source.getDescription(), generated.getDescription());
        assertEquals(source.getTopic(), generated.getTopic());
        assertEquals(source.getNote(), generated.getNote());
        assertEquals(source.getEnergyRating(), generated.getEnergyRating());
        assertEquals(source.getSensoryRating(), generated.getSensoryRating());
        assertFalse(generated.isComplete());
    }
}
