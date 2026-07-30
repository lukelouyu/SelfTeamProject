package seedu.unienable.command.topic;

import seedu.unienable.command.CommandResult;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.LocalDate;
import java.time.LocalTime;

import org.junit.jupiter.api.Test;

import seedu.unienable.exception.DuplicateActivityException;
import seedu.unienable.exception.InvalidIndexException;
import seedu.unienable.logic.ActivityManager;
import seedu.unienable.logic.TopicManager;
import seedu.unienable.model.classes.EnergyRating;
import seedu.unienable.model.classes.FixedActivity;
import seedu.unienable.model.classes.SensoryRating;
import seedu.unienable.model.enums.ActivityCategory;

class TopicDeleteCommandTest {
    @Test
    public void execute_unusedTopic_removesItAndFormatsConfirmation() throws Exception {
        TopicManager topicManager = new TopicManager(new ActivityManager());
        topicManager.add(ActivityCategory.ACADEMIC, "CG3207");

        CommandResult result = new TopicDeleteCommand(topicManager, ActivityCategory.ACADEMIC, "CG3207").execute();

        assertEquals("Topic CG3207 has been deleted.", result.getFeedback());
        assertFalse(topicManager.exists(ActivityCategory.ACADEMIC, "CG3207"));
    }

    @Test
    public void execute_topicStillInUse_throwsDuplicateActivityExceptionAndKeepsTopic() throws Exception {
        ActivityManager activityManager = new ActivityManager();
        activityManager.add(new FixedActivity(activityManager.getNextId(), "Activity 1", ActivityCategory.ACADEMIC,
                LocalDate.of(2026, 8, 15), LocalTime.of(9, 0), LocalTime.of(10, 0),
                EnergyRating.of(3), SensoryRating.of(2), "CG3207", null));
        TopicManager topicManager = new TopicManager(activityManager);
        topicManager.add(ActivityCategory.ACADEMIC, "CG3207");

        assertThrows(DuplicateActivityException.class,
                () -> new TopicDeleteCommand(topicManager, ActivityCategory.ACADEMIC, "CG3207").execute());
        assertEquals(1, activityManager.size());
        assertEquals("CG3207", activityManager.getById(1).getTopic());
    }

    @Test
    public void execute_unknownTopic_throwsInvalidIndexException() {
        TopicManager topicManager = new TopicManager(new ActivityManager());

        assertThrows(InvalidIndexException.class,
                () -> new TopicDeleteCommand(topicManager, ActivityCategory.ACADEMIC, "CG3207").execute());
    }

    @Test
    public void getters_returnConstructorValues() {
        TopicManager topicManager = new TopicManager(new ActivityManager());

        TopicDeleteCommand command = new TopicDeleteCommand(topicManager, ActivityCategory.CCA, "Computing Club");

        assertEquals(ActivityCategory.CCA, command.getCategory());
        assertEquals("Computing Club", command.getName());
    }
}
