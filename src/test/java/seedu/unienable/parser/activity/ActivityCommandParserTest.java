package seedu.unienable.parser.activity;

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
import seedu.unienable.logic.TopicManager;
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
        TopicManager topicManager = new TopicManager(manager);
        topicManager.add(ActivityCategory.ACADEMIC, "CG3207");
        AddCommand command = parser.parseAdd(manager, topicManager,
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
        TopicManager topicManager = new TopicManager(manager);
        topicManager.add(ActivityCategory.ACADEMIC, "CG3207");
        AddCommand command = parser.parseAdd(manager, topicManager,
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
        TopicManager topicManager = new TopicManager(manager);
        AddCommand command = parser.parseAdd(manager, topicManager,
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
        TopicManager topicManager = new TopicManager(manager);
        topicManager.add(ActivityCategory.OTHERS, "Misc");
        AddCommand command = parser.parseAdd(manager, topicManager,
                "n/Consultation c/OTHERS date/2026-08-15 type/FIXED from/09:00 to/10:00 "
                        + "energy/2 sensory/2 topic/Misc");

        command.execute();

        Activity activity = manager.getById(1);
        assertEquals("Misc", activity.getTopic());
        assertNull(activity.getNote());
    }

    @Test
    public void parseAdd_topicNeverCreated_throwsInvalidIndexExceptionAndConsumesNoId() {
        // Regression test: topic/ was previously accepted as an unvalidated free-text string, so
        // an activity could reference a topic that was never created with "topic add".
        ActivityManager manager = new ActivityManager();
        TopicManager topicManager = new TopicManager(manager);
        int idBefore = manager.getNextId();

        assertThrows(InvalidIndexException.class, () -> parser.parseAdd(manager, topicManager,
                "n/Consultation c/OTHERS date/2026-08-15 type/FIXED from/09:00 to/10:00 "
                        + "energy/2 sensory/2 topic/NeverCreated"));

        assertEquals(idBefore, manager.getNextId());
        assertEquals(0, manager.size());
    }

    @Test
    public void parseAdd_topicExistsUnderDifferentCategory_throwsInvalidIndexException() throws Exception {
        // A topic name is scoped to its category, so a topic that exists under ACADEMIC must not
        // satisfy a topic/ reference on an activity being added under OTHERS.
        ActivityManager manager = new ActivityManager();
        TopicManager topicManager = new TopicManager(manager);
        topicManager.add(ActivityCategory.ACADEMIC, "Misc");

        assertThrows(InvalidIndexException.class, () -> parser.parseAdd(manager, topicManager,
                "n/Consultation c/OTHERS date/2026-08-15 type/FIXED from/09:00 to/10:00 "
                        + "energy/2 sensory/2 topic/Misc"));
    }

    @Test
    public void parseAdd_whitespaceOnlyTopic_isTreatedAsAbsent() throws Exception {
        // Regression test: "topic/   " (whitespace only) previously stored an empty string
        // instead of being treated the same as omitting topic/ entirely.
        ActivityManager manager = new ActivityManager();
        TopicManager topicManager = new TopicManager(manager);
        AddCommand command = parser.parseAdd(manager, topicManager,
                "n/Consultation c/OTHERS date/2026-08-15 type/FIXED from/09:00 to/10:00 "
                        + "energy/2 sensory/2 topic/    note/Bring headphones");

        command.execute();

        assertNull(manager.getById(1).getTopic());
    }

    @Test
    public void parseAdd_whitespaceOnlyNote_isTreatedAsAbsent() throws Exception {
        ActivityManager manager = new ActivityManager();
        TopicManager topicManager = new TopicManager(manager);
        AddCommand command = parser.parseAdd(manager, topicManager,
                "n/Consultation c/OTHERS date/2026-08-15 type/FIXED from/09:00 to/10:00 "
                        + "energy/2 sensory/2 note/    ");

        command.execute();

        assertNull(manager.getById(1).getNote());
    }

    @Test
    public void parseAdd_whitespaceOnlyDescription_throwsMissingInputException() {
        // Required fields already reject a whitespace-only value via requireField's
        // isEmpty()-after-trim check; this pins that existing (correct) behaviour.
        ActivityManager manager = new ActivityManager();
        TopicManager topicManager = new TopicManager(manager);

        assertThrows(MissingInputException.class, () -> parser.parseAdd(manager, topicManager,
                "n/    c/ACADEMIC date/2026-08-15 type/FIXED from/09:00 to/11:00 energy/4 sensory/3"));
    }

    @Test
    public void parseAdd_descriptionContainsDelimiter_throwsInvalidActivityException() {
        // Regression test: activities.txt uses '|' as its delimiter and cannot escape it, so a
        // description containing '|' was previously accepted here, reported as added, and then
        // permanently failed to persist on every later save instead of being rejected up front.
        ActivityManager manager = new ActivityManager();
        TopicManager topicManager = new TopicManager(manager);

        assertThrows(InvalidActivityException.class, () -> parser.parseAdd(manager, topicManager,
                "n/Bad|Desc c/ACADEMIC date/2026-08-15 type/FIXED from/09:00 to/11:00 energy/4 sensory/3"));
    }

    @Test
    public void parseAdd_topicContainsDelimiter_throwsInvalidActivityException() {
        ActivityManager manager = new ActivityManager();
        TopicManager topicManager = new TopicManager(manager);

        assertThrows(InvalidActivityException.class, () -> parser.parseAdd(manager, topicManager,
                "n/Lecture c/ACADEMIC date/2026-08-15 type/FIXED from/09:00 to/11:00 energy/4 sensory/3 "
                        + "topic/Bad|Topic"));
    }

    @Test
    public void parseAdd_noteContainsDelimiter_throwsInvalidActivityException() {
        ActivityManager manager = new ActivityManager();
        TopicManager topicManager = new TopicManager(manager);

        assertThrows(InvalidActivityException.class, () -> parser.parseAdd(manager, topicManager,
                "n/Lecture c/ACADEMIC date/2026-08-15 type/FIXED from/09:00 to/11:00 energy/4 sensory/3 "
                        + "note/Bad|Note"));
    }

    @Test
    public void parseAdd_markerSuppliedTwice_firstOccurrenceValueAbsorbsTheSecond() throws Exception {
        // Pinning test, not a bug fix: add's fields must appear in the documented order, so
        // "n/A n/B c/..." is already outside the documented grammar. Because extraction is purely
        // boundary-based (first "n/" to the next distinct marker), the value is "A n/B" -- the
        // second "n/" is absorbed as literal text rather than starting a new field or erroring.
        ActivityManager manager = new ActivityManager();
        TopicManager topicManager = new TopicManager(manager);
        AddCommand command = parser.parseAdd(manager, topicManager,
                "n/A n/B c/ACADEMIC date/2026-08-15 type/FIXED from/09:00 to/11:00 energy/4 sensory/3");

        command.execute();

        assertEquals("A n/B", manager.getById(1).getDescription());
    }

    @Test
    public void parseAdd_missingDescription_throwsMissingInputException() {
        ActivityManager manager = new ActivityManager();
        TopicManager topicManager = new TopicManager(manager);

        assertThrows(MissingInputException.class, () -> parser.parseAdd(manager, topicManager,
                "c/ACADEMIC date/2026-08-15 type/FIXED from/09:00 to/11:00 energy/4 sensory/3"));
    }

    @Test
    public void parseAdd_missingType_throwsMissingInputException() {
        ActivityManager manager = new ActivityManager();
        TopicManager topicManager = new TopicManager(manager);

        assertThrows(MissingInputException.class, () -> parser.parseAdd(manager, topicManager,
                "n/Lecture c/ACADEMIC date/2026-08-15"));
    }

    @Test
    public void parseAdd_invalidType_throwsInvalidCommandException() {
        ActivityManager manager = new ActivityManager();
        TopicManager topicManager = new TopicManager(manager);

        assertThrows(InvalidCommandException.class, () -> parser.parseAdd(manager, topicManager,
                "n/Lecture c/ACADEMIC date/2026-08-15 type/BOGUS from/09:00 to/11:00 energy/4 sensory/3"));
    }

    @Test
    public void parseAdd_endNotAfterStart_throwsInvalidActivityException() {
        ActivityManager manager = new ActivityManager();
        TopicManager topicManager = new TopicManager(manager);

        assertThrows(InvalidActivityException.class, () -> parser.parseAdd(manager, topicManager,
                "n/Lecture c/ACADEMIC date/2026-08-15 type/FIXED from/11:00 to/09:00 energy/4 sensory/3"));
    }

    @Test
    public void parseAdd_invalidCategory_throwsInvalidActivityException() {
        ActivityManager manager = new ActivityManager();
        TopicManager topicManager = new TopicManager(manager);

        assertThrows(InvalidActivityException.class, () -> parser.parseAdd(manager, topicManager,
                "n/Lecture c/BOGUS date/2026-08-15 type/FIXED from/09:00 to/11:00 energy/4 sensory/3"));
    }

    @Test
    public void parseAdd_invalidDate_throwsInvalidDateTimeException() {
        ActivityManager manager = new ActivityManager();
        TopicManager topicManager = new TopicManager(manager);

        assertThrows(InvalidDateTimeException.class, () -> parser.parseAdd(manager, topicManager,
                "n/Lecture c/ACADEMIC date/15-08-2026 type/FIXED from/09:00 to/11:00 energy/4 sensory/3"));
    }

    @Test
    public void parseAdd_nonExistentCalendarDate_rejectedWithoutConsumingIdOrMutatingManager() {
        // Regression test: DateTimeParser previously accepted "2026-02-30" (silently normalised
        // to 2026-02-28), so this add would have succeeded and consumed ID 1. Parsing must fail
        // before AddCommand is even built, so no activity is added and no ID is consumed.
        ActivityManager manager = new ActivityManager();
        TopicManager topicManager = new TopicManager(manager);
        int idBefore = manager.getNextId();

        assertThrows(InvalidDateTimeException.class, () -> parser.parseAdd(manager, topicManager,
                "n/Exam c/ACADEMIC date/2026-02-30 type/FIXED from/09:00 to/10:00 energy/3 sensory/3"));

        assertEquals(idBefore, manager.getNextId());
        assertEquals(0, manager.size());
    }

    @Test
    public void parseAdd_hourTwentyFour_throwsInvalidDateTimeException() {
        ActivityManager manager = new ActivityManager();
        TopicManager topicManager = new TopicManager(manager);

        assertThrows(InvalidDateTimeException.class, () -> parser.parseAdd(manager, topicManager,
                "n/Late c/ACADEMIC date/2026-08-20 type/FIXED from/24:00 to/01:00 energy/3 sensory/3"));
    }

    @Test
    public void parseAdd_invalidEnergyRating_throwsInvalidActivityException() {
        ActivityManager manager = new ActivityManager();
        TopicManager topicManager = new TopicManager(manager);

        assertThrows(InvalidActivityException.class, () -> parser.parseAdd(manager, topicManager,
                "n/Lecture c/ACADEMIC date/2026-08-15 type/FIXED from/09:00 to/11:00 energy/7 sensory/3"));
    }

    @Test
    public void parseAdd_flexibleInvalidDuration_throwsInvalidActivityException() {
        ActivityManager manager = new ActivityManager();
        TopicManager topicManager = new TopicManager(manager);

        assertThrows(InvalidActivityException.class, () -> parser.parseAdd(manager, topicManager,
                "n/Task c/ACADEMIC date/2026-08-15 type/FLEXIBLE earliest/10:00 latest/18:00 "
                        + "dur/0 energy/5 sensory/2"));
    }

    @Test
    public void parseAdd_flexibleNegativeDuration_throwsInvalidActivityException() {
        ActivityManager manager = new ActivityManager();
        TopicManager topicManager = new TopicManager(manager);

        assertThrows(InvalidActivityException.class, () -> parser.parseAdd(manager, topicManager,
                "n/Task c/ACADEMIC date/2026-08-15 type/FLEXIBLE earliest/10:00 latest/18:00 "
                        + "dur/-30 energy/5 sensory/2"));
    }

    @Test
    public void parseAdd_flexibleDurationExceedsWindow_throwsInvalidActivityException() throws Exception {
        // Regression test: earliest/10:00 latest/11:00 is a 60-minute window, but dur/500 was
        // previously accepted with no validation at all against the window size.
        ActivityManager manager = new ActivityManager();
        TopicManager topicManager = new TopicManager(manager);

        InvalidActivityException exception = assertThrows(InvalidActivityException.class, () -> parser.parseAdd(
                manager, topicManager, "n/Task c/ACADEMIC date/2026-08-15 type/FLEXIBLE earliest/10:00 latest/11:00 "
                        + "dur/500 energy/5 sensory/2"));
        assertTrue(exception.getMessage().contains("60 min available"));
    }

    @Test
    public void parseAdd_flexibleDurationExactlyFillsWindow_succeeds() throws Exception {
        // Boundary: duration equal to the window size is the edge of "must fit inside the
        // window" and should be accepted, not rejected.
        ActivityManager manager = new ActivityManager();
        TopicManager topicManager = new TopicManager(manager);
        AddCommand command = parser.parseAdd(manager, topicManager,
                "n/Task c/ACADEMIC date/2026-08-15 type/FLEXIBLE earliest/10:00 latest/11:00 "
                        + "dur/60 energy/5 sensory/2");

        command.execute();

        assertEquals(60, ((FlexibleActivity) manager.getById(1)).getDurationMinutes());
    }

    @Test
    public void parseAdd_flexibleMissingDurationEntirely_throwsInvalidDateTimeException() {
        // dur/ is dropped entirely, so latest/'s end marker ("dur/") is never found and its
        // extraction greedily captures the trailing "energy/5 sensory/2" text, which then fails
        // time parsing before a dedicated "dur is required" check is ever reached.
        ActivityManager manager = new ActivityManager();
        TopicManager topicManager = new TopicManager(manager);

        assertThrows(InvalidDateTimeException.class, () -> parser.parseAdd(manager, topicManager,
                "n/Task c/ACADEMIC date/2026-08-15 type/FLEXIBLE earliest/10:00 latest/18:00 "
                        + "energy/5 sensory/2"));
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
    public void parseList_noFields_listsEverythingInDefaultOrder() throws Exception {
        ActivityManager manager = new ActivityManager();
        TopicManager topicManager = new TopicManager(manager);
        manager.add(new FixedActivity(manager.getNextId(), "Lecture", ActivityCategory.ACADEMIC,
                LocalDate.of(2026, 8, 15), LocalTime.of(9, 0), LocalTime.of(11, 0),
                EnergyRating.of(4), SensoryRating.of(3), null, null));

        CommandResult result = parser.parseList(manager, "").execute();

        assertTrue(result.getFeedback().contains("Lecture"));
    }

    @Test
    public void parseList_statusIncompleteFilter_excludesCompletedActivities() throws Exception {
        ActivityManager manager = new ActivityManager();
        TopicManager topicManager = new TopicManager(manager);
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
        TopicManager topicManager = new TopicManager(manager);
        manager.add(new FixedActivity(manager.getNextId(), "Lecture", ActivityCategory.ACADEMIC,
                LocalDate.of(2026, 8, 15), LocalTime.of(9, 0), LocalTime.of(11, 0),
                EnergyRating.of(4), SensoryRating.of(3), null, null));

        CommandResult result = parser.parseList(manager, "view/detail").execute();

        assertTrue(result.getFeedback().contains("Status: Incomplete | Type: FIXED"));
    }

    @Test
    public void parseList_categoryAndTopicFilter_combineWithAnd() throws Exception {
        ActivityManager manager = new ActivityManager();
        TopicManager topicManager = new TopicManager(manager);
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
        TopicManager topicManager = new TopicManager(manager);
        manager.add(new FixedActivity(manager.getNextId(), "CG3207 lecture", ActivityCategory.ACADEMIC,
                LocalDate.of(2026, 8, 15), LocalTime.of(9, 0), LocalTime.of(10, 0),
                EnergyRating.of(4), SensoryRating.of(3), "CG3207", null));

        CommandResult result = parser.parseList(manager, "topic/CG3207").execute();

        assertTrue(result.getFeedback().contains("CG3207 lecture"));
    }

    @Test
    public void parseList_whitespaceOnlyTopicFilter_isIgnoredNotTreatedAsLiteralFilter() throws Exception {
        // Regression test: "list topic/   " previously filtered for an activity whose topic
        // equals the literal empty string, which no activity ever has (topic is null when unset),
        // so the filter silently excluded everything instead of being ignored like an omitted
        // topic/ field.
        ActivityManager manager = new ActivityManager();
        TopicManager topicManager = new TopicManager(manager);
        manager.add(new FixedActivity(manager.getNextId(), "No-topic lecture", ActivityCategory.ACADEMIC,
                LocalDate.of(2026, 8, 15), LocalTime.of(9, 0), LocalTime.of(10, 0),
                EnergyRating.of(4), SensoryRating.of(3), null, null));

        CommandResult result = parser.parseList(manager, "topic/   ").execute();

        assertTrue(result.getFeedback().contains("No-topic lecture"));
    }

    @Test
    public void parseList_invalidStatus_throwsInvalidCommandException() {
        ActivityManager manager = new ActivityManager();
        TopicManager topicManager = new TopicManager(manager);

        assertThrows(InvalidCommandException.class, () -> parser.parseList(manager, "status/bogus"));
    }

    @Test
    public void parseList_invalidOrder_throwsInvalidCommandException() {
        ActivityManager manager = new ActivityManager();
        TopicManager topicManager = new TopicManager(manager);

        assertThrows(InvalidCommandException.class, () -> parser.parseList(manager, "order/bogus"));
    }

    @Test
    public void parseFind_singleKeyword_findsMatchingActivity() throws Exception {
        ActivityManager manager = new ActivityManager();
        TopicManager topicManager = new TopicManager(manager);
        manager.add(new FixedActivity(manager.getNextId(), "Finish assignment 1", ActivityCategory.ACADEMIC,
                LocalDate.of(2026, 8, 15), LocalTime.of(9, 0), LocalTime.of(10, 0),
                EnergyRating.of(4), SensoryRating.of(3), null, null));

        CommandResult result = parser.parseFind(manager, "k/assignment").execute();

        assertTrue(result.getFeedback().contains("Finish assignment 1"));
    }

    @Test
    public void parseFind_multiWordKeyword_splitsIntoAndedKeywords() throws Exception {
        ActivityManager manager = new ActivityManager();
        TopicManager topicManager = new TopicManager(manager);
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
        TopicManager topicManager = new TopicManager(manager);
        manager.add(new FixedActivity(manager.getNextId(), "CG3207 lecture", ActivityCategory.ACADEMIC,
                LocalDate.of(2026, 8, 15), LocalTime.of(9, 0), LocalTime.of(10, 0),
                EnergyRating.of(4), SensoryRating.of(3), "CG3207", null));

        CommandResult result = parser.parseFind(manager, "c/ACADEMIC topic/CG3207").execute();

        assertTrue(result.getFeedback().contains("CG3207 lecture"));
    }

    @Test
    public void parseFind_topicFilterAloneWithNoExplicitCategory_doesNotThrow() throws Exception {
        ActivityManager manager = new ActivityManager();
        TopicManager topicManager = new TopicManager(manager);
        manager.add(new FixedActivity(manager.getNextId(), "CG3207 lecture", ActivityCategory.ACADEMIC,
                LocalDate.of(2026, 8, 15), LocalTime.of(9, 0), LocalTime.of(10, 0),
                EnergyRating.of(4), SensoryRating.of(3), "CG3207", null));

        CommandResult result = parser.parseFind(manager, "topic/CG3207").execute();

        assertTrue(result.getFeedback().contains("CG3207 lecture"));
    }

    @Test
    public void parseFind_neitherKeywordNorFilter_throwsMissingInputException() {
        ActivityManager manager = new ActivityManager();
        TopicManager topicManager = new TopicManager(manager);

        assertThrows(MissingInputException.class, () -> parser.parseFind(manager, ""));
    }

    @Test
    public void parseFind_whitespaceOnlyTopicFilterAlone_throwsMissingInputException() {
        // Regression test: a blank topic/ does not count as a supplied filter -- same principle
        // as order/ alone not counting -- so "find topic/   " with nothing else must still be
        // rejected rather than silently matching every activity.
        ActivityManager manager = new ActivityManager();
        TopicManager topicManager = new TopicManager(manager);

        assertThrows(MissingInputException.class, () -> parser.parseFind(manager, "topic/   "));
    }

    @Test
    public void parseFind_whitespaceOnlyTopicWithOtherFilter_ignoresTopicUsesOtherFilter() throws Exception {
        ActivityManager manager = new ActivityManager();
        TopicManager topicManager = new TopicManager(manager);
        manager.add(new FixedActivity(manager.getNextId(), "No-topic lecture", ActivityCategory.ACADEMIC,
                LocalDate.of(2026, 8, 15), LocalTime.of(9, 0), LocalTime.of(10, 0),
                EnergyRating.of(4), SensoryRating.of(3), null, null));

        CommandResult result = parser.parseFind(manager, "c/ACADEMIC topic/   ").execute();

        assertTrue(result.getFeedback().contains("No-topic lecture"));
    }

    @Test
    public void parseFind_whitespaceOnlyArgs_throwsMissingInputException() {
        ActivityManager manager = new ActivityManager();
        TopicManager topicManager = new TopicManager(manager);

        assertThrows(MissingInputException.class, () -> parser.parseFind(manager, "   "));
    }

    @Test
    public void parseFind_orderMarkerAloneWithNoKeywordOrFilter_throwsMissingInputException() {
        // Regression test: order/ is find's last marker but is a display-ordering directive, not
        // a keyword or filter. "find order/time" alone was previously accepted (since the fields
        // map was non-empty) and silently returned every activity.
        ActivityManager manager = new ActivityManager();
        TopicManager topicManager = new TopicManager(manager);

        assertThrows(MissingInputException.class, () -> parser.parseFind(manager, "order/time"));
    }

    @Test
    public void parseFind_whitespaceOnlyKeywordAlone_throwsMissingInputException() {
        // Regression test: a blank k/ does not count as a supplied keyword, same principle as
        // topic/ and order/ above. Previously "find k/   " passed the "at least one keyword or
        // filter" check (fields.containsKey("k/") was true) and String.split on the resulting
        // trimmed-to-empty value produced a single empty-string "keyword" that every activity's
        // description trivially contains, silently matching every activity instead of being
        // rejected.
        ActivityManager manager = new ActivityManager();
        TopicManager topicManager = new TopicManager(manager);

        assertThrows(MissingInputException.class, () -> parser.parseFind(manager, "k/   "));
    }

    @Test
    public void parseFind_whitespaceOnlyKeywordWithOtherFilter_ignoresKeywordUsesOtherFilter() throws Exception {
        ActivityManager manager = new ActivityManager();
        TopicManager topicManager = new TopicManager(manager);
        manager.add(new FixedActivity(manager.getNextId(), "No-topic lecture", ActivityCategory.ACADEMIC,
                LocalDate.of(2026, 8, 15), LocalTime.of(9, 0), LocalTime.of(10, 0),
                EnergyRating.of(4), SensoryRating.of(3), null, null));

        CommandResult result = parser.parseFind(manager, "c/ACADEMIC k/   ").execute();

        assertTrue(result.getFeedback().contains("No-topic lecture"));
    }

    @Test
    public void parseNext_buildsWorkingNextCommand() throws Exception {
        ActivityManager manager = new ActivityManager();
        TopicManager topicManager = new TopicManager(manager);
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
        TopicManager topicManager = new TopicManager(manager);
        topicManager.add(ActivityCategory.ACADEMIC, "CG3207");
        manager.add(new FlexibleActivity(manager.getNextId(), "Finish assignment 1", ActivityCategory.ACADEMIC,
                LocalDate.of(2026, 8, 15), LocalTime.of(10, 0), LocalTime.of(18, 0), 90,
                EnergyRating.of(5), SensoryRating.of(2), "CG3207", null));

        parser.parseEdit(manager, topicManager, "1 dur/60").execute();

        Activity updated = manager.getById(1);
        assertEquals(60, ((FlexibleActivity) updated).getDurationMinutes());
        assertEquals("Finish assignment 1", updated.getDescription());
        assertEquals("CG3207", updated.getTopic());
    }

    @Test
    public void parseEdit_whitespaceOnlyTopic_clearsTopicToNull() throws Exception {
        // Regression test: editing topic/ to a whitespace-only value previously stored an empty
        // string rather than clearing the topic to null, the same way it is represented when
        // never set.
        ActivityManager manager = new ActivityManager();
        TopicManager topicManager = new TopicManager(manager);
        manager.add(new FixedActivity(manager.getNextId(), "Lecture", ActivityCategory.ACADEMIC,
                LocalDate.of(2026, 8, 15), LocalTime.of(9, 0), LocalTime.of(11, 0),
                EnergyRating.of(4), SensoryRating.of(3), "CG3207", null));

        parser.parseEdit(manager, topicManager, "1 topic/   ").execute();

        assertNull(manager.getById(1).getTopic());
    }

    @Test
    public void parseEdit_whitespaceOnlyNote_clearsNoteToNull() throws Exception {
        ActivityManager manager = new ActivityManager();
        TopicManager topicManager = new TopicManager(manager);
        manager.add(new FixedActivity(manager.getNextId(), "Lecture", ActivityCategory.ACADEMIC,
                LocalDate.of(2026, 8, 15), LocalTime.of(9, 0), LocalTime.of(11, 0),
                EnergyRating.of(4), SensoryRating.of(3), null, "Bring laptop"));

        parser.parseEdit(manager, topicManager, "1 note/   ").execute();

        assertNull(manager.getById(1).getNote());
    }

    @Test
    public void parseEdit_descriptionContainsDelimiter_throwsInvalidActivityException() throws Exception {
        // Regression test: same root cause as parseAdd's equivalent test, exercised through
        // edit's any-order field map instead.
        ActivityManager manager = new ActivityManager();
        TopicManager topicManager = new TopicManager(manager);
        manager.add(new FixedActivity(manager.getNextId(), "Lecture", ActivityCategory.ACADEMIC,
                LocalDate.of(2026, 8, 15), LocalTime.of(9, 0), LocalTime.of(11, 0),
                EnergyRating.of(4), SensoryRating.of(3), null, null));

        assertThrows(InvalidActivityException.class, () -> parser.parseEdit(manager, topicManager, "1 n/Bad|Desc"));
    }

    @Test
    public void parseEdit_topicContainsDelimiter_throwsInvalidActivityException() throws Exception {
        ActivityManager manager = new ActivityManager();
        TopicManager topicManager = new TopicManager(manager);
        manager.add(new FixedActivity(manager.getNextId(), "Lecture", ActivityCategory.ACADEMIC,
                LocalDate.of(2026, 8, 15), LocalTime.of(9, 0), LocalTime.of(11, 0),
                EnergyRating.of(4), SensoryRating.of(3), null, null));

        assertThrows(InvalidActivityException.class,
                () -> parser.parseEdit(manager, topicManager, "1 topic/Bad|Topic"));
    }

    @Test
    public void parseEdit_noteContainsDelimiter_throwsInvalidActivityException() throws Exception {
        ActivityManager manager = new ActivityManager();
        TopicManager topicManager = new TopicManager(manager);
        manager.add(new FixedActivity(manager.getNextId(), "Lecture", ActivityCategory.ACADEMIC,
                LocalDate.of(2026, 8, 15), LocalTime.of(9, 0), LocalTime.of(11, 0),
                EnergyRating.of(4), SensoryRating.of(3), null, null));

        assertThrows(InvalidActivityException.class, () -> parser.parseEdit(manager, topicManager, "1 note/Bad|Note"));
    }

    @Test
    public void parseEdit_markerSuppliedTwice_firstOccurrenceValueAbsorbsTheSecond() throws Exception {
        // Pinning test, not a bug fix: same boundary-based extraction behaviour as add's
        // equivalent test, exercised through edit's any-order field map instead.
        ActivityManager manager = new ActivityManager();
        TopicManager topicManager = new TopicManager(manager);
        manager.add(new FixedActivity(manager.getNextId(), "Lecture", ActivityCategory.ACADEMIC,
                LocalDate.of(2026, 8, 15), LocalTime.of(9, 0), LocalTime.of(11, 0),
                EnergyRating.of(4), SensoryRating.of(3), null, null));

        parser.parseEdit(manager, topicManager, "1 n/X n/Y").execute();

        assertEquals("X n/Y", manager.getById(1).getDescription());
    }

    @Test
    public void parseEdit_topicOnlyWithNoExplicitCategory_doesNotMistakeTopicForCategory() throws Exception {
        // Regression test: "topic/" ends in the substring "c/", so editing topic/ alone (with no
        // c/ field at all) previously caused extractPresentFields to falsely detect a "c/" field
        // embedded inside "topic/"'s own marker text, and reject the edit with "category must be
        // one of ACADEMIC, CCA, WORK_INTERNSHIP, OTHERS."
        ActivityManager manager = new ActivityManager();
        TopicManager topicManager = new TopicManager(manager);
        topicManager.add(ActivityCategory.ACADEMIC, "CS2113");
        manager.add(new FixedActivity(manager.getNextId(), "Lecture", ActivityCategory.ACADEMIC,
                LocalDate.of(2026, 8, 15), LocalTime.of(9, 0), LocalTime.of(11, 0),
                EnergyRating.of(4), SensoryRating.of(3), null, null));

        parser.parseEdit(manager, topicManager, "1 topic/CS2113").execute();

        Activity updated = manager.getById(1);
        assertEquals("CS2113", updated.getTopic());
        assertEquals(ActivityCategory.ACADEMIC, updated.getCategory());
    }

    @Test
    public void parseEdit_categoryChangeWouldOrphanExistingTopic_throwsInvalidIndexExceptionAndDoesNotMutate()
            throws Exception {
        // Regression test: topics are one-level groupings inside a fixed category, but editing an
        // activity's category previously carried its old topic straight over with no check that
        // the topic still exists under the new category, silently stranding the topic outside the
        // category it is registered under (e.g. an ACADEMIC/CS2113 activity became CCA/CS2113
        // while CS2113 stayed registered only under ACADEMIC).
        ActivityManager manager = new ActivityManager();
        TopicManager topicManager = new TopicManager(manager);
        topicManager.add(ActivityCategory.ACADEMIC, "CS2113");
        manager.add(new FixedActivity(manager.getNextId(), "Lecture", ActivityCategory.ACADEMIC,
                LocalDate.of(2026, 8, 15), LocalTime.of(9, 0), LocalTime.of(11, 0),
                EnergyRating.of(4), SensoryRating.of(3), "CS2113", null));

        assertThrows(InvalidIndexException.class, () -> parser.parseEdit(manager, topicManager, "1 c/CCA"));

        Activity unchanged = manager.getById(1);
        assertEquals(ActivityCategory.ACADEMIC, unchanged.getCategory());
        assertEquals("CS2113", unchanged.getTopic());
    }

    @Test
    public void parseEdit_categoryChangeWithValidTargetTopic_succeedsAtomically() throws Exception {
        ActivityManager manager = new ActivityManager();
        TopicManager topicManager = new TopicManager(manager);
        topicManager.add(ActivityCategory.ACADEMIC, "CS2113");
        topicManager.add(ActivityCategory.CCA, "Basketball Club");
        manager.add(new FixedActivity(manager.getNextId(), "Lecture", ActivityCategory.ACADEMIC,
                LocalDate.of(2026, 8, 15), LocalTime.of(9, 0), LocalTime.of(11, 0),
                EnergyRating.of(4), SensoryRating.of(3), "CS2113", null));

        parser.parseEdit(manager, topicManager, "1 c/CCA topic/Basketball Club").execute();

        Activity updated = manager.getById(1);
        assertEquals(ActivityCategory.CCA, updated.getCategory());
        assertEquals("Basketball Club", updated.getTopic());
    }

    @Test
    public void parseEdit_categoryChangeWithNoTopic_isAllowed() throws Exception {
        ActivityManager manager = new ActivityManager();
        TopicManager topicManager = new TopicManager(manager);
        manager.add(new FixedActivity(manager.getNextId(), "Lecture", ActivityCategory.ACADEMIC,
                LocalDate.of(2026, 8, 15), LocalTime.of(9, 0), LocalTime.of(11, 0),
                EnergyRating.of(4), SensoryRating.of(3), null, null));

        parser.parseEdit(manager, topicManager, "1 c/CCA").execute();

        assertEquals(ActivityCategory.CCA, manager.getById(1).getCategory());
    }

    @Test
    public void parseEdit_topicNeverCreated_throwsInvalidIndexException() throws Exception {
        ActivityManager manager = new ActivityManager();
        TopicManager topicManager = new TopicManager(manager);
        manager.add(new FixedActivity(manager.getNextId(), "Lecture", ActivityCategory.ACADEMIC,
                LocalDate.of(2026, 8, 15), LocalTime.of(9, 0), LocalTime.of(11, 0),
                EnergyRating.of(4), SensoryRating.of(3), null, null));

        assertThrows(InvalidIndexException.class,
                () -> parser.parseEdit(manager, topicManager, "1 topic/NeverCreated"));
    }

    @Test
    public void parseEdit_multipleFields_updatesAllGivenFields() throws Exception {
        ActivityManager manager = new ActivityManager();
        TopicManager topicManager = new TopicManager(manager);
        manager.add(new FlexibleActivity(manager.getNextId(), "Prepare slides", ActivityCategory.ACADEMIC,
                LocalDate.of(2026, 8, 15), LocalTime.of(10, 0), LocalTime.of(18, 0), 90,
                EnergyRating.of(3), SensoryRating.of(3), null, null));

        parser.parseEdit(manager, topicManager, "1 n/New activity name energy/4 sensory/2").execute();

        Activity updated = manager.getById(1);
        assertEquals("New activity name", updated.getDescription());
        assertEquals(4, updated.getEnergyRating().getValue());
        assertEquals(2, updated.getSensoryRating().getValue());
    }

    @Test
    public void parseEdit_noteOnly_leavesOtherFieldsUnchanged() throws Exception {
        ActivityManager manager = new ActivityManager();
        TopicManager topicManager = new TopicManager(manager);
        topicManager.add(ActivityCategory.OTHERS, "Misc");
        manager.add(new FixedActivity(manager.getNextId(), "Consultation", ActivityCategory.OTHERS,
                LocalDate.of(2026, 8, 15), LocalTime.of(9, 0), LocalTime.of(10, 0),
                EnergyRating.of(2), SensoryRating.of(2), "Misc", null));

        parser.parseEdit(manager, topicManager, "1 note/Bring headphones").execute();

        Activity updated = manager.getById(1);
        assertEquals("Bring headphones", updated.getNote());
        assertEquals("Misc", updated.getTopic());
        assertEquals(LocalTime.of(9, 0), ((FixedActivity) updated).getStartTime());
    }

    @Test
    public void parseEdit_preservesCompletionStatus() throws Exception {
        ActivityManager manager = new ActivityManager();
        TopicManager topicManager = new TopicManager(manager);
        manager.add(new FlexibleActivity(manager.getNextId(), "Finish assignment 1", ActivityCategory.ACADEMIC,
                LocalDate.of(2026, 8, 15), LocalTime.of(10, 0), LocalTime.of(18, 0), 90,
                EnergyRating.of(5), SensoryRating.of(2), null, null));
        manager.mark(1);

        parser.parseEdit(manager, topicManager, "1 dur/60").execute();

        assertTrue(manager.getById(1).isComplete());
    }

    @Test
    public void parseEdit_changingTypeWithAllNewTimingFields_succeeds() throws Exception {
        ActivityManager manager = new ActivityManager();
        TopicManager topicManager = new TopicManager(manager);
        manager.add(new FixedActivity(manager.getNextId(), "Lecture", ActivityCategory.ACADEMIC,
                LocalDate.of(2026, 8, 15), LocalTime.of(9, 0), LocalTime.of(11, 0),
                EnergyRating.of(4), SensoryRating.of(3), null, null));

        parser.parseEdit(manager, topicManager, "1 type/FLEXIBLE earliest/10:00 latest/18:00 dur/90").execute();

        Activity updated = manager.getById(1);
        assertEquals(ScheduleType.FLEXIBLE, updated.getScheduleType());
        assertEquals(90, ((FlexibleActivity) updated).getDurationMinutes());
    }

    @Test
    public void parseEdit_durationExceedsExistingWindow_throwsInvalidActivityException() throws Exception {
        // Regression test: editing only dur/ must still validate against the activity's existing
        // (unchanged) earliest/latest window, not just when the window is also being edited.
        ActivityManager manager = new ActivityManager();
        TopicManager topicManager = new TopicManager(manager);
        manager.add(new FlexibleActivity(manager.getNextId(), "Finish assignment 1", ActivityCategory.ACADEMIC,
                LocalDate.of(2026, 8, 15), LocalTime.of(10, 0), LocalTime.of(11, 0), 60,
                EnergyRating.of(5), SensoryRating.of(2), null, null));

        assertThrows(InvalidActivityException.class, () -> parser.parseEdit(manager, topicManager, "1 dur/500"));
    }

    @Test
    public void parseEdit_changingTypeWithDurationExceedingNewWindow_throwsInvalidActivityException()
            throws Exception {
        ActivityManager manager = new ActivityManager();
        TopicManager topicManager = new TopicManager(manager);
        manager.add(new FixedActivity(manager.getNextId(), "Lecture", ActivityCategory.ACADEMIC,
                LocalDate.of(2026, 8, 15), LocalTime.of(9, 0), LocalTime.of(11, 0),
                EnergyRating.of(4), SensoryRating.of(3), null, null));

        assertThrows(InvalidActivityException.class, () -> parser.parseEdit(manager, topicManager,
                "1 type/FLEXIBLE earliest/10:00 latest/11:00 dur/500"));
    }

    @Test
    public void parseEdit_changingTypeWithoutNewTimingFields_throwsMissingInputException() throws Exception {
        ActivityManager manager = new ActivityManager();
        TopicManager topicManager = new TopicManager(manager);
        manager.add(new FixedActivity(manager.getNextId(), "Lecture", ActivityCategory.ACADEMIC,
                LocalDate.of(2026, 8, 15), LocalTime.of(9, 0), LocalTime.of(11, 0),
                EnergyRating.of(4), SensoryRating.of(3), null, null));

        assertThrows(MissingInputException.class, () -> parser.parseEdit(manager, topicManager, "1 type/FLEXIBLE"));
    }

    @Test
    public void parseEdit_noFieldsSupplied_throwsMissingInputException() throws Exception {
        ActivityManager manager = new ActivityManager();
        TopicManager topicManager = new TopicManager(manager);
        manager.add(new FixedActivity(manager.getNextId(), "Lecture", ActivityCategory.ACADEMIC,
                LocalDate.of(2026, 8, 15), LocalTime.of(9, 0), LocalTime.of(11, 0),
                EnergyRating.of(4), SensoryRating.of(3), null, null));

        assertThrows(MissingInputException.class, () -> parser.parseEdit(manager, topicManager, "1"));
    }

    @Test
    public void parseEdit_unknownId_throwsInvalidIndexException() {
        ActivityManager manager = new ActivityManager();
        TopicManager topicManager = new TopicManager(manager);

        assertThrows(InvalidIndexException.class, () -> parser.parseEdit(manager, topicManager, "999 dur/60"));
    }

    @Test
    public void parseEdit_nonNumericId_throwsInvalidCommandException() {
        ActivityManager manager = new ActivityManager();
        TopicManager topicManager = new TopicManager(manager);

        assertThrows(InvalidCommandException.class, () -> parser.parseEdit(manager, topicManager, "abc dur/60"));
    }

    @Test
    public void parseEdit_invalidNewCategory_throwsInvalidActivityException() throws Exception {
        ActivityManager manager = new ActivityManager();
        TopicManager topicManager = new TopicManager(manager);
        manager.add(new FixedActivity(manager.getNextId(), "Lecture", ActivityCategory.ACADEMIC,
                LocalDate.of(2026, 8, 15), LocalTime.of(9, 0), LocalTime.of(11, 0),
                EnergyRating.of(4), SensoryRating.of(3), null, null));

        assertThrows(InvalidActivityException.class, () -> parser.parseEdit(manager, topicManager, "1 c/BOGUS"));
    }
}
