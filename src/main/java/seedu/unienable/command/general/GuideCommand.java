package seedu.unienable.command.general;

import java.util.LinkedHashMap;
import java.util.Locale;
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
     * The topic each numbered menu item resolves to, in menu order (index 0 is item "1"). Menu
     * items that span more than one command (e.g. item 2 covers add/edit/delete, item 3 covers
     * list/find/view) map to a dedicated overview topic ("activities", "browse") rather than to a
     * single command's own topic, so the numbered entry and its own keyword always agree with
     * each other; each overview topic points onward to the individual command topics for detail.
     * Item 10 ("Return") is handled separately, not through this list.
     */
    private static final String[] MENU_NUMBER_TOPICS = {
        "getting-started", "activities", "browse", "topic", "dashboard",
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
        String text = TOPICS.get(resolveMenuNumber(topic).toLowerCase(Locale.ROOT));
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
        topics.put("activities", "Add, edit and delete activities\n"
                + "add    - create a FIXED or FLEXIBLE activity. See: guide add\n"
                + "edit   - change one or more fields of an existing activity. See: guide edit\n"
                + "delete - remove an activity by its stable ID. See: guide delete\n"
                + "\n"
                + "Quick examples:\n"
                + "  add n/CG3207 lecture c/ACADEMIC date/2026-08-15 type/FIXED from/09:00 to/11:00 "
                + "energy/1 sensory/1\n"
                + "  edit 1 energy/5\n"
                + "  delete 1");
        topics.put("browse", "List, find and view activities\n"
                + "list - show activities matching optional filters. See: guide list\n"
                + "find - search by keyword and/or filter. See: guide find\n"
                + "view - show every field of one activity. See: guide view\n"
                + "\n"
                + "Quick examples:\n"
                + "  list today\n"
                + "  find k/lecture\n"
                + "  view 1");
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
        topics.put("view", "View one activity\n"
                + "Format: view ID\n"
                + "Shows every field of a single activity, including its note.\n"
                + "\n"
                + "Example:\n"
                + "  list\n"
                + "  view 3");
        topics.put("list", "List activities\n"
                + "Format: list [today|tomorrow|this week] [FILTERS]\n"
                + "Every filter is optional and combinable. Results use your saved\n"
                + "default order (see guide order) unless order/ORDER is given, which\n"
                + "overrides it for this one command only.\n"
                + "\n"
                + "Views:\n"
                + "  view/concise (default) - one line per activity\n"
                + "  view/detail - every field, including the note\n"
                + "\n"
                + "Filters:\n"
                + "  status/all|completed|incomplete\n"
                + "  c/CATEGORY\n"
                + "  topic/TOPIC\n"
                + "  date/YYYY-MM-DD\n"
                + "  order/input|time|chronological\n"
                + "\n"
                + "Relative dates (cannot combine with date/ or with each other):\n"
                + "  today     - activities on the current date\n"
                + "  tomorrow  - activities on the next date\n"
                + "  this week - activities from Monday through Sunday of the current week\n"
                + "\n"
                + "Examples:\n"
                + "  list\n"
                + "  list view/detail\n"
                + "  list status/incomplete\n"
                + "  list c/ACADEMIC topic/CG3207\n"
                + "  list date/2026-08-15\n"
                + "  list today\n"
                + "  list tomorrow view/detail\n"
                + "  list this week status/incomplete c/ACADEMIC order/time");
        topics.put("edit", "Edit an activity\n"
                + "Format: edit ID PREFIX/NEW_VALUE [PREFIX/NEW_VALUE ...]\n"
                + "Any subset of fields may be supplied, in any order. Stable IDs never\n"
                + "change. Every change is validated before the y/n prompt is shown.\n"
                + "\n"
                + "Example - list first, then edit one field:\n"
                + "  list\n"
                + "  edit 3 energy/5\n"
                + "\n"
                + "Example - change several fields at once:\n"
                + "  edit 3 n/CG3207 tutorial date/2026-08-17 topic/CG3207\n"
                + "\n"
                + "Example - change a fixed activity's time:\n"
                + "  edit 3 from/14:00 to/15:00\n"
                + "\n"
                + "Example - clear an existing note:\n"
                + "  edit 3 note/\n"
                + "\n"
                + "After reviewing the before/after summary, answer y to save or n to\n"
                + "cancel (anything other than y is treated as n).");
        topics.put("delete", "Delete an activity\n"
                + "Format: delete ID\n"
                + "Shows the selected activity and asks for y/n before removing it.\n"
                + "Deleting an activity never changes any other activity's ID, and a\n"
                + "future add never reuses the deleted ID; use reset all to restart ID\n"
                + "assignment from [1] (see guide reset).\n"
                + "\n"
                + "Example:\n"
                + "  list\n"
                + "  delete 3");
        topics.put("completion", "Track completion\n"
                + "Format: mark ID, or unmark ID\n"
                + "Related commands: list status/completed, list status/incomplete\n"
                + "\n"
                + "Examples:\n"
                + "  mark 3\n"
                + "  unmark 3\n"
                + "  list status/completed\n"
                + "  list status/incomplete\n"
                + "\n"
                + "mark and unmark are reversible and need no confirmation, unlike\n"
                + "delete, edit, topic rename, and topic delete, which do.");
        topics.put("mark", "Mark an activity as completed\n"
                + "Format: mark ID\n"
                + "Related commands: unmark\n"
                + "See guide completion for the full completion-tracking workflow.\n"
                + "\n"
                + "Example:\n"
                + "  mark 3");
        topics.put("unmark", "Change an activity back to incomplete\n"
                + "Format: unmark ID\n"
                + "Related commands: mark\n"
                + "See guide completion for the full completion-tracking workflow.\n"
                + "\n"
                + "Example:\n"
                + "  unmark 3");
        topics.put("next", "Find your next relevant activity\n"
                + "Format: next\n"
                + "Shows an incomplete fixed activity currently in progress if there is\n"
                + "one, otherwise the nearest upcoming incomplete fixed activity,\n"
                + "otherwise the incomplete flexible activity whose window ends soonest.\n"
                + "\n"
                + "Example:\n"
                + "  next");
        topics.put("order", "Choose your default activity order\n"
                + "Format: order view, or order set input|time|chronological\n"
                + "Use the one-shot order/ORDER filter on list/find to override the\n"
                + "saved default for a single command.\n"
                + "\n"
                + "Examples:\n"
                + "  order view\n"
                + "  order set time\n"
                + "  list order/chronological");
        topics.put("reset", "Reset all user data\n"
                + "Format: reset all\n"
                + "Clears every activity and topic, resets your saved default order to\n"
                + "chronological, and resets the next activity ID back to [1].\n"
                + "Facility and connection reference data is kept.\n"
                + "\n"
                + "Example:\n"
                + "  reset all\n"
                + "\n"
                + "Shows a preview (how many activities and topics would be deleted) and\n"
                + "asks for y/n before making any change. This cannot be undone.");
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
                + "Completion tracking is available now - see guide completion for\n"
                + "mark, unmark, and list status/....\n"
                + "\n"
                + "A dedicated completion/workload dashboard (dashboard today,\n"
                + "tomorrow, YYYY-MM-DD, or this week, with a detail rating\n"
                + "distribution) is coming soon in a future release.");
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
                + "Application data (activities, topics, and your saved default order)\n"
                + "is stored under data/. Do not edit data files while the application runs.");
        return topics;
    }
}
