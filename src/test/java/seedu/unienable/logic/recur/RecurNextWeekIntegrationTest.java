package seedu.unienable.logic.recur;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;
import java.time.LocalDateTime;
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
import seedu.unienable.parser.activity.ActivityCommandParser;
import seedu.unienable.storage.recur.AcademicCalendarStorage;

/**
 * B09 coverage: verifies recur-created occurrences interact correctly with list's "next week"
 * date-range filter under an explicitly injected clock, since the two features are otherwise
 * only ever exercised independently. The clock is passed directly to
 * {@link ActivityCommandParser#parseList}, never read from the system clock - production code
 * has no scattered {@code LocalDate.now()} call sites for this filter.
 */
class RecurNextWeekIntegrationTest {
    @Test
    public void listNextWeek_afterRecur_onlySourceWeekOccurrenceInRangeAndCompletionIsolated() throws Exception {
        ActivityManager manager = new ActivityManager();
        FixedActivity source = new FixedActivity(manager.getNextId(), "CS2113 Lecture", ActivityCategory.ACADEMIC,
                LocalDate.of(2026, 8, 10), LocalTime.of(10, 0), LocalTime.of(12, 0),
                EnergyRating.of(3), SensoryRating.of(3), null, null);
        manager.add(source);
        AcademicCalendar calendar = new AcademicCalendarStorage().load(java.nio.file.Path.of("data",
                "academic-calendar.txt"));
        RecurrencePlan plan = new RecurrencePlanner().plan(source, List.of(1, 2, 3), calendar, manager);
        manager.addAllAtomically(plan.getOccurrencesToCreate().stream()
                .map(RecurrencePlan.PlannedOccurrence::getActivity).toList());

        ActivityCommandParser parser = new ActivityCommandParser();
        LocalDateTime clock = LocalDateTime.of(2026, 8, 9, 12, 0);

        String nextWeek = parser.parseList(manager, clock, "next week").execute().getFeedback();
        assertTrue(nextWeek.contains("2026-08-10"), "the source's own Week 1 date must appear in next week");
        assertTrue(!nextWeek.contains("2026-08-17"), "Week 2 must not leak into next week");
        assertTrue(!nextWeek.contains("2026-08-24"), "Week 3 must not leak into next week");

        parser.parseMark(manager, "1").execute();

        String incomplete = parser.parseList(manager, clock, "next week status/incomplete").execute()
                .getFeedback();
        assertTrue(incomplete.contains("No activities found"));

        String completed = parser.parseList(manager, clock, "next week status/completed").execute()
                .getFeedback();
        assertTrue(completed.contains("2026-08-10"));

        String thisWeek = parser.parseList(manager, clock, "this week").execute().getFeedback();
        assertTrue(!thisWeek.contains("2026-08-10"),
                "this week (ending Sunday 2026-08-09) must not include Monday's session");
    }
}
