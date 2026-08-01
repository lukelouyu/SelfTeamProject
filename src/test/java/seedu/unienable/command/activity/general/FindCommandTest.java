package seedu.unienable.command.activity.general;

import seedu.unienable.command.CommandResult;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import org.junit.jupiter.api.Test;

import seedu.unienable.logic.ActivityFilter;
import seedu.unienable.logic.ActivityManager;
import seedu.unienable.model.classes.EnergyRating;
import seedu.unienable.model.classes.FixedActivity;
import seedu.unienable.model.classes.SensoryRating;
import seedu.unienable.model.enums.ActivityCategory;
import seedu.unienable.model.enums.ActivityOrder;

class FindCommandTest {
    private static FixedActivity newActivity(int id, String description, LocalTime start, LocalTime end)
            throws Exception {
        return new FixedActivity(id, description, ActivityCategory.ACADEMIC, LocalDate.of(2026, 8, 15),
                start, end, EnergyRating.of(4), SensoryRating.of(3), null, null);
    }

    @Test
    public void execute_keywordMatch_findsActivity() throws Exception {
        ActivityManager manager = new ActivityManager();
        manager.add(newActivity(manager.getNextId(), "Finish assignment 1", LocalTime.of(9, 0), LocalTime.of(10, 0)));

        CommandResult result = new FindCommand(manager, List.of("assignment"),
                new ActivityFilter(null, null, null, null), ActivityOrder.INPUT, false).execute();

        String feedback = result.getFeedback();
        assertTrue(feedback.contains("Found 1 activity:"));
        assertTrue(feedback.contains("Finish assignment 1"));
    }

    @Test
    public void execute_multipleMatches_usesPluralHeader() throws Exception {
        ActivityManager manager = new ActivityManager();
        manager.add(newActivity(manager.getNextId(), "Finish assignment 1", LocalTime.of(9, 0), LocalTime.of(10, 0)));
        manager.add(newActivity(manager.getNextId(), "Finish assignment 2", LocalTime.of(11, 0), LocalTime.of(12, 0)));

        CommandResult result = new FindCommand(manager, List.of("finish"),
                new ActivityFilter(null, null, null, null), ActivityOrder.INPUT, false).execute();

        assertTrue(result.getFeedback().contains("Found 2 activities:"));
    }

    @Test
    public void execute_noMatch_reportsNoActivitiesFound() throws Exception {
        ActivityManager manager = new ActivityManager();
        manager.add(newActivity(manager.getNextId(), "Finish assignment 1", LocalTime.of(9, 0), LocalTime.of(10, 0)));

        CommandResult result = new FindCommand(manager, List.of("unrelated"),
                new ActivityFilter(null, null, null, null), ActivityOrder.INPUT, false).execute();

        assertEquals("No activities found.", result.getFeedback());
    }
}
