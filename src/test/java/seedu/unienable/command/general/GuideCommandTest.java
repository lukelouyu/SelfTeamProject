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
                + "7. Accessible facilities and routes\n"
                + "8. CSV export\n"
                + "9. Data files and storage\n"
                + "10. Return\n"
                + "\n"
                + "Enter a number from 1 to 10.", result.getFeedback());
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
                + "Multiple keywords and filters use AND.\n"
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
    public void execute_v2OnlyTopic_appendsComingSoonNote() {
        CommandResult result = new GuideCommand("route").execute();

        assertTrue(result.getFeedback().endsWith("(Coming soon in a future release.)"));
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
        // "guide facility" and "guide connection" resolve independently even though neither
        // keyword literally appears in the numbered main menu text (which groups them under item
        // 7, like every other multi-command menu item) - this asserts both are genuinely
        // reachable and distinct, not that the word "connection" appears in MAIN_MENU's text.
        assertTrue(new GuideCommand("facility").execute().getFeedback().startsWith("Accessible facilities"));
        assertTrue(new GuideCommand("connection").execute().getFeedback().startsWith("Accessible connections"));
        assertTrue(!new GuideCommand("facility").execute().getFeedback()
                .equals(new GuideCommand("connection").execute().getFeedback()));
    }

    @Test
    public void execute_storageTopic_doesNotClaimCsvHistoryExists() {
        // Regression test: the "storage" topic previously said "CSV history is under exports/"
        // as present fact, even though CSV export is v2.0 scope with no working command yet -
        // directly contradicted by the "export" topic's own "(Coming soon...)" note.
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

        assertTrue(result.getFeedback().startsWith("Completion and daily load"));
    }

    @Test
    public void execute_menuNumberSix_resolvesToRecommend() {
        CommandResult result = new GuideCommand("6").execute();

        assertTrue(result.getFeedback().startsWith("Recommended timetable"));
    }

    @Test
    public void execute_menuNumberSeven_resolvesToAccessibilityOverview() {
        // Regression test for the "separate facility and connection guide topics" bug report:
        // item 7 previously resolved straight to the "facility" topic, whose text (before this
        // fix) happened to also describe connection commands; now that "facility" describes only
        // facility commands, item 7 must resolve to an overview that genuinely covers both, the
        // same overview-topic pattern already used for items 2 and 3.
        CommandResult result = new GuideCommand("7").execute();
        String feedback = result.getFeedback();

        assertTrue(feedback.startsWith("Accessible facilities and routes"));
        assertTrue(feedback.contains("facility"));
        assertTrue(feedback.contains("connection"));
    }

    @Test
    public void execute_menuNumberSevenAgreesWithItsOwnKeyword() {
        assertEquals(new GuideCommand("7").execute().getFeedback(),
                new GuideCommand("accessibility").execute().getFeedback());
    }

    @Test
    public void execute_everyNumberedMenuMapping_resolvesToItsAdvertisedTopic() {
        // Every numbered menu item from 1-9 must resolve to a real, distinct topic (never falling
        // back to the "No guide topic named" error), and the topic text it shows must start with
        // the same subject the main menu line advertises for that number.
        assertTrue(new GuideCommand("1").execute().getFeedback().startsWith("Getting started"));
        assertTrue(new GuideCommand("2").execute().getFeedback().startsWith("Add, edit and delete activities"));
        assertTrue(new GuideCommand("3").execute().getFeedback().startsWith("List, find and view activities"));
        assertTrue(new GuideCommand("4").execute().getFeedback().startsWith("Categories and topics"));
        assertTrue(new GuideCommand("5").execute().getFeedback().startsWith("Completion and daily load"));
        assertTrue(new GuideCommand("6").execute().getFeedback().startsWith("Recommended timetable"));
        assertTrue(new GuideCommand("7").execute().getFeedback().startsWith("Accessible facilities and routes"));
        assertTrue(new GuideCommand("8").execute().getFeedback().startsWith("CSV exports"));
        assertTrue(new GuideCommand("9").execute().getFeedback().startsWith("Data files and storage"));
    }

    @Test
    public void execute_menuNumberEight_resolvesToExport() {
        CommandResult result = new GuideCommand("8").execute();

        assertTrue(result.getFeedback().startsWith("CSV exports"));
    }

    @Test
    public void execute_menuNumberNine_resolvesToStorage() {
        CommandResult result = new GuideCommand("9").execute();

        assertTrue(result.getFeedback().startsWith("Data files and storage"));
    }

    @Test
    public void execute_menuNumberTen_returnsWithoutShowingATopic() {
        CommandResult result = new GuideCommand("10").execute();

        assertEquals("Returning to the command prompt.", result.getFeedback());
    }

    @Test
    public void execute_menuNumberOutOfRangeBelow_showsFallbackMessage() {
        // "0" is deliberately not treated as a menu number, matching CommandDispatcher's dispatch
        // table which also only recognises "1" through "10" as bare commands.
        CommandResult result = new GuideCommand("0").execute();

        assertEquals("No guide topic named \"0\". Enter guide to see the list of topics.",
                result.getFeedback());
    }

    @Test
    public void execute_menuNumberOutOfRangeAbove_showsFallbackMessage() {
        CommandResult result = new GuideCommand("11").execute();

        assertEquals("No guide topic named \"11\". Enter guide to see the list of topics.",
                result.getFeedback());
    }
}
