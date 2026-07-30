package seedu.unienable.parser;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Map;

import org.junit.jupiter.api.Test;

import seedu.unienable.command.activity.AddCommand;
import seedu.unienable.command.CommandResult;
import seedu.unienable.exception.InvalidActivityException;
import seedu.unienable.exception.InvalidCommandException;
import seedu.unienable.exception.InvalidDateTimeException;
import seedu.unienable.exception.InvalidIndexException;
import seedu.unienable.exception.MissingInputException;
import seedu.unienable.logic.ActivityManager;
import seedu.unienable.model.classes.Activity;
import seedu.unienable.model.classes.EnergyRating;
import seedu.unienable.model.classes.FixedActivity;
import seedu.unienable.model.classes.FlexibleActivity;
import seedu.unienable.model.classes.SensoryRating;
import seedu.unienable.model.enums.ActivityCategory;
import seedu.unienable.model.enums.ActivityOrder;
import seedu.unienable.model.enums.ScheduleType;

class ActivityCommandParserTest {
    private final ActivityCommandParser parser = new ActivityCommandParser();

    @Test
    public void parseAdd_fixedActivity_buildsMatchingActivity() throws Exception {
        ActivityManager manager = new ActivityManager();
        AddCommand command = parser.parseAdd(manager,
                "n/CG3207 lecture c/ACADEMIC date/2026-08-15 type/FIXED from/09:00 to/11:00 "
                        + "energy/4 sensory/3 topic/CG3207 note/Bring laptop");

        command.execute();

        Activity activity = manager.getById(1);
        assertEquals(ScheduleType.FIXED, activity.getScheduleType());
        assertEquals("CG3207 lecture", activity.getDescription());
        assertEquals(ActivityCategory.ACADEMIC, activity.getCategory());
        assertEquals(LocalDate.of(2026, 8, 15), activity.getDate());
        assertEquals(LocalTime.of(9, 0), ((FixedActivity) activity).getStartTime());
        assertEquals(LocalTime.of(11, 0), ((FixedActivity) activity).getEndTime());
        assertEquals(4, activity.getEnergyRating().getValue());
        assertEquals(3, activity.getSensoryRating().getValue());
        assertEquals("CG3207", activity.getTopic());
        assertEquals("Bring laptop", activity.getNote());
    }

    @Test
    public void parseAdd_flexibleActivity_buildsMatchingActivity() throws Exception {
        ActivityManager manager = new ActivityManager();
        AddCommand command = parser.parseAdd(manager,
                "n/Finish assignment 1 c/ACADEMIC date/2026-08-15 type/FLEXIBLE earliest/10:00 latest/18:00 "
                        + "dur/90 energy/5 sensory/2 topic/CG3207");

        command.execute();

        Activity activity = manager.getById(1);
        assertEquals(ScheduleType.FLEXIBLE, activity.getScheduleType());
        assertEquals(LocalTime.of(10, 0), ((FlexibleActivity) activity).getEarliestStart());
        assertEquals(LocalTime.of(18, 0), ((FlexibleActivity) activity).getLatestEnd());
        assertEquals(90, ((FlexibleActivity) activity).getDurationMinutes());
        assertEquals("CG3207", activity.getTopic());
        assertNull(activity.getNote());
    }

    @Test
    public void parseAdd_noteWithoutTopic_parsesNoteCorrectly() throws Exception {
        ActivityManager manager = new ActivityManager();
        AddCommand command = parser.parseAdd(manager,
                "n/Consultation c/OTHERS date/2026-08-15 type/FIXED from/09:00 to/10:00 "
                        + "energy/2 sensory/2 note/Bring headphones");

        command.execute();

        Activity activity = manager.getById(1);
        assertNull(activity.getTopic());
        assertEquals("Bring headphones", activity.getNote());
    }

    @Test
    public void parseAdd_topicWithoutNote_parsesTopicCorrectly() throws Exception {
        ActivityManager manager = new ActivityManager();
        AddCommand command = parser.parseAdd(manager,
                "n/Consultation c/OTHERS date/2026-08-15 type/FIXED from/09:00 to/10:00 "
                        + "energy/2 sensory/2 topic/Misc");

        command.execute();

        Activity activity = manager.getById(1);
        assertEquals("Misc", activity.getTopic());
        assertNull(activity.getNote());
    }

    @Test
    public void parseAdd_missingDescription_throwsMissingInputException() {
        ActivityManager manager = new ActivityManager();

        assertThrows(MissingInputException.class, () -> parser.parseAdd(manager,
                "c/ACADEMIC date/2026-08-15 type/FIXED from/09:00 to/11:00 energy/4 sensory/3"));
    }

    @Test
    public void parseAdd_missingType_throwsMissingInputException() {
        ActivityManager manager = new ActivityManager();

        assertThrows(MissingInputException.class, () -> parser.parseAdd(manager,
                "n/Lecture c/ACADEMIC date/2026-08-15"));
    }

    @Test
    public void parseAdd_invalidType_throwsInvalidCommandException() {
        ActivityManager manager = new ActivityManager();

        assertThrows(InvalidCommandException.class, () -> parser.parseAdd(manager,
                "n/Lecture c/ACADEMIC date/2026-08-15 type/BOGUS from/09:00 to/11:00 energy/4 sensory/3"));
    }

    @Test
    public void parseAdd_endNotAfterStart_throwsInvalidActivityException() {
        ActivityManager manager = new ActivityManager();

        assertThrows(InvalidActivityException.class, () -> parser.parseAdd(manager,
                "n/Lecture c/ACADEMIC date/2026-08-15 type/FIXED from/11:00 to/09:00 energy/4 sensory/3"));
    }

    @Test
    public void parseAdd_invalidCategory_throwsInvalidActivityException() {
        ActivityManager manager = new ActivityManager();

        assertThrows(InvalidActivityException.class, () -> parser.parseAdd(manager,
                "n/Lecture c/BOGUS date/2026-08-15 type/FIXED from/09:00 to/11:00 energy/4 sensory/3"));
    }

    @Test
    public void parseAdd_invalidDate_throwsInvalidDateTimeException() {
        ActivityManager manager = new ActivityManager();

        assertThrows(InvalidDateTimeException.class, () -> parser.parseAdd(manager,
                "n/Lecture c/ACADEMIC date/15-08-2026 type/FIXED from/09:00 to/11:00 energy/4 sensory/3"));
    }

    @Test
    public void parseAdd_invalidEnergyRating_throwsInvalidActivityException() {
        ActivityManager manager = new ActivityManager();

        assertThrows(InvalidActivityException.class, () -> parser.parseAdd(manager,
                "n/Lecture c/ACADEMIC date/2026-08-15 type/FIXED from/09:00 to/11:00 energy/7 sensory/3"));
    }

    @Test
    public void parseAdd_flexibleInvalidDuration_throwsInvalidActivityException() {
        ActivityManager manager = new ActivityManager();

        assertThrows(InvalidActivityException.class, () -> parser.parseAdd(manager,
                "n/Task c/ACADEMIC date/2026-08-15 type/FLEXIBLE earliest/10:00 latest/18:00 "
                        + "dur/0 energy/5 sensory/2"));
    }

    @Test
    public void parseAdd_flexibleMissingDurationEntirely_throwsInvalidDateTimeException() {
        // dur/ is dropped entirely, so latest/'s end marker ("dur/") is never found and its
        // extraction greedily captures the trailing "energy/5 sensory/2" text, which then fails
        // time parsing before a dedicated "dur is required" check is ever reached.
        ActivityManager manager = new ActivityManager();

        assertThrows(InvalidDateTimeException.class, () -> parser.parseAdd(manager,
                "n/Task c/ACADEMIC date/2026-08-15 type/FLEXIBLE earliest/10:00 latest/18:00 "
                        + "energy/5 sensory/2"));
    }

    @Test
    public void parseDelete_validId_returnsWorkingDeleteCommand() throws Exception {
        ActivityManager manager = new ActivityManager();
        manager.add(new FixedActivity(manager.getNextId(), "Briefing",
                ActivityCategory.WORK_INTERNSHIP, LocalDate.of(2026, 8, 16), LocalTime.of(10, 0),
                LocalTime.of(11, 0), EnergyRating.of(3), SensoryRating.of(2), null, null));

        parser.parseDelete(manager, "1").execute();

        assertEquals(0, manager.size());
    }

    @Test
    public void parseDelete_missingId_throwsMissingInputException() {
        ActivityManager manager = new ActivityManager();

        assertThrows(MissingInputException.class, () -> parser.parseDelete(manager, "  "));
    }

    @Test
    public void parseDelete_nonNumericId_throwsInvalidCommandException() {
        ActivityManager manager = new ActivityManager();

        assertThrows(InvalidCommandException.class, () -> parser.parseDelete(manager, "abc"));
    }

    @Test
    public void parseMark_validId_returnsWorkingMarkCommand() throws Exception {
        ActivityManager manager = new ActivityManager();
        manager.add(new FixedActivity(manager.getNextId(), "Briefing", ActivityCategory.WORK_INTERNSHIP,
                LocalDate.of(2026, 8, 16), LocalTime.of(10, 0), LocalTime.of(11, 0),
                EnergyRating.of(3), SensoryRating.of(2), null, null));

        parser.parseMark(manager, "1").execute();

        assertEquals(true, manager.getById(1).isComplete());
    }

    @Test
    public void parseMark_nonNumericId_throwsInvalidCommandException() {
        ActivityManager manager = new ActivityManager();

        assertThrows(InvalidCommandException.class, () -> parser.parseMark(manager, "abc"));
    }

    @Test
    public void parseUnmark_validId_returnsWorkingUnmarkCommand() throws Exception {
        ActivityManager manager = new ActivityManager();
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

        assertThrows(MissingInputException.class, () -> parser.parseUnmark(manager, ""));
    }

    @Test
    public void parseView_validId_returnsWorkingViewCommand() throws Exception {
        ActivityManager manager = new ActivityManager();
        manager.add(new FixedActivity(manager.getNextId(), "Briefing", ActivityCategory.WORK_INTERNSHIP,
                LocalDate.of(2026, 8, 16), LocalTime.of(10, 0), LocalTime.of(11, 0),
                EnergyRating.of(3), SensoryRating.of(2), null, null));

        CommandResult result = parser.parseView(manager, "1").execute();

        assertTrue(result.getFeedback().contains("Activity [1]"));
    }

    @Test
    public void parseView_missingId_throwsMissingInputException() {
        ActivityManager manager = new ActivityManager();

        assertThrows(MissingInputException.class, () -> parser.parseView(manager, ""));
    }

    @Test
    public void parseList_noFields_listsEverythingInDefaultOrder() throws Exception {
        ActivityManager manager = new ActivityManager();
        manager.add(new FixedActivity(manager.getNextId(), "Lecture", ActivityCategory.ACADEMIC,
                LocalDate.of(2026, 8, 15), LocalTime.of(9, 0), LocalTime.of(11, 0),
                EnergyRating.of(4), SensoryRating.of(3), null, null));

        CommandResult result = parser.parseList(manager, "").execute();

        assertTrue(result.getFeedback().contains("Lecture"));
    }

    @Test
    public void parseList_statusIncompleteFilter_excludesCompletedActivities() throws Exception {
        ActivityManager manager = new ActivityManager();
        manager.add(new FixedActivity(manager.getNextId(), "Done task", ActivityCategory.ACADEMIC,
                LocalDate.of(2026, 8, 15), LocalTime.of(9, 0), LocalTime.of(10, 0),
                EnergyRating.of(4), SensoryRating.of(3), null, null));
        manager.mark(1);
        manager.add(new FixedActivity(manager.getNextId(), "Pending task", ActivityCategory.ACADEMIC,
                LocalDate.of(2026, 8, 15), LocalTime.of(11, 0), LocalTime.of(12, 0),
                EnergyRating.of(4), SensoryRating.of(3), null, null));

        CommandResult result = parser.parseList(manager, "status/incomplete").execute();

        String feedback = result.getFeedback();
        assertTrue(feedback.contains("Pending task"));
        assertTrue(!feedback.contains("Done task"));
    }

    @Test
    public void parseList_viewDetail_usesDetailFormat() throws Exception {
        ActivityManager manager = new ActivityManager();
        manager.add(new FixedActivity(manager.getNextId(), "Lecture", ActivityCategory.ACADEMIC,
                LocalDate.of(2026, 8, 15), LocalTime.of(9, 0), LocalTime.of(11, 0),
                EnergyRating.of(4), SensoryRating.of(3), null, null));

        CommandResult result = parser.parseList(manager, "view/detail").execute();

        assertTrue(result.getFeedback().contains("Status: Incomplete | Type: FIXED"));
    }

    @Test
    public void parseList_categoryAndTopicFilter_combineWithAnd() throws Exception {
        ActivityManager manager = new ActivityManager();
        manager.add(new FixedActivity(manager.getNextId(), "CG3207 lecture", ActivityCategory.ACADEMIC,
                LocalDate.of(2026, 8, 15), LocalTime.of(9, 0), LocalTime.of(10, 0),
                EnergyRating.of(4), SensoryRating.of(3), "CG3207", null));
        manager.add(new FixedActivity(manager.getNextId(), "CS2113 lecture", ActivityCategory.ACADEMIC,
                LocalDate.of(2026, 8, 15), LocalTime.of(11, 0), LocalTime.of(12, 0),
                EnergyRating.of(4), SensoryRating.of(3), "CS2113", null));

        CommandResult result = parser.parseList(manager, "c/ACADEMIC topic/CG3207").execute();

        String feedback = result.getFeedback();
        assertTrue(feedback.contains("CG3207 lecture"));
        assertTrue(!feedback.contains("CS2113 lecture"));
    }

    @Test
    public void parseList_topicFilterAloneWithNoExplicitCategory_doesNotThrow() throws Exception {
        // Regression test: same root cause as parseEdit's - "topic/" alone (no c/) previously
        // triggered a false "c/" match embedded inside the "topic/" marker text.
        ActivityManager manager = new ActivityManager();
        manager.add(new FixedActivity(manager.getNextId(), "CG3207 lecture", ActivityCategory.ACADEMIC,
                LocalDate.of(2026, 8, 15), LocalTime.of(9, 0), LocalTime.of(10, 0),
                EnergyRating.of(4), SensoryRating.of(3), "CG3207", null));

        CommandResult result = parser.parseList(manager, "topic/CG3207").execute();

        assertTrue(result.getFeedback().contains("CG3207 lecture"));
    }

    @Test
    public void parseList_invalidStatus_throwsInvalidCommandException() {
        ActivityManager manager = new ActivityManager();

        assertThrows(InvalidCommandException.class, () -> parser.parseList(manager, "status/bogus"));
    }

    @Test
    public void parseList_invalidOrder_throwsInvalidCommandException() {
        ActivityManager manager = new ActivityManager();

        assertThrows(InvalidCommandException.class, () -> parser.parseList(manager, "order/bogus"));
    }

    @Test
    public void parseFind_singleKeyword_findsMatchingActivity() throws Exception {
        ActivityManager manager = new ActivityManager();
        manager.add(new FixedActivity(manager.getNextId(), "Finish assignment 1", ActivityCategory.ACADEMIC,
                LocalDate.of(2026, 8, 15), LocalTime.of(9, 0), LocalTime.of(10, 0),
                EnergyRating.of(4), SensoryRating.of(3), null, null));

        CommandResult result = parser.parseFind(manager, "k/assignment").execute();

        assertTrue(result.getFeedback().contains("Finish assignment 1"));
    }

    @Test
    public void parseFind_multiWordKeyword_splitsIntoAndedKeywords() throws Exception {
        ActivityManager manager = new ActivityManager();
        manager.add(new FixedActivity(manager.getNextId(), "Finish assignment 1", ActivityCategory.ACADEMIC,
                LocalDate.of(2026, 8, 15), LocalTime.of(9, 0), LocalTime.of(10, 0),
                EnergyRating.of(4), SensoryRating.of(3), null, null));
        manager.add(new FixedActivity(manager.getNextId(), "Finish reading", ActivityCategory.ACADEMIC,
                LocalDate.of(2026, 8, 15), LocalTime.of(11, 0), LocalTime.of(12, 0),
                EnergyRating.of(4), SensoryRating.of(3), null, null));

        CommandResult result = parser.parseFind(manager, "k/finish assignment").execute();

        String feedback = result.getFeedback();
        assertTrue(feedback.contains("Finish assignment 1"));
        assertTrue(!feedback.contains("Finish reading"));
    }

    @Test
    public void parseFind_filterOnlyNoKeyword_isAllowed() throws Exception {
        ActivityManager manager = new ActivityManager();
        manager.add(new FixedActivity(manager.getNextId(), "CG3207 lecture", ActivityCategory.ACADEMIC,
                LocalDate.of(2026, 8, 15), LocalTime.of(9, 0), LocalTime.of(10, 0),
                EnergyRating.of(4), SensoryRating.of(3), "CG3207", null));

        CommandResult result = parser.parseFind(manager, "c/ACADEMIC topic/CG3207").execute();

        assertTrue(result.getFeedback().contains("CG3207 lecture"));
    }

    @Test
    public void parseFind_topicFilterAloneWithNoExplicitCategory_doesNotThrow() throws Exception {
        ActivityManager manager = new ActivityManager();
        manager.add(new FixedActivity(manager.getNextId(), "CG3207 lecture", ActivityCategory.ACADEMIC,
                LocalDate.of(2026, 8, 15), LocalTime.of(9, 0), LocalTime.of(10, 0),
                EnergyRating.of(4), SensoryRating.of(3), "CG3207", null));

        CommandResult result = parser.parseFind(manager, "topic/CG3207").execute();

        assertTrue(result.getFeedback().contains("CG3207 lecture"));
    }

    @Test
    public void parseFind_neitherKeywordNorFilter_throwsMissingInputException() {
        ActivityManager manager = new ActivityManager();

        assertThrows(MissingInputException.class, () -> parser.parseFind(manager, ""));
    }

    @Test
    public void parseNext_buildsWorkingNextCommand() throws Exception {
        ActivityManager manager = new ActivityManager();
        manager.add(new FixedActivity(manager.getNextId(), "CG3207 lecture", ActivityCategory.ACADEMIC,
                LocalDate.of(2026, 8, 15), LocalTime.of(9, 0), LocalTime.of(11, 0),
                EnergyRating.of(4), SensoryRating.of(3), null, null));
        LocalDateTime now = LocalDateTime.of(2026, 8, 15, 10, 0);

        CommandResult result = parser.parseNext(manager, now).execute();

        assertTrue(result.getFeedback().contains("CG3207 lecture"));
    }

    @Test
    public void parseOrder_view_returnsWorkingOrderViewCommand() throws Exception {
        ActivityManager manager = new ActivityManager();

        CommandResult result = parser.parseOrder(manager, "view").execute();

        assertTrue(result.getFeedback().contains("Saved default activity order:"));
    }

    @Test
    public void parseOrder_setInput_updatesManagerDefaultOrder() throws Exception {
        ActivityManager manager = new ActivityManager();

        parser.parseOrder(manager, "set input").execute();

        assertEquals(ActivityOrder.INPUT, manager.getDefaultOrder());
    }

    @Test
    public void parseOrder_missingSubCommand_throwsMissingInputException() {
        ActivityManager manager = new ActivityManager();

        assertThrows(MissingInputException.class, () -> parser.parseOrder(manager, ""));
    }

    @Test
    public void parseOrder_setMissingOrderValue_throwsMissingInputException() {
        ActivityManager manager = new ActivityManager();

        assertThrows(MissingInputException.class, () -> parser.parseOrder(manager, "set"));
    }

    @Test
    public void parseOrder_unknownSubCommand_throwsInvalidCommandException() {
        ActivityManager manager = new ActivityManager();

        assertThrows(InvalidCommandException.class, () -> parser.parseOrder(manager, "bogus"));
    }

    @Test
    public void parseOrder_setInvalidOrderValue_throwsInvalidCommandException() {
        ActivityManager manager = new ActivityManager();

        assertThrows(InvalidCommandException.class, () -> parser.parseOrder(manager, "set bogus"));
    }

    @Test
    public void extractPresentFields_singleField_returnsItsValue() {
        Map<String, String> fields = parser.extractPresentFields("dur/60", "n/", "dur/", "energy/");

        assertEquals(Map.of("dur/", "60"), fields);
    }

    @Test
    public void extractPresentFields_multipleFieldsAnyOrder_boundsEachByNextPresentMarker() {
        Map<String, String> fields = parser.extractPresentFields(
                "energy/4 n/New activity name sensory/2", "n/", "energy/", "sensory/");

        assertEquals("4", fields.get("energy/"));
        assertEquals("New activity name", fields.get("n/"));
        assertEquals("2", fields.get("sensory/"));
    }

    @Test
    public void extractPresentFields_absentMarkers_areOmittedFromResult() {
        Map<String, String> fields = parser.extractPresentFields("note/Bring headphones", "n/", "note/", "topic/");

        assertEquals(1, fields.size());
        assertEquals("Bring headphones", fields.get("note/"));
    }

    @Test
    public void extractPresentFields_noMarkersPresent_returnsEmptyMap() {
        assertTrue(parser.extractPresentFields("nothing relevant here", "n/", "c/").isEmpty());
    }

    @Test
    public void parseEdit_singleField_updatesOnlyThatField() throws Exception {
        ActivityManager manager = new ActivityManager();
        manager.add(new FlexibleActivity(manager.getNextId(), "Finish assignment 1", ActivityCategory.ACADEMIC,
                LocalDate.of(2026, 8, 15), LocalTime.of(10, 0), LocalTime.of(18, 0), 90,
                EnergyRating.of(5), SensoryRating.of(2), "CG3207", null));

        parser.parseEdit(manager, "1 dur/60").execute();

        Activity updated = manager.getById(1);
        assertEquals(60, ((FlexibleActivity) updated).getDurationMinutes());
        assertEquals("Finish assignment 1", updated.getDescription());
        assertEquals("CG3207", updated.getTopic());
    }

    @Test
    public void parseEdit_topicOnlyWithNoExplicitCategory_doesNotMistakeTopicForCategory() throws Exception {
        // Regression test: "topic/" ends in the substring "c/", so editing topic/ alone (with no
        // c/ field at all) previously caused extractPresentFields to falsely detect a "c/" field
        // embedded inside "topic/"'s own marker text, and reject the edit with "category must be
        // one of ACADEMIC, CCA, WORK_INTERNSHIP, OTHERS."
        ActivityManager manager = new ActivityManager();
        manager.add(new FixedActivity(manager.getNextId(), "Lecture", ActivityCategory.ACADEMIC,
                LocalDate.of(2026, 8, 15), LocalTime.of(9, 0), LocalTime.of(11, 0),
                EnergyRating.of(4), SensoryRating.of(3), null, null));

        parser.parseEdit(manager, "1 topic/CS2113").execute();

        Activity updated = manager.getById(1);
        assertEquals("CS2113", updated.getTopic());
        assertEquals(ActivityCategory.ACADEMIC, updated.getCategory());
    }

    @Test
    public void parseEdit_multipleFields_updatesAllGivenFields() throws Exception {
        ActivityManager manager = new ActivityManager();
        manager.add(new FlexibleActivity(manager.getNextId(), "Prepare slides", ActivityCategory.ACADEMIC,
                LocalDate.of(2026, 8, 15), LocalTime.of(10, 0), LocalTime.of(18, 0), 90,
                EnergyRating.of(3), SensoryRating.of(3), null, null));

        parser.parseEdit(manager, "1 n/New activity name energy/4 sensory/2").execute();

        Activity updated = manager.getById(1);
        assertEquals("New activity name", updated.getDescription());
        assertEquals(4, updated.getEnergyRating().getValue());
        assertEquals(2, updated.getSensoryRating().getValue());
    }

    @Test
    public void parseEdit_noteOnly_leavesOtherFieldsUnchanged() throws Exception {
        ActivityManager manager = new ActivityManager();
        manager.add(new FixedActivity(manager.getNextId(), "Consultation", ActivityCategory.OTHERS,
                LocalDate.of(2026, 8, 15), LocalTime.of(9, 0), LocalTime.of(10, 0),
                EnergyRating.of(2), SensoryRating.of(2), "Misc", null));

        parser.parseEdit(manager, "1 note/Bring headphones").execute();

        Activity updated = manager.getById(1);
        assertEquals("Bring headphones", updated.getNote());
        assertEquals("Misc", updated.getTopic());
        assertEquals(LocalTime.of(9, 0), ((FixedActivity) updated).getStartTime());
    }

    @Test
    public void parseEdit_preservesCompletionStatus() throws Exception {
        ActivityManager manager = new ActivityManager();
        manager.add(new FlexibleActivity(manager.getNextId(), "Finish assignment 1", ActivityCategory.ACADEMIC,
                LocalDate.of(2026, 8, 15), LocalTime.of(10, 0), LocalTime.of(18, 0), 90,
                EnergyRating.of(5), SensoryRating.of(2), null, null));
        manager.mark(1);

        parser.parseEdit(manager, "1 dur/60").execute();

        assertTrue(manager.getById(1).isComplete());
    }

    @Test
    public void parseEdit_changingTypeWithAllNewTimingFields_succeeds() throws Exception {
        ActivityManager manager = new ActivityManager();
        manager.add(new FixedActivity(manager.getNextId(), "Lecture", ActivityCategory.ACADEMIC,
                LocalDate.of(2026, 8, 15), LocalTime.of(9, 0), LocalTime.of(11, 0),
                EnergyRating.of(4), SensoryRating.of(3), null, null));

        parser.parseEdit(manager, "1 type/FLEXIBLE earliest/10:00 latest/18:00 dur/90").execute();

        Activity updated = manager.getById(1);
        assertEquals(ScheduleType.FLEXIBLE, updated.getScheduleType());
        assertEquals(90, ((FlexibleActivity) updated).getDurationMinutes());
    }

    @Test
    public void parseEdit_changingTypeWithoutNewTimingFields_throwsMissingInputException() throws Exception {
        ActivityManager manager = new ActivityManager();
        manager.add(new FixedActivity(manager.getNextId(), "Lecture", ActivityCategory.ACADEMIC,
                LocalDate.of(2026, 8, 15), LocalTime.of(9, 0), LocalTime.of(11, 0),
                EnergyRating.of(4), SensoryRating.of(3), null, null));

        assertThrows(MissingInputException.class, () -> parser.parseEdit(manager, "1 type/FLEXIBLE"));
    }

    @Test
    public void parseEdit_noFieldsSupplied_throwsMissingInputException() throws Exception {
        ActivityManager manager = new ActivityManager();
        manager.add(new FixedActivity(manager.getNextId(), "Lecture", ActivityCategory.ACADEMIC,
                LocalDate.of(2026, 8, 15), LocalTime.of(9, 0), LocalTime.of(11, 0),
                EnergyRating.of(4), SensoryRating.of(3), null, null));

        assertThrows(MissingInputException.class, () -> parser.parseEdit(manager, "1"));
    }

    @Test
    public void parseEdit_unknownId_throwsInvalidIndexException() {
        ActivityManager manager = new ActivityManager();

        assertThrows(InvalidIndexException.class, () -> parser.parseEdit(manager, "999 dur/60"));
    }

    @Test
    public void parseEdit_nonNumericId_throwsInvalidCommandException() {
        ActivityManager manager = new ActivityManager();

        assertThrows(InvalidCommandException.class, () -> parser.parseEdit(manager, "abc dur/60"));
    }

    @Test
    public void parseEdit_invalidNewCategory_throwsInvalidActivityException() throws Exception {
        ActivityManager manager = new ActivityManager();
        manager.add(new FixedActivity(manager.getNextId(), "Lecture", ActivityCategory.ACADEMIC,
                LocalDate.of(2026, 8, 15), LocalTime.of(9, 0), LocalTime.of(11, 0),
                EnergyRating.of(4), SensoryRating.of(3), null, null));

        assertThrows(InvalidActivityException.class, () -> parser.parseEdit(manager, "1 c/BOGUS"));
    }
}
