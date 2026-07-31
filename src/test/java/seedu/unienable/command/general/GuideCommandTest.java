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
    public void execute_implementedTopic_showsExactGuideText() {
        // "edit" has no worked examples (out of scope for this pass), so it's still a compact,
        // stable exact-text check; the topics with new examples (1-4) get their own tests below.
        CommandResult result = new GuideCommand("edit").execute();

        assertEquals("Edit an activity\n"
                + "Format: edit ID PREFIX/NEW_VALUE [PREFIX/NEW_VALUE ...]\n"
                + "The application validates all changes before asking for y/n.", result.getFeedback());
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
    public void execute_facilityTopic_showsExactGuideTextWithExamples() {
        CommandResult result = new GuideCommand("facility").execute();

        assertEquals("Accessible facilities\n"
                + "Use facility list, facility view, or facility find (and connection list/view/find\n"
                + "for the route graph between facilities). This data is read-only.\n"
                + "\n"
                + "Example - list and view a facility:\n"
                + "  facility list\n"
                + "  facility view AS1\n"
                + "\n"
                + "Example - find facilities with a feature:\n"
                + "  facility find type/LIFT\n"
                + "  facility find type/LIFT status/NO\n"
                + "\n"
                + "Example - list and view a connection:\n"
                + "  connection list\n"
                + "  connection view 1\n"
                + "\n"
                + "Example - find connections from a facility:\n"
                + "  connection find from/AS6\n"
                + "\n"
                + "Sample local accessibility reference data. Distances are estimates and may be "
                + "incomplete. Please verify with current campus information when needed.", result.getFeedback());
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
    public void execute_menuNumberTwo_resolvesToAdd() {
        CommandResult result = new GuideCommand("2").execute();

        assertTrue(result.getFeedback().startsWith("Add activities"));
    }

    @Test
    public void execute_menuNumberThree_resolvesToFind() {
        CommandResult result = new GuideCommand("3").execute();

        assertTrue(result.getFeedback().startsWith("Find activities"));
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
    public void execute_menuNumberSeven_resolvesToFacility() {
        CommandResult result = new GuideCommand("7").execute();

        assertTrue(result.getFeedback().startsWith("Accessible facilities"));
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
