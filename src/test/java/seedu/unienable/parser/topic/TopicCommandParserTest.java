package seedu.unienable.parser.topic;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import seedu.unienable.command.CommandResult;
import seedu.unienable.exception.InvalidActivityException;
import seedu.unienable.exception.MissingInputException;
import seedu.unienable.logic.ActivityManager;
import seedu.unienable.logic.TopicManager;
import seedu.unienable.model.enums.ActivityCategory;

class TopicCommandParserTest {
    private final TopicCommandParser parser = new TopicCommandParser();

    @Test
    public void parseAdd_validFields_createsTopic() throws Exception {
        TopicManager manager = new TopicManager(new ActivityManager());

        CommandResult result = parser.parseAdd(manager, "c/ACADEMIC n/CG3207").execute();

        assertEquals("Topic created:\nCategory: ACADEMIC\nTopic   : CG3207", result.getFeedback());
        assertTrue(manager.exists(ActivityCategory.ACADEMIC, "CG3207"));
    }

    @Test
    public void parseAdd_missingName_throwsMissingInputException() {
        TopicManager manager = new TopicManager(new ActivityManager());

        assertThrows(MissingInputException.class, () -> parser.parseAdd(manager, "c/ACADEMIC"));
    }

    @Test
    public void parseAdd_invalidCategory_throwsInvalidActivityException() {
        TopicManager manager = new TopicManager(new ActivityManager());

        assertThrows(InvalidActivityException.class, () -> parser.parseAdd(manager, "c/BOGUS n/CG3207"));
    }

    @Test
    public void parseList_noFilter_listsEveryCategory() throws Exception {
        TopicManager manager = new TopicManager(new ActivityManager());
        manager.add(ActivityCategory.ACADEMIC, "CG3207");

        CommandResult result = parser.parseList(manager, "").execute();

        assertTrue(result.getFeedback().contains("Here are your topics:"));
    }

    @Test
    public void parseList_categoryFilter_numbersOnlyThatCategory() throws Exception {
        TopicManager manager = new TopicManager(new ActivityManager());
        manager.add(ActivityCategory.ACADEMIC, "CG3207");

        CommandResult result = parser.parseList(manager, "c/ACADEMIC").execute();

        assertEquals("ACADEMIC topics:\n1. CG3207", result.getFeedback());
    }

    @Test
    public void parseList_invalidCategory_throwsInvalidActivityException() {
        TopicManager manager = new TopicManager(new ActivityManager());

        assertThrows(InvalidActivityException.class, () -> parser.parseList(manager, "c/BOGUS"));
    }

    @Test
    public void parseRename_validFields_renamesTopic() throws Exception {
        TopicManager manager = new TopicManager(new ActivityManager());
        manager.add(ActivityCategory.ACADEMIC, "CG3207");

        CommandResult result = parser.parseRename(manager, "c/ACADEMIC old/CG3207 new/CS3207").execute();

        assertEquals("Topic renamed from CG3207 to CS3207.\nUpdated linked activities: 0", result.getFeedback());
        assertTrue(manager.exists(ActivityCategory.ACADEMIC, "CS3207"));
    }

    @Test
    public void parseRename_missingNewName_throwsMissingInputException() {
        TopicManager manager = new TopicManager(new ActivityManager());

        assertThrows(MissingInputException.class, () -> parser.parseRename(manager, "c/ACADEMIC old/CG3207"));
    }

    @Test
    public void parseRename_invalidCategory_throwsInvalidActivityException() {
        TopicManager manager = new TopicManager(new ActivityManager());

        assertThrows(InvalidActivityException.class,
                () -> parser.parseRename(manager, "c/BOGUS old/CG3207 new/CS3207"));
    }

    @Test
    public void parseDelete_validFields_deletesTopic() throws Exception {
        TopicManager manager = new TopicManager(new ActivityManager());
        manager.add(ActivityCategory.ACADEMIC, "CG3207");

        CommandResult result = parser.parseDelete(manager, "c/ACADEMIC n/CG3207").execute();

        assertEquals("Topic CG3207 has been deleted.", result.getFeedback());
        assertFalse(manager.exists(ActivityCategory.ACADEMIC, "CG3207"));
    }

    @Test
    public void parseDelete_missingName_throwsMissingInputException() {
        TopicManager manager = new TopicManager(new ActivityManager());

        assertThrows(MissingInputException.class, () -> parser.parseDelete(manager, "c/ACADEMIC"));
    }

    @Test
    public void parseDelete_invalidCategory_throwsInvalidActivityException() {
        TopicManager manager = new TopicManager(new ActivityManager());

        assertThrows(InvalidActivityException.class, () -> parser.parseDelete(manager, "c/BOGUS n/CG3207"));
    }
}
