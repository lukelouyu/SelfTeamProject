package seedu.unienable.model.classes;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import seedu.unienable.model.enums.ActivityCategory;

class TopicTest {
    @Test
    public void constructor_setsCategoryAndName() {
        Topic topic = new Topic(ActivityCategory.ACADEMIC, "CG3207");

        assertEquals(ActivityCategory.ACADEMIC, topic.getCategory());
        assertEquals("CG3207", topic.getName());
    }

    @Test
    public void setName_updatesNameOnly() {
        Topic topic = new Topic(ActivityCategory.ACADEMIC, "CG3207");

        topic.setName("CS3207");

        assertEquals("CS3207", topic.getName());
        assertEquals(ActivityCategory.ACADEMIC, topic.getCategory());
    }

    @Test
    public void toString_showsCategoryAndName() {
        Topic topic = new Topic(ActivityCategory.CCA, "Computing Club");

        assertEquals("CCA / Computing Club", topic.toString());
    }
}
