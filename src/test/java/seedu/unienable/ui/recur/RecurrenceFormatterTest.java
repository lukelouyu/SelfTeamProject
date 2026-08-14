package seedu.unienable.ui.recur;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

import seedu.unienable.logic.ActivityManager;
import seedu.unienable.logic.recur.RecurrencePlanner;
import seedu.unienable.model.classes.FixedActivity;
import seedu.unienable.model.recur.RecurrencePlan;
import seedu.unienable.testutil.recur.RecurrenceTestData;

class RecurrenceFormatterTest {
    @Test
    public void formatPreview_showsSourceCalendarCreatesSkipsAndConfirmation() throws Exception {
        ActivityManager manager = new ActivityManager();
        FixedActivity source = RecurrenceTestData.cs2113Lecture(manager.getNextId());
        manager.add(source);
        RecurrencePlan plan = new RecurrencePlanner().plan(source,
                List.of(1, 2, 8), RecurrenceTestData.calendar(), manager, RecurrenceTestData.NOW);

        String preview = RecurrenceFormatter.formatPreview(plan);

        assertTrue(preview.contains("Source       : CS2113 LEC [1]"));
        assertTrue(preview.contains("Calendar     : AY2026/2027 SEM1"));
        assertTrue(preview.contains("Week 2 | 2026-08-21 | Activity [2]"));
        assertTrue(preview.contains("Week 8 | 2026-10-09 | NUS Well-Being Day - no classes"));
        assertTrue(preview.endsWith("Continue? (y/n)"));
    }

    @Test
    public void formatSuccess_sparsePlan_listsStableIdsAndDates() throws Exception {
        ActivityManager manager = new ActivityManager();
        FixedActivity source = RecurrenceTestData.cg3201Lab(manager.getNextId());
        manager.add(source);
        RecurrencePlan plan = new RecurrencePlanner().plan(source, List.of(3, 7, 9, 11),
                RecurrenceTestData.calendar(), manager, RecurrenceTestData.NOW);

        String output = RecurrenceFormatter.formatSuccess(plan);

        assertTrue(output.contains("Created 3 recurring sessions from activity [1]"));
        assertTrue(output.contains("Week 7 | 2027-03-02 | Activity [2]"));
        assertTrue(output.contains("Week 11 | 2027-03-30 | Activity [4]"));
    }
}
