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
import seedu.unienable.exception.MissingInputException;
import seedu.unienable.logic.ActivityManager;
import seedu.unienable.logic.TopicManager;
import seedu.unienable.model.classes.EnergyRating;
import seedu.unienable.model.classes.FixedActivity;
import seedu.unienable.model.classes.FlexibleActivity;
import seedu.unienable.model.classes.SensoryRating;
import seedu.unienable.model.enums.ActivityCategory;
import seedu.unienable.model.enums.ActivityOrder;

class FindCommandParserTest {
    private static final LocalDateTime NOW = LocalDateTime.of(2026, 8, 17, 10, 0);

    private final FindCommandParser parser = new FindCommandParser();

    @Test
    public void parseFind_singleKeyword_findsMatchingActivity() throws Exception {
        ActivityManager manager = new ActivityManager();
        TopicManager topicManager = new TopicManager(manager);
        manager.add(new FixedActivity(manager.getNextId(), "Finish assignment 1", ActivityCategory.ACADEMIC,
                LocalDate.of(2026, 8, 15), LocalTime.of(9, 0), LocalTime.of(10, 0),
                EnergyRating.of(4), SensoryRating.of(3), null, null));

        CommandResult result = parser.parse(manager, NOW, "k/assignment").execute();

        assertTrue(result.getFeedback().contains("Finish assignment 1"));
    }

    @Test
    public void parseFind_unrecognisedLeadingToken_throwsInvalidCommandException() {
        // Regression test for RC05 (v1.0 RC retest, 2026-08-01).
        ActivityManager manager = new ActivityManager();

        assertThrows(InvalidCommandException.class,
                () -> parser.parse(manager, NOW, "ignored/yes c/ACADEMIC"));
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

        CommandResult result = parser.parse(manager, NOW, "k/finish assignment").execute();

        String feedback = result.getFeedback();
        assertTrue(feedback.contains("Finish assignment 1"));
        assertTrue(!feedback.contains("Finish reading"));
    }

    @Test
    public void parseFind_threeWordKeyword_throwsInvalidCommandException() {
        // Regression test for BUG-05 (v1.0 manual release test, 2026-08-01): "find k/Edited exact
        // extra" was previously accepted as a valid (if zero-result) three-word AND search,
        // contradicting the documented one-or-two-word k/ scope.
        ActivityManager manager = new ActivityManager();

        InvalidCommandException exception = assertThrows(InvalidCommandException.class,
                () -> parser.parse(manager, NOW, "k/Edited exact extra"));
        assertEquals("keyword must contain one or two words.", exception.getMessage());
    }

    @Test
    public void parseFind_fourWordKeyword_throwsInvalidCommandException() {
        ActivityManager manager = new ActivityManager();

        assertThrows(InvalidCommandException.class, () -> parser.parse(manager, NOW, "k/one two three four"));
    }

    @Test
    public void parseFind_threeWordKeywordCombinedWithFilters_rejectedBeforeAnyResult() {
        // The word-count check must reject before any search executes, even when valid filters
        // are also supplied alongside the over-long keyword.
        ActivityManager manager = new ActivityManager();

        assertThrows(InvalidCommandException.class,
                () -> parser.parse(manager, NOW, "k/one two three c/ACADEMIC order/time"));
    }

    @Test
    public void parseFind_twoWordKeywordWithIrregularWhitespace_isTreatedAsTwoWords() throws Exception {
        // Leading, trailing, and repeated internal whitespace must not be counted as extra words.
        ActivityManager manager = new ActivityManager();
        manager.add(new FixedActivity(manager.getNextId(), "Flexible study session", ActivityCategory.ACADEMIC,
                LocalDate.of(2026, 8, 15), LocalTime.of(9, 0), LocalTime.of(10, 0),
                EnergyRating.of(4), SensoryRating.of(3), null, null));

        CommandResult result = parser.parse(manager, NOW, "k/  Flexible   study  ").execute();

        assertTrue(result.getFeedback().contains("Flexible study session"));
    }

    @Test
    public void parseFind_oneWordKeyword_isAccepted() throws Exception {
        ActivityManager manager = new ActivityManager();
        manager.add(new FixedActivity(manager.getNextId(), "Class", ActivityCategory.ACADEMIC,
                LocalDate.of(2026, 8, 15), LocalTime.of(9, 0), LocalTime.of(10, 0),
                EnergyRating.of(4), SensoryRating.of(3), null, null));

        CommandResult result = parser.parse(manager, NOW, "k/Class").execute();

        assertTrue(result.getFeedback().contains("Class"));
    }

    @Test
    public void parseFind_filterOnlyNoKeyword_isAllowed() throws Exception {
        ActivityManager manager = new ActivityManager();
        TopicManager topicManager = new TopicManager(manager);
        manager.add(new FixedActivity(manager.getNextId(), "CG3207 lecture", ActivityCategory.ACADEMIC,
                LocalDate.of(2026, 8, 15), LocalTime.of(9, 0), LocalTime.of(10, 0),
                EnergyRating.of(4), SensoryRating.of(3), "CG3207", null));

        CommandResult result = parser.parse(manager, NOW, "c/ACADEMIC topic/CG3207").execute();

        assertTrue(result.getFeedback().contains("CG3207 lecture"));
    }

    @Test
    public void parseFind_topicFilterAloneWithNoExplicitCategory_doesNotThrow() throws Exception {
        ActivityManager manager = new ActivityManager();
        TopicManager topicManager = new TopicManager(manager);
        manager.add(new FixedActivity(manager.getNextId(), "CG3207 lecture", ActivityCategory.ACADEMIC,
                LocalDate.of(2026, 8, 15), LocalTime.of(9, 0), LocalTime.of(10, 0),
                EnergyRating.of(4), SensoryRating.of(3), "CG3207", null));

        CommandResult result = parser.parse(manager, NOW, "topic/CG3207").execute();

        assertTrue(result.getFeedback().contains("CG3207 lecture"));
    }

    @Test
    public void parseFind_neitherKeywordNorFilter_throwsMissingInputException() {
        ActivityManager manager = new ActivityManager();
        TopicManager topicManager = new TopicManager(manager);

        assertThrows(MissingInputException.class, () -> parser.parse(manager, NOW, ""));
    }

    @Test
    public void parseFind_whitespaceOnlyTopicFilterAlone_throwsMissingInputException() {
        // Regression test: a blank topic/ does not count as a supplied filter -- same principle
        // as order/ alone not counting -- so "find topic/   " with nothing else must still be
        // rejected rather than silently matching every activity.
        ActivityManager manager = new ActivityManager();
        TopicManager topicManager = new TopicManager(manager);

        assertThrows(MissingInputException.class, () -> parser.parse(manager, NOW, "topic/   "));
    }

    @Test
    public void parseFind_whitespaceOnlyTopicWithOtherFilter_ignoresTopicUsesOtherFilter() throws Exception {
        ActivityManager manager = new ActivityManager();
        TopicManager topicManager = new TopicManager(manager);
        manager.add(new FixedActivity(manager.getNextId(), "No-topic lecture", ActivityCategory.ACADEMIC,
                LocalDate.of(2026, 8, 15), LocalTime.of(9, 0), LocalTime.of(10, 0),
                EnergyRating.of(4), SensoryRating.of(3), null, null));

        CommandResult result = parser.parse(manager, NOW, "c/ACADEMIC topic/   ").execute();

        assertTrue(result.getFeedback().contains("No-topic lecture"));
    }

    @Test
    public void parseFind_whitespaceOnlyArgs_throwsMissingInputException() {
        ActivityManager manager = new ActivityManager();
        TopicManager topicManager = new TopicManager(manager);

        assertThrows(MissingInputException.class, () -> parser.parse(manager, NOW, "   "));
    }

    @Test
    public void parseFind_orderMarkerAloneWithNoKeywordOrFilter_throwsMissingInputException() {
        // Regression test: order/ is find's last marker but is a display-ordering directive, not
        // a keyword or filter. "find order/time" alone was previously accepted (since the fields
        // map was non-empty) and silently returned every activity.
        ActivityManager manager = new ActivityManager();
        TopicManager topicManager = new TopicManager(manager);

        assertThrows(MissingInputException.class, () -> parser.parse(manager, NOW, "order/time"));
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

        assertThrows(MissingInputException.class, () -> parser.parse(manager, NOW, "k/   "));
    }

    @Test
    public void parseFind_whitespaceOnlyKeywordWithOtherFilter_ignoresKeywordUsesOtherFilter() throws Exception {
        ActivityManager manager = new ActivityManager();
        TopicManager topicManager = new TopicManager(manager);
        manager.add(new FixedActivity(manager.getNextId(), "No-topic lecture", ActivityCategory.ACADEMIC,
                LocalDate.of(2026, 8, 15), LocalTime.of(9, 0), LocalTime.of(10, 0),
                EnergyRating.of(4), SensoryRating.of(3), null, null));

        CommandResult result = parser.parse(manager, NOW, "c/ACADEMIC k/   ").execute();

        assertTrue(result.getFeedback().contains("No-topic lecture"));
    }

    @Test
    public void parseFind_today_matchesOnlyTodaysActivities() throws Exception {
        LocalDate today = NOW.toLocalDate();
        ActivityManager manager = new ActivityManager();
        manager.add(new FixedActivity(manager.getNextId(), "Today lecture", ActivityCategory.ACADEMIC,
                today, LocalTime.of(9, 0), LocalTime.of(10, 0), EnergyRating.of(4), SensoryRating.of(3), null, null));
        manager.add(new FixedActivity(manager.getNextId(), "Tomorrow lecture", ActivityCategory.ACADEMIC,
                today.plusDays(1), LocalTime.of(9, 0), LocalTime.of(10, 0),
                EnergyRating.of(4), SensoryRating.of(3), null, null));

        String feedback = parser.parse(manager, NOW, "today").execute().getFeedback();

        assertTrue(feedback.contains("Today lecture"));
        assertTrue(!feedback.contains("Tomorrow lecture"));
    }

    @Test
    public void parseFind_todayIsCaseInsensitiveAndSatisfiesFilterRequirementAlone() throws Exception {
        LocalDate today = NOW.toLocalDate();
        ActivityManager manager = new ActivityManager();
        manager.add(new FixedActivity(manager.getNextId(), "Today lecture", ActivityCategory.ACADEMIC,
                today, LocalTime.of(9, 0), LocalTime.of(10, 0), EnergyRating.of(4), SensoryRating.of(3), null, null));

        String feedback = parser.parse(manager, NOW, "TODAY").execute().getFeedback();

        assertTrue(feedback.contains("Today lecture"));
    }

    @Test
    public void parseFind_tomorrow_matchesOnlyTomorrowsActivities() throws Exception {
        LocalDate today = NOW.toLocalDate();
        ActivityManager manager = new ActivityManager();
        manager.add(new FixedActivity(manager.getNextId(), "Today lecture", ActivityCategory.ACADEMIC,
                today, LocalTime.of(9, 0), LocalTime.of(10, 0), EnergyRating.of(4), SensoryRating.of(3), null, null));
        manager.add(new FixedActivity(manager.getNextId(), "Tomorrow lecture", ActivityCategory.ACADEMIC,
                today.plusDays(1), LocalTime.of(9, 0), LocalTime.of(10, 0),
                EnergyRating.of(4), SensoryRating.of(3), null, null));

        String feedback = parser.parse(manager, NOW, "tomorrow").execute().getFeedback();

        assertTrue(feedback.contains("Tomorrow lecture"));
        assertTrue(!feedback.contains("Today lecture"));
    }

    @Test
    public void parseFind_thisWeek_matchesActivityInsideCurrentMondayToSundayOnly() throws Exception {
        // NOW is Monday 2026-08-17; the current week runs 2026-08-17 to 2026-08-23.
        ActivityManager manager = new ActivityManager();
        manager.add(new FixedActivity(manager.getNextId(), "This week lecture", ActivityCategory.ACADEMIC,
                LocalDate.of(2026, 8, 21), LocalTime.of(9, 0), LocalTime.of(10, 0),
                EnergyRating.of(4), SensoryRating.of(3), null, null));
        manager.add(new FixedActivity(manager.getNextId(), "Next week lecture", ActivityCategory.ACADEMIC,
                LocalDate.of(2026, 8, 24), LocalTime.of(9, 0), LocalTime.of(10, 0),
                EnergyRating.of(4), SensoryRating.of(3), null, null));

        String feedback = parser.parse(manager, NOW, "this week").execute().getFeedback();

        assertTrue(feedback.contains("This week lecture"));
        assertTrue(!feedback.contains("Next week lecture"));
    }

    @Test
    public void parseFind_nextWeek_matchesActivityInFollowingMondayToSundayOnly() throws Exception {
        ActivityManager manager = new ActivityManager();
        manager.add(new FixedActivity(manager.getNextId(), "This week lecture", ActivityCategory.ACADEMIC,
                LocalDate.of(2026, 8, 21), LocalTime.of(9, 0), LocalTime.of(10, 0),
                EnergyRating.of(4), SensoryRating.of(3), null, null));
        manager.add(new FixedActivity(manager.getNextId(), "Next week lecture", ActivityCategory.ACADEMIC,
                LocalDate.of(2026, 8, 24), LocalTime.of(9, 0), LocalTime.of(10, 0),
                EnergyRating.of(4), SensoryRating.of(3), null, null));

        String feedback = parser.parse(manager, NOW, "next week").execute().getFeedback();

        assertTrue(feedback.contains("Next week lecture"));
        assertTrue(!feedback.contains("This week lecture"));
    }

    @Test
    public void parseFind_relativeDatePhraseCombinedWithMarkerFilters_appliesBoth() throws Exception {
        LocalDate today = NOW.toLocalDate();
        ActivityManager manager = new ActivityManager();
        manager.add(new FixedActivity(manager.getNextId(), "Today CCA session", ActivityCategory.CCA,
                today, LocalTime.of(9, 0), LocalTime.of(10, 0), EnergyRating.of(4), SensoryRating.of(3), null, null));
        manager.add(new FixedActivity(manager.getNextId(), "Today academic lecture", ActivityCategory.ACADEMIC,
                today, LocalTime.of(11, 0), LocalTime.of(12, 0),
                EnergyRating.of(4), SensoryRating.of(3), null, null));

        String feedback = parser.parse(manager, NOW, "today c/ACADEMIC").execute().getFeedback();

        assertTrue(feedback.contains("Today academic lecture"));
        assertTrue(!feedback.contains("Today CCA session"));
    }

    @Test
    public void parseFind_relativeDatePhraseCombinedWithDateMarker_throwsInvalidCommandException() {
        ActivityManager manager = new ActivityManager();

        InvalidCommandException exception = assertThrows(InvalidCommandException.class,
                () -> parser.parse(manager, NOW, "today date/2026-08-20"));
        assertEquals("date/ cannot be combined with today, tomorrow, this week, or next week.",
                exception.getMessage());
    }

    @Test
    public void parseFind_thisWithoutWeek_throwsInvalidCommandException() {
        ActivityManager manager = new ActivityManager();

        assertThrows(InvalidCommandException.class, () -> parser.parse(manager, NOW, "this k/lecture"));
    }

    @Test
    public void parseFind_nextWithoutWeek_throwsInvalidCommandException() {
        ActivityManager manager = new ActivityManager();

        assertThrows(InvalidCommandException.class, () -> parser.parse(manager, NOW, "next k/lecture"));
    }

    @Test
    public void parseFind_relativeDateWordAsSearchKeywordStillWorks() throws Exception {
        // "today" is only treated as a relative-date phrase when it leads the whole argument
        // text; as a k/ value it must remain an ordinary search keyword.
        ActivityManager manager = new ActivityManager();
        manager.add(new FixedActivity(manager.getNextId(), "Plan for today", ActivityCategory.ACADEMIC,
                LocalDate.of(2026, 8, 15), LocalTime.of(9, 0), LocalTime.of(10, 0),
                EnergyRating.of(4), SensoryRating.of(3), null, null));

        String feedback = parser.parse(manager, NOW, "k/today").execute().getFeedback();

        assertTrue(feedback.contains("Plan for today"));
    }

    @Test
    public void parseFind_repeatedKeywordMarker_andCombinesBothKeywordsAcrossOccurrences() throws Exception {
        // History: "find k/Study k/Assignment" originally (Bug F) let the first "k/" silently
        // absorb the second as literal text; a later pass (second-pass QA audit) closed that by
        // rejecting any repeated "k/" outright as a duplicate marker, same as every other prefix.
        // DEFECT-02 (regression report, 2026-08-16) found that rejection itself wrong: find's own
        // documented grammar ("multiple keywords ... use AND") requires k/ to repeat. It must now
        // narrow results to activities matching every repeated keyword, exactly like the existing
        // two-words-in-one-k/ case already does.
        ActivityManager manager = new ActivityManager();
        manager.add(new FixedActivity(manager.getNextId(), "Study", ActivityCategory.ACADEMIC,
                LocalDate.of(2026, 8, 15), LocalTime.of(9, 0), LocalTime.of(10, 0),
                EnergyRating.of(2), SensoryRating.of(2), null, null));
        manager.add(new FixedActivity(manager.getNextId(), "Assignment", ActivityCategory.ACADEMIC,
                LocalDate.of(2026, 8, 15), LocalTime.of(11, 0), LocalTime.of(12, 0),
                EnergyRating.of(2), SensoryRating.of(2), null, null));
        manager.add(new FixedActivity(manager.getNextId(), "Study Assignment", ActivityCategory.ACADEMIC,
                LocalDate.of(2026, 8, 15), LocalTime.of(13, 0), LocalTime.of(14, 0),
                EnergyRating.of(2), SensoryRating.of(2), null, null));

        String feedback = parser.parse(manager, NOW, "k/Study k/Assignment").execute().getFeedback();

        assertTrue(feedback.contains("Found 1 activity"));
        assertTrue(feedback.contains("Study Assignment"));
    }

    @Test
    public void parseFind_repeatedKeywordMarker_reproducesReportCommandAndNarrowsResults() throws Exception {
        // Exact reproduction command from the regression report's DEFECT-02.
        ActivityManager manager = new ActivityManager();
        manager.add(new FixedActivity(manager.getNextId(), "CS2113 lecture", ActivityCategory.ACADEMIC,
                LocalDate.of(2026, 8, 15), LocalTime.of(9, 0), LocalTime.of(10, 0),
                EnergyRating.of(2), SensoryRating.of(2), null, null));
        manager.add(new FixedActivity(manager.getNextId(), "Peer review", ActivityCategory.ACADEMIC,
                LocalDate.of(2026, 8, 15), LocalTime.of(11, 0), LocalTime.of(12, 0),
                EnergyRating.of(2), SensoryRating.of(2), null, null));
        manager.add(new FixedActivity(manager.getNextId(), "CS2113 review session", ActivityCategory.ACADEMIC,
                LocalDate.of(2026, 8, 15), LocalTime.of(13, 0), LocalTime.of(14, 0),
                EnergyRating.of(2), SensoryRating.of(2), null, null));

        String feedback = parser.parse(manager, NOW, "k/CS2113 k/review").execute().getFeedback();

        assertTrue(feedback.contains("Found 1 activity"));
        assertTrue(feedback.contains("CS2113 review session"));
    }

    @Test
    public void parseFind_threeRepeatedKeywordMarkers_allMustMatch() throws Exception {
        ActivityManager manager = new ActivityManager();
        manager.add(new FixedActivity(manager.getNextId(), "one two", ActivityCategory.ACADEMIC,
                LocalDate.of(2026, 8, 15), LocalTime.of(9, 0), LocalTime.of(10, 0),
                EnergyRating.of(2), SensoryRating.of(2), null, null));
        manager.add(new FixedActivity(manager.getNextId(), "one two three", ActivityCategory.ACADEMIC,
                LocalDate.of(2026, 8, 15), LocalTime.of(11, 0), LocalTime.of(12, 0),
                EnergyRating.of(2), SensoryRating.of(2), null, null));

        String feedback = parser.parse(manager, NOW, "k/one k/two k/three").execute().getFeedback();

        assertTrue(feedback.contains("Found 1 activity"));
        assertTrue(feedback.contains("one two three"));
    }

    @Test
    public void parseFind_repeatedKeywordMarkerCombinedWithCategoryFilter_andCombinesBoth() throws Exception {
        ActivityManager manager = new ActivityManager();
        manager.add(new FixedActivity(manager.getNextId(), "CS2113 review session", ActivityCategory.ACADEMIC,
                LocalDate.of(2026, 8, 15), LocalTime.of(9, 0), LocalTime.of(10, 0),
                EnergyRating.of(2), SensoryRating.of(2), null, null));
        manager.add(new FixedActivity(manager.getNextId(), "CS2113 review session", ActivityCategory.CCA,
                LocalDate.of(2026, 8, 15), LocalTime.of(11, 0), LocalTime.of(12, 0),
                EnergyRating.of(2), SensoryRating.of(2), null, null));

        String feedback = parser.parse(manager, NOW, "k/CS2113 k/review c/ACADEMIC").execute().getFeedback();

        assertTrue(feedback.contains("Found 1 activity"));
    }

    @Test
    public void parseFind_repeatedKeywordOccurrenceWithMoreThanTwoWords_throwsInvalidCommandException() {
        // The one-or-two-word cap still applies per occurrence, even though the total number of
        // occurrences is now unlimited.
        ActivityManager manager = new ActivityManager();

        assertThrows(InvalidCommandException.class,
                () -> parser.parse(manager, NOW, "k/one two three k/four"));
    }

    @Test
    public void parseFind_duplicateCategoryMarker_throwsInvalidCommandException() {
        ActivityManager manager = new ActivityManager();

        InvalidCommandException exception = assertThrows(InvalidCommandException.class,
                () -> parser.parse(manager, NOW, "c/ACADEMIC c/CCA"));

        assertTrue(exception.getMessage().contains("Duplicate option \"c/\""));
    }

    // Ordering-semantics regression: ActivityOrder.TIME's comparator only ever compared
    // time-of-day, never date - so two matching activities sharing the same start time on
    // different dates fell through to an ID tie-break instead of the earlier date sorting
    // first. list and find always shared the exact same ActivityManager.sort() method, so this
    // was never a find-specific defect; it just went unnoticed because no existing scripted or
    // JUnit scenario for either command exercised same-time-different-date results under
    // order/time. Fixed by making the TIME case use the identical canonical
    // date-then-time-then-id comparator CHRONOLOGICAL already used, rather than maintaining a
    // second, subtly different one.

    @Test
    public void find_orderTime_sortsAcrossDifferentDatesChronologically() throws Exception {
        ActivityManager manager = new ActivityManager();
        manager.add(new FixedActivity(manager.getNextId(), "QA Lecture", ActivityCategory.ACADEMIC,
                LocalDate.of(2026, 8, 25), LocalTime.of(18, 30), LocalTime.of(19, 0),
                EnergyRating.of(2), SensoryRating.of(2), null, null));
        manager.add(new FixedActivity(manager.getNextId(), "QA Lecture", ActivityCategory.ACADEMIC,
                LocalDate.of(2026, 8, 18), LocalTime.of(18, 30), LocalTime.of(19, 0),
                EnergyRating.of(2), SensoryRating.of(2), null, null));

        String feedback = parser.parse(manager, NOW, "k/QA Lecture order/time").execute().getFeedback();

        assertTrue(feedback.indexOf("[2]") < feedback.indexOf("[1]"),
                "the earlier-dated activity (2026-08-18, id 2) must be listed before the "
                        + "later-dated one (2026-08-25, id 1) even though both share the same "
                        + "18:30 start time: " + feedback);
    }

    @Test
    public void find_orderTime_sortsSameDateByStartTime() throws Exception {
        ActivityManager manager = new ActivityManager();
        manager.add(new FixedActivity(manager.getNextId(), "QA Lecture", ActivityCategory.ACADEMIC,
                LocalDate.of(2026, 8, 18), LocalTime.of(16, 0), LocalTime.of(17, 0),
                EnergyRating.of(2), SensoryRating.of(2), null, null));
        manager.add(new FixedActivity(manager.getNextId(), "QA Lecture", ActivityCategory.ACADEMIC,
                LocalDate.of(2026, 8, 18), LocalTime.of(9, 0), LocalTime.of(10, 0),
                EnergyRating.of(2), SensoryRating.of(2), null, null));

        String feedback = parser.parse(manager, NOW, "k/QA Lecture order/time").execute().getFeedback();

        assertTrue(feedback.indexOf("[2]") < feedback.indexOf("[1]"),
                "the earlier-start-time activity (09:00, id 2) must be listed before the "
                        + "later one (16:00, id 1) on the same date: " + feedback);
    }

    @Test
    public void find_orderTimeMixedFixedFlexible_usesCanonicalOrdering() throws Exception {
        // id 1: adopted flexible activity occupying 2026-08-18 09:00-10:00 (its adopted start,
        // not its earliest/latest window, must be what's compared).
        ActivityManager manager = new ActivityManager();
        manager.add(new FlexibleActivity(manager.getNextId(), "QA Lecture", ActivityCategory.ACADEMIC,
                LocalDate.of(2026, 8, 18), LocalTime.of(8, 0), LocalTime.of(18, 0), 60,
                EnergyRating.of(2), SensoryRating.of(2), null, null, LocalTime.of(9, 0)));
        // id 2: fixed activity on the same date, later in the day.
        manager.add(new FixedActivity(manager.getNextId(), "QA Lecture", ActivityCategory.ACADEMIC,
                LocalDate.of(2026, 8, 18), LocalTime.of(16, 0), LocalTime.of(17, 0),
                EnergyRating.of(2), SensoryRating.of(2), null, null));

        String feedback = parser.parse(manager, NOW, "k/QA Lecture order/time").execute().getFeedback();

        assertTrue(feedback.indexOf("[1]") < feedback.indexOf("[2]"),
                "the adopted flexible activity's 09:00 placement must sort before the fixed "
                        + "activity's 16:00 slot on the same date: " + feedback);
    }

    @Test
    public void find_withoutOrderTime_preservesExistingDefaultOrdering() throws Exception {
        // Regression guard: this fix only changes the TIME case's own comparator - the null
        // order path (saved default) must be completely unaffected. Uses a saved INPUT default
        // (not the out-of-the-box CHRONOLOGICAL default) specifically so a regression that
        // silently forced every find onto chronological ordering would also be caught.
        ActivityManager manager = new ActivityManager();
        manager.setDefaultOrder(ActivityOrder.INPUT);
        manager.add(new FixedActivity(manager.getNextId(), "QA Lecture", ActivityCategory.ACADEMIC,
                LocalDate.of(2026, 8, 25), LocalTime.of(9, 0), LocalTime.of(10, 0),
                EnergyRating.of(2), SensoryRating.of(2), null, null));
        manager.add(new FixedActivity(manager.getNextId(), "QA Lecture", ActivityCategory.ACADEMIC,
                LocalDate.of(2026, 8, 18), LocalTime.of(9, 0), LocalTime.of(10, 0),
                EnergyRating.of(2), SensoryRating.of(2), null, null));

        String feedback = parser.parse(manager, NOW, "k/QA Lecture").execute().getFeedback();

        assertTrue(feedback.indexOf("[1]") < feedback.indexOf("[2]"),
                "with no order/ override and a saved INPUT default, insertion order must remain "
                        + "unaffected by the order/time fix: " + feedback);
    }

}
