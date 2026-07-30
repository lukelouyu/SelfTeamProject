package seedu.unienable.command;

import static org.junit.jupiter.api.Assertions.assertEquals;
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

class TopicRenameCommandTest {
    private static FixedActivity newActivity(int id, String topic) throws Exception {
        return new FixedActivity(id, "Activity " + id, ActivityCategory.ACADEMIC, LocalDate.of(2026, 8, 15),
                LocalTime.of(8, 0).plusHours(id), LocalTime.of(9, 0).plusHours(id),
                EnergyRating.of(3), SensoryRating.of(2), topic, null);
    }

    @Test
    public void execute_existingTopic_renamesAndFormatsConfirmationWithUpdatedCount() throws Exception {
        ActivityManager activityManager = new ActivityManager();
        activityManager.add(newActivity(activityManager.getNextId(), "CG3207"));
        activityManager.add(newActivity(activityManager.getNextId(), "CG3207"));
        TopicManager topicManager = new TopicManager(activityManager);
        topicManager.add(ActivityCategory.ACADEMIC, "CG3207");

        CommandResult result = new TopicRenameCommand(topicManager, ActivityCategory.ACADEMIC, "CG3207", "CS3207")
                .execute();

        assertEquals("Topic renamed from CG3207 to CS3207.\nUpdated linked activities: 2", result.getFeedback());
        assertEquals("CS3207", activityManager.getById(1).getTopic());
        assertEquals("CS3207", activityManager.getById(2).getTopic());
    }

    @Test
    public void execute_unknownOldName_throwsInvalidIndexException() {
        TopicManager topicManager = new TopicManager(new ActivityManager());

        assertThrows(InvalidIndexException.class,
                () -> new TopicRenameCommand(topicManager, ActivityCategory.ACADEMIC, "CG3207", "CS3207").execute());
    }

    @Test
    public void execute_newNameAlreadyExists_throwsDuplicateActivityException() throws Exception {
        TopicManager topicManager = new TopicManager(new ActivityManager());
        topicManager.add(ActivityCategory.ACADEMIC, "CG3207");
        topicManager.add(ActivityCategory.ACADEMIC, "CS2113");

        assertThrows(DuplicateActivityException.class,
                () -> new TopicRenameCommand(topicManager, ActivityCategory.ACADEMIC, "CG3207", "CS2113").execute());
    }
}
