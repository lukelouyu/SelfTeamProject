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
import seedu.unienable.model.enums.ActivityOrder;
import seedu.unienable.model.enums.ScheduleType;

class ActivityCommandParserTest {
    private static final LocalDateTime NOW = LocalDateTime.of(2026, 8, 15, 10, 0);
    // Deliberately far earlier than every date literal used in this file's existing tests, so
    // adding the new not-before-today/not-before-now checks to parseAdd/parseEdit doesn't require
    // touching them. Midnight of that date, not just the date, since parseAdd/parseEdit now also
    // reject a same-day start time at or before "now" - midnight is always earlier than the
    // daytime from/earliest values these existing tests use.
    private static final LocalDateTime TODAY = LocalDate.of(2020, 1, 1).atStartOfDay();

    private final ActivityCommandParser parser = new ActivityCommandParser();

    @Test
    public void parseAdd_fixedActivity_buildsMatchingActivity() throws Exception {
        ActivityManager manager = new ActivityManager();
        TopicManager topicManager = new TopicManager(manager);
        topicManager.add(ActivityCategory.ACADEMIC, "CG3207");
        AddCommand command = parser.parseAdd(manager, topicManager, TODAY,
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
        AddCommand command = parser.parseAdd(manager, topicManager, TODAY,
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
        AddCommand command = parser.parseAdd(manager, topicManager, TODAY,
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
        AddCommand command = parser.parseAdd(manager, topicManager, TODAY,
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

        assertThrows(InvalidIndexException.class, () -> parser.parseAdd(manager, topicManager, TODAY,
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

        assertThrows(InvalidIndexException.class, () -> parser.parseAdd(manager, topicManager, TODAY,
                "n/Consultation c/OTHERS date/2026-08-15 type/FIXED from/09:00 to/10:00 "
                        + "energy/2 sensory/2 topic/Misc"));
    }

    @Test
    public void parseAdd_whitespaceOnlyTopic_isTreatedAsAbsent() throws Exception {
        // Regression test: "topic/   " (whitespace only) previously stored an empty string
        // instead of being treated the same as omitting topic/ entirely.
        ActivityManager manager = new ActivityManager();
        TopicManager topicManager = new TopicManager(manager);
        AddCommand command = parser.parseAdd(manager, topicManager, TODAY,
                "n/Consultation c/OTHERS date/2026-08-15 type/FIXED from/09:00 to/10:00 "
                        + "energy/2 sensory/2 topic/    note/Bring headphones");

        command.execute();

        assertNull(manager.getById(1).getTopic());
    }

    @Test
    public void parseAdd_whitespaceOnlyNote_isTreatedAsAbsent() throws Exception {
        ActivityManager manager = new ActivityManager();
        TopicManager topicManager = new TopicManager(manager);
        AddCommand command = parser.parseAdd(manager, topicManager, TODAY,
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

        assertThrows(MissingInputException.class, () -> parser.parseAdd(manager, topicManager, TODAY,
                "n/    c/ACADEMIC date/2026-08-15 type/FIXED from/09:00 to/11:00 energy/4 sensory/3"));
    }

    @Test
    public void parseAdd_descriptionContainsDelimiter_throwsInvalidActivityException() {
        // Regression test: activities.txt uses '|' as its delimiter and cannot escape it, so a
        // description containing '|' was previously accepted here, reported as added, and then
        // permanently failed to persist on every later save instead of being rejected up front.
        ActivityManager manager = new ActivityManager();
        TopicManager topicManager = new TopicManager(manager);

        assertThrows(InvalidActivityException.class, () -> parser.parseAdd(manager, topicManager, TODAY,
                "n/Bad|Desc c/ACADEMIC date/2026-08-15 type/FIXED from/09:00 to/11:00 energy/4 sensory/3"));
    }

    @Test
    public void parseAdd_topicContainsDelimiter_throwsInvalidActivityException() {
        ActivityManager manager = new ActivityManager();
        TopicManager topicManager = new TopicManager(manager);

        assertThrows(InvalidActivityException.class, () -> parser.parseAdd(manager, topicManager, TODAY,
                "n/Lecture c/ACADEMIC date/2026-08-15 type/FIXED from/09:00 to/11:00 energy/4 sensory/3 "
                        + "topic/Bad|Topic"));
    }

    @Test
    public void parseAdd_noteContainsDelimiter_throwsInvalidActivityException() {
        ActivityManager manager = new ActivityManager();
        TopicManager topicManager = new TopicManager(manager);

        assertThrows(InvalidActivityException.class, () -> parser.parseAdd(manager, topicManager, TODAY,
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
        AddCommand command = parser.parseAdd(manager, topicManager, TODAY,
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

        assertThrows(InvalidCommandException.class, () -> parser.parseAdd(manager, topicManager, TODAY,
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

        assertThrows(InvalidCommandException.class, () -> parser.parseAdd(manager, topicManager, TODAY,
                "n/Test activity c/ACADEMIC date/2026-08-15 type/FIXED ignored/again from/09:00 to/10:00 "
                        + "energy/2 sensory/2"));
        assertEquals(0, manager.size());
    }

    @Test
    public void parseAdd_missingDescription_throwsMissingInputException() {
        ActivityManager manager = new ActivityManager();
        TopicManager topicManager = new TopicManager(manager);

        assertThrows(MissingInputException.class, () -> parser.parseAdd(manager, topicManager, TODAY,
                "c/ACADEMIC date/2026-08-15 type/FIXED from/09:00 to/11:00 energy/4 sensory/3"));
    }

    @Test
    public void parseAdd_missingType_throwsMissingInputException() {
        ActivityManager manager = new ActivityManager();
        TopicManager topicManager = new TopicManager(manager);

        assertThrows(MissingInputException.class, () -> parser.parseAdd(manager, topicManager, TODAY,
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
                () -> parser.parseAdd(manager, topicManager, TODAY,
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
                () -> parser.parseAdd(manager, topicManager, TODAY,
                        "n/Missing end time c/ACADEMIC date/2026-08-02 type/FIXED from/09:00 "
                                + "energy/3 sensory/3"));
        assertEquals("to is required.", exception.getMessage());
    }

    @Test
    public void parseAdd_missingEnergyMarkerAfterTo_throwsMissingInputExceptionNamingEnergy() {
        ActivityManager manager = new ActivityManager();
        TopicManager topicManager = new TopicManager(manager);

        MissingInputException exception = assertThrows(MissingInputException.class,
                () -> parser.parseAdd(manager, topicManager, TODAY,
                        "n/Missing energy c/ACADEMIC date/2026-08-02 type/FIXED from/09:00 to/10:00 sensory/3"));
        assertEquals("energy is required.", exception.getMessage());
    }

    @Test
    public void parseAdd_missingLatestMarker_throwsMissingInputExceptionNamingLatestNotEarliest() {
        ActivityManager manager = new ActivityManager();
        TopicManager topicManager = new TopicManager(manager);

        MissingInputException exception = assertThrows(MissingInputException.class,
                () -> parser.parseAdd(manager, topicManager, TODAY,
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
                () -> parser.parseAdd(manager, topicManager, TODAY,
                        "n/Missing category date/2026-08-15 type/FIXED from/09:00 to/10:00 "
                                + "energy/3 sensory/3"));
        assertEquals("category is required.", exception.getMessage());
    }

    @Test
    public void parseAdd_invalidType_throwsInvalidCommandException() {
        ActivityManager manager = new ActivityManager();
        TopicManager topicManager = new TopicManager(manager);

        assertThrows(InvalidCommandException.class, () -> parser.parseAdd(manager, topicManager, TODAY,
                "n/Lecture c/ACADEMIC date/2026-08-15 type/BOGUS from/09:00 to/11:00 energy/4 sensory/3"));
    }

    @Test
    public void parseAdd_endNotAfterStart_throwsInvalidActivityException() {
        ActivityManager manager = new ActivityManager();
        TopicManager topicManager = new TopicManager(manager);

        assertThrows(InvalidActivityException.class, () -> parser.parseAdd(manager, topicManager, TODAY,
                "n/Lecture c/ACADEMIC date/2026-08-15 type/FIXED from/11:00 to/09:00 energy/4 sensory/3"));
    }

    @Test
    public void parseAdd_invalidCategory_throwsInvalidActivityException() {
        ActivityManager manager = new ActivityManager();
        TopicManager topicManager = new TopicManager(manager);

        assertThrows(InvalidActivityException.class, () -> parser.parseAdd(manager, topicManager, TODAY,
                "n/Lecture c/BOGUS date/2026-08-15 type/FIXED from/09:00 to/11:00 energy/4 sensory/3"));
    }

    @Test
    public void parseAdd_invalidDate_throwsInvalidDateTimeException() {
        ActivityManager manager = new ActivityManager();
        TopicManager topicManager = new TopicManager(manager);

        assertThrows(InvalidDateTimeException.class, () -> parser.parseAdd(manager, topicManager, TODAY,
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

        assertThrows(InvalidDateTimeException.class, () -> parser.parseAdd(manager, topicManager, TODAY,
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
                () -> parser.parseAdd(manager, topicManager, today,
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

        parser.parseAdd(manager, topicManager, today,
                "n/Exam c/ACADEMIC date/2026-08-01 type/FIXED from/09:00 to/10:00 energy/3 sensory/3").execute();

        assertEquals(LocalDate.of(2026, 8, 1), manager.getById(1).getDate());
    }

    @Test
    public void parseAdd_futureDate_isAccepted() throws Exception {
        ActivityManager manager = new ActivityManager();
        TopicManager topicManager = new TopicManager(manager);
        LocalDateTime today = LocalDate.of(2026, 8, 1).atStartOfDay();

        parser.parseAdd(manager, topicManager, today,
                "n/Exam c/ACADEMIC date/2026-08-02 type/FIXED from/09:00 to/10:00 energy/3 sensory/3").execute();

        assertEquals(LocalDate.of(2026, 8, 2), manager.getById(1).getDate());
    }

    @Test
    public void parseAdd_futureLeapDate_isAccepted() throws Exception {
        ActivityManager manager = new ActivityManager();
        TopicManager topicManager = new TopicManager(manager);
        LocalDateTime today = LocalDate.of(2026, 8, 1).atStartOfDay();

        parser.parseAdd(manager, topicManager, today,
                "n/Leap day event c/ACADEMIC date/2028-02-29 type/FIXED from/09:00 to/10:00 "
                        + "energy/3 sensory/3").execute();

        assertEquals(LocalDate.of(2028, 2, 29), manager.getById(1).getDate());
    }

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
                () -> parser.parseEdit(manager, topicManager, today, "1 date/2026-07-31"));

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

        parser.parseEdit(manager, topicManager, today, "1 energy/5").execute();

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

        parser.parseEdit(manager, topicManager, today, "1 date/2026-08-01").execute();

        assertEquals(LocalDate.of(2026, 8, 1), manager.getById(1).getDate());
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
                () -> parser.parseAdd(manager, topicManager, now,
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

        assertThrows(InvalidDateTimeException.class, () -> parser.parseAdd(manager, topicManager, now,
                "n/Today date c/ACADEMIC date/2026-08-01 type/FIXED from/15:00 to/15:40 "
                        + "energy/3 sensory/3"));
    }

    @Test
    public void parseAdd_todayStartsInFuture_isAccepted() throws Exception {
        ActivityManager manager = new ActivityManager();
        TopicManager topicManager = new TopicManager(manager);
        LocalDateTime now = LocalDateTime.of(2026, 8, 1, 15, 40);

        parser.parseAdd(manager, topicManager, now,
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

        parser.parseAdd(manager, topicManager, now,
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
                () -> parser.parseAdd(manager, topicManager, now,
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

        assertThrows(InvalidDateTimeException.class, () -> parser.parseAdd(manager, topicManager, now,
                "n/Starts now c/ACADEMIC date/2026-08-01 type/FIXED from/16:00 to/17:00 "
                        + "energy/3 sensory/3"));
    }

    @Test
    public void parseAdd_todayStartTimeJustAfterNow_isAccepted() throws Exception {
        ActivityManager manager = new ActivityManager();
        TopicManager topicManager = new TopicManager(manager);
        LocalDateTime now = LocalDateTime.of(2026, 8, 1, 16, 0);

        parser.parseAdd(manager, topicManager, now,
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

        assertThrows(InvalidDateTimeException.class, () -> parser.parseAdd(manager, topicManager, now,
                "n/Flexible today c/ACADEMIC date/2026-08-01 type/FLEXIBLE earliest/15:00 latest/18:00 "
                        + "dur/60 energy/3 sensory/3"));
    }

    @Test
    public void parseAdd_flexibleTodayEarliestAfterNow_isAccepted() throws Exception {
        ActivityManager manager = new ActivityManager();
        TopicManager topicManager = new TopicManager(manager);
        LocalDateTime now = LocalDateTime.of(2026, 8, 1, 16, 0);

        parser.parseAdd(manager, topicManager, now,
                "n/Flexible today c/ACADEMIC date/2026-08-01 type/FLEXIBLE earliest/16:30 latest/18:00 "
                        + "dur/60 energy/3 sensory/3").execute();

        assertEquals(1, manager.size());
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
                () -> parser.parseEdit(manager, topicManager, now, "1 date/2026-08-01 from/11:30 to/12:00"));

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

        parser.parseEdit(manager, topicManager, now, "1 date/2026-08-01 from/16:30 to/17:30").execute();

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
                () -> parser.parseEdit(manager, topicManager, now, "1 from/15:00 to/18:00"));
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

        parser.parseEdit(manager, topicManager, now, "1 energy/5").execute();

        assertEquals(5, manager.getById(1).getEnergyRating().getValue());
        assertEquals(LocalTime.of(9, 0), ((FixedActivity) manager.getById(1)).getStartTime());
    }

    @Test
    public void parseAdd_hourTwentyFour_throwsInvalidDateTimeException() {
        ActivityManager manager = new ActivityManager();
        TopicManager topicManager = new TopicManager(manager);

        assertThrows(InvalidDateTimeException.class, () -> parser.parseAdd(manager, topicManager, TODAY,
                "n/Late c/ACADEMIC date/2026-08-20 type/FIXED from/24:00 to/01:00 energy/3 sensory/3"));
    }

    @Test
    public void parseAdd_invalidEnergyRating_throwsInvalidActivityException() {
        ActivityManager manager = new ActivityManager();
        TopicManager topicManager = new TopicManager(manager);

        assertThrows(InvalidActivityException.class, () -> parser.parseAdd(manager, topicManager, TODAY,
                "n/Lecture c/ACADEMIC date/2026-08-15 type/FIXED from/09:00 to/11:00 energy/7 sensory/3"));
    }

    @Test
    public void parseAdd_flexibleInvalidDuration_throwsInvalidActivityException() {
        ActivityManager manager = new ActivityManager();
        TopicManager topicManager = new TopicManager(manager);

        assertThrows(InvalidActivityException.class, () -> parser.parseAdd(manager, topicManager, TODAY,
                "n/Task c/ACADEMIC date/2026-08-15 type/FLEXIBLE earliest/10:00 latest/18:00 "
                        + "dur/0 energy/5 sensory/2"));
    }

    @Test
    public void parseAdd_flexibleNegativeDuration_throwsInvalidActivityException() {
        ActivityManager manager = new ActivityManager();
        TopicManager topicManager = new TopicManager(manager);

        assertThrows(InvalidActivityException.class, () -> parser.parseAdd(manager, topicManager, TODAY,
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
        AddCommand command = parser.parseAdd(manager, topicManager, TODAY,
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
                () -> parser.parseAdd(manager, topicManager, TODAY,
                        "n/Task c/ACADEMIC date/2026-08-15 type/FLEXIBLE earliest/10:00 latest/18:00 "
                                + "energy/5 sensory/2"));
        assertEquals("dur is required.", exception.getMessage());
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

        CommandResult result = parser.parseList(manager, NOW, "").execute();

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

        CommandResult result = parser.parseList(manager, NOW, "status/incomplete").execute();

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

        CommandResult result = parser.parseList(manager, NOW, "view/detail").execute();

        assertTrue(result.getFeedback().contains("Status: Incomplete | Type: FIXED"));
    }

    @Test
    public void parseList_unrecognisedViewValue_throwsInvalidCommandException() {
        // Regression test: view/ compared its value only against "detail" (via equalsIgnoreCase)
        // and silently treated everything else as concise, so a typo like "view/nonsense" was
        // never rejected.
        ActivityManager manager = new ActivityManager();

        assertThrows(InvalidCommandException.class, () -> parser.parseList(manager, NOW, "view/nonsense"));
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

        CommandResult result = parser.parseList(manager, NOW, "c/ACADEMIC topic/CG3207").execute();

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

        CommandResult result = parser.parseList(manager, NOW, "topic/CG3207").execute();

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

        CommandResult result = parser.parseList(manager, NOW, "topic/   ").execute();

        assertTrue(result.getFeedback().contains("No-topic lecture"));
    }

    @Test
    public void parseList_invalidStatus_throwsInvalidCommandException() {
        ActivityManager manager = new ActivityManager();
        TopicManager topicManager = new TopicManager(manager);

        assertThrows(InvalidCommandException.class, () -> parser.parseList(manager, NOW, "status/bogus"));
    }

    @Test
    public void parseList_invalidOrder_throwsInvalidCommandException() {
        ActivityManager manager = new ActivityManager();
        TopicManager topicManager = new TopicManager(manager);

        assertThrows(InvalidCommandException.class, () -> parser.parseList(manager, NOW, "order/bogus"));
    }

    @Test
    public void parseList_today_matchesOnlyTodaysActivities() throws Exception {
        ActivityManager manager = new ActivityManager();
        LocalDate today = NOW.toLocalDate();
        manager.add(new FixedActivity(manager.getNextId(), "Today's lecture", ActivityCategory.ACADEMIC,
                today, LocalTime.of(9, 0), LocalTime.of(10, 0), EnergyRating.of(4), SensoryRating.of(3), null, null));
        manager.add(new FixedActivity(manager.getNextId(), "Tomorrow's lecture", ActivityCategory.ACADEMIC,
                today.plusDays(1), LocalTime.of(9, 0), LocalTime.of(10, 0),
                EnergyRating.of(4), SensoryRating.of(3), null, null));

        String feedback = parser.parseList(manager, NOW, "today").execute().getFeedback();

        assertTrue(feedback.contains("Today's lecture"));
        assertTrue(!feedback.contains("Tomorrow's lecture"));
    }

    @Test
    public void parseList_todayIsCaseInsensitive_matchesTodaysActivities() throws Exception {
        ActivityManager manager = new ActivityManager();
        LocalDate today = NOW.toLocalDate();
        manager.add(new FixedActivity(manager.getNextId(), "Today's lecture", ActivityCategory.ACADEMIC,
                today, LocalTime.of(9, 0), LocalTime.of(10, 0), EnergyRating.of(4), SensoryRating.of(3), null, null));

        String feedback = parser.parseList(manager, NOW, "TODAY").execute().getFeedback();

        assertTrue(feedback.contains("Today's lecture"));
    }

    @Test
    public void parseList_tomorrow_matchesOnlyTomorrowsActivities() throws Exception {
        ActivityManager manager = new ActivityManager();
        LocalDate today = NOW.toLocalDate();
        manager.add(new FixedActivity(manager.getNextId(), "Today's lecture", ActivityCategory.ACADEMIC,
                today, LocalTime.of(9, 0), LocalTime.of(10, 0), EnergyRating.of(4), SensoryRating.of(3), null, null));
        manager.add(new FixedActivity(manager.getNextId(), "Tomorrow's lecture", ActivityCategory.ACADEMIC,
                today.plusDays(1), LocalTime.of(9, 0), LocalTime.of(10, 0),
                EnergyRating.of(4), SensoryRating.of(3), null, null));

        String feedback = parser.parseList(manager, NOW, "tomorrow").execute().getFeedback();

        assertTrue(feedback.contains("Tomorrow's lecture"));
        assertTrue(!feedback.contains("Today's lecture"));
    }

    @Test
    public void parseList_tomorrowCombinedWithViewDetail_appliesBothFilterAndView() throws Exception {
        ActivityManager manager = new ActivityManager();
        LocalDate today = NOW.toLocalDate();
        manager.add(new FixedActivity(manager.getNextId(), "Tomorrow's lecture", ActivityCategory.ACADEMIC,
                today.plusDays(1), LocalTime.of(9, 0), LocalTime.of(10, 0),
                EnergyRating.of(4), SensoryRating.of(3), null, null));

        String feedback = parser.parseList(manager, NOW, "tomorrow view/detail").execute().getFeedback();

        assertTrue(feedback.contains("Tomorrow's lecture"));
        assertTrue(feedback.contains("Status: Incomplete | Type: FIXED"));
    }

    @Test
    public void parseList_thisWeekMondayNow_matchesMondayThroughSunday() throws Exception {
        LocalDateTime mondayNow = LocalDateTime.of(2026, 8, 17, 10, 0); // a Monday
        LocalDate monday = mondayNow.toLocalDate();
        LocalDate sunday = monday.plusDays(6);

        ActivityManager manager = new ActivityManager();
        manager.add(new FixedActivity(manager.getNextId(), "Monday activity", ActivityCategory.ACADEMIC,
                monday, LocalTime.of(9, 0), LocalTime.of(10, 0), EnergyRating.of(4), SensoryRating.of(3),
                null, null));
        manager.add(new FixedActivity(manager.getNextId(), "Sunday activity", ActivityCategory.ACADEMIC,
                sunday, LocalTime.of(9, 0), LocalTime.of(10, 0), EnergyRating.of(4), SensoryRating.of(3),
                null, null));
        manager.add(new FixedActivity(manager.getNextId(), "Next Monday activity", ActivityCategory.ACADEMIC,
                sunday.plusDays(1), LocalTime.of(9, 0), LocalTime.of(10, 0),
                EnergyRating.of(4), SensoryRating.of(3), null, null));

        String feedback = parser.parseList(manager, mondayNow, "this week").execute().getFeedback();

        assertTrue(feedback.contains("Monday activity"));
        assertTrue(feedback.contains("Sunday activity"));
        assertTrue(!feedback.contains("Next Monday activity"));
    }

    @Test
    public void parseList_thisWeekSundayNow_stillMatchesSameWeek() throws Exception {
        LocalDateTime sundayNow = LocalDateTime.of(2026, 8, 23, 10, 0); // a Sunday
        LocalDate sunday = sundayNow.toLocalDate();
        LocalDate monday = sunday.minusDays(6);

        ActivityManager manager = new ActivityManager();
        manager.add(new FixedActivity(manager.getNextId(), "Monday activity", ActivityCategory.ACADEMIC,
                monday, LocalTime.of(9, 0), LocalTime.of(10, 0), EnergyRating.of(4), SensoryRating.of(3),
                null, null));
        manager.add(new FixedActivity(manager.getNextId(), "Sunday activity", ActivityCategory.ACADEMIC,
                sunday, LocalTime.of(9, 0), LocalTime.of(10, 0), EnergyRating.of(4), SensoryRating.of(3),
                null, null));
        manager.add(new FixedActivity(manager.getNextId(), "Day after activity", ActivityCategory.ACADEMIC,
                sunday.plusDays(1), LocalTime.of(9, 0), LocalTime.of(10, 0),
                EnergyRating.of(4), SensoryRating.of(3), null, null));

        String feedback = parser.parseList(manager, sundayNow, "this week").execute().getFeedback();

        assertTrue(feedback.contains("Monday activity"));
        assertTrue(feedback.contains("Sunday activity"));
        assertTrue(!feedback.contains("Day after activity"));
    }

    @Test
    public void parseList_thisWeekYearBoundary_computesCorrectWeek() throws Exception {
        // 2026-01-01 is a Thursday; its week runs 2025-12-29 (Mon) to 2026-01-04 (Sun), crossing
        // the year boundary.
        LocalDateTime now = LocalDateTime.of(2026, 1, 1, 10, 0);

        ActivityManager manager = new ActivityManager();
        manager.add(new FixedActivity(manager.getNextId(), "Last year activity", ActivityCategory.ACADEMIC,
                LocalDate.of(2025, 12, 29), LocalTime.of(9, 0), LocalTime.of(10, 0),
                EnergyRating.of(4), SensoryRating.of(3), null, null));
        manager.add(new FixedActivity(manager.getNextId(), "New year activity", ActivityCategory.ACADEMIC,
                LocalDate.of(2026, 1, 4), LocalTime.of(9, 0), LocalTime.of(10, 0),
                EnergyRating.of(4), SensoryRating.of(3), null, null));
        manager.add(new FixedActivity(manager.getNextId(), "Outside week activity", ActivityCategory.ACADEMIC,
                LocalDate.of(2026, 1, 5), LocalTime.of(9, 0), LocalTime.of(10, 0),
                EnergyRating.of(4), SensoryRating.of(3), null, null));

        String feedback = parser.parseList(manager, now, "this week").execute().getFeedback();

        assertTrue(feedback.contains("Last year activity"));
        assertTrue(feedback.contains("New year activity"));
        assertTrue(!feedback.contains("Outside week activity"));
    }

    @Test
    public void parseList_thisWeekCombinedWithFilters_appliesAllFilters() throws Exception {
        LocalDate today = NOW.toLocalDate();
        ActivityManager manager = new ActivityManager();
        manager.add(new FixedActivity(manager.getNextId(), "Matching", ActivityCategory.ACADEMIC,
                today, LocalTime.of(9, 0), LocalTime.of(10, 0), EnergyRating.of(4), SensoryRating.of(3),
                null, null));
        manager.add(new FixedActivity(manager.getNextId(), "Wrong category", ActivityCategory.CCA,
                today, LocalTime.of(11, 0), LocalTime.of(12, 0), EnergyRating.of(4), SensoryRating.of(3),
                null, null));
        manager.mark(2);

        String feedback = parser.parseList(manager, NOW,
                "this week status/incomplete c/ACADEMIC order/time").execute().getFeedback();

        assertTrue(feedback.contains("Matching"));
        assertTrue(!feedback.contains("Wrong category"));
    }

    @Test
    public void parseList_todayCombinedWithDateMarker_throwsInvalidCommandException() {
        ActivityManager manager = new ActivityManager();

        assertThrows(InvalidCommandException.class,
                () -> parser.parseList(manager, NOW, "today date/2026-08-15"));
    }

    @Test
    public void parseList_todayThenTomorrow_throwsInvalidCommandException() {
        ActivityManager manager = new ActivityManager();

        assertThrows(InvalidCommandException.class, () -> parser.parseList(manager, NOW, "today tomorrow"));
    }

    @Test
    public void parseList_thisMonth_throwsInvalidCommandException() {
        ActivityManager manager = new ActivityManager();

        assertThrows(InvalidCommandException.class, () -> parser.parseList(manager, NOW, "this month"));
    }

    @Test
    public void parseList_todayWithTrailingGarbage_throwsInvalidCommandException() {
        ActivityManager manager = new ActivityManager();

        assertThrows(InvalidCommandException.class, () -> parser.parseList(manager, NOW, "today extra"));
    }

    @Test
    public void parseList_unknownLeadingWord_throwsInvalidCommandException() {
        // Family bug check: leading text that is neither a marker nor a recognised relative-date
        // phrase must be rejected, not silently ignored as if "list" had no arguments at all.
        ActivityManager manager = new ActivityManager();

        assertThrows(InvalidCommandException.class, () -> parser.parseList(manager, NOW, "bogus"));
    }

    @Test
    public void parseList_dateMarkerStillWorksWithoutRelativeDate() throws Exception {
        ActivityManager manager = new ActivityManager();
        manager.add(new FixedActivity(manager.getNextId(), "Fixed-date lecture", ActivityCategory.ACADEMIC,
                LocalDate.of(2026, 8, 20), LocalTime.of(9, 0), LocalTime.of(10, 0),
                EnergyRating.of(4), SensoryRating.of(3), null, null));

        String feedback = parser.parseList(manager, NOW, "date/2026-08-20").execute().getFeedback();

        assertTrue(feedback.contains("Fixed-date lecture"));
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
    public void parseFind_unrecognisedLeadingToken_throwsInvalidCommandException() {
        // Regression test for RC05 (v1.0 RC retest, 2026-08-01).
        ActivityManager manager = new ActivityManager();

        assertThrows(InvalidCommandException.class,
                () -> parser.parseFind(manager, "ignored/yes c/ACADEMIC"));
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
    public void parseEdit_unrecognisedLeadingToken_throwsInvalidCommandExceptionAndDoesNotMutate() throws Exception {
        // Regression test for RC05 (v1.0 RC retest, 2026-08-01).
        ActivityManager manager = new ActivityManager();
        TopicManager topicManager = new TopicManager(manager);
        manager.add(new FixedActivity(manager.getNextId(), "Lecture", ActivityCategory.ACADEMIC,
                LocalDate.of(2026, 8, 15), LocalTime.of(9, 0), LocalTime.of(10, 0),
                EnergyRating.of(2), SensoryRating.of(2), null, null));

        assertThrows(InvalidCommandException.class,
                () -> parser.parseEdit(manager, topicManager, TODAY, "1 ignored/yes energy/5"));
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

        parser.parseEdit(manager, topicManager, TODAY, "1 dur/60").execute();

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

        parser.parseEdit(manager, topicManager, TODAY, "1 topic/   ").execute();

        assertNull(manager.getById(1).getTopic());
    }

    @Test
    public void parseEdit_whitespaceOnlyNote_clearsNoteToNull() throws Exception {
        ActivityManager manager = new ActivityManager();
        TopicManager topicManager = new TopicManager(manager);
        manager.add(new FixedActivity(manager.getNextId(), "Lecture", ActivityCategory.ACADEMIC,
                LocalDate.of(2026, 8, 15), LocalTime.of(9, 0), LocalTime.of(11, 0),
                EnergyRating.of(4), SensoryRating.of(3), null, "Bring laptop"));

        parser.parseEdit(manager, topicManager, TODAY, "1 note/   ").execute();

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
                () -> parser.parseEdit(manager, topicManager, TODAY, "1 n/Bad|Desc"));
    }

    @Test
    public void parseEdit_topicContainsDelimiter_throwsInvalidActivityException() throws Exception {
        ActivityManager manager = new ActivityManager();
        TopicManager topicManager = new TopicManager(manager);
        manager.add(new FixedActivity(manager.getNextId(), "Lecture", ActivityCategory.ACADEMIC,
                LocalDate.of(2026, 8, 15), LocalTime.of(9, 0), LocalTime.of(11, 0),
                EnergyRating.of(4), SensoryRating.of(3), null, null));

        assertThrows(InvalidActivityException.class,
                () -> parser.parseEdit(manager, topicManager, TODAY, "1 topic/Bad|Topic"));
    }

    @Test
    public void parseEdit_noteContainsDelimiter_throwsInvalidActivityException() throws Exception {
        ActivityManager manager = new ActivityManager();
        TopicManager topicManager = new TopicManager(manager);
        manager.add(new FixedActivity(manager.getNextId(), "Lecture", ActivityCategory.ACADEMIC,
                LocalDate.of(2026, 8, 15), LocalTime.of(9, 0), LocalTime.of(11, 0),
                EnergyRating.of(4), SensoryRating.of(3), null, null));

        assertThrows(InvalidActivityException.class,
                () -> parser.parseEdit(manager, topicManager, TODAY, "1 note/Bad|Note"));
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

        parser.parseEdit(manager, topicManager, TODAY, "1 n/X n/Y").execute();

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

        parser.parseEdit(manager, topicManager, TODAY, "1 topic/CS2113").execute();

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

        assertThrows(InvalidIndexException.class, () -> parser.parseEdit(manager, topicManager, TODAY, "1 c/CCA"));

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

        parser.parseEdit(manager, topicManager, TODAY, "1 c/CCA topic/Basketball Club").execute();

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

        parser.parseEdit(manager, topicManager, TODAY, "1 c/CCA").execute();

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
                () -> parser.parseEdit(manager, topicManager, TODAY, "1 topic/NeverCreated"));
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
                () -> parser.parseEdit(manager, topicManager, TODAY, "2 from/10:30 to/11:30"));

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
                () -> parser.parseEdit(manager, topicManager, TODAY, "2 date/2026-08-20"));
    }

    @Test
    public void parseEdit_nonConflictingChange_stillSucceeds() throws Exception {
        ActivityManager manager = new ActivityManager();
        TopicManager topicManager = new TopicManager(manager);
        manager.add(new FixedActivity(manager.getNextId(), "Base", ActivityCategory.ACADEMIC,
                LocalDate.of(2026, 8, 20), LocalTime.of(10, 0), LocalTime.of(12, 0),
                EnergyRating.of(2), SensoryRating.of(2), null, null));

        parser.parseEdit(manager, topicManager, TODAY, "1 from/09:00 to/11:00").execute();

        assertEquals(LocalTime.of(9, 0), ((FixedActivity) manager.getById(1)).getStartTime());
    }

    @Test
    public void parseEdit_multipleFields_updatesAllGivenFields() throws Exception {
        ActivityManager manager = new ActivityManager();
        TopicManager topicManager = new TopicManager(manager);
        manager.add(new FlexibleActivity(manager.getNextId(), "Prepare slides", ActivityCategory.ACADEMIC,
                LocalDate.of(2026, 8, 15), LocalTime.of(10, 0), LocalTime.of(18, 0), 90,
                EnergyRating.of(3), SensoryRating.of(3), null, null));

        parser.parseEdit(manager, topicManager, TODAY, "1 n/New activity name energy/4 sensory/2").execute();

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

        parser.parseEdit(manager, topicManager, TODAY, "1 note/Bring headphones").execute();

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

        parser.parseEdit(manager, topicManager, TODAY, "1 dur/60").execute();

        assertTrue(manager.getById(1).isComplete());
    }

    @Test
    public void parseEdit_changingTypeWithAllNewTimingFields_succeeds() throws Exception {
        ActivityManager manager = new ActivityManager();
        TopicManager topicManager = new TopicManager(manager);
        manager.add(new FixedActivity(manager.getNextId(), "Lecture", ActivityCategory.ACADEMIC,
                LocalDate.of(2026, 8, 15), LocalTime.of(9, 0), LocalTime.of(11, 0),
                EnergyRating.of(4), SensoryRating.of(3), null, null));

        parser.parseEdit(manager, topicManager, TODAY, "1 type/FLEXIBLE earliest/10:00 latest/18:00 dur/90").execute();

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

        assertThrows(InvalidActivityException.class, () -> parser.parseEdit(manager, topicManager, TODAY, "1 dur/500"));
    }

    @Test
    public void parseEdit_changingTypeWithDurationExceedingNewWindow_throwsInvalidActivityException()
            throws Exception {
        ActivityManager manager = new ActivityManager();
        TopicManager topicManager = new TopicManager(manager);
        manager.add(new FixedActivity(manager.getNextId(), "Lecture", ActivityCategory.ACADEMIC,
                LocalDate.of(2026, 8, 15), LocalTime.of(9, 0), LocalTime.of(11, 0),
                EnergyRating.of(4), SensoryRating.of(3), null, null));

        assertThrows(InvalidActivityException.class, () -> parser.parseEdit(manager, topicManager, TODAY,
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
                () -> parser.parseEdit(manager, topicManager, TODAY, "1 type/FLEXIBLE"));
    }

    @Test
    public void parseEdit_noFieldsSupplied_throwsMissingInputException() throws Exception {
        ActivityManager manager = new ActivityManager();
        TopicManager topicManager = new TopicManager(manager);
        manager.add(new FixedActivity(manager.getNextId(), "Lecture", ActivityCategory.ACADEMIC,
                LocalDate.of(2026, 8, 15), LocalTime.of(9, 0), LocalTime.of(11, 0),
                EnergyRating.of(4), SensoryRating.of(3), null, null));

        assertThrows(MissingInputException.class, () -> parser.parseEdit(manager, topicManager, TODAY, "1"));
    }

    @Test
    public void parseEdit_unknownId_throwsInvalidIndexException() {
        ActivityManager manager = new ActivityManager();
        TopicManager topicManager = new TopicManager(manager);

        assertThrows(InvalidIndexException.class, () -> parser.parseEdit(manager, topicManager, TODAY, "999 dur/60"));
    }

    @Test
    public void parseEdit_nonNumericId_throwsInvalidCommandException() {
        ActivityManager manager = new ActivityManager();
        TopicManager topicManager = new TopicManager(manager);

        assertThrows(InvalidCommandException.class, () -> parser.parseEdit(manager, topicManager, TODAY, "abc dur/60"));
    }

    @Test
    public void parseEdit_invalidNewCategory_throwsInvalidActivityException() throws Exception {
        ActivityManager manager = new ActivityManager();
        TopicManager topicManager = new TopicManager(manager);
        manager.add(new FixedActivity(manager.getNextId(), "Lecture", ActivityCategory.ACADEMIC,
                LocalDate.of(2026, 8, 15), LocalTime.of(9, 0), LocalTime.of(11, 0),
                EnergyRating.of(4), SensoryRating.of(3), null, null));

        assertThrows(InvalidActivityException.class, () -> parser.parseEdit(manager, topicManager, TODAY, "1 c/BOGUS"));
    }
}
