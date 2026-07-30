package seedu.unienable.logic;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import org.junit.jupiter.api.Test;

import seedu.unienable.exception.DuplicateActivityException;
import seedu.unienable.exception.InvalidIndexException;
import seedu.unienable.model.classes.EnergyRating;
import seedu.unienable.model.classes.FixedActivity;
import seedu.unienable.model.classes.SensoryRating;
import seedu.unienable.model.classes.Topic;
import seedu.unienable.model.enums.ActivityCategory;

class TopicManagerTest {
    private static FixedActivity newActivity(int id, ActivityCategory category, String topic) throws Exception {
        return new FixedActivity(id, "Activity " + id, category, LocalDate.of(2026, 8, 15),
                LocalTime.of(8, 0).plusHours(id), LocalTime.of(9, 0).plusHours(id),
                EnergyRating.of(3), SensoryRating.of(2), topic, null);
    }

    @Test
    public void add_newTopic_becomesListedAndExists() throws Exception {
        TopicManager manager = new TopicManager(new ActivityManager());

        manager.add(ActivityCategory.ACADEMIC, "CG3207");

        assertTrue(manager.exists(ActivityCategory.ACADEMIC, "CG3207"));
        List<Topic> topics = manager.list(ActivityCategory.ACADEMIC);
        assertEquals(1, topics.size());
        assertEquals(ActivityCategory.ACADEMIC, topics.get(0).getCategory());
        assertEquals("CG3207", topics.get(0).getName());
    }

    @Test
    public void exists_isCaseInsensitive() throws Exception {
        TopicManager manager = new TopicManager(new ActivityManager());
        manager.add(ActivityCategory.ACADEMIC, "CG3207");

        assertTrue(manager.exists(ActivityCategory.ACADEMIC, "cg3207"));
    }

    @Test
    public void add_duplicateNameSameCategory_throwsDuplicateActivityException() throws Exception {
        TopicManager manager = new TopicManager(new ActivityManager());
        manager.add(ActivityCategory.ACADEMIC, "CG3207");

        assertThrows(DuplicateActivityException.class, () -> manager.add(ActivityCategory.ACADEMIC, "cg3207"));
    }

    @Test
    public void add_sameNameDifferentCategory_isAllowed() throws Exception {
        TopicManager manager = new TopicManager(new ActivityManager());
        manager.add(ActivityCategory.ACADEMIC, "Project X");

        manager.add(ActivityCategory.CCA, "Project X");

        assertTrue(manager.exists(ActivityCategory.CCA, "Project X"));
    }

    @Test
    public void list_categoryWithNoTopics_isEmpty() {
        TopicManager manager = new TopicManager(new ActivityManager());

        assertTrue(manager.list(ActivityCategory.OTHERS).isEmpty());
    }

    @Test
    public void exists_unknownTopic_returnsFalse() {
        TopicManager manager = new TopicManager(new ActivityManager());

        assertFalse(manager.exists(ActivityCategory.ACADEMIC, "CG3207"));
    }

    @Test
    public void rename_updatesRegistryAndCascadesToMatchingActivities() throws Exception {
        ActivityManager activityManager = new ActivityManager();
        activityManager.add(newActivity(activityManager.getNextId(), ActivityCategory.ACADEMIC, "CG3207"));
        activityManager.add(newActivity(activityManager.getNextId(), ActivityCategory.ACADEMIC, "CG3207"));
        TopicManager topicManager = new TopicManager(activityManager);
        topicManager.add(ActivityCategory.ACADEMIC, "CG3207");

        int updated = topicManager.rename(ActivityCategory.ACADEMIC, "CG3207", "CS3207");

        assertEquals(2, updated);
        assertFalse(topicManager.exists(ActivityCategory.ACADEMIC, "CG3207"));
        assertTrue(topicManager.exists(ActivityCategory.ACADEMIC, "CS3207"));
        assertEquals("CS3207", activityManager.getById(1).getTopic());
        assertEquals("CS3207", activityManager.getById(2).getTopic());
    }

    @Test
    public void rename_doesNotAffectDifferentCategoryWithSameTopicName() throws Exception {
        ActivityManager activityManager = new ActivityManager();
        activityManager.add(newActivity(activityManager.getNextId(), ActivityCategory.CCA, "Project X"));
        TopicManager topicManager = new TopicManager(activityManager);
        topicManager.add(ActivityCategory.ACADEMIC, "Project X");
        topicManager.add(ActivityCategory.CCA, "Project X");

        topicManager.rename(ActivityCategory.ACADEMIC, "Project X", "Project Y");

        assertEquals("Project X", activityManager.getById(1).getTopic());
        assertTrue(topicManager.exists(ActivityCategory.CCA, "Project X"));
    }

    @Test
    public void rename_caseOnlyChange_isAllowed() throws Exception {
        TopicManager manager = new TopicManager(new ActivityManager());
        manager.add(ActivityCategory.ACADEMIC, "cg3207");

        manager.rename(ActivityCategory.ACADEMIC, "cg3207", "CG3207");

        assertEquals("CG3207", manager.list(ActivityCategory.ACADEMIC).get(0).getName());
    }

    @Test
    public void rename_unknownOldName_throwsInvalidIndexException() {
        TopicManager manager = new TopicManager(new ActivityManager());

        assertThrows(InvalidIndexException.class,
                () -> manager.rename(ActivityCategory.ACADEMIC, "CG3207", "CS3207"));
    }

    @Test
    public void rename_toExistingDifferentTopic_throwsDuplicateActivityException() throws Exception {
        TopicManager manager = new TopicManager(new ActivityManager());
        manager.add(ActivityCategory.ACADEMIC, "CG3207");
        manager.add(ActivityCategory.ACADEMIC, "CS2113");

        assertThrows(DuplicateActivityException.class,
                () -> manager.rename(ActivityCategory.ACADEMIC, "CG3207", "CS2113"));
    }

    @Test
    public void delete_unusedTopic_removesFromRegistry() throws Exception {
        TopicManager manager = new TopicManager(new ActivityManager());
        manager.add(ActivityCategory.ACADEMIC, "CG3207");

        manager.delete(ActivityCategory.ACADEMIC, "cg3207");

        assertFalse(manager.exists(ActivityCategory.ACADEMIC, "CG3207"));
    }

    @Test
    public void delete_topicStillInUse_throwsDuplicateActivityExceptionWithCount() throws Exception {
        ActivityManager activityManager = new ActivityManager();
        activityManager.add(newActivity(activityManager.getNextId(), ActivityCategory.ACADEMIC, "CG3207"));
        activityManager.add(newActivity(activityManager.getNextId(), ActivityCategory.ACADEMIC, "CG3207"));
        TopicManager topicManager = new TopicManager(activityManager);
        topicManager.add(ActivityCategory.ACADEMIC, "CG3207");

        DuplicateActivityException exception = assertThrows(DuplicateActivityException.class,
                () -> topicManager.delete(ActivityCategory.ACADEMIC, "CG3207"));
        assertEquals("Topic CG3207 is used by 2 activities.", exception.getMessage());
        assertTrue(topicManager.exists(ActivityCategory.ACADEMIC, "CG3207"));
    }

    @Test
    public void delete_unknownTopic_throwsInvalidIndexException() {
        TopicManager manager = new TopicManager(new ActivityManager());

        assertThrows(InvalidIndexException.class, () -> manager.delete(ActivityCategory.ACADEMIC, "CG3207"));
    }
}
