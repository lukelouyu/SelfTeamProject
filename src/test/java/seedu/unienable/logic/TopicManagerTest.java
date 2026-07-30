package seedu.unienable.logic;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

import seedu.unienable.exception.DuplicateActivityException;
import seedu.unienable.model.enums.ActivityCategory;

class TopicManagerTest {
    @Test
    public void add_newTopic_becomesListedAndExists() throws Exception {
        TopicManager manager = new TopicManager();

        manager.add(ActivityCategory.ACADEMIC, "CG3207");

        assertTrue(manager.exists(ActivityCategory.ACADEMIC, "CG3207"));
        assertEquals(List.of("CG3207"), manager.list(ActivityCategory.ACADEMIC));
    }

    @Test
    public void exists_isCaseInsensitive() throws Exception {
        TopicManager manager = new TopicManager();
        manager.add(ActivityCategory.ACADEMIC, "CG3207");

        assertTrue(manager.exists(ActivityCategory.ACADEMIC, "cg3207"));
    }

    @Test
    public void add_duplicateNameSameCategory_throwsDuplicateActivityException() throws Exception {
        TopicManager manager = new TopicManager();
        manager.add(ActivityCategory.ACADEMIC, "CG3207");

        assertThrows(DuplicateActivityException.class, () -> manager.add(ActivityCategory.ACADEMIC, "cg3207"));
    }

    @Test
    public void add_sameNameDifferentCategory_isAllowed() throws Exception {
        TopicManager manager = new TopicManager();
        manager.add(ActivityCategory.ACADEMIC, "Project X");

        manager.add(ActivityCategory.CCA, "Project X");

        assertTrue(manager.exists(ActivityCategory.CCA, "Project X"));
    }

    @Test
    public void list_categoryWithNoTopics_isEmpty() {
        TopicManager manager = new TopicManager();

        assertTrue(manager.list(ActivityCategory.OTHERS).isEmpty());
    }

    @Test
    public void exists_unknownTopic_returnsFalse() {
        TopicManager manager = new TopicManager();

        assertFalse(manager.exists(ActivityCategory.ACADEMIC, "CG3207"));
    }
}
