package seedu.unienable.command.general;

import seedu.unienable.command.CommandResult;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class GuideCommandTest {
    @Test
    public void execute_nullTopic_showsMainMenu() {
        CommandResult result = new GuideCommand(null).execute();

        assertEquals("Application Guide\n"
                + "\n"
                + "1. Getting started\n"
                + "2. Add, edit and delete activities\n"
                + "3. List, find and view activities\n"
                + "4. Categories and topics\n"
                + "5. Completion and dashboard\n"
                + "6. Recommended timetable\n"
                + "7. Accessible facilities\n"
                + "8. Accessible connections\n"
                + "9. Data files and storage\n"
                + "10. Accessible routes\n"
                + "11. Text timetable\n"
                + "12. Return\n"
                + "\n"
                + "Enter a number from 1 to 12.", result.getFeedback());
    }

    @Test
    public void execute_editTopic_coversMultiFieldTimeAndNoteClearingExamples() {
        CommandResult result = new GuideCommand("edit").execute();
        String feedback = result.getFeedback();

        assertTrue(feedback.startsWith("Edit an activity"));
        assertTrue(feedback.contains("list"));
        assertTrue(feedback.contains("edit 3 n/CG3207 tutorial date/2026-08-17 topic/CG3207"));
        assertTrue(feedback.contains("edit 3 from/14:00 to/15:00"));
        assertTrue(feedback.contains("edit 3 note/"));
        assertTrue(feedback.contains("Stable IDs never"));
        assertTrue(feedback.contains("validated"));
        assertTrue(feedback.contains("y/n"));
    }

    @Test
    public void execute_gettingStartedTopic_showsExactGuideTextWithExamples() {
        CommandResult result = new GuideCommand("getting-started").execute();

        assertEquals("Getting started\n"
                + "Run the JAR, enter guide when needed, and use bye to exit.\n"
                + "Related commands: guide, bye\n"
                + "\n"
                + "Enter guide to see the numbered menu, then either type a number\n"
                + "such as 2 or go straight to a topic by name, for example guide add.\n"
                + "\n"
                + "Examples:\n"
                + "  guide\n"
                + "  guide 2\n"
                + "  guide add\n"
                + "  bye", result.getFeedback());
    }

    @Test
    public void execute_addTopic_showsExactGuideTextWithExamples() {
        CommandResult result = new GuideCommand("add").execute();

        assertEquals("Add activities\n"
                + "Use add with FIXED timing or a FLEXIBLE window and duration.\n"
                + "Related commands: topic add, list, view\n"
                + "\n"
                + "Replace the description, date, times, and ratings below with\n"
                + "your own values.\n"
                + "\n"
                + "Example - add a fixed activity:\n"
                + "  add n/PL1101E Lecture c/ACADEMIC date/2026-08-15 type/FIXED from/09:00 to/11:00 "
                + "energy/1 sensory/1 note/Bring laptop and charger\n"
                + "\n"
                + "Example - add a flexible activity:\n"
                + "  add n/Finish assignment c/ACADEMIC date/2026-08-15 type/FLEXIBLE earliest/12:00 "
                + "latest/18:00 dur/90 energy/4 sensory/2\n"
                + "\n"
                + "Then view your activities:\n"
                + "  list\n"
                + "  list view/detail\n"
                + "  list date/2026-08-15\n"
                + "\n"
                + "Optional - create and use a topic first:\n"
                + "  topic add c/ACADEMIC n/PL1101E\n"
                + "  add n/PL1101E Tutorial c/ACADEMIC date/2026-08-16 type/FIXED from/10:00 to/11:00 "
                + "energy/2 sensory/2 topic/PL1101E", result.getFeedback());
    }

    @Test
    public void execute_addTopicFixedExample_doesNotUseATopicField() {
        // Regression guard for the task requirement: the basic fixed-activity example must not
        // reference topic/ before the guide explains that the topic needs to be created first.
        String firstExample = new GuideCommand("add").execute().getFeedback()
                .split("Example - add a flexible activity:")[0];

        assertTrue(!firstExample.contains("topic/"));
    }

    @Test
    public void execute_findTopic_showsExactGuideTextWithExamples() {
        CommandResult result = new GuideCommand("find").execute();

        assertEquals("Find activities\n"
                + "Format: find [k/KEYWORD ...] [FILTERS]\n"
                + "Multiple keywords and filters use AND. k/ accepts one or two\n"
                + "words; three or more words is rejected.\n"
                + "\n"
                + "Examples:\n"
                + "  find k/PL1101E\n"
                + "  find k/lecture\n"
                + "  find c/ACADEMIC\n"
                + "  find date/2026-08-15\n"
                + "  find k/PL1101E c/ACADEMIC\n"
                + "  find k/finish assignment order/time\n"
                + "\n"
                + "Example - checking when nothing matches:\n"
                + "  find k/nonexistentkeyword12345\n"
                + "\n"
                + "Every supplied keyword and filter must match. Search is\n"
                + "case-insensitive and can match part of a word.", result.getFeedback());
    }

    @Test
    public void execute_topicTopic_showsExactGuideTextWithExamples() {
        CommandResult result = new GuideCommand("topic").execute();

        assertEquals("Categories and topics\n"
                + "Topics are optional one-level groupings inside fixed categories.\n"
                + "Related commands: topic add, topic list, topic rename, topic delete\n"
                + "\n"
                + "Example - create and use a topic:\n"
                + "  topic add c/ACADEMIC n/PL1101E\n"
                + "  topic list c/ACADEMIC\n"
                + "  add n/PL1101E Lecture c/ACADEMIC date/2026-08-15 type/FIXED from/09:00 to/11:00 "
                + "energy/1 sensory/1 topic/PL1101E\n"
                + "\n"
                + "Example - rename a topic:\n"
                + "  topic rename c/ACADEMIC old/PL1101E new/PL1101E Revision\n"
                + "\n"
                + "Renaming asks for y or n before saving, and updates every\n"
                + "activity already using the old topic name.\n"
                + "\n"
                + "Example - delete an unused topic:\n"
                + "  topic delete c/ACADEMIC n/Unused Topic\n"
                + "\n"
                + "A topic cannot be deleted while any activity is still using it.", result.getFeedback());
    }

    @Test
    public void execute_topicIsCaseInsensitive() {
        CommandResult result = new GuideCommand("FIND").execute();

        assertTrue(result.getFeedback().startsWith("Find activities"));
    }

    @Test
    public void execute_recommendTopic_documentsLiveGrammarAndScope() {
        CommandResult result = new GuideCommand("recommend").execute();
        String feedback = result.getFeedback();

        assertTrue(feedback.startsWith("Recommended timetable"));
        assertTrue(!feedback.contains("Coming soon"));
        assertTrue(feedback.contains("recommend this week"));
        assertTrue(feedback.contains("recommend date/YYYY-MM-DD"));
        assertTrue(feedback.contains("recommend view"));
        assertTrue(feedback.contains("recommend adopt"));
        assertTrue(feedback.contains("recommend cancel"));
        assertTrue(feedback.contains("Minimum buffer is enforced"));
        assertTrue(feedback.contains("Tomato"));
    }

    @Test
    public void execute_preferenceTopic_documentsGrammarDefaultsAndAdvisoryScope() {
        String feedback = new GuideCommand("preference").execute().getFeedback();

        assertTrue(feedback.startsWith("Global planning preferences"));
        assertTrue(feedback.contains("preference view"));
        assertTrue(feedback.contains("preference set start/HH:mm"));
        assertTrue(feedback.contains("buffer/MINUTES"));
        assertTrue(feedback.contains("tomato/on|off"));
        assertTrue(feedback.contains("preference reset"));
        assertTrue(feedback.contains("start 08:00, end 20:00, buffer 15 minutes, tomato off"));
        assertTrue(feedback.contains("advisory"));
        assertTrue(feedback.contains("planning inputs for recommend"));
    }

    @Test
    public void execute_routeTopic_isAvailableAndHasRouteExamples() {
        CommandResult result = new GuideCommand("route").execute();
        String feedback = result.getFeedback();

        assertTrue(feedback.startsWith("Accessible routes"));
        assertTrue(!feedback.contains("Coming soon"));
        assertTrue(feedback.contains("route from/AS6 to/AS8"));
        assertTrue(feedback.contains("route from/AS1 to/AS1"));
        assertTrue(feedback.contains("Format: route from/FACILITY to/FACILITY"));
        assertTrue(feedback.contains("Sample local accessibility reference data."));
    }

    @Test
    public void execute_routeTopicIsCaseInsensitive() {
        assertTrue(new GuideCommand("ROUTE").execute().getFeedback().startsWith("Accessible routes"));
    }

    @Test
    public void execute_implementedFacilityTopic_hasNoComingSoonNote() {
        CommandResult result = new GuideCommand("facility").execute();

        assertTrue(!result.getFeedback().contains("Coming soon"));
    }

    @Test
    public void execute_facilityTopic_isAvailableAndHasFacilityExamples() {
        // Regression test for the "separate facility and connection guide topics" bug report.
        CommandResult result = new GuideCommand("facility").execute();
        String feedback = result.getFeedback();

        assertTrue(feedback.startsWith("Accessible facilities"));
        assertTrue(feedback.contains("facility list"));
        assertTrue(feedback.contains("facility view AS1"));
        assertTrue(feedback.contains("facility find type/LIFT"));
        assertTrue(feedback.contains("facility find type/LIFT status/NO"));
    }

    @Test
    public void execute_facilityTopic_doesNotContainConnectionInstructions() {
        CommandResult result = new GuideCommand("facility").execute();
        String feedback = result.getFeedback();

        assertTrue(!feedback.contains("connection list"));
        assertTrue(!feedback.contains("connection view"));
        assertTrue(!feedback.contains("connection find"));
    }

    @Test
    public void execute_facilityTopic_keepsAccessibilityDisclaimer() {
        CommandResult result = new GuideCommand("facility").execute();

        assertTrue(result.getFeedback().contains("Sample local accessibility reference data."));
    }

    @Test
    public void execute_connectionTopic_isAvailableAndHasConnectionExamples() {
        CommandResult result = new GuideCommand("connection").execute();
        String feedback = result.getFeedback();

        assertTrue(feedback.startsWith("Accessible connections"));
        assertTrue(feedback.contains("connection list"));
        assertTrue(feedback.contains("connection view 1"));
        assertTrue(feedback.contains("connection find from/AS6"));
    }

    @Test
    public void execute_connectionTopic_doesNotContainFacilityInstructions() {
        CommandResult result = new GuideCommand("connection").execute();
        String feedback = result.getFeedback();

        assertTrue(!feedback.contains("facility list"));
        assertTrue(!feedback.contains("facility view"));
        assertTrue(!feedback.contains("facility find"));
    }

    @Test
    public void execute_connectionTopic_keepsAccessibilityDisclaimer() {
        CommandResult result = new GuideCommand("connection").execute();

        assertTrue(result.getFeedback().contains("Sample local accessibility reference data."));
    }

    @Test
    public void execute_connectionTopicIsCaseInsensitive() {
        CommandResult result = new GuideCommand("CONNECTION").execute();

        assertTrue(result.getFeedback().startsWith("Accessible connections"));
    }

    @Test
    public void execute_mainMenu_listsBothFacilityAndConnectionAsAvailableTopics() {
        // "guide facility" and "guide connection" resolve independently, each reachable by its
        // own numbered menu item (7 and 8) as well as by keyword - see BUG-01.
        assertTrue(new GuideCommand("facility").execute().getFeedback().startsWith("Accessible facilities"));
        assertTrue(new GuideCommand("connection").execute().getFeedback().startsWith("Accessible connections"));
        assertTrue(!new GuideCommand("facility").execute().getFeedback()
                .equals(new GuideCommand("connection").execute().getFeedback()));
    }

    @Test
    public void execute_storageTopic_doesNotClaimCsvHistoryExists() {
        // Regression test: the "storage" topic previously claimed other generated history paths
        // as present fact; storage guidance must stay aligned with the files UniEnable actually
        // owns today.
        CommandResult result = new GuideCommand("storage").execute();

        assertTrue(!result.getFeedback().contains("exports/"));
        assertTrue(!result.getFeedback().contains("CSV"));
    }

    @Test
    public void execute_deleteTopic_showsGuideText() {
        CommandResult result = new GuideCommand("delete").execute();

        assertTrue(result.getFeedback().startsWith("Delete an activity"));
        assertTrue(result.getFeedback().contains("delete ID"));
        assertTrue(result.getFeedback().contains("never reuses the deleted ID"));
    }

    @Test
    public void execute_viewTopic_showsGuideText() {
        CommandResult result = new GuideCommand("view").execute();

        assertTrue(result.getFeedback().startsWith("View one activity"));
        assertTrue(result.getFeedback().contains("view ID"));
    }

    @Test
    public void execute_listTopic_coversViewsFiltersAndRelativeDates() {
        CommandResult result = new GuideCommand("list").execute();
        String feedback = result.getFeedback();

        assertTrue(feedback.startsWith("List activities"));
        assertTrue(feedback.contains("view/concise"));
        assertTrue(feedback.contains("view/detail"));
        assertTrue(feedback.contains("status/all|completed|incomplete"));
        assertTrue(feedback.contains("c/CATEGORY"));
        assertTrue(feedback.contains("topic/TOPIC"));
        assertTrue(feedback.contains("date/YYYY-MM-DD"));
        assertTrue(feedback.contains("order/input|time|chronological"));
        assertTrue(feedback.contains("today"));
        assertTrue(feedback.contains("tomorrow"));
        assertTrue(feedback.contains("this week"));
        assertTrue(feedback.contains("list this week status/incomplete c/ACADEMIC order/time"));
    }

    @Test
    public void execute_completionTopic_coversMarkUnmarkAndStatusFilters() {
        CommandResult result = new GuideCommand("completion").execute();
        String feedback = result.getFeedback();

        assertTrue(feedback.startsWith("Track completion"));
        assertTrue(feedback.contains("mark 3"));
        assertTrue(feedback.contains("unmark 3"));
        assertTrue(feedback.contains("list status/completed"));
        assertTrue(feedback.contains("list status/incomplete"));
        assertTrue(feedback.contains("reversible and need no confirmation"));
    }

    @Test
    public void execute_resetTopic_showsGuideText() {
        CommandResult result = new GuideCommand("reset").execute();

        assertTrue(result.getFeedback().startsWith("Reset all user data"));
        assertTrue(result.getFeedback().contains("reset all"));
    }

    @Test
    public void execute_markTopic_showsGuideText() {
        CommandResult result = new GuideCommand("mark").execute();

        assertTrue(result.getFeedback().startsWith("Mark an activity as completed"));
    }

    @Test
    public void execute_unmarkTopic_showsGuideText() {
        CommandResult result = new GuideCommand("unmark").execute();

        assertTrue(result.getFeedback().startsWith("Change an activity back to incomplete"));
    }

    @Test
    public void execute_nextTopic_showsGuideText() {
        CommandResult result = new GuideCommand("next").execute();

        assertTrue(result.getFeedback().startsWith("Find your next relevant activity"));
    }

    @Test
    public void execute_orderTopic_showsGuideText() {
        CommandResult result = new GuideCommand("order").execute();

        assertTrue(result.getFeedback().startsWith("Choose your default activity order"));
        assertTrue(result.getFeedback().contains("order set input|time|chronological"));
    }

    @Test
    public void execute_unknownTopic_showsFallbackMessage() {
        CommandResult result = new GuideCommand("bogus").execute();

        assertEquals("No guide topic named \"bogus\". Enter guide to see the list of topics.",
                result.getFeedback());
    }

    @Test
    public void execute_menuNumberOne_resolvesToGettingStarted() {
        CommandResult result = new GuideCommand("1").execute();

        assertTrue(result.getFeedback().startsWith("Getting started"));
    }

    @Test
    public void execute_menuNumberTwo_resolvesToActivitiesOverview() {
        CommandResult result = new GuideCommand("2").execute();

        assertTrue(result.getFeedback().startsWith("Add, edit and delete activities"));
        assertTrue(result.getFeedback().contains("add"));
        assertTrue(result.getFeedback().contains("edit"));
        assertTrue(result.getFeedback().contains("delete"));
    }

    @Test
    public void execute_menuNumberTwoAgreesWithItsOwnKeyword() {
        assertEquals(new GuideCommand("2").execute().getFeedback(),
                new GuideCommand("activities").execute().getFeedback());
    }

    @Test
    public void execute_menuNumberThree_resolvesToBrowseOverview() {
        CommandResult result = new GuideCommand("3").execute();

        assertTrue(result.getFeedback().startsWith("List, find and view activities"));
        assertTrue(result.getFeedback().contains("list"));
        assertTrue(result.getFeedback().contains("find"));
        assertTrue(result.getFeedback().contains("view"));
    }

    @Test
    public void execute_menuNumberThreeAgreesWithItsOwnKeyword() {
        assertEquals(new GuideCommand("3").execute().getFeedback(),
                new GuideCommand("browse").execute().getFeedback());
    }

    @Test
    public void execute_menuNumberFour_resolvesToTopic() {
        CommandResult result = new GuideCommand("4").execute();

        assertTrue(result.getFeedback().startsWith("Categories and topics"));
    }

    @Test
    public void execute_menuNumberFive_resolvesToDashboard() {
        CommandResult result = new GuideCommand("5").execute();

        assertTrue(result.getFeedback().startsWith("Dashboard"));
    }

    @Test
    public void execute_menuNumberFiveAgreesWithItsOwnKeyword() {
        assertEquals(new GuideCommand("5").execute().getFeedback(),
                new GuideCommand("dashboard").execute().getFeedback());
    }

    @Test
    public void execute_dashboardTopic_isAvailableAndHasDashboardExamples() {
        CommandResult result = new GuideCommand("dashboard").execute();
        String feedback = result.getFeedback();

        assertTrue(feedback.startsWith("Dashboard"));
        assertTrue(!feedback.contains("Coming soon"));
        assertTrue(feedback.contains("dashboard today"));
        assertTrue(feedback.contains("dashboard this week detail"));
        assertTrue(feedback.contains("Format: dashboard today|tomorrow|date/YYYY-MM-DD|this week [detail]"));
    }

    @Test
    public void execute_dashboardTopicIsCaseInsensitive() {
        assertTrue(new GuideCommand("DASHBOARD").execute().getFeedback().startsWith("Dashboard"));
    }

    @Test
    public void execute_timetableTopic_isAvailableAndHasExactGrammar() {
        String feedback = new GuideCommand("timetable").execute().getFeedback();

        assertTrue(feedback.startsWith("Text timetable"));
        assertTrue(!feedback.contains("Coming soon"));
        assertTrue(feedback.contains("timetable day/YYYY-MM-DD [detail]"));
        assertTrue(feedback.contains("timetable week/YYYY-MM-DD [compact|detail]"));
        assertTrue(feedback.contains("timetable this week [compact|detail]"));
        assertTrue(feedback.contains("read-only"));
    }

    @Test
    public void execute_timetableTopicIsCaseInsensitive() {
        assertTrue(new GuideCommand("TIMETABLE").execute().getFeedback().startsWith("Text timetable"));
    }

    @Test
    public void execute_menuNumberSix_resolvesToRecommend() {
        CommandResult result = new GuideCommand("6").execute();

        assertTrue(result.getFeedback().startsWith("Recommended timetable"));
    }

    @Test
    public void execute_menuNumberSeven_resolvesToFacility() {
        // Regression test for BUG-01 (v1.0 manual release test, 2026-08-01): the main guide
        // previously combined facility and connection under one item 7, resolving to a merged
        // overview instead of exposing each as its own numbered, uniquely-mapped topic.
        CommandResult result = new GuideCommand("7").execute();

        assertTrue(result.getFeedback().startsWith("Accessible facilities"));
        assertTrue(result.getFeedback().contains("facility list"));
    }

    @Test
    public void execute_menuNumberSevenAgreesWithItsOwnKeyword() {
        assertEquals(new GuideCommand("7").execute().getFeedback(),
                new GuideCommand("facility").execute().getFeedback());
    }

    @Test
    public void execute_menuNumberEight_resolvesToConnection() {
        CommandResult result = new GuideCommand("8").execute();

        assertTrue(result.getFeedback().startsWith("Accessible connections"));
        assertTrue(result.getFeedback().contains("connection list"));
    }

    @Test
    public void execute_menuNumberEightAgreesWithItsOwnKeyword() {
        assertEquals(new GuideCommand("8").execute().getFeedback(),
                new GuideCommand("connection").execute().getFeedback());
    }

    @Test
    public void execute_menuNumberSevenAndEight_areDistinctTopics() {
        // The core BUG-01 assertion: items 7 and 8 must map to genuinely different, independently
        // reachable topics, not the same combined overview.
        assertTrue(!new GuideCommand("7").execute().getFeedback()
                .equals(new GuideCommand("8").execute().getFeedback()));
    }

    @Test
    public void execute_everyNumberedMenuMapping_resolvesToItsAdvertisedTopic() {
        // Every numbered menu item from 1-11 must resolve to a real, distinct topic (never
        // falling back to the "No guide topic named" error), and the topic text it shows must
        // start with the same subject the main menu line advertises for that number.
        assertTrue(new GuideCommand("1").execute().getFeedback().startsWith("Getting started"));
        assertTrue(new GuideCommand("2").execute().getFeedback().startsWith("Add, edit and delete activities"));
        assertTrue(new GuideCommand("3").execute().getFeedback().startsWith("List, find and view activities"));
        assertTrue(new GuideCommand("4").execute().getFeedback().startsWith("Categories and topics"));
        assertTrue(new GuideCommand("5").execute().getFeedback().startsWith("Dashboard"));
        assertTrue(new GuideCommand("6").execute().getFeedback().startsWith("Recommended timetable"));
        assertTrue(new GuideCommand("7").execute().getFeedback().startsWith("Accessible facilities"));
        assertTrue(new GuideCommand("8").execute().getFeedback().startsWith("Accessible connections"));
        assertTrue(new GuideCommand("9").execute().getFeedback().startsWith("Data files and storage"));
        assertTrue(new GuideCommand("10").execute().getFeedback().startsWith("Accessible routes"));
        assertTrue(new GuideCommand("11").execute().getFeedback().startsWith("Text timetable"));
    }

    @Test
    public void execute_menuNumberTen_resolvesToRoute() {
        CommandResult result = new GuideCommand("10").execute();

        assertTrue(result.getFeedback().startsWith("Accessible routes"));
    }

    @Test
    public void execute_menuNumberTenAgreesWithItsOwnKeyword() {
        assertEquals(new GuideCommand("10").execute().getFeedback(),
                new GuideCommand("route").execute().getFeedback());
    }

    @Test
    public void execute_menuNumberNine_resolvesToStorage() {
        CommandResult result = new GuideCommand("9").execute();

        assertTrue(result.getFeedback().startsWith("Data files and storage"));
    }

    @Test
    public void execute_menuNumberEleven_resolvesToTimetable() {
        CommandResult result = new GuideCommand("11").execute();

        assertTrue(result.getFeedback().startsWith("Text timetable"));
    }

    @Test
    public void execute_menuNumberTwelve_resolvesToTimetable() {
        CommandResult result = new GuideCommand("12").execute();

        assertEquals("Returning to the command prompt.", result.getFeedback());
    }

    @Test
    public void execute_menuNumberThirteen_returnsWithoutShowingATopic() {
        CommandResult result = new GuideCommand("13").execute();

        assertEquals("No guide topic named \"13\". Enter guide to see the list of topics.",
                result.getFeedback());
    }

    @Test
    public void execute_menuNumberOutOfRangeBelow_showsFallbackMessage() {
        // "0" is deliberately not treated as a menu number, matching CommandDispatcher's dispatch
        // table which also only recognises "1" through "13" as bare commands.
        CommandResult result = new GuideCommand("0").execute();

        assertEquals("No guide topic named \"0\". Enter guide to see the list of topics.",
                result.getFeedback());
    }

    @Test
    public void execute_menuNumberOutOfRangeAbove_showsFallbackMessage() {
        CommandResult result = new GuideCommand("13").execute();

        assertEquals("No guide topic named \"13\". Enter guide to see the list of topics.",
                result.getFeedback());
    }
}
