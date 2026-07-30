package seedu.unienable.command;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import seedu.unienable.logic.ActivityManager;
import seedu.unienable.logic.TopicManager;
import seedu.unienable.model.enums.ActivityCategory;

class TopicListCommandTest {
    @Test
    public void execute_noFilter_listsEveryCategoryOneLineEach() throws Exception {
        TopicManager topicManager = new TopicManager(new ActivityManager());
        topicManager.add(ActivityCategory.ACADEMIC, "CG3207");
        topicManager.add(ActivityCategory.ACADEMIC, "CS2113");
        topicManager.add(ActivityCategory.CCA, "Computing Club");
        topicManager.add(ActivityCategory.WORK_INTERNSHIP, "Summer Internship");

        CommandResult result = new TopicListCommand(topicManager, null).execute();

        assertEquals("Here are your topics:\n"
                + "ACADEMIC       : CG3207, CS2113\n"
                + "CCA            : Computing Club\n"
                + "WORK_INTERNSHIP: Summer Internship\n"
                + "OTHERS         : No topics", result.getFeedback());
    }

    @Test
    public void execute_categoryFilter_numbersOnlyThatCategorysTopics() throws Exception {
        TopicManager topicManager = new TopicManager(new ActivityManager());
        topicManager.add(ActivityCategory.ACADEMIC, "CG3207");
        topicManager.add(ActivityCategory.ACADEMIC, "CS2113");
        topicManager.add(ActivityCategory.CCA, "Computing Club");

        CommandResult result = new TopicListCommand(topicManager, ActivityCategory.ACADEMIC).execute();

        assertEquals("ACADEMIC topics:\n1. CG3207\n2. CS2113", result.getFeedback());
    }

    @Test
    public void execute_categoryFilterWithNoTopics_showsNoTopicsMessage() {
        TopicManager topicManager = new TopicManager(new ActivityManager());

        CommandResult result = new TopicListCommand(topicManager, ActivityCategory.OTHERS).execute();

        assertEquals("OTHERS topics:\nNo topics.", result.getFeedback());
    }
}
