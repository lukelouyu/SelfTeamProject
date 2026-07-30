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
        CommandResult result = new GuideCommand("find").execute();

        assertEquals("Find activities\n"
                + "Format: find [k/KEYWORD ...] [FILTERS]\n"
                + "Multiple keywords and filters use AND.", result.getFeedback());
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
    public void execute_unknownTopic_showsFallbackMessage() {
        CommandResult result = new GuideCommand("bogus").execute();

        assertEquals("No guide topic named \"bogus\". Enter guide to see the list of topics.",
                result.getFeedback());
    }
}
