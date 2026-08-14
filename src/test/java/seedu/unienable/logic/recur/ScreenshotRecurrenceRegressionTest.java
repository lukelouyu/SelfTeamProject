package seedu.unienable.logic.recur;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import org.junit.jupiter.api.Test;

import seedu.unienable.logic.ActivityManager;
import seedu.unienable.model.classes.EnergyRating;
import seedu.unienable.model.classes.FixedActivity;
import seedu.unienable.model.classes.SensoryRating;
import seedu.unienable.model.enums.ActivityCategory;
import seedu.unienable.model.recur.AcademicCalendar;
import seedu.unienable.model.recur.RecurrencePlan;
import seedu.unienable.storage.recur.AcademicCalendarStorage;
import seedu.unienable.testutil.recur.RecurrenceTestData;

/** Regression fixtures transcribed from the supplied Semester 1 and Semester 2 NUSMods images. */
class ScreenshotRecurrenceRegressionTest {
    private final RecurrencePlanner planner = new RecurrencePlanner();
    private final AcademicCalendar calendar = loadCalendar();

    @Test
    public void plan_semesterOneScreenshotPatterns_generateExpectedWeekDates() throws Exception {
        assertPlannedDates("GEC1044 TUT", LocalDate.of(2026, 8, 31),
                LocalTime.of(12, 0), LocalTime.of(14, 0), List.of(4, 6, 8, 10, 12),
                List.of("2026-09-14", "2026-10-05", "2026-10-19", "2026-11-02"));
        assertPlannedDates("CG2027 LEC", LocalDate.of(2026, 10, 6),
                LocalTime.of(14, 0), LocalTime.of(17, 0), List.of(8, 9, 10, 11, 12, 13),
                List.of("2026-10-13", "2026-10-20", "2026-10-27", "2026-11-03", "2026-11-10"));
        assertPlannedDates("CG2028 LAB", LocalDate.of(2026, 8, 27),
                LocalTime.of(14, 0), LocalTime.of(17, 0), List.of(3, 4, 5, 7),
                List.of("2026-09-03", "2026-09-10", "2026-10-01"));
        assertPlannedDates("CDE2501 TUT", LocalDate.of(2026, 8, 27),
                LocalTime.of(14, 0), LocalTime.of(17, 0), List.of(3, 5, 6, 7, 10, 12, 13),
                List.of("2026-09-10", "2026-09-17", "2026-10-01", "2026-10-22",
                        "2026-11-05", "2026-11-12"));
    }

    @Test
    public void plan_sameThursdayTimeButDisjointWeekRanges_doesNotReportConflict() throws Exception {
        ActivityManager manager = new ActivityManager();
        FixedActivity cg2028 = fixed(manager, "CG2028 TUT", LocalDate.of(2026, 8, 13),
                LocalTime.of(13, 0), LocalTime.of(14, 0));
        RecurrencePlan first = planner.plan(cg2028, List.of(1, 2, 3, 4, 5, 6), calendar, manager,
                RecurrenceTestData.NOW);
        manager.addAllAtomically(first.getOccurrencesToCreate().stream()
                .map(RecurrencePlan.PlannedOccurrence::getActivity).toList());

        FixedActivity cg2027 = fixed(manager, "CG2027 TUT", LocalDate.of(2026, 10, 8),
                LocalTime.of(13, 0), LocalTime.of(14, 0));
        RecurrencePlan second = planner.plan(cg2027, List.of(8, 9, 10, 11, 12, 13), calendar, manager,
                RecurrenceTestData.NOW);

        assertEquals(5, second.getOccurrencesToCreate().size());
    }

    @Test
    public void plan_adjacentCg2028LectureBlocks_areNotOverlaps() throws Exception {
        ActivityManager manager = new ActivityManager();
        fixed(manager, "CG2028 LEC first", LocalDate.of(2026, 8, 11),
                LocalTime.of(14, 0), LocalTime.of(15, 0));
        FixedActivity second = fixed(manager, "CG2028 LEC second", LocalDate.of(2026, 8, 11),
                LocalTime.of(15, 0), LocalTime.of(17, 0));

        RecurrencePlan plan = planner.plan(second, List.of(1, 2, 3, 4, 5, 6), calendar, manager,
                RecurrenceTestData.NOW);

        assertEquals(5, plan.getOccurrencesToCreate().size());
    }

    @Test
    public void plan_semesterTwoSectionTeaching_skipsTxtDefinedNoClassMonday() throws Exception {
        assertPlannedDates("ES2631 SEC", LocalDate.of(2027, 1, 18),
                LocalTime.of(15, 0), LocalTime.of(18, 0),
                List.of(2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13),
                List.of("2027-01-25", "2027-02-01", "2027-02-15", "2027-03-01",
                        "2027-03-08", "2027-03-15", "2027-03-22", "2027-03-29",
                        "2027-04-05", "2027-04-12"));
    }

    private void assertPlannedDates(String description, LocalDate sourceDate,
            LocalTime start, LocalTime end, List<Integer> weeks, List<String> expectedDates)
            throws Exception {
        ActivityManager manager = new ActivityManager();
        FixedActivity source = fixed(manager, description, sourceDate, start, end);

        RecurrencePlan plan = planner.plan(source, weeks, calendar, manager, RecurrenceTestData.NOW);

        assertEquals(expectedDates, plan.getOccurrencesToCreate().stream()
                .map(item -> item.getActivity().getDate().toString()).toList());
    }

    private FixedActivity fixed(ActivityManager manager, String description, LocalDate date,
            LocalTime start, LocalTime end) throws Exception {
        FixedActivity activity = new FixedActivity(manager.getNextId(), description,
                ActivityCategory.ACADEMIC, date, start, end, EnergyRating.of(3),
                SensoryRating.of(3), null, null);
        manager.add(activity);
        return activity;
    }

    private AcademicCalendar loadCalendar() {
        try {
            return new AcademicCalendarStorage().load(Path.of("data", "academic-calendar.txt"));
        } catch (Exception e) {
            throw new IllegalStateException("test calendar could not be loaded", e);
        }
    }
}
