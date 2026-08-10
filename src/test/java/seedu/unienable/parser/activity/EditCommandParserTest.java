package seedu.unienable.parser.activity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

import org.junit.jupiter.api.Test;

import seedu.unienable.exception.DuplicateActivityException;
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
import seedu.unienable.model.enums.ScheduleType;

class EditCommandParserTest {
    private static final LocalDateTime TODAY = LocalDate.of(2020, 1, 1).atStartOfDay();

    private final EditCommandParser parser = new EditCommandParser();

    @Test
    public void parseEdit_pastDate_rejectedWithoutMutatingActivity() throws Exception {
        // A rejected edit must not change the stored activity, matching the "no confirmation
        // prompt is even reached" guarantee: parseEdit throws before EditCommand is built, so
        // CommandConfirmationHandler never sees this edit at all.
        ActivityManager manager = new ActivityManager();
        TopicManager topicManager = new TopicManager(manager);
        LocalDateTime today = LocalDate.of(2026, 8, 1).atStartOfDay();
        manager.add(new FixedActivity(manager.getNextId(), "Lecture", ActivityCategory.ACADEMIC,
                LocalDate.of(2026, 8, 15), LocalTime.of(9, 0), LocalTime.of(10, 0),
                EnergyRating.of(2), SensoryRating.of(2), null, null));

        InvalidDateTimeException exception = assertThrows(InvalidDateTimeException.class,
                () -> parser.parse(manager, topicManager, today, "1 date/2026-07-31"));

        assertEquals("date has passed. Please enter a date from 2026-08-01 onwards.", exception.getMessage());
        assertEquals(LocalDate.of(2026, 8, 15), manager.getById(1).getDate());
    }

    @Test
    public void parseEdit_dateUntouchedWithExistingPastDate_stillSucceeds() throws Exception {
        // The not-before-today check only applies when date/ is actually supplied; editing an
        // unrelated field on an activity that already has a past date (e.g. a legitimate overdue
        // activity) must not be blocked by it.
        ActivityManager manager = new ActivityManager();
        TopicManager topicManager = new TopicManager(manager);
        LocalDateTime today = LocalDate.of(2026, 8, 1).atStartOfDay();
        manager.add(new FixedActivity(manager.getNextId(), "Overdue lecture", ActivityCategory.ACADEMIC,
                LocalDate.of(2026, 7, 1), LocalTime.of(9, 0), LocalTime.of(10, 0),
                EnergyRating.of(2), SensoryRating.of(2), null, null));

        parser.parse(manager, topicManager, today, "1 energy/5").execute();

        assertEquals(5, manager.getById(1).getEnergyRating().getValue());
        assertEquals(LocalDate.of(2026, 7, 1), manager.getById(1).getDate());
    }

    @Test
    public void parseEdit_todayOrFutureDate_isAccepted() throws Exception {
        ActivityManager manager = new ActivityManager();
        TopicManager topicManager = new TopicManager(manager);
        LocalDateTime today = LocalDate.of(2026, 8, 1).atStartOfDay();
        manager.add(new FixedActivity(manager.getNextId(), "Lecture", ActivityCategory.ACADEMIC,
                LocalDate.of(2026, 8, 15), LocalTime.of(9, 0), LocalTime.of(10, 0),
                EnergyRating.of(2), SensoryRating.of(2), null, null));

        parser.parse(manager, topicManager, today, "1 date/2026-08-01").execute();

        assertEquals(LocalDate.of(2026, 8, 1), manager.getById(1).getDate());
    }

    @Test
    public void parseEdit_movingActivityToAlreadyPassedTimeToday_rejectedBeforeAnyMutation() throws Exception {
        // Reproduction B from BUG-03: editing date/ and from/ together into an already-passed
        // slot must be rejected before any confirmation preview is even reachable, and must not
        // change the stored activity.
        ActivityManager manager = new ActivityManager();
        TopicManager topicManager = new TopicManager(manager);
        LocalDateTime now = LocalDateTime.of(2026, 8, 1, 16, 0);
        manager.add(new FixedActivity(manager.getNextId(), "Future slot", ActivityCategory.ACADEMIC,
                LocalDate.of(2026, 8, 2), LocalTime.of(17, 0), LocalTime.of(18, 0),
                EnergyRating.of(2), SensoryRating.of(2), null, null));

        InvalidDateTimeException exception = assertThrows(InvalidDateTimeException.class,
                () -> parser.parse(manager, topicManager, now, "1 date/2026-08-01 from/11:30 to/12:00"));

        assertEquals("activity start time has passed. Please enter a start time after 16:00.",
                exception.getMessage());
        assertEquals(LocalDate.of(2026, 8, 2), manager.getById(1).getDate());
        assertEquals(LocalTime.of(17, 0), ((FixedActivity) manager.getById(1)).getStartTime());
    }

    @Test
    public void parseEdit_movingActivityToFutureTimeToday_isAccepted() throws Exception {
        ActivityManager manager = new ActivityManager();
        TopicManager topicManager = new TopicManager(manager);
        LocalDateTime now = LocalDateTime.of(2026, 8, 1, 16, 0);
        manager.add(new FixedActivity(manager.getNextId(), "Future slot", ActivityCategory.ACADEMIC,
                LocalDate.of(2026, 8, 2), LocalTime.of(17, 0), LocalTime.of(18, 0),
                EnergyRating.of(2), SensoryRating.of(2), null, null));

        parser.parse(manager, topicManager, now, "1 date/2026-08-01 from/16:30 to/17:30").execute();

        assertEquals(LocalDate.of(2026, 8, 1), manager.getById(1).getDate());
    }

    @Test
    public void parseEdit_activelySupplyingFromOnActivityAlreadyDatedToday_reappliesTheCheck() throws Exception {
        // Even when date/ isn't touched, actively supplying from/ on an activity already dated
        // today must still be checked against now.
        ActivityManager manager = new ActivityManager();
        TopicManager topicManager = new TopicManager(manager);
        LocalDateTime now = LocalDateTime.of(2026, 8, 1, 16, 0);
        manager.add(new FixedActivity(manager.getNextId(), "Today activity", ActivityCategory.ACADEMIC,
                LocalDate.of(2026, 8, 1), LocalTime.of(17, 0), LocalTime.of(18, 0),
                EnergyRating.of(2), SensoryRating.of(2), null, null));

        assertThrows(InvalidDateTimeException.class,
                () -> parser.parse(manager, topicManager, now, "1 from/15:00 to/18:00"));
    }

    @Test
    public void parseEdit_untouchedFromOnAlreadyPassedTodayActivity_stillSucceeds() throws Exception {
        // The start-time-not-passed check only applies when date/ or the start marker is
        // actively supplied - editing an unrelated field on an activity that has simply become
        // overdue during the session must not be blocked by it.
        ActivityManager manager = new ActivityManager();
        TopicManager topicManager = new TopicManager(manager);
        LocalDateTime now = LocalDateTime.of(2026, 8, 1, 16, 0);
        manager.add(new FixedActivity(manager.getNextId(), "Already passed today", ActivityCategory.ACADEMIC,
                LocalDate.of(2026, 8, 1), LocalTime.of(9, 0), LocalTime.of(10, 0),
                EnergyRating.of(2), SensoryRating.of(2), null, null));

        parser.parse(manager, topicManager, now, "1 energy/5").execute();

        assertEquals(5, manager.getById(1).getEnergyRating().getValue());
        assertEquals(LocalTime.of(9, 0), ((FixedActivity) manager.getById(1)).getStartTime());
    }

    @Test
    public void parseEdit_unrecognisedLeadingToken_throwsInvalidCommandExceptionAndDoesNotMutate() throws Exception {
        // Regression test for RC05 (v1.0 RC retest, 2026-08-01).
        ActivityManager manager = new ActivityManager();
        TopicManager topicManager = new TopicManager(manager);
        manager.add(new FixedActivity(manager.getNextId(), "Lecture", ActivityCategory.ACADEMIC,
                LocalDate.of(2026, 8, 15), LocalTime.of(9, 0), LocalTime.of(10, 0),
                EnergyRating.of(2), SensoryRating.of(2), null, null));

        assertThrows(InvalidCommandException.class,
                () -> parser.parse(manager, topicManager, TODAY, "1 ignored/yes energy/5"));
        assertEquals(2, manager.getById(1).getEnergyRating().getValue());
    }

    @Test
    public void parseEdit_singleField_updatesOnlyThatField() throws Exception {
        ActivityManager manager = new ActivityManager();
        TopicManager topicManager = new TopicManager(manager);
        topicManager.add(ActivityCategory.ACADEMIC, "CG3207");
        manager.add(new FlexibleActivity(manager.getNextId(), "Finish assignment 1", ActivityCategory.ACADEMIC,
                LocalDate.of(2026, 8, 15), LocalTime.of(10, 0), LocalTime.of(18, 0), 90,
                EnergyRating.of(5), SensoryRating.of(2), "CG3207", null));

        parser.parse(manager, topicManager, TODAY, "1 dur/60").execute();

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

        parser.parse(manager, topicManager, TODAY, "1 topic/   ").execute();

        assertNull(manager.getById(1).getTopic());
    }

    @Test
    public void parseEdit_whitespaceOnlyNote_clearsNoteToNull() throws Exception {
        ActivityManager manager = new ActivityManager();
        TopicManager topicManager = new TopicManager(manager);
        manager.add(new FixedActivity(manager.getNextId(), "Lecture", ActivityCategory.ACADEMIC,
                LocalDate.of(2026, 8, 15), LocalTime.of(9, 0), LocalTime.of(11, 0),
                EnergyRating.of(4), SensoryRating.of(3), null, "Bring laptop"));

        parser.parse(manager, topicManager, TODAY, "1 note/   ").execute();

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

        assertThrows(InvalidActivityException.class,
                () -> parser.parse(manager, topicManager, TODAY, "1 n/Bad|Desc"));
    }

    @Test
    public void parseEdit_topicContainsDelimiter_throwsInvalidActivityException() throws Exception {
        ActivityManager manager = new ActivityManager();
        TopicManager topicManager = new TopicManager(manager);
        manager.add(new FixedActivity(manager.getNextId(), "Lecture", ActivityCategory.ACADEMIC,
                LocalDate.of(2026, 8, 15), LocalTime.of(9, 0), LocalTime.of(11, 0),
                EnergyRating.of(4), SensoryRating.of(3), null, null));

        assertThrows(InvalidActivityException.class,
                () -> parser.parse(manager, topicManager, TODAY, "1 topic/Bad|Topic"));
    }

    @Test
    public void parseEdit_noteContainsDelimiter_throwsInvalidActivityException() throws Exception {
        ActivityManager manager = new ActivityManager();
        TopicManager topicManager = new TopicManager(manager);
        manager.add(new FixedActivity(manager.getNextId(), "Lecture", ActivityCategory.ACADEMIC,
                LocalDate.of(2026, 8, 15), LocalTime.of(9, 0), LocalTime.of(11, 0),
                EnergyRating.of(4), SensoryRating.of(3), null, null));

        assertThrows(InvalidActivityException.class,
                () -> parser.parse(manager, topicManager, TODAY, "1 note/Bad|Note"));
    }

    @Test
    public void parseEdit_duplicateNameMarker_throwsInvalidCommandException() throws Exception {
        // Regression test: "n/X n/Y" previously let the first "n/" silently absorb the second as
        // literal text ("X n/Y") instead of being rejected as a repeated field.
        ActivityManager manager = new ActivityManager();
        TopicManager topicManager = new TopicManager(manager);
        manager.add(new FixedActivity(manager.getNextId(), "Lecture", ActivityCategory.ACADEMIC,
                LocalDate.of(2026, 8, 15), LocalTime.of(9, 0), LocalTime.of(11, 0),
                EnergyRating.of(4), SensoryRating.of(3), null, null));

        InvalidCommandException exception = assertThrows(InvalidCommandException.class,
                () -> parser.parse(manager, topicManager, TODAY, "1 n/X n/Y"));

        assertTrue(exception.getMessage().contains("Duplicate option \"n/\""));
    }

    @Test
    public void parseEdit_duplicateDateMarker_throwsInvalidCommandException() throws Exception {
        ActivityManager manager = new ActivityManager();
        TopicManager topicManager = new TopicManager(manager);
        manager.add(new FixedActivity(manager.getNextId(), "Lecture", ActivityCategory.ACADEMIC,
                LocalDate.of(2026, 8, 15), LocalTime.of(9, 0), LocalTime.of(11, 0),
                EnergyRating.of(4), SensoryRating.of(3), null, null));

        InvalidCommandException exception = assertThrows(InvalidCommandException.class,
                () -> parser.parse(manager, topicManager, TODAY, "1 date/2026-08-16 date/2026-08-17"));

        assertTrue(exception.getMessage().contains("Duplicate option \"date/\""));
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

        parser.parse(manager, topicManager, TODAY, "1 topic/CS2113").execute();

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

        assertThrows(InvalidIndexException.class, () -> parser.parse(manager, topicManager, TODAY, "1 c/CCA"));

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

        parser.parse(manager, topicManager, TODAY, "1 c/CCA topic/Basketball Club").execute();

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

        parser.parse(manager, topicManager, TODAY, "1 c/CCA").execute();

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
                () -> parser.parse(manager, topicManager, TODAY, "1 topic/NeverCreated"));
    }

    @Test
    public void parseEdit_resultingOverlap_throwsDuplicateActivityExceptionAndDoesNotMutate() throws Exception {
        // Regression test: overlap/duplicate validation previously only ran inside
        // ActivityManager.replace(), which EditCommand.execute() calls - after the user has
        // already been shown the before/after diff and asked "Save changes? (y/n)". Moving the
        // same check into parseEdit (via the new checkNoConflicts() preflight) means a doomed
        // edit is rejected before the confirmation prompt is ever shown.
        ActivityManager manager = new ActivityManager();
        TopicManager topicManager = new TopicManager(manager);
        manager.add(new FixedActivity(manager.getNextId(), "Base", ActivityCategory.ACADEMIC,
                LocalDate.of(2026, 8, 20), LocalTime.of(10, 0), LocalTime.of(12, 0),
                EnergyRating.of(2), SensoryRating.of(2), null, null));
        manager.add(new FixedActivity(manager.getNextId(), "Contained", ActivityCategory.ACADEMIC,
                LocalDate.of(2026, 8, 20), LocalTime.of(13, 0), LocalTime.of(14, 0),
                EnergyRating.of(2), SensoryRating.of(2), null, null));

        assertThrows(DuplicateActivityException.class,
                () -> parser.parse(manager, topicManager, TODAY, "2 from/10:30 to/11:30"));

        Activity unchanged = manager.getById(2);
        assertEquals(LocalTime.of(13, 0), ((FixedActivity) unchanged).getStartTime());
    }

    @Test
    public void parseEdit_resultingExactDuplicate_throwsDuplicateActivityException() throws Exception {
        ActivityManager manager = new ActivityManager();
        TopicManager topicManager = new TopicManager(manager);
        manager.add(new FixedActivity(manager.getNextId(), "Lecture", ActivityCategory.ACADEMIC,
                LocalDate.of(2026, 8, 20), LocalTime.of(9, 0), LocalTime.of(10, 0),
                EnergyRating.of(2), SensoryRating.of(2), null, null));
        manager.add(new FixedActivity(manager.getNextId(), "Lecture", ActivityCategory.ACADEMIC,
                LocalDate.of(2026, 8, 21), LocalTime.of(9, 0), LocalTime.of(10, 0),
                EnergyRating.of(2), SensoryRating.of(2), null, null));

        assertThrows(DuplicateActivityException.class,
                () -> parser.parse(manager, topicManager, TODAY, "2 date/2026-08-20"));
    }

    @Test
    public void parseEdit_nonConflictingChange_stillSucceeds() throws Exception {
        ActivityManager manager = new ActivityManager();
        TopicManager topicManager = new TopicManager(manager);
        manager.add(new FixedActivity(manager.getNextId(), "Base", ActivityCategory.ACADEMIC,
                LocalDate.of(2026, 8, 20), LocalTime.of(10, 0), LocalTime.of(12, 0),
                EnergyRating.of(2), SensoryRating.of(2), null, null));

        parser.parse(manager, topicManager, TODAY, "1 from/09:00 to/11:00").execute();

        assertEquals(LocalTime.of(9, 0), ((FixedActivity) manager.getById(1)).getStartTime());
    }

    @Test
    public void parseEdit_multipleFields_updatesAllGivenFields() throws Exception {
        ActivityManager manager = new ActivityManager();
        TopicManager topicManager = new TopicManager(manager);
        manager.add(new FlexibleActivity(manager.getNextId(), "Prepare slides", ActivityCategory.ACADEMIC,
                LocalDate.of(2026, 8, 15), LocalTime.of(10, 0), LocalTime.of(18, 0), 90,
                EnergyRating.of(3), SensoryRating.of(3), null, null));

        parser.parse(manager, topicManager, TODAY, "1 n/New activity name energy/4 sensory/2").execute();

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

        parser.parse(manager, topicManager, TODAY, "1 note/Bring headphones").execute();

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

        parser.parse(manager, topicManager, TODAY, "1 dur/60").execute();

        assertTrue(manager.getById(1).isComplete());
    }

    @Test
    public void parseEdit_changingTypeWithAllNewTimingFields_succeeds() throws Exception {
        ActivityManager manager = new ActivityManager();
        TopicManager topicManager = new TopicManager(manager);
        manager.add(new FixedActivity(manager.getNextId(), "Lecture", ActivityCategory.ACADEMIC,
                LocalDate.of(2026, 8, 15), LocalTime.of(9, 0), LocalTime.of(11, 0),
                EnergyRating.of(4), SensoryRating.of(3), null, null));

        parser.parse(manager, topicManager, TODAY, "1 type/FLEXIBLE earliest/10:00 latest/18:00 dur/90").execute();

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

        assertThrows(InvalidActivityException.class, () -> parser.parse(manager, topicManager, TODAY, "1 dur/500"));
    }

    @Test
    public void parseEdit_changingTypeWithDurationExceedingNewWindow_throwsInvalidActivityException()
            throws Exception {
        ActivityManager manager = new ActivityManager();
        TopicManager topicManager = new TopicManager(manager);
        manager.add(new FixedActivity(manager.getNextId(), "Lecture", ActivityCategory.ACADEMIC,
                LocalDate.of(2026, 8, 15), LocalTime.of(9, 0), LocalTime.of(11, 0),
                EnergyRating.of(4), SensoryRating.of(3), null, null));

        assertThrows(InvalidActivityException.class, () -> parser.parse(manager, topicManager, TODAY,
                "1 type/FLEXIBLE earliest/10:00 latest/11:00 dur/500"));
    }

    @Test
    public void parseEdit_changingTypeWithoutNewTimingFields_throwsMissingInputException() throws Exception {
        ActivityManager manager = new ActivityManager();
        TopicManager topicManager = new TopicManager(manager);
        manager.add(new FixedActivity(manager.getNextId(), "Lecture", ActivityCategory.ACADEMIC,
                LocalDate.of(2026, 8, 15), LocalTime.of(9, 0), LocalTime.of(11, 0),
                EnergyRating.of(4), SensoryRating.of(3), null, null));

        assertThrows(MissingInputException.class,
                () -> parser.parse(manager, topicManager, TODAY, "1 type/FLEXIBLE"));
    }

    @Test
    public void parseEdit_noFieldsSupplied_throwsMissingInputException() throws Exception {
        ActivityManager manager = new ActivityManager();
        TopicManager topicManager = new TopicManager(manager);
        manager.add(new FixedActivity(manager.getNextId(), "Lecture", ActivityCategory.ACADEMIC,
                LocalDate.of(2026, 8, 15), LocalTime.of(9, 0), LocalTime.of(11, 0),
                EnergyRating.of(4), SensoryRating.of(3), null, null));

        assertThrows(MissingInputException.class, () -> parser.parse(manager, topicManager, TODAY, "1"));
    }

    @Test
    public void parseEdit_unknownId_throwsInvalidIndexException() {
        ActivityManager manager = new ActivityManager();
        TopicManager topicManager = new TopicManager(manager);

        assertThrows(InvalidIndexException.class, () -> parser.parse(manager, topicManager, TODAY, "999 dur/60"));
    }

    @Test
    public void parseEdit_nonNumericId_throwsInvalidCommandException() {
        ActivityManager manager = new ActivityManager();
        TopicManager topicManager = new TopicManager(manager);

        assertThrows(InvalidCommandException.class, () -> parser.parse(manager, topicManager, TODAY, "abc dur/60"));
    }

    @Test
    public void parseEdit_invalidNewCategory_throwsInvalidActivityException() throws Exception {
        ActivityManager manager = new ActivityManager();
        TopicManager topicManager = new TopicManager(manager);
        manager.add(new FixedActivity(manager.getNextId(), "Lecture", ActivityCategory.ACADEMIC,
                LocalDate.of(2026, 8, 15), LocalTime.of(9, 0), LocalTime.of(11, 0),
                EnergyRating.of(4), SensoryRating.of(3), null, null));

        assertThrows(InvalidActivityException.class, () -> parser.parse(manager, topicManager, TODAY, "1 c/BOGUS"));
    }
}
