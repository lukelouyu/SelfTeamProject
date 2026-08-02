package seedu.unienable.parser.activity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

import org.junit.jupiter.api.Test;

import seedu.unienable.command.CommandResult;
import seedu.unienable.exception.InvalidCommandException;
import seedu.unienable.logic.ActivityManager;
import seedu.unienable.logic.TopicManager;
import seedu.unienable.model.classes.EnergyRating;
import seedu.unienable.model.classes.FixedActivity;
import seedu.unienable.model.classes.SensoryRating;
import seedu.unienable.model.enums.ActivityCategory;

class ListCommandParserTest {
    private static final LocalDateTime NOW = LocalDateTime.of(2026, 8, 15, 10, 0);

    private final ListCommandParser parser = new ListCommandParser();

    @Test
    public void parseList_noFields_listsEverythingInDefaultOrder() throws Exception {
        ActivityManager manager = new ActivityManager();
        TopicManager topicManager = new TopicManager(manager);
        manager.add(new FixedActivity(manager.getNextId(), "Lecture", ActivityCategory.ACADEMIC,
                LocalDate.of(2026, 8, 15), LocalTime.of(9, 0), LocalTime.of(11, 0),
                EnergyRating.of(4), SensoryRating.of(3), null, null));

        CommandResult result = parser.parse(manager, NOW, "").execute();

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

        CommandResult result = parser.parse(manager, NOW, "status/incomplete").execute();

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

        CommandResult result = parser.parse(manager, NOW, "view/detail").execute();

        assertTrue(result.getFeedback().contains("Status: Incomplete | Type: FIXED"));
    }

    @Test
    public void parseList_unrecognisedViewValue_throwsInvalidCommandException() {
        // Regression test: view/ compared its value only against "detail" (via equalsIgnoreCase)
        // and silently treated everything else as concise, so a typo like "view/nonsense" was
        // never rejected.
        ActivityManager manager = new ActivityManager();

        assertThrows(InvalidCommandException.class, () -> parser.parse(manager, NOW, "view/nonsense"));
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

        CommandResult result = parser.parse(manager, NOW, "c/ACADEMIC topic/CG3207").execute();

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

        CommandResult result = parser.parse(manager, NOW, "topic/CG3207").execute();

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

        CommandResult result = parser.parse(manager, NOW, "topic/   ").execute();

        assertTrue(result.getFeedback().contains("No-topic lecture"));
    }

    @Test
    public void parseList_invalidStatus_throwsInvalidCommandException() {
        ActivityManager manager = new ActivityManager();
        TopicManager topicManager = new TopicManager(manager);

        assertThrows(InvalidCommandException.class, () -> parser.parse(manager, NOW, "status/bogus"));
    }

    @Test
    public void parseList_invalidOrder_throwsInvalidCommandException() {
        ActivityManager manager = new ActivityManager();
        TopicManager topicManager = new TopicManager(manager);

        assertThrows(InvalidCommandException.class, () -> parser.parse(manager, NOW, "order/bogus"));
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

        String feedback = parser.parse(manager, NOW, "today").execute().getFeedback();

        assertTrue(feedback.contains("Today's lecture"));
        assertTrue(!feedback.contains("Tomorrow's lecture"));
    }

    @Test
    public void parseList_todayIsCaseInsensitive_matchesTodaysActivities() throws Exception {
        ActivityManager manager = new ActivityManager();
        LocalDate today = NOW.toLocalDate();
        manager.add(new FixedActivity(manager.getNextId(), "Today's lecture", ActivityCategory.ACADEMIC,
                today, LocalTime.of(9, 0), LocalTime.of(10, 0), EnergyRating.of(4), SensoryRating.of(3), null, null));

        String feedback = parser.parse(manager, NOW, "TODAY").execute().getFeedback();

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

        String feedback = parser.parse(manager, NOW, "tomorrow").execute().getFeedback();

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

        String feedback = parser.parse(manager, NOW, "tomorrow view/detail").execute().getFeedback();

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

        String feedback = parser.parse(manager, mondayNow, "this week").execute().getFeedback();

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

        String feedback = parser.parse(manager, sundayNow, "this week").execute().getFeedback();

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

        String feedback = parser.parse(manager, now, "this week").execute().getFeedback();

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

        String feedback = parser.parse(manager, NOW,
                "this week status/incomplete c/ACADEMIC order/time").execute().getFeedback();

        assertTrue(feedback.contains("Matching"));
        assertTrue(!feedback.contains("Wrong category"));
    }

    @Test
    public void parseList_nextWeekFromSaturday_matchesFollowingMondayThroughSunday() throws Exception {
        // FEATURE-02 (v1.0 manual release test, 2026-08-01): with now fixed at Saturday
        // 2026-08-01, the current week is 2026-07-27 to 2026-08-02, so "list next week" must
        // resolve to 2026-08-03 (Mon) through 2026-08-09 (Sun) - the report's own worked example.
        LocalDateTime now = LocalDateTime.of(2026, 8, 1, 10, 0);

        ActivityManager manager = new ActivityManager();
        manager.add(new FixedActivity(manager.getNextId(), "Next Monday activity", ActivityCategory.ACADEMIC,
                LocalDate.of(2026, 8, 3), LocalTime.of(9, 0), LocalTime.of(10, 0),
                EnergyRating.of(4), SensoryRating.of(3), null, null));
        manager.add(new FixedActivity(manager.getNextId(), "Next Sunday activity", ActivityCategory.ACADEMIC,
                LocalDate.of(2026, 8, 9), LocalTime.of(9, 0), LocalTime.of(10, 0),
                EnergyRating.of(4), SensoryRating.of(3), null, null));
        manager.add(new FixedActivity(manager.getNextId(), "This Sunday activity", ActivityCategory.ACADEMIC,
                LocalDate.of(2026, 8, 2), LocalTime.of(9, 0), LocalTime.of(10, 0),
                EnergyRating.of(4), SensoryRating.of(3), null, null));
        manager.add(new FixedActivity(manager.getNextId(), "Week after next activity", ActivityCategory.ACADEMIC,
                LocalDate.of(2026, 8, 10), LocalTime.of(9, 0), LocalTime.of(10, 0),
                EnergyRating.of(4), SensoryRating.of(3), null, null));

        String feedback = parser.parse(manager, now, "next week").execute().getFeedback();

        assertTrue(feedback.contains("Next Monday activity"));
        assertTrue(feedback.contains("Next Sunday activity"));
        assertTrue(!feedback.contains("This Sunday activity"));
        assertTrue(!feedback.contains("Week after next activity"));
    }

    @Test
    public void parseList_nextWeekYearBoundary_computesCorrectWeek() throws Exception {
        // Regression case from the report: with now = 2026-12-31, next week is 2027-01-04
        // through 2027-01-10.
        LocalDateTime now = LocalDateTime.of(2026, 12, 31, 10, 0);

        ActivityManager manager = new ActivityManager();
        manager.add(new FixedActivity(manager.getNextId(), "Next year Monday", ActivityCategory.ACADEMIC,
                LocalDate.of(2027, 1, 4), LocalTime.of(9, 0), LocalTime.of(10, 0),
                EnergyRating.of(4), SensoryRating.of(3), null, null));
        manager.add(new FixedActivity(manager.getNextId(), "Next year Sunday", ActivityCategory.ACADEMIC,
                LocalDate.of(2027, 1, 10), LocalTime.of(9, 0), LocalTime.of(10, 0),
                EnergyRating.of(4), SensoryRating.of(3), null, null));
        manager.add(new FixedActivity(manager.getNextId(), "Outside next week", ActivityCategory.ACADEMIC,
                LocalDate.of(2027, 1, 11), LocalTime.of(9, 0), LocalTime.of(10, 0),
                EnergyRating.of(4), SensoryRating.of(3), null, null));

        String feedback = parser.parse(manager, now, "next week").execute().getFeedback();

        assertTrue(feedback.contains("Next year Monday"));
        assertTrue(feedback.contains("Next year Sunday"));
        assertTrue(!feedback.contains("Outside next week"));
    }

    @Test
    public void parseList_nextWeekCombinedWithFilters_appliesAllFilters() throws Exception {
        LocalDateTime now = LocalDateTime.of(2026, 8, 1, 10, 0);
        ActivityManager manager = new ActivityManager();
        manager.add(new FixedActivity(manager.getNextId(), "Matching", ActivityCategory.ACADEMIC,
                LocalDate.of(2026, 8, 5), LocalTime.of(9, 0), LocalTime.of(10, 0),
                EnergyRating.of(4), SensoryRating.of(3), null, null));
        manager.add(new FixedActivity(manager.getNextId(), "Wrong category", ActivityCategory.CCA,
                LocalDate.of(2026, 8, 5), LocalTime.of(11, 0), LocalTime.of(12, 0),
                EnergyRating.of(4), SensoryRating.of(3), null, null));

        String feedback = parser.parse(manager, now, "next week c/ACADEMIC order/time").execute()
                .getFeedback();

        assertTrue(feedback.contains("Matching"));
        assertTrue(!feedback.contains("Wrong category"));
    }

    @Test
    public void parseList_nextWeekCombinedWithDateMarker_throwsInvalidCommandException() {
        ActivityManager manager = new ActivityManager();

        assertThrows(InvalidCommandException.class,
                () -> parser.parse(manager, NOW, "next week date/2026-08-15"));
    }

    @Test
    public void parseList_nextMonth_throwsInvalidCommandException() {
        ActivityManager manager = new ActivityManager();

        assertThrows(InvalidCommandException.class, () -> parser.parse(manager, NOW, "next month"));
    }

    @Test
    public void parseList_bareNext_messageHasNoStrayTrailingSpace() {
        // Regression test: "next" alone used to render as `Unknown list option "next "` (a
        // literal trailing space baked into the string before the word, unconditionally
        // appended even when there was no second word to report).
        ActivityManager manager = new ActivityManager();

        InvalidCommandException exception = assertThrows(InvalidCommandException.class,
                () -> parser.parse(manager, NOW, "next"));

        assertEquals("Unknown list option \"next\"; only \"next week\" is supported.", exception.getMessage());
    }

    @Test
    public void parseList_bareThis_messageHasNoStrayTrailingSpace() {
        ActivityManager manager = new ActivityManager();

        InvalidCommandException exception = assertThrows(InvalidCommandException.class,
                () -> parser.parse(manager, NOW, "this"));

        assertEquals("Unknown list option \"this\"; only \"this week\" is supported.", exception.getMessage());
    }

    @Test
    public void parseList_nextWeekCombinedWithToday_namesTheActualProblem() {
        // Regression test: combining two relative-date phrases used to fall through to the
        // generic "Unknown list option" message (since the leading phrase was already consumed,
        // leaving only the second phrase's own keyword to report) instead of a specific message
        // naming the real problem, unlike combining with date/ which already had one.
        ActivityManager manager = new ActivityManager();

        InvalidCommandException exception = assertThrows(InvalidCommandException.class,
                () -> parser.parse(manager, NOW, "next week today"));

        assertEquals("today, tomorrow, this week, next week, and overdue cannot be combined with each other.",
                exception.getMessage());
    }

    @Test
    public void parseList_thisWeekCombinedWithNextWeek_namesTheActualProblem() {
        ActivityManager manager = new ActivityManager();

        InvalidCommandException exception = assertThrows(InvalidCommandException.class,
                () -> parser.parse(manager, NOW, "this week next week"));

        assertEquals("today, tomorrow, this week, next week, and overdue cannot be combined with each other.",
                exception.getMessage());
    }

    @Test
    public void parseList_nextWeekExtra_stillGivesGenericMessageForNonRelativeTrailingText() {
        // Unrelated trailing text (not another relative-date keyword) must still fall through to
        // the generic message - only the two combined-relative-phrase cases above changed.
        ActivityManager manager = new ActivityManager();

        InvalidCommandException exception = assertThrows(InvalidCommandException.class,
                () -> parser.parse(manager, NOW, "next week extra"));

        assertEquals("Unknown list option \"extra\".", exception.getMessage());
    }

    @Test
    public void parseList_overdue_matchesOnlyIncompletePastActivities() throws Exception {
        // FEATURE-01 (v1.0 manual release test, 2026-08-01): an incomplete activity whose
        // scheduled time has passed must appear; a completed one must not; an upcoming one must
        // not either.
        LocalDateTime now = LocalDateTime.of(2026, 8, 15, 12, 0);
        ActivityManager manager = new ActivityManager();
        manager.add(new FixedActivity(manager.getNextId(), "Overdue incomplete", ActivityCategory.ACADEMIC,
                LocalDate.of(2026, 8, 15), LocalTime.of(9, 0), LocalTime.of(10, 0),
                EnergyRating.of(4), SensoryRating.of(3), null, null));
        manager.add(new FixedActivity(manager.getNextId(), "Overdue but completed", ActivityCategory.ACADEMIC,
                LocalDate.of(2026, 8, 15), LocalTime.of(10, 0), LocalTime.of(11, 0),
                EnergyRating.of(4), SensoryRating.of(3), null, null));
        manager.mark(2);
        manager.add(new FixedActivity(manager.getNextId(), "Still upcoming", ActivityCategory.ACADEMIC,
                LocalDate.of(2026, 8, 15), LocalTime.of(13, 0), LocalTime.of(14, 0),
                EnergyRating.of(4), SensoryRating.of(3), null, null));

        String feedback = parser.parse(manager, now, "overdue").execute().getFeedback();

        assertTrue(feedback.contains("Overdue incomplete"));
        assertTrue(!feedback.contains("Overdue but completed"));
        assertTrue(!feedback.contains("Still upcoming"));
    }

    @Test
    public void parseList_overdue_omittedFromNormalListIsStillShownByPlainList() throws Exception {
        // Confirms the additive scope decision: "list overdue" is a new, separate selector, and
        // the plain "list" continues to show every activity exactly as before - nothing is
        // hidden from it.
        LocalDateTime now = LocalDateTime.of(2026, 8, 15, 12, 0);
        ActivityManager manager = new ActivityManager();
        manager.add(new FixedActivity(manager.getNextId(), "Overdue incomplete", ActivityCategory.ACADEMIC,
                LocalDate.of(2026, 8, 15), LocalTime.of(9, 0), LocalTime.of(10, 0),
                EnergyRating.of(4), SensoryRating.of(3), null, null));

        String feedback = parser.parse(manager, now, "").execute().getFeedback();

        assertTrue(feedback.contains("Overdue incomplete"));
    }

    @Test
    public void parseList_overdueNoOverdueActivities_reportsNoActivitiesFound() throws Exception {
        LocalDateTime now = LocalDateTime.of(2026, 8, 15, 12, 0);
        ActivityManager manager = new ActivityManager();
        manager.add(new FixedActivity(manager.getNextId(), "Upcoming", ActivityCategory.ACADEMIC,
                LocalDate.of(2026, 8, 15), LocalTime.of(13, 0), LocalTime.of(14, 0),
                EnergyRating.of(4), SensoryRating.of(3), null, null));

        assertEquals("No activities found.", parser.parse(manager, now, "overdue").execute().getFeedback());
    }

    @Test
    public void parseList_overdueCombinedWithCategoryFilter_appliesBothConditions() throws Exception {
        LocalDateTime now = LocalDateTime.of(2026, 8, 15, 12, 0);
        ActivityManager manager = new ActivityManager();
        manager.add(new FixedActivity(manager.getNextId(), "Overdue academic", ActivityCategory.ACADEMIC,
                LocalDate.of(2026, 8, 15), LocalTime.of(9, 0), LocalTime.of(10, 0),
                EnergyRating.of(4), SensoryRating.of(3), null, null));
        manager.add(new FixedActivity(manager.getNextId(), "Overdue cca", ActivityCategory.CCA,
                LocalDate.of(2026, 8, 15), LocalTime.of(10, 0), LocalTime.of(11, 0),
                EnergyRating.of(4), SensoryRating.of(3), null, null));

        String feedback = parser.parse(manager, now, "overdue c/ACADEMIC").execute().getFeedback();

        assertTrue(feedback.contains("Overdue academic"));
        assertTrue(!feedback.contains("Overdue cca"));
    }

    @Test
    public void parseList_overdueCombinedWithStatusMarker_throwsInvalidCommandException() {
        ActivityManager manager = new ActivityManager();

        assertThrows(InvalidCommandException.class,
                () -> parser.parse(manager, NOW, "overdue status/completed"));
    }

    @Test
    public void parseList_overdueThenToday_throwsInvalidCommandException() {
        ActivityManager manager = new ActivityManager();

        assertThrows(InvalidCommandException.class, () -> parser.parse(manager, NOW, "overdue today"));
    }

    @Test
    public void parseList_overdueWithTrailingGarbage_throwsInvalidCommandException() {
        ActivityManager manager = new ActivityManager();

        assertThrows(InvalidCommandException.class, () -> parser.parse(manager, NOW, "overdue extra"));
    }

    @Test
    public void parseList_todayCombinedWithDateMarker_throwsInvalidCommandException() {
        ActivityManager manager = new ActivityManager();

        assertThrows(InvalidCommandException.class,
                () -> parser.parse(manager, NOW, "today date/2026-08-15"));
    }

    @Test
    public void parseList_todayThenTomorrow_throwsInvalidCommandException() {
        ActivityManager manager = new ActivityManager();

        assertThrows(InvalidCommandException.class, () -> parser.parse(manager, NOW, "today tomorrow"));
    }

    @Test
    public void parseList_thisMonth_throwsInvalidCommandException() {
        ActivityManager manager = new ActivityManager();

        assertThrows(InvalidCommandException.class, () -> parser.parse(manager, NOW, "this month"));
    }

    @Test
    public void parseList_todayWithTrailingGarbage_throwsInvalidCommandException() {
        ActivityManager manager = new ActivityManager();

        assertThrows(InvalidCommandException.class, () -> parser.parse(manager, NOW, "today extra"));
    }

    @Test
    public void parseList_unknownLeadingWord_throwsInvalidCommandException() {
        // Family bug check: leading text that is neither a marker nor a recognised relative-date
        // phrase must be rejected, not silently ignored as if "list" had no arguments at all.
        ActivityManager manager = new ActivityManager();

        assertThrows(InvalidCommandException.class, () -> parser.parse(manager, NOW, "bogus"));
    }

    @Test
    public void parseList_dateMarkerStillWorksWithoutRelativeDate() throws Exception {
        ActivityManager manager = new ActivityManager();
        manager.add(new FixedActivity(manager.getNextId(), "Fixed-date lecture", ActivityCategory.ACADEMIC,
                LocalDate.of(2026, 8, 20), LocalTime.of(9, 0), LocalTime.of(10, 0),
                EnergyRating.of(4), SensoryRating.of(3), null, null));

        String feedback = parser.parse(manager, NOW, "date/2026-08-20").execute().getFeedback();

        assertTrue(feedback.contains("Fixed-date lecture"));
    }

}
