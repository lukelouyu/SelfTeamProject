package seedu.unienable.parser.activity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

import org.junit.jupiter.api.Test;

import seedu.unienable.command.activity.crud.AddCommand;
import seedu.unienable.exception.DuplicateActivityException;
import seedu.unienable.exception.InvalidActivityException;
import seedu.unienable.exception.InvalidCommandException;
import seedu.unienable.exception.InvalidDateTimeException;
import seedu.unienable.exception.InvalidIndexException;
import seedu.unienable.exception.MissingInputException;
import seedu.unienable.logic.ActivityManager;
import seedu.unienable.logic.TopicManager;
import seedu.unienable.model.classes.Activity;
import seedu.unienable.model.classes.FixedActivity;
import seedu.unienable.model.classes.FlexibleActivity;
import seedu.unienable.model.enums.ActivityCategory;
import seedu.unienable.model.enums.ScheduleType;

class AddCommandParserTest {
    private static final LocalDateTime TODAY = LocalDate.of(2020, 1, 1).atStartOfDay();

    private final AddCommandParser parser = new AddCommandParser();

    @Test
    public void parseAdd_fixedActivity_buildsMatchingActivity() throws Exception {
        ActivityManager manager = new ActivityManager();
        TopicManager topicManager = new TopicManager(manager);
        topicManager.add(ActivityCategory.ACADEMIC, "CG3207");
        AddCommand command = parser.parse(manager, topicManager, TODAY,
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
        AddCommand command = parser.parse(manager, topicManager, TODAY,
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
        AddCommand command = parser.parse(manager, topicManager, TODAY,
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
        AddCommand command = parser.parse(manager, topicManager, TODAY,
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

        assertThrows(InvalidIndexException.class, () -> parser.parse(manager, topicManager, TODAY,
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

        assertThrows(InvalidIndexException.class, () -> parser.parse(manager, topicManager, TODAY,
                "n/Consultation c/OTHERS date/2026-08-15 type/FIXED from/09:00 to/10:00 "
                        + "energy/2 sensory/2 topic/Misc"));
    }

    @Test
    public void parseAdd_whitespaceOnlyTopic_isTreatedAsAbsent() throws Exception {
        // Regression test: "topic/   " (whitespace only) previously stored an empty string
        // instead of being treated the same as omitting topic/ entirely.
        ActivityManager manager = new ActivityManager();
        TopicManager topicManager = new TopicManager(manager);
        AddCommand command = parser.parse(manager, topicManager, TODAY,
                "n/Consultation c/OTHERS date/2026-08-15 type/FIXED from/09:00 to/10:00 "
                        + "energy/2 sensory/2 topic/    note/Bring headphones");

        command.execute();

        assertNull(manager.getById(1).getTopic());
    }

    @Test
    public void parseAdd_whitespaceOnlyNote_isTreatedAsAbsent() throws Exception {
        ActivityManager manager = new ActivityManager();
        TopicManager topicManager = new TopicManager(manager);
        AddCommand command = parser.parse(manager, topicManager, TODAY,
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

        assertThrows(MissingInputException.class, () -> parser.parse(manager, topicManager, TODAY,
                "n/    c/ACADEMIC date/2026-08-15 type/FIXED from/09:00 to/11:00 energy/4 sensory/3"));
    }

    @Test
    public void parseAdd_descriptionContainsDelimiter_throwsInvalidActivityException() {
        // Regression test: activities.txt uses '|' as its delimiter and cannot escape it, so a
        // description containing '|' was previously accepted here, reported as added, and then
        // permanently failed to persist on every later save instead of being rejected up front.
        ActivityManager manager = new ActivityManager();
        TopicManager topicManager = new TopicManager(manager);

        assertThrows(InvalidActivityException.class, () -> parser.parse(manager, topicManager, TODAY,
                "n/Bad|Desc c/ACADEMIC date/2026-08-15 type/FIXED from/09:00 to/11:00 energy/4 sensory/3"));
    }

    @Test
    public void parseAdd_topicContainsDelimiter_throwsInvalidActivityException() {
        ActivityManager manager = new ActivityManager();
        TopicManager topicManager = new TopicManager(manager);

        assertThrows(InvalidActivityException.class, () -> parser.parse(manager, topicManager, TODAY,
                "n/Lecture c/ACADEMIC date/2026-08-15 type/FIXED from/09:00 to/11:00 energy/4 sensory/3 "
                        + "topic/Bad|Topic"));
    }

    @Test
    public void parseAdd_noteContainsDelimiter_throwsInvalidActivityException() {
        ActivityManager manager = new ActivityManager();
        TopicManager topicManager = new TopicManager(manager);

        assertThrows(InvalidActivityException.class, () -> parser.parse(manager, topicManager, TODAY,
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
        AddCommand command = parser.parse(manager, topicManager, TODAY,
                "n/A n/B c/ACADEMIC date/2026-08-15 type/FIXED from/09:00 to/11:00 energy/4 sensory/3");

        command.execute();

        assertEquals("A n/B", manager.getById(1).getDescription());
    }

    @Test
    public void parseAdd_unrecognisedLeadingToken_throwsInvalidCommandExceptionAndConsumesNoId() {
        // Regression test for RC05 (v1.0 RC retest, 2026-08-01): text before the first recognised
        // marker was previously invisible to extraction and silently discarded, letting a typo'd
        // command mutate data anyway.
        ActivityManager manager = new ActivityManager();
        TopicManager topicManager = new TopicManager(manager);

        assertThrows(InvalidCommandException.class, () -> parser.parse(manager, topicManager, TODAY,
                "ignored/yes n/Test activity c/ACADEMIC date/2026-08-15 type/FIXED from/09:00 to/10:00 "
                        + "energy/2 sensory/2"));
        assertEquals(1, manager.getNextId());
        assertEquals(0, manager.size());
    }

    @Test
    public void parseAdd_unrecognisedTokenBetweenTypeAndTiming_throwsInvalidCommandException() {
        // Regression test for RC05: type/'s value previously took only its first word and
        // discarded everything else up to the next marker, so "type/FIXED ignored/again" silently
        // dropped "ignored/again" instead of rejecting it.
        ActivityManager manager = new ActivityManager();
        TopicManager topicManager = new TopicManager(manager);

        assertThrows(InvalidCommandException.class, () -> parser.parse(manager, topicManager, TODAY,
                "n/Test activity c/ACADEMIC date/2026-08-15 type/FIXED ignored/again from/09:00 to/10:00 "
                        + "energy/2 sensory/2"));
        assertEquals(0, manager.size());
    }

    @Test
    public void parseAdd_missingDescription_throwsMissingInputException() {
        ActivityManager manager = new ActivityManager();
        TopicManager topicManager = new TopicManager(manager);

        assertThrows(MissingInputException.class, () -> parser.parse(manager, topicManager, TODAY,
                "c/ACADEMIC date/2026-08-15 type/FIXED from/09:00 to/11:00 energy/4 sensory/3"));
    }

    @Test
    public void parseAdd_missingType_throwsMissingInputException() {
        ActivityManager manager = new ActivityManager();
        TopicManager topicManager = new TopicManager(manager);

        assertThrows(MissingInputException.class, () -> parser.parse(manager, topicManager, TODAY,
                "n/Lecture c/ACADEMIC date/2026-08-15"));
    }

    @Test
    public void parseAdd_missingDateMarker_throwsMissingInputExceptionNamingDateNotCategory() {
        // Regression test for BUG-04 (v1.0 manual release test, 2026-08-01, reproduction A): the
        // supplied category (ACADEMIC) is perfectly valid; date/ is the field that's actually
        // missing. Before the fix, c/'s extraction had no end marker to stop at, so it greedily
        // absorbed everything up to the end of input and was misreported as an invalid category.
        ActivityManager manager = new ActivityManager();
        TopicManager topicManager = new TopicManager(manager);

        MissingInputException exception = assertThrows(MissingInputException.class,
                () -> parser.parse(manager, topicManager, TODAY,
                        "n/Missing date c/ACADEMIC type/FIXED from/09:00 to/10:00 energy/3 sensory/3"));
        assertEquals("date is required.", exception.getMessage());
    }

    @Test
    public void parseAdd_missingToMarker_throwsMissingInputExceptionNamingToNotFrom() {
        // Regression test for BUG-04, reproduction B: the supplied from/09:00 is perfectly valid;
        // to/ is the field that's actually missing.
        ActivityManager manager = new ActivityManager();
        TopicManager topicManager = new TopicManager(manager);

        MissingInputException exception = assertThrows(MissingInputException.class,
                () -> parser.parse(manager, topicManager, TODAY,
                        "n/Missing end time c/ACADEMIC date/2026-08-02 type/FIXED from/09:00 "
                                + "energy/3 sensory/3"));
        assertEquals("to is required.", exception.getMessage());
    }

    @Test
    public void parseAdd_missingEnergyMarkerAfterTo_throwsMissingInputExceptionNamingEnergy() {
        ActivityManager manager = new ActivityManager();
        TopicManager topicManager = new TopicManager(manager);

        MissingInputException exception = assertThrows(MissingInputException.class,
                () -> parser.parse(manager, topicManager, TODAY,
                        "n/Missing energy c/ACADEMIC date/2026-08-02 type/FIXED from/09:00 to/10:00 sensory/3"));
        assertEquals("energy is required.", exception.getMessage());
    }

    @Test
    public void parseAdd_missingLatestMarker_throwsMissingInputExceptionNamingLatestNotEarliest() {
        ActivityManager manager = new ActivityManager();
        TopicManager topicManager = new TopicManager(manager);

        MissingInputException exception = assertThrows(MissingInputException.class,
                () -> parser.parse(manager, topicManager, TODAY,
                        "n/Missing latest c/ACADEMIC date/2026-08-03 type/FLEXIBLE earliest/09:00 dur/30 "
                                + "energy/3 sensory/3"));
        assertEquals("latest is required.", exception.getMessage());
    }

    @Test
    public void parseAdd_missingCMarkerEntirely_stillCorrectlyReportsCategoryMissing() {
        // Sanity check that the fix doesn't regress the already-correct case: when c/ is absent
        // entirely (not just its own end marker), the independent c/-lookup downstream already
        // finds nothing and reports "category is required." regardless of what n/'s value absorbs.
        ActivityManager manager = new ActivityManager();
        TopicManager topicManager = new TopicManager(manager);

        MissingInputException exception = assertThrows(MissingInputException.class,
                () -> parser.parse(manager, topicManager, TODAY,
                        "n/Missing category date/2026-08-15 type/FIXED from/09:00 to/10:00 "
                                + "energy/3 sensory/3"));
        assertEquals("category is required.", exception.getMessage());
    }

    @Test
    public void parseAdd_multipleRejectedAddsOfDifferentKinds_doNotConsumeIdsBeforeNextValidAdd()
            throws Exception {
        // Regression test for INVESTIGATION-01 (v1.0 manual release test, 2026-08-01): an earlier
        // manual session observed a non-contiguous ID gap, but a clean rerun could not reproduce
        // it and concluded ordinary rejected adds do not consume IDs - recorded as an
        // investigation item, not a confirmed defect, and the report explicitly says not to
        // change ID allocation based on the unreproduced observation alone. This locks in the
        // clean rerun's actual finding across every rejection kind it called out: a parser-level
        // rejection (malformed date), and two manager-level rejections (an overlap conflict and
        // an exact duplicate).
        ActivityManager manager = new ActivityManager();
        TopicManager topicManager = new TopicManager(manager);

        parser.parse(manager, topicManager, TODAY,
                "n/Base class c/ACADEMIC date/2026-08-15 type/FIXED from/09:00 to/10:00 "
                        + "energy/2 sensory/2").execute();
        assertEquals(2, manager.getNextId());

        // Parser-level rejection: thrown before AddCommand is even constructed.
        assertThrows(InvalidDateTimeException.class, () -> parser.parse(manager, topicManager, TODAY,
                "n/Bad date c/ACADEMIC date/15-08-2026 type/FIXED from/11:00 to/12:00 "
                        + "energy/2 sensory/2"));
        assertEquals(2, manager.getNextId());

        // Manager-level rejection: thrown from ActivityManager.add() during execute().
        assertThrows(DuplicateActivityException.class, () -> parser.parse(manager, topicManager, TODAY,
                "n/Overlapping class c/ACADEMIC date/2026-08-15 type/FIXED from/09:30 to/10:30 "
                        + "energy/2 sensory/2").execute());
        assertEquals(2, manager.getNextId());

        // Also manager-level: an exact duplicate of the base class.
        assertThrows(DuplicateActivityException.class, () -> parser.parse(manager, topicManager, TODAY,
                "n/Base class c/ACADEMIC date/2026-08-15 type/FIXED from/09:00 to/10:00 "
                        + "energy/2 sensory/2").execute());
        assertEquals(2, manager.getNextId());

        parser.parse(manager, topicManager, TODAY,
                "n/Class after rejections c/ACADEMIC date/2026-08-16 type/FIXED from/09:00 to/10:00 "
                        + "energy/2 sensory/2").execute();

        assertEquals(2, manager.size());
        assertEquals("Class after rejections", manager.getById(2).getDescription());
        assertEquals(3, manager.getNextId());
    }

    @Test
    public void parseAdd_invalidType_throwsInvalidCommandException() {
        ActivityManager manager = new ActivityManager();
        TopicManager topicManager = new TopicManager(manager);

        assertThrows(InvalidCommandException.class, () -> parser.parse(manager, topicManager, TODAY,
                "n/Lecture c/ACADEMIC date/2026-08-15 type/BOGUS from/09:00 to/11:00 energy/4 sensory/3"));
    }

    @Test
    public void parseAdd_endNotAfterStart_throwsInvalidActivityException() {
        ActivityManager manager = new ActivityManager();
        TopicManager topicManager = new TopicManager(manager);

        assertThrows(InvalidActivityException.class, () -> parser.parse(manager, topicManager, TODAY,
                "n/Lecture c/ACADEMIC date/2026-08-15 type/FIXED from/11:00 to/09:00 energy/4 sensory/3"));
    }

    @Test
    public void parseAdd_invalidCategory_throwsInvalidActivityException() {
        ActivityManager manager = new ActivityManager();
        TopicManager topicManager = new TopicManager(manager);

        assertThrows(InvalidActivityException.class, () -> parser.parse(manager, topicManager, TODAY,
                "n/Lecture c/BOGUS date/2026-08-15 type/FIXED from/09:00 to/11:00 energy/4 sensory/3"));
    }

    @Test
    public void parseAdd_invalidDate_throwsInvalidDateTimeException() {
        ActivityManager manager = new ActivityManager();
        TopicManager topicManager = new TopicManager(manager);

        assertThrows(InvalidDateTimeException.class, () -> parser.parse(manager, topicManager, TODAY,
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

        assertThrows(InvalidDateTimeException.class, () -> parser.parse(manager, topicManager, TODAY,
                "n/Exam c/ACADEMIC date/2026-02-30 type/FIXED from/09:00 to/10:00 energy/3 sensory/3"));

        assertEquals(idBefore, manager.getNextId());
        assertEquals(0, manager.size());
    }

    @Test
    public void parseAdd_pastDate_rejectedWithoutConsumingIdOrMutatingManager() {
        // Regression test for the v1.0 RC retest bug report: a valid, calendar-existent date that
        // is earlier than today must be rejected with a "date has passed" message, using the same
        // "reject before AddCommand is built" discipline as every other add validation failure.
        ActivityManager manager = new ActivityManager();
        TopicManager topicManager = new TopicManager(manager);
        LocalDateTime today = LocalDate.of(2026, 8, 1).atStartOfDay();
        int idBefore = manager.getNextId();

        InvalidDateTimeException exception = assertThrows(InvalidDateTimeException.class,
                () -> parser.parse(manager, topicManager, today,
                        "n/Old exam c/ACADEMIC date/2026-07-31 type/FIXED from/09:00 to/10:00 "
                                + "energy/3 sensory/3"));

        assertEquals("date has passed. Please enter a date from 2026-08-01 onwards.", exception.getMessage());
        assertEquals(idBefore, manager.getNextId());
        assertEquals(0, manager.size());
    }

    @Test
    public void parseAdd_today_isAccepted() throws Exception {
        ActivityManager manager = new ActivityManager();
        TopicManager topicManager = new TopicManager(manager);
        LocalDateTime today = LocalDate.of(2026, 8, 1).atStartOfDay();

        parser.parse(manager, topicManager, today,
                "n/Exam c/ACADEMIC date/2026-08-01 type/FIXED from/09:00 to/10:00 energy/3 sensory/3").execute();

        assertEquals(LocalDate.of(2026, 8, 1), manager.getById(1).getDate());
    }

    @Test
    public void parseAdd_futureDate_isAccepted() throws Exception {
        ActivityManager manager = new ActivityManager();
        TopicManager topicManager = new TopicManager(manager);
        LocalDateTime today = LocalDate.of(2026, 8, 1).atStartOfDay();

        parser.parse(manager, topicManager, today,
                "n/Exam c/ACADEMIC date/2026-08-02 type/FIXED from/09:00 to/10:00 energy/3 sensory/3").execute();

        assertEquals(LocalDate.of(2026, 8, 2), manager.getById(1).getDate());
    }

    @Test
    public void parseAdd_futureLeapDate_isAccepted() throws Exception {
        ActivityManager manager = new ActivityManager();
        TopicManager topicManager = new TopicManager(manager);
        LocalDateTime today = LocalDate.of(2026, 8, 1).atStartOfDay();

        parser.parse(manager, topicManager, today,
                "n/Leap day event c/ACADEMIC date/2028-02-29 type/FIXED from/09:00 to/10:00 "
                        + "energy/3 sensory/3").execute();

        assertEquals(LocalDate.of(2028, 2, 29), manager.getById(1).getDate());
    }

    @Test
    public void parseAdd_todayEndTimeAlreadyPassed_throwsInvalidDateTimeException() {
        // BUG-02 (v1.0 manual release test, 2026-08-01): an activity scheduled for today must be
        // rejected once its scheduled time has fully passed, not just when the date itself is
        // before today.
        ActivityManager manager = new ActivityManager();
        TopicManager topicManager = new TopicManager(manager);
        LocalDateTime now = LocalDateTime.of(2026, 8, 1, 15, 40);
        int idBefore = manager.getNextId();

        InvalidDateTimeException exception = assertThrows(InvalidDateTimeException.class,
                () -> parser.parse(manager, topicManager, now,
                        "n/Today date c/ACADEMIC date/2026-08-01 type/FIXED from/09:00 to/10:00 "
                                + "energy/3 sensory/3"));

        assertTrue(exception.getMessage().contains("start time has passed"));
        assertEquals(idBefore, manager.getNextId());
        assertEquals(0, manager.size());
    }

    @Test
    public void parseAdd_todayEndTimeExactlyNow_throwsInvalidDateTimeException() {
        ActivityManager manager = new ActivityManager();
        TopicManager topicManager = new TopicManager(manager);
        LocalDateTime now = LocalDateTime.of(2026, 8, 1, 15, 40);

        assertThrows(InvalidDateTimeException.class, () -> parser.parse(manager, topicManager, now,
                "n/Today date c/ACADEMIC date/2026-08-01 type/FIXED from/15:00 to/15:40 "
                        + "energy/3 sensory/3"));
    }

    @Test
    public void parseAdd_todayStartsInFuture_isAccepted() throws Exception {
        ActivityManager manager = new ActivityManager();
        TopicManager topicManager = new TopicManager(manager);
        LocalDateTime now = LocalDateTime.of(2026, 8, 1, 15, 40);

        parser.parse(manager, topicManager, now,
                "n/Today date c/ACADEMIC date/2026-08-01 type/FIXED from/16:00 to/17:00 "
                        + "energy/3 sensory/3").execute();

        assertEquals(1, manager.size());
    }

    @Test
    public void parseAdd_futureDateWithEarlyTime_isAccepted() throws Exception {
        // A time value that would be "already passed" on today's date must still be accepted
        // when the activity's own date is genuinely in the future.
        ActivityManager manager = new ActivityManager();
        TopicManager topicManager = new TopicManager(manager);
        LocalDateTime now = LocalDateTime.of(2026, 8, 1, 15, 40);

        parser.parse(manager, topicManager, now,
                "n/Tomorrow early c/ACADEMIC date/2026-08-02 type/FIXED from/09:00 to/10:00 "
                        + "energy/3 sensory/3").execute();

        assertEquals(1, manager.size());
    }

    @Test
    public void parseAdd_todayStartTimeBeforeNow_throwsInvalidDateTimeExceptionWithoutConsumingId() {
        // BUG-03: a start time at or before now must be rejected even when the end time has not
        // itself passed.
        ActivityManager manager = new ActivityManager();
        TopicManager topicManager = new TopicManager(manager);
        LocalDateTime now = LocalDateTime.of(2026, 8, 1, 16, 0);
        int idBefore = manager.getNextId();

        InvalidDateTimeException exception = assertThrows(InvalidDateTimeException.class,
                () -> parser.parse(manager, topicManager, now,
                        "n/Already started c/ACADEMIC date/2026-08-01 type/FIXED from/15:59 to/18:00 "
                                + "energy/3 sensory/3"));

        assertEquals("activity start time has passed. Please enter a start time after 16:00.",
                exception.getMessage());
        assertEquals(idBefore, manager.getNextId());
        assertEquals(0, manager.size());
    }

    @Test
    public void parseAdd_todayStartTimeExactlyNow_throwsInvalidDateTimeException() {
        ActivityManager manager = new ActivityManager();
        TopicManager topicManager = new TopicManager(manager);
        LocalDateTime now = LocalDateTime.of(2026, 8, 1, 16, 0);

        assertThrows(InvalidDateTimeException.class, () -> parser.parse(manager, topicManager, now,
                "n/Starts now c/ACADEMIC date/2026-08-01 type/FIXED from/16:00 to/17:00 "
                        + "energy/3 sensory/3"));
    }

    @Test
    public void parseAdd_todayStartTimeJustAfterNow_isAccepted() throws Exception {
        ActivityManager manager = new ActivityManager();
        TopicManager topicManager = new TopicManager(manager);
        LocalDateTime now = LocalDateTime.of(2026, 8, 1, 16, 0);

        parser.parse(manager, topicManager, now,
                "n/Starts soon c/ACADEMIC date/2026-08-01 type/FIXED from/16:01 to/17:00 "
                        + "energy/3 sensory/3").execute();

        assertEquals(1, manager.size());
    }

    @Test
    public void parseAdd_flexibleTodayEarliestAtOrBeforeNow_throwsInvalidDateTimeException() {
        // The same start-time rule applies to a flexible activity's earliest/ field.
        ActivityManager manager = new ActivityManager();
        TopicManager topicManager = new TopicManager(manager);
        LocalDateTime now = LocalDateTime.of(2026, 8, 1, 16, 0);

        assertThrows(InvalidDateTimeException.class, () -> parser.parse(manager, topicManager, now,
                "n/Flexible today c/ACADEMIC date/2026-08-01 type/FLEXIBLE earliest/15:00 latest/18:00 "
                        + "dur/60 energy/3 sensory/3"));
    }

    @Test
    public void parseAdd_flexibleTodayEarliestAfterNow_isAccepted() throws Exception {
        ActivityManager manager = new ActivityManager();
        TopicManager topicManager = new TopicManager(manager);
        LocalDateTime now = LocalDateTime.of(2026, 8, 1, 16, 0);

        parser.parse(manager, topicManager, now,
                "n/Flexible today c/ACADEMIC date/2026-08-01 type/FLEXIBLE earliest/16:30 latest/18:00 "
                        + "dur/60 energy/3 sensory/3").execute();

        assertEquals(1, manager.size());
    }

    @Test
    public void parseAdd_hourTwentyFour_throwsInvalidDateTimeException() {
        ActivityManager manager = new ActivityManager();
        TopicManager topicManager = new TopicManager(manager);

        assertThrows(InvalidDateTimeException.class, () -> parser.parse(manager, topicManager, TODAY,
                "n/Late c/ACADEMIC date/2026-08-20 type/FIXED from/24:00 to/01:00 energy/3 sensory/3"));
    }

    @Test
    public void parseAdd_invalidEnergyRating_throwsInvalidActivityException() {
        ActivityManager manager = new ActivityManager();
        TopicManager topicManager = new TopicManager(manager);

        assertThrows(InvalidActivityException.class, () -> parser.parse(manager, topicManager, TODAY,
                "n/Lecture c/ACADEMIC date/2026-08-15 type/FIXED from/09:00 to/11:00 energy/7 sensory/3"));
    }

    @Test
    public void parseAdd_flexibleInvalidDuration_throwsInvalidActivityException() {
        ActivityManager manager = new ActivityManager();
        TopicManager topicManager = new TopicManager(manager);

        assertThrows(InvalidActivityException.class, () -> parser.parse(manager, topicManager, TODAY,
                "n/Task c/ACADEMIC date/2026-08-15 type/FLEXIBLE earliest/10:00 latest/18:00 "
                        + "dur/0 energy/5 sensory/2"));
    }

    @Test
    public void parseAdd_flexibleNegativeDuration_throwsInvalidActivityException() {
        ActivityManager manager = new ActivityManager();
        TopicManager topicManager = new TopicManager(manager);

        assertThrows(InvalidActivityException.class, () -> parser.parse(manager, topicManager, TODAY,
                "n/Task c/ACADEMIC date/2026-08-15 type/FLEXIBLE earliest/10:00 latest/18:00 "
                        + "dur/-30 energy/5 sensory/2"));
    }

    @Test
    public void parseAdd_flexibleDurationExceedsWindow_throwsInvalidActivityException() throws Exception {
        // Regression test: earliest/10:00 latest/11:00 is a 60-minute window, but dur/500 was
        // previously accepted with no validation at all against the window size.
        ActivityManager manager = new ActivityManager();
        TopicManager topicManager = new TopicManager(manager);

        InvalidActivityException exception = assertThrows(InvalidActivityException.class, () -> parser.parse(
                manager, topicManager, TODAY,
                "n/Task c/ACADEMIC date/2026-08-15 type/FLEXIBLE earliest/10:00 latest/11:00 "
                        + "dur/500 energy/5 sensory/2"));
        assertTrue(exception.getMessage().contains("60 min available"));
    }

    @Test
    public void parseAdd_flexibleDurationExactlyFillsWindow_succeeds() throws Exception {
        // Boundary: duration equal to the window size is the edge of "must fit inside the
        // window" and should be accepted, not rejected.
        ActivityManager manager = new ActivityManager();
        TopicManager topicManager = new TopicManager(manager);
        AddCommand command = parser.parse(manager, topicManager, TODAY,
                "n/Task c/ACADEMIC date/2026-08-15 type/FLEXIBLE earliest/10:00 latest/11:00 "
                        + "dur/60 energy/5 sensory/2");

        command.execute();

        assertEquals(60, ((FlexibleActivity) manager.getById(1)).getDurationMinutes());
    }

    @Test
    public void parseAdd_flexibleMissingDurationEntirely_throwsMissingInputExceptionNamingDur() {
        // Regression test for BUG-04 (v1.0 manual release test, 2026-08-01, reproduction C): dur/
        // is dropped entirely. Before the fix, latest/'s end marker ("dur/") was never found, so
        // its extraction greedily captured the trailing "energy/5 sensory/2" text, which then
        // failed time parsing and misreported "latest" as invalid - even though the supplied
        // latest/18:00 value was perfectly valid and the real problem was the missing dur/.
        ActivityManager manager = new ActivityManager();
        TopicManager topicManager = new TopicManager(manager);

        MissingInputException exception = assertThrows(MissingInputException.class,
                () -> parser.parse(manager, topicManager, TODAY,
                        "n/Task c/ACADEMIC date/2026-08-15 type/FLEXIBLE earliest/10:00 latest/18:00 "
                                + "energy/5 sensory/2"));
        assertEquals("dur is required.", exception.getMessage());
    }

}
