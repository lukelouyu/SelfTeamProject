package seedu.unienable.command.general;

import java.util.LinkedHashMap;
import java.util.Map;

import seedu.unienable.command.Command;
import seedu.unienable.command.CommandResult;
import seedu.unienable.command.accessibility.AccessibilityDisclaimer;

/**
 * Displays the built-in application guide: either the main numbered menu or one topic's text.
 *
 * <p>The main menu's items can be selected either by their existing text keyword (e.g. "add") or
 * by the number shown next to them in the menu (e.g. "1"), both as the argument to "guide" and as
 * a bare top-level command entered right after the menu is displayed. Menu item 10 ("Return") is
 * not a topic; it's a no-op that acknowledges the selection instead of showing topic text.
 */
public class GuideCommand extends Command {
    private static final String COMING_SOON_NOTE = "\n(Coming soon in a future release.)";
    private static final String RETURN_MESSAGE = "Returning to the command prompt.";

    /**
     * The topic each numbered menu item resolves to, in menu order (index 0 is item "1"). Some
     * menu items span more than one existing topic keyword (e.g. item 2 covers add/edit/delete);
     * those map to the single most representative keyword rather than a topic that doesn't exist.
     * Item 10 ("Return") is handled separately, not through this list.
     */
    private static final String[] MENU_NUMBER_TOPICS = {
        "getting-started", "add", "find", "topic", "dashboard",
        "recommend", "facility", "export", "storage",
    };

    private static final String MAIN_MENU = "Application Guide\n\n"
            + "1. Getting started\n"
            + "2. Add, edit and delete activities\n"
            + "3. List, find and view activities\n"
            + "4. Categories and topics\n"
            + "5. Completion and dashboard\n"
            + "6. Recommended timetable\n"
            + "7. Accessible facilities and routes\n"
            + "8. CSV export\n"
            + "9. Data files and storage\n"
            + "10. Return\n\n"
            + "Enter a number from 1 to 10.";

    private static final Map<String, String> TOPICS = buildTopics();

    private final String topic;

    /**
     * Creates a GuideCommand.
     *
     * @param topic the topic keyword or menu number after "guide", or null for the main menu
     */
    public GuideCommand(String topic) {
        this.topic = topic;
    }

    @Override
    public CommandResult execute() {
        if (topic == null) {
            return new CommandResult(MAIN_MENU);
        }
        if (topic.equals("10")) {
            return new CommandResult(RETURN_MESSAGE);
        }
        String text = TOPICS.get(resolveMenuNumber(topic).toLowerCase());
        if (text == null) {
            return new CommandResult("No guide topic named \"" + topic + "\". Enter guide to see the list "
                    + "of topics.");
        }
        return new CommandResult(text);
    }

    /**
     * Translates a main-menu number (e.g. "1") into the topic keyword it stands for. A value that
     * isn't a whole number from 1 to {@code MENU_NUMBER_TOPICS.length}, including topic keywords
     * themselves, is returned unchanged.
     *
     * @param rawTopic the raw topic argument supplied to "guide"
     * @return the resolved topic keyword
     */
    private String resolveMenuNumber(String rawTopic) {
        int number;
        try {
            number = Integer.parseInt(rawTopic);
        } catch (NumberFormatException e) {
            return rawTopic;
        }
        return number >= 1 && number <= MENU_NUMBER_TOPICS.length ? MENU_NUMBER_TOPICS[number - 1] : rawTopic;
    }

    private static Map<String, String> buildTopics() {
        Map<String, String> topics = new LinkedHashMap<>();
        topics.put("getting-started", "Getting started\n"
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
                + "  bye");
        topics.put("add", "Add activities\n"
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
                + "energy/2 sensory/2 topic/PL1101E");
        topics.put("edit", "Edit an activity\n"
                + "Format: edit ID PREFIX/NEW_VALUE [PREFIX/NEW_VALUE ...]\n"
                + "The application validates all changes before asking for y/n.");
        topics.put("find", "Find activities\n"
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
                + "case-insensitive and can match part of a word.");
        topics.put("topic", "Categories and topics\n"
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
                + "A topic cannot be deleted while any activity is still using it.");
        topics.put("dashboard", "Completion and daily load\n"
                + "Use dashboard today, tomorrow, YYYY-MM-DD, or this week.\n"
                + "Add detail to display the full 1-to-5 rating distribution."
                + COMING_SOON_NOTE);
        topics.put("timetable", "Text timetable\n"
                + "Use timetable day/DATE or timetable week/START_DATE.\n"
                + "Use timetable item/ID to inspect one entry."
                + COMING_SOON_NOTE);
        topics.put("recommend", "Recommended timetable\n"
                + "Use recommend PERIOD [PREFERENCE_OVERRIDES].\n"
                + "Review the plan before choosing whether to adopt it."
                + COMING_SOON_NOTE);
        topics.put("facility", "Accessible facilities\n"
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
                + AccessibilityDisclaimer.TEXT);
        topics.put("route", "Accessible routes\n"
                + "Format: route from/START_FACILITY to/DESTINATION_FACILITY\n"
                + "The planner returns one best confirmed route from local data."
                + COMING_SOON_NOTE);
        topics.put("export", "CSV exports\n"
                + "Use export activities, export schedule, export all, or export.\n"
                + "Each export creates a timestamped historical record."
                + COMING_SOON_NOTE);
        topics.put("storage", "Data files and storage\n"
                + "Application data is stored under data/. CSV history is under\n"
                + "exports/. Do not edit data files while the application runs.");
        return topics;
    }
}
