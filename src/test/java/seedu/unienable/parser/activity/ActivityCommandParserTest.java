package seedu.unienable.parser.activity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

import org.junit.jupiter.api.Test;

import seedu.unienable.command.activity.crud.AddCommand;
import seedu.unienable.command.CommandResult;
import seedu.unienable.exception.InvalidCommandException;
import seedu.unienable.exception.MissingInputException;
import seedu.unienable.logic.ActivityManager;
import seedu.unienable.logic.TopicManager;
import seedu.unienable.model.classes.EnergyRating;
import seedu.unienable.model.classes.FixedActivity;
import seedu.unienable.model.classes.SensoryRating;
import seedu.unienable.model.enums.ActivityCategory;
import seedu.unienable.model.enums.ActivityOrder;

/**
 * Tests ActivityCommandParser's own inline commands (delete/mark/unmark/view/next/order, each
 * a bare ID or trivial sub-command with no grammar of its own worth a dedicated file) plus a
 * smoke test per delegated command (add/edit/list/find), proving the router wires each one to
 * the right command-specific parser. The full grammar/validation matrix for those delegated
 * commands lives in AddCommandParserTest/EditCommandParserTest/ListCommandParserTest/
 * FindCommandParserTest instead, alongside the class each one actually tests.
 */
class ActivityCommandParserTest {
    private static final LocalDateTime NOW = LocalDateTime.of(2026, 8, 15, 10, 0);
    private static final LocalDateTime TODAY = LocalDate.of(2020, 1, 1).atStartOfDay();

    private final ActivityCommandParser parser = new ActivityCommandParser();

    @Test
    public void parseAdd_delegatesToAddCommandParser_buildsWorkingCommand() throws Exception {
        ActivityManager manager = new ActivityManager();
        TopicManager topicManager = new TopicManager(manager);

        AddCommand command = parser.parseAdd(manager, topicManager, TODAY,
                "n/CG3207 lecture c/ACADEMIC date/2026-08-15 type/FIXED from/09:00 to/11:00 energy/4 sensory/3");
        command.execute();

        assertEquals(1, manager.size());
    }

    @Test
    public void parseEdit_delegatesToEditCommandParser_buildsWorkingCommand() throws Exception {
        ActivityManager manager = new ActivityManager();
        TopicManager topicManager = new TopicManager(manager);
        manager.add(new FixedActivity(manager.getNextId(), "Lecture", ActivityCategory.ACADEMIC,
                LocalDate.of(2026, 8, 15), LocalTime.of(9, 0), LocalTime.of(11, 0),
                EnergyRating.of(4), SensoryRating.of(3), null, null));

        parser.parseEdit(manager, topicManager, TODAY, "1 n/Updated lecture").execute();

        assertEquals("Updated lecture", manager.getById(1).getDescription());
    }

    @Test
    public void parseList_delegatesToListCommandParser_returnsMatchingActivities() throws Exception {
        ActivityManager manager = new ActivityManager();
        manager.add(new FixedActivity(manager.getNextId(), "Lecture", ActivityCategory.ACADEMIC,
                LocalDate.of(2026, 8, 15), LocalTime.of(9, 0), LocalTime.of(11, 0),
                EnergyRating.of(4), SensoryRating.of(3), null, null));

        CommandResult result = parser.parseList(manager, NOW, "").execute();

        assertTrue(result.getFeedback().contains("Lecture"));
    }

    @Test
    public void parseFind_delegatesToFindCommandParser_returnsMatchingActivities() throws Exception {
        ActivityManager manager = new ActivityManager();
        manager.add(new FixedActivity(manager.getNextId(), "Lecture", ActivityCategory.ACADEMIC,
                LocalDate.of(2026, 8, 15), LocalTime.of(9, 0), LocalTime.of(11, 0),
                EnergyRating.of(4), SensoryRating.of(3), null, null));

        CommandResult result = parser.parseFind(manager, NOW, "k/Lecture").execute();

        assertTrue(result.getFeedback().contains("Lecture"));
    }

    @Test
    public void parseDelete_validId_returnsWorkingDeleteCommand() throws Exception {
        ActivityManager manager = new ActivityManager();
        TopicManager topicManager = new TopicManager(manager);
        manager.add(new FixedActivity(manager.getNextId(), "Briefing",
                ActivityCategory.WORK_INTERNSHIP, LocalDate.of(2026, 8, 16), LocalTime.of(10, 0),
                LocalTime.of(11, 0), EnergyRating.of(3), SensoryRating.of(2), null, null));

        parser.parseDelete(manager, "1").execute();

        assertEquals(0, manager.size());
    }

    @Test
    public void parseDelete_missingId_throwsMissingInputException() {
        ActivityManager manager = new ActivityManager();
        TopicManager topicManager = new TopicManager(manager);

        assertThrows(MissingInputException.class, () -> parser.parseDelete(manager, "  "));
    }

    @Test
    public void parseDelete_nonNumericId_throwsInvalidCommandException() {
        ActivityManager manager = new ActivityManager();
        TopicManager topicManager = new TopicManager(manager);

        assertThrows(InvalidCommandException.class, () -> parser.parseDelete(manager, "abc"));
    }

    @Test
    public void parseMark_validId_returnsWorkingMarkCommand() throws Exception {
        ActivityManager manager = new ActivityManager();
        TopicManager topicManager = new TopicManager(manager);
        manager.add(new FixedActivity(manager.getNextId(), "Briefing", ActivityCategory.WORK_INTERNSHIP,
                LocalDate.of(2026, 8, 16), LocalTime.of(10, 0), LocalTime.of(11, 0),
                EnergyRating.of(3), SensoryRating.of(2), null, null));

        parser.parseMark(manager, "1").execute();

        assertEquals(true, manager.getById(1).isComplete());
    }

    @Test
    public void parseMark_nonNumericId_throwsInvalidCommandException() {
        ActivityManager manager = new ActivityManager();
        TopicManager topicManager = new TopicManager(manager);

        assertThrows(InvalidCommandException.class, () -> parser.parseMark(manager, "abc"));
    }

    @Test
    public void parseUnmark_validId_returnsWorkingUnmarkCommand() throws Exception {
        ActivityManager manager = new ActivityManager();
        TopicManager topicManager = new TopicManager(manager);
        manager.add(new FixedActivity(manager.getNextId(), "Briefing", ActivityCategory.WORK_INTERNSHIP,
                LocalDate.of(2026, 8, 16), LocalTime.of(10, 0), LocalTime.of(11, 0),
                EnergyRating.of(3), SensoryRating.of(2), null, null));
        manager.mark(1);

        parser.parseUnmark(manager, "1").execute();

        assertEquals(false, manager.getById(1).isComplete());
    }

    @Test
    public void parseUnmark_missingId_throwsMissingInputException() {
        ActivityManager manager = new ActivityManager();
        TopicManager topicManager = new TopicManager(manager);

        assertThrows(MissingInputException.class, () -> parser.parseUnmark(manager, ""));
    }

    @Test
    public void parseView_validId_returnsWorkingViewCommand() throws Exception {
        ActivityManager manager = new ActivityManager();
        TopicManager topicManager = new TopicManager(manager);
        manager.add(new FixedActivity(manager.getNextId(), "Briefing", ActivityCategory.WORK_INTERNSHIP,
                LocalDate.of(2026, 8, 16), LocalTime.of(10, 0), LocalTime.of(11, 0),
                EnergyRating.of(3), SensoryRating.of(2), null, null));

        CommandResult result = parser.parseView(manager, "1").execute();

        assertTrue(result.getFeedback().contains("Activity [1]"));
    }

    @Test
    public void parseView_missingId_throwsMissingInputException() {
        ActivityManager manager = new ActivityManager();
        TopicManager topicManager = new TopicManager(manager);

        assertThrows(MissingInputException.class, () -> parser.parseView(manager, ""));
    }

    @Test
    public void parseNext_buildsWorkingNextCommand() throws Exception {
        ActivityManager manager = new ActivityManager();
        TopicManager topicManager = new TopicManager(manager);
        manager.add(new FixedActivity(manager.getNextId(), "CG3207 lecture", ActivityCategory.ACADEMIC,
                LocalDate.of(2026, 8, 15), LocalTime.of(9, 0), LocalTime.of(11, 0),
                EnergyRating.of(4), SensoryRating.of(3), null, null));
        LocalDateTime now = LocalDateTime.of(2026, 8, 15, 10, 0);

        CommandResult result = parser.parseNext(manager, now, "").execute();

        assertTrue(result.getFeedback().contains("CG3207 lecture"));
    }

    @Test
    public void parseNext_trailingArguments_throwsInvalidCommandException() {
        // Regression test: "next" is documented as taking no arguments, but any trailing text was
        // previously silently ignored rather than rejected.
        ActivityManager manager = new ActivityManager();

        assertThrows(InvalidCommandException.class,
                () -> parser.parseNext(manager, LocalDateTime.of(2026, 8, 15, 10, 0), "extra-argument"));
    }

    @Test
    public void parseOrder_viewWithTrailingArguments_throwsInvalidCommandException() {
        ActivityManager manager = new ActivityManager();

        assertThrows(InvalidCommandException.class, () -> parser.parseOrder(manager, "view extra"));
    }

    @Test
    public void parseOrder_view_returnsWorkingOrderViewCommand() throws Exception {
        ActivityManager manager = new ActivityManager();
        TopicManager topicManager = new TopicManager(manager);

        CommandResult result = parser.parseOrder(manager, "view").execute();

        assertTrue(result.getFeedback().contains("Saved default activity order:"));
    }

    @Test
    public void parseOrder_setInput_updatesManagerDefaultOrder() throws Exception {
        ActivityManager manager = new ActivityManager();
        TopicManager topicManager = new TopicManager(manager);

        parser.parseOrder(manager, "set input").execute();

        assertEquals(ActivityOrder.INPUT, manager.getDefaultOrder());
    }

    @Test
    public void parseOrder_missingSubCommand_throwsMissingInputException() {
        ActivityManager manager = new ActivityManager();
        TopicManager topicManager = new TopicManager(manager);

        assertThrows(MissingInputException.class, () -> parser.parseOrder(manager, ""));
    }

    @Test
    public void parseOrder_setMissingOrderValue_throwsMissingInputException() {
        ActivityManager manager = new ActivityManager();
        TopicManager topicManager = new TopicManager(manager);

        assertThrows(MissingInputException.class, () -> parser.parseOrder(manager, "set"));
    }

    @Test
    public void parseOrder_unknownSubCommand_throwsInvalidCommandException() {
        ActivityManager manager = new ActivityManager();
        TopicManager topicManager = new TopicManager(manager);

        assertThrows(InvalidCommandException.class, () -> parser.parseOrder(manager, "bogus"));
    }

    @Test
    public void parseOrder_setInvalidOrderValue_throwsInvalidCommandException() {
        ActivityManager manager = new ActivityManager();
        TopicManager topicManager = new TopicManager(manager);

        assertThrows(InvalidCommandException.class, () -> parser.parseOrder(manager, "set bogus"));
    }

}
