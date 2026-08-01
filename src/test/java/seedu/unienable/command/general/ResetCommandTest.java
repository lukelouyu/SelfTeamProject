package seedu.unienable.command.general;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;
import java.time.LocalTime;

import org.junit.jupiter.api.Test;

import seedu.unienable.command.CommandResult;
import seedu.unienable.command.MenuOutcome;
import seedu.unienable.logic.ActivityManager;
import seedu.unienable.logic.TopicManager;
import seedu.unienable.model.classes.EnergyRating;
import seedu.unienable.model.classes.FixedActivity;
import seedu.unienable.model.classes.SensoryRating;
import seedu.unienable.model.enums.ActivityCategory;
import seedu.unienable.model.enums.ActivityOrder;

class ResetCommandTest {
    private static FixedActivity newFixedActivity(int id, String description, ActivityCategory category,
            LocalTime start, LocalTime end) throws Exception {
        return new FixedActivity(id, description, category, LocalDate.of(2026, 8, 15), start, end,
                EnergyRating.of(2), SensoryRating.of(2), null, null);
    }

    @Test
    public void hasAnythingToReset_emptyFreshManagers_returnsFalse() {
        ActivityManager activityManager = new ActivityManager();
        TopicManager topicManager = new TopicManager(activityManager);

        assertFalse(new ResetCommand(activityManager, topicManager).hasAnythingToReset());
    }

    @Test
    public void hasAnythingToReset_onlyDefaultOrderChanged_returnsTrue() {
        ActivityManager activityManager = new ActivityManager();
        activityManager.setDefaultOrder(ActivityOrder.INPUT);
        TopicManager topicManager = new TopicManager(activityManager);

        assertTrue(new ResetCommand(activityManager, topicManager).hasAnythingToReset());
    }

    @Test
    public void hasAnythingToReset_onlyTopicExists_returnsTrue() throws Exception {
        ActivityManager activityManager = new ActivityManager();
        TopicManager topicManager = new TopicManager(activityManager);
        topicManager.add(ActivityCategory.ACADEMIC, "CG3207");

        assertTrue(new ResetCommand(activityManager, topicManager).hasAnythingToReset());
    }

    @Test
    public void execute_defaultSelection_clearsActivitiesAndTopicsAndReturnsSuccessMessage() throws Exception {
        // execute() called without going through applyMenuAnswer() at all (e.g. a direct unit
        // test, or the "nothing to reset" no-menu-shown path) must behave like the full-reset
        // option, matching v1.0's original single-outcome reset.
        ActivityManager activityManager = new ActivityManager();
        TopicManager topicManager = new TopicManager(activityManager);
        activityManager.add(newFixedActivity(activityManager.getNextId(), "Lecture", ActivityCategory.ACADEMIC,
                LocalTime.of(9, 0), LocalTime.of(10, 0)));
        topicManager.add(ActivityCategory.ACADEMIC, "CG3207");
        activityManager.setDefaultOrder(ActivityOrder.INPUT);

        CommandResult result = new ResetCommand(activityManager, topicManager).execute();

        assertEquals(0, activityManager.size());
        assertTrue(topicManager.getAll().isEmpty());
        assertEquals(1, activityManager.getNextId());
        assertEquals(ActivityOrder.CHRONOLOGICAL, activityManager.getDefaultOrder());
        assertEquals("All user data has been reset.\nYour next activity will use ID [1].", result.getFeedback());
    }

    @Test
    public void getActivityCountAndTopicCount_reflectCurrentManagerState() throws Exception {
        ActivityManager activityManager = new ActivityManager();
        TopicManager topicManager = new TopicManager(activityManager);
        activityManager.add(newFixedActivity(activityManager.getNextId(), "Lecture", ActivityCategory.ACADEMIC,
                LocalTime.of(9, 0), LocalTime.of(10, 0)));
        topicManager.add(ActivityCategory.ACADEMIC, "CG3207");
        topicManager.add(ActivityCategory.CCA, "Computing Club");

        ResetCommand command = new ResetCommand(activityManager, topicManager);

        assertEquals(1, command.getActivityCount());
        assertEquals(2, command.getTopicCount());
    }

    @Test
    public void getClassScheduleCountAndOtherActivityCount_reflectClassSchedulePolicy() throws Exception {
        ActivityManager activityManager = new ActivityManager();
        TopicManager topicManager = new TopicManager(activityManager);
        activityManager.add(newFixedActivity(activityManager.getNextId(), "CG3207 Lecture",
                ActivityCategory.ACADEMIC, LocalTime.of(9, 0), LocalTime.of(10, 0)));
        activityManager.add(newFixedActivity(activityManager.getNextId(), "Team meeting",
                ActivityCategory.WORK_INTERNSHIP, LocalTime.of(11, 0), LocalTime.of(12, 0)));

        ResetCommand command = new ResetCommand(activityManager, topicManager);

        assertEquals(1, command.getClassScheduleCount());
        assertEquals(1, command.getOtherActivityCount());
    }

    @Test
    public void getMenuPrompt_nothingToReset_returnsNull() {
        ActivityManager activityManager = new ActivityManager();
        TopicManager topicManager = new TopicManager(activityManager);

        assertNull(new ResetCommand(activityManager, topicManager).getMenuPrompt());
    }

    @Test
    public void getMenuPrompt_hasDataToReset_showsFullMenuWithCounts() throws Exception {
        ActivityManager activityManager = new ActivityManager();
        TopicManager topicManager = new TopicManager(activityManager);
        activityManager.add(newFixedActivity(activityManager.getNextId(), "CG3207 Lecture",
                ActivityCategory.ACADEMIC, LocalTime.of(9, 0), LocalTime.of(10, 0)));
        activityManager.add(newFixedActivity(activityManager.getNextId(), "Team meeting",
                ActivityCategory.WORK_INTERNSHIP, LocalTime.of(11, 0), LocalTime.of(12, 0)));
        topicManager.add(ActivityCategory.ACADEMIC, "CG3207");

        String prompt = new ResetCommand(activityManager, topicManager).getMenuPrompt();

        assertEquals("Reset user data\n\n"
                + "Activities      : 2\n"
                + "Class schedules : 1\n"
                + "Other activities: 1\n"
                + "Topics          : 1\n\n"
                + "[1] Delete all user data\n"
                + "[2] Delete other activities but keep class schedules\n"
                + "[3] Do not delete anything\n\n"
                + "Facility, connection, and academic-calendar reference data will be kept.\n"
                + "Enter 1, 2, or 3:", prompt);
    }

    @Test
    public void applyMenuAnswer_one_selectsDeleteAllAndProceeds() throws Exception {
        ActivityManager activityManager = new ActivityManager();
        TopicManager topicManager = new TopicManager(activityManager);
        activityManager.add(newFixedActivity(activityManager.getNextId(), "Lecture", ActivityCategory.ACADEMIC,
                LocalTime.of(9, 0), LocalTime.of(10, 0)));
        ResetCommand command = new ResetCommand(activityManager, topicManager);

        MenuOutcome outcome = command.applyMenuAnswer("1");
        command.execute();

        assertTrue(outcome.isProceed());
        assertEquals(0, activityManager.size());
    }

    @Test
    public void applyMenuAnswer_two_selectsKeepClassScheduleAndProceeds() {
        ActivityManager activityManager = new ActivityManager();
        TopicManager topicManager = new TopicManager(activityManager);
        ResetCommand command = new ResetCommand(activityManager, topicManager);

        assertTrue(command.applyMenuAnswer("2").isProceed());
    }

    @Test
    public void applyMenuAnswer_three_cancelsWithMessage() {
        ActivityManager activityManager = new ActivityManager();
        TopicManager topicManager = new TopicManager(activityManager);
        ResetCommand command = new ResetCommand(activityManager, topicManager);

        MenuOutcome outcome = command.applyMenuAnswer("3");

        assertFalse(outcome.isProceed());
        assertEquals("Cancelled. No changes were made.", outcome.getMessage());
    }

    @Test
    public void applyMenuAnswer_invalidText_cancelsWithEnterOneTwoThreeMessage() {
        ActivityManager activityManager = new ActivityManager();
        TopicManager topicManager = new TopicManager(activityManager);
        ResetCommand command = new ResetCommand(activityManager, topicManager);

        MenuOutcome outcome = command.applyMenuAnswer("bogus");

        assertFalse(outcome.isProceed());
        assertEquals("Enter 1, 2, or 3. No changes were made.", outcome.getMessage());
    }

    @Test
    public void applyMenuAnswer_emptyString_cancelsWithEnterOneTwoThreeMessage() {
        // Empty string is what CommandConfirmationHandler passes on EOF; the spec requires EOF to
        // be treated the same as any other unrecognised answer, not silently mapped to "3".
        ActivityManager activityManager = new ActivityManager();
        TopicManager topicManager = new TopicManager(activityManager);
        ResetCommand command = new ResetCommand(activityManager, topicManager);

        MenuOutcome outcome = command.applyMenuAnswer("");

        assertFalse(outcome.isProceed());
        assertEquals("Enter 1, 2, or 3. No changes were made.", outcome.getMessage());
    }

    @Test
    public void execute_keepClassSchedule_retainsClassesAndTheirTopicsDeletesOthersPreservesIds() throws Exception {
        ActivityManager activityManager = new ActivityManager();
        TopicManager topicManager = new TopicManager(activityManager);
        topicManager.add(ActivityCategory.ACADEMIC, "CG3207");
        topicManager.add(ActivityCategory.CCA, "Basketball Club");
        activityManager.add(new FixedActivity(activityManager.getNextId(), "CG3207 Lecture",
                ActivityCategory.ACADEMIC, LocalDate.of(2026, 8, 15), LocalTime.of(9, 0), LocalTime.of(10, 0),
                EnergyRating.of(2), SensoryRating.of(2), "CG3207", null));
        activityManager.add(newFixedActivity(activityManager.getNextId(), "Doctor appointment",
                ActivityCategory.OTHERS, LocalTime.of(11, 0), LocalTime.of(12, 0)));
        activityManager.add(new FixedActivity(activityManager.getNextId(), "CG3207 Tutorial",
                ActivityCategory.ACADEMIC, LocalDate.of(2026, 8, 16), LocalTime.of(9, 0), LocalTime.of(10, 0),
                EnergyRating.of(2), SensoryRating.of(2), "CG3207", null));
        ResetCommand command = new ResetCommand(activityManager, topicManager);
        command.applyMenuAnswer("2");

        CommandResult result = command.execute();

        assertEquals(2, activityManager.size());
        assertEquals(1, activityManager.getById(1).getId());
        assertEquals("CG3207 Lecture", activityManager.getById(1).getDescription());
        assertEquals(3, activityManager.getById(3).getId());
        assertEquals("CG3207 Tutorial", activityManager.getById(3).getDescription());
        assertEquals(4, activityManager.getNextId());
        assertEquals(ActivityOrder.CHRONOLOGICAL, activityManager.getDefaultOrder());
        assertTrue(topicManager.exists(ActivityCategory.ACADEMIC, "CG3207"));
        assertFalse(topicManager.exists(ActivityCategory.CCA, "Basketball Club"));
        assertEquals("Reset complete. Kept 2 class-schedule activities and deleted 1 other activity."
                + "\nYour next activity will use ID [4].", result.getFeedback());
    }

    @Test
    public void execute_keepClassScheduleWithNothingEligible_nextIdFallsBackToOne() throws Exception {
        ActivityManager activityManager = new ActivityManager();
        TopicManager topicManager = new TopicManager(activityManager);
        activityManager.add(newFixedActivity(activityManager.getNextId(), "Doctor appointment",
                ActivityCategory.OTHERS, LocalTime.of(9, 0), LocalTime.of(10, 0)));
        ResetCommand command = new ResetCommand(activityManager, topicManager);
        command.applyMenuAnswer("2");

        command.execute();

        assertEquals(0, activityManager.size());
        assertEquals(1, activityManager.getNextId());
    }
}
