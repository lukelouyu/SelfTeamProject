package seedu.unienable.command.activity;

import seedu.unienable.command.CommandResult;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;
import java.time.LocalTime;

import org.junit.jupiter.api.Test;

import seedu.unienable.logic.ActivityFilter;
import seedu.unienable.logic.ActivityManager;
import seedu.unienable.model.classes.EnergyRating;
import seedu.unienable.model.classes.FixedActivity;
import seedu.unienable.model.classes.SensoryRating;
import seedu.unienable.model.enums.ActivityCategory;
import seedu.unienable.model.enums.ActivityOrder;

class ListCommandTest {
    private static FixedActivity newActivity(int id, String description, LocalTime start, LocalTime end,
            ActivityCategory category) throws Exception {
        return new FixedActivity(id, description, category, LocalDate.of(2026, 8, 15), start, end,
                EnergyRating.of(4), SensoryRating.of(3), null, null);
    }

    @Test
    public void execute_conciseView_listsAllMatchingActivities() throws Exception {
        ActivityManager manager = new ActivityManager();
        manager.add(newActivity(manager.getNextId(), "First", LocalTime.of(9, 0), LocalTime.of(10, 0),
                ActivityCategory.ACADEMIC));
        manager.add(newActivity(manager.getNextId(), "Second", LocalTime.of(11, 0), LocalTime.of(12, 0),
                ActivityCategory.ACADEMIC));

        CommandResult result = new ListCommand(manager, new ActivityFilter(null, null, null, null),
                ActivityOrder.INPUT, false).execute();

        String feedback = result.getFeedback();
        assertTrue(feedback.contains("Here are 2 matching activities:"));
        assertTrue(feedback.contains("First"));
        assertTrue(feedback.contains("Second"));
    }

    @Test
    public void execute_detailView_usesDetailFormat() throws Exception {
        ActivityManager manager = new ActivityManager();
        manager.add(newActivity(manager.getNextId(), "First", LocalTime.of(9, 0), LocalTime.of(10, 0),
                ActivityCategory.ACADEMIC));

        CommandResult result = new ListCommand(manager, new ActivityFilter(null, null, null, null),
                ActivityOrder.INPUT, true).execute();

        assertTrue(result.getFeedback().contains("Status: Incomplete | Type: FIXED"));
    }

    @Test
    public void execute_filterAppliesBeforeFormatting() throws Exception {
        ActivityManager manager = new ActivityManager();
        manager.add(newActivity(manager.getNextId(), "Academic task", LocalTime.of(9, 0), LocalTime.of(10, 0),
                ActivityCategory.ACADEMIC));
        manager.add(newActivity(manager.getNextId(), "CCA task", LocalTime.of(11, 0), LocalTime.of(12, 0),
                ActivityCategory.CCA));

        CommandResult result = new ListCommand(manager,
                new ActivityFilter(null, ActivityCategory.CCA, null, null), ActivityOrder.INPUT, false).execute();

        String feedback = result.getFeedback();
        assertTrue(feedback.contains("Here are 1 matching activity:"));
        assertTrue(feedback.contains("CCA task"));
    }

    @Test
    public void execute_noMatches_reportsNoActivitiesFound() {
        ActivityManager manager = new ActivityManager();

        CommandResult result = new ListCommand(manager, new ActivityFilter(null, null, null, null),
                ActivityOrder.INPUT, false).execute();

        assertEquals("No activities found.", result.getFeedback());
    }
}
