package seedu.unienable.command;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import seedu.unienable.exception.DuplicateActivityException;
import seedu.unienable.logic.ActivityManager;
import seedu.unienable.logic.TopicManager;
import seedu.unienable.model.enums.ActivityCategory;

class TopicAddCommandTest {
    @Test
    public void execute_newTopic_createsItAndFormatsConfirmation() throws Exception {
        TopicManager topicManager = new TopicManager(new ActivityManager());

        CommandResult result = new TopicAddCommand(topicManager, ActivityCategory.ACADEMIC, "CG3207").execute();

        assertEquals("Topic created:\nCategory: ACADEMIC\nTopic   : CG3207", result.getFeedback());
        assertTrue(topicManager.exists(ActivityCategory.ACADEMIC, "CG3207"));
    }

    @Test
    public void execute_duplicateName_throwsDuplicateActivityException() throws Exception {
        TopicManager topicManager = new TopicManager(new ActivityManager());
        topicManager.add(ActivityCategory.ACADEMIC, "CG3207");

        assertThrows(DuplicateActivityException.class,
                () -> new TopicAddCommand(topicManager, ActivityCategory.ACADEMIC, "CG3207").execute());
    }
}
