package seedu.unienable.command.general;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;
import java.time.LocalTime;

import org.junit.jupiter.api.Test;

import seedu.unienable.command.CommandResult;
import seedu.unienable.logic.ActivityManager;
import seedu.unienable.logic.TopicManager;
import seedu.unienable.model.classes.EnergyRating;
import seedu.unienable.model.classes.FixedActivity;
import seedu.unienable.model.classes.SensoryRating;
import seedu.unienable.model.enums.ActivityCategory;
import seedu.unienable.model.enums.ActivityOrder;

class ResetCommandTest {
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
    public void execute_clearsActivitiesAndTopicsAndReturnsSuccessMessage() throws Exception {
        ActivityManager activityManager = new ActivityManager();
        TopicManager topicManager = new TopicManager(activityManager);
        activityManager.add(new FixedActivity(activityManager.getNextId(), "Lecture", ActivityCategory.ACADEMIC,
                LocalDate.of(2026, 8, 15), LocalTime.of(9, 0), LocalTime.of(10, 0),
                EnergyRating.of(2), SensoryRating.of(2), null, null));
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
        activityManager.add(new FixedActivity(activityManager.getNextId(), "Lecture", ActivityCategory.ACADEMIC,
                LocalDate.of(2026, 8, 15), LocalTime.of(9, 0), LocalTime.of(10, 0),
                EnergyRating.of(2), SensoryRating.of(2), null, null));
        topicManager.add(ActivityCategory.ACADEMIC, "CG3207");
        topicManager.add(ActivityCategory.CCA, "Computing Club");

        ResetCommand command = new ResetCommand(activityManager, topicManager);

        assertEquals(1, command.getActivityCount());
        assertEquals(2, command.getTopicCount());
    }
}
