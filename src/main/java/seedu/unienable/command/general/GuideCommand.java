package seedu.unienable.command.general;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

import seedu.unienable.command.Command;
import seedu.unienable.command.CommandResult;
import seedu.unienable.command.accessibility.common.AccessibilityDisclaimer;

/**
 * Displays the built-in application guide: either the main numbered menu or one topic's text.
 *
 * <p>The main menu's items can be selected either by their existing text keyword (e.g. "add") or
 * by the number shown next to them in the menu (e.g. "1"), both as the argument to "guide" and as
 * a bare top-level command entered right after the menu is displayed. Menu item 12 ("Return") is
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
     * Facility and connection are independent command families with independent guide topics, so
     * unlike "activities"/"browse" they each get their own direct number (7 and 8) rather than
     * being folded into one combined overview. Item 11 ("Route search") was added as the next
     * available number when v2.0's {@code route} shipped. Item 11 ("Text timetable") was added
     * when v2.0's {@code timetable} shipped. Item 12 ("Return") is handled separately, not
     * through this list.
     */
    private static final String[] MENU_NUMBER_TOPICS = {
        "getting-started", "activities", "browse", "topic", "dashboard",
        "recommend", "facility", "connection", "storage", "route", "timetable",
    };

    private static final String MAIN_MENU = "Application Guide\n\n"
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
            + "12. Return\n\n"
            + "Enter a number from 1 to 12.";

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
        if (topic.equals("12")) {
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
                + "recur  - turn one FIXED class session into a whole semester of "
                + "independent sessions. See: guide recur\n"
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
                + "Format: list [today|tomorrow|this week|next week|overdue] [FILTERS]\n"
                + "Every filter is optional and combinable. Results use your saved\n"
                + "default order (see guide order) unless order/ORDER is given, which\n"
                + "overrides it for this one command only.\n"
                + "\n"
                + "Views:\n"
                + "  view/concise (default) - one line per activity\n"
                + "  view/detail - every field, including the note\n"
                + "\n"
                + "Filters:\n"
                + "  status/all|completed|incomplete (not with overdue - see below)\n"
                + "  c/CATEGORY\n"
                + "  topic/TOPIC\n"
                + "  date/YYYY-MM-DD\n"
                + "  order/input|time|chronological\n"
                + "\n"
                + "Relative dates (cannot combine with date/ or with each other):\n"
                + "  today     - activities on the current date\n"
                + "  tomorrow  - activities on the next date\n"
                + "  this week - activities from Monday through Sunday of the current week\n"
                + "  next week - activities from Monday through Sunday of the following week\n"
                + "\n"
                + "overdue is different: it shows incomplete activities whose scheduled\n"
                + "time has already passed. It's a separate view, not a replacement -\n"
                + "plain list still shows every activity regardless of overdue status.\n"
                + "Cannot combine with status/, since overdue already means incomplete.\n"
                + "\n"
                + "Examples:\n"
                + "  list\n"
                + "  list view/detail\n"
                + "  list status/incomplete\n"
                + "  list c/ACADEMIC topic/CG3207\n"
                + "  list date/2026-08-15\n"
                + "  list today\n"
                + "  list tomorrow view/detail\n"
                + "  list this week status/incomplete c/ACADEMIC order/time\n"
                + "  list next week\n"
                + "  list overdue");
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
                + "Shows a preview (how many activities, class schedules, and topics\n"
                + "exist) and a menu with three explicit choices - nothing is deleted\n"
                + "until you pick one. This cannot be undone.\n"
                + "Facility, connection, and academic-calendar reference data is\n"
                + "always kept, whichever choice you pick.\n"
                + "\n"
                + "Choices:\n"
                + "  [1] Delete all user data - every activity and topic is cleared,\n"
                + "      saved default order and preference profile reset to defaults, and the\n"
                + "      next activity ID restarts at [1].\n"
                + "  [2] Delete other activities but keep class schedules - keeps\n"
                + "      every fixed lecture/tutorial/lab/section-teaching activity\n"
                + "      (the same activities recur can build on - see guide recur)\n"
                + "      with its original ID, note, and completion status, deletes\n"
                + "      everything else, and keeps only the topics still used by a\n"
                + "      kept activity. Your preference profile is retained.\n"
                + "  [3] Do not delete anything - cancels with no change.\n"
                + "\n"
                + "Example:\n"
                + "  reset all\n"
                + "  2\n"
                + "\n"
                + "Entering anything other than 1, 2, or 3 (including a blank line)\n"
                + "cancels safely with no change.");
        topics.put("recur", "Create recurring class sessions\n"
                + "Format: recur TASK_ID week WEEK_SPEC\n"
                + "WEEK_SPEC is one or more week numbers or inclusive ranges, separated\n"
                + "by semicolons, for example \"1 to 6; 7 to 13\" or \"3;7;9;11\". Week\n"
                + "numbers refer to the external data/academic-calendar.txt file, not a\n"
                + "fixed maximum - ask whoever maintains that file if a week you expect\n"
                + "is missing.\n"
                + "\n"
                + "Only a FIXED activity in the ACADEMIC category whose description\n"
                + "contains lecture, lec, tutorial, tut, lab, laboratory, section\n"
                + "teaching, sectional teaching, or sec is eligible (see guide reset\n"
                + "for the same rule used by reset's \"keep class schedules\" choice).\n"
                + "\n"
                + "Example:\n"
                + "  add n/CG3207 Lecture c/ACADEMIC date/2026-08-14 type/FIXED "
                + "from/16:00 to/18:00 energy/2 sensory/2\n"
                + "  recur 1 week 1 to 6; 7 to 13\n"
                + "\n"
                + "Each new session becomes its own independent activity with its own\n"
                + "ID - mark, unmark, edit, and delete only ever affect the one\n"
                + "occurrence you target, not the others. The original activity's own\n"
                + "week is always included automatically; you do not need to list it.\n"
                + "\n"
                + "Before creating anything, a preview lists every date to be created\n"
                + "and every date skipped (already the source week, an identical\n"
                + "session that already exists, or a date with no classes in the\n"
                + "calendar file) and asks Continue? (y/n). Only y creates the\n"
                + "sessions; if a target date would overlap another fixed activity,\n"
                + "the whole command is rejected instead of creating a partial batch.\n"
                + "Running the same recur command again creates nothing new.\n"
                + "\n"
                + "If data/academic-calendar.txt is missing or cannot be read, recur\n"
                + "reports the problem and every other command keeps working normally.");
        topics.put("find", "Find activities\n"
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
        topics.put("dashboard", "Dashboard: a planning-load summary\n"
                + "Format: dashboard today|tomorrow|date/YYYY-MM-DD|this week [detail]\n"
                + "Related commands: completion (see guide completion for mark/unmark/list status/)\n"
                + "\n"
                + "Shows how much is planned for a period, how much room is left, how\n"
                + "energy/sensory-demanding it is, and how much of what's already due is\n"
                + "complete - using only activity data you already entered. this week\n"
                + "means Monday through Sunday of the current week, the same as list\n"
                + "this week. detail adds fixed/flexible counts, a category breakdown,\n"
                + "and the full 1-to-5 energy/sensory rating distribution.\n"
                + "\n"
                + "Examples:\n"
                + "  dashboard today\n"
                + "  dashboard tomorrow detail\n"
                + "  dashboard date/2026-08-20\n"
                + "  dashboard this week detail\n"
                + "\n"
                + "\"Nominal buffer\" is arithmetic capacity minus planned workload - not\n"
                + "a promise that time is actually free or usable. Energy and sensory\n"
                + "numbers are shown exactly as you entered them, as planning data only,\n"
                + "never as a medical or performance judgement.");
        topics.put("timetable", "Text timetable\n"
                + "Format: timetable day/YYYY-MM-DD [detail]\n"
                + "        timetable week/YYYY-MM-DD [compact|detail]\n"
                + "        timetable this week [compact|detail]\n"
                + "\n"
                + "Shows fixed activities in chronological day sections. Weekly views cover\n"
                + "Monday through Sunday. Flexible activities stay in a separate unscheduled\n"
                + "section with their window and required duration; no time is invented for\n"
                + "them. compact omits empty days for narrow terminals, while detail adds\n"
                + "completion, category, rating, topic, and note information.\n"
                + "\n"
                + "Examples:\n"
                + "  timetable day/2026-08-17\n"
                + "  timetable day/2026-08-17 detail\n"
                + "  timetable week/2026-08-19 compact\n"
                + "  timetable this week detail\n"
                + "\n"
                + "Timetable is read-only. It does not schedule flexible activities, create\n"
                + "recommendations, infer buffers, or change any stored data.");
        topics.put("recommend", "Recommended timetable\n"
                + "Format: recommend\n"
                + "        recommend this week\n"
                + "        recommend date/YYYY-MM-DD\n"
                + "        recommend view\n"
                + "        recommend adopt\n"
                + "        recommend cancel\n"
                + "\n"
                + "recommend and recommend this week generate one deterministic weekly\n"
                + "preview for incomplete flexible activities. recommend date/YYYY-MM-DD\n"
                + "generates a one-day preview. recommend view re-shows the current\n"
                + "proposal, recommend adopt confirms and saves it once, and recommend\n"
                + "cancel discards it with no change.\n"
                + "\n"
                + "The recommender uses fixed activities, flexible windows, and your\n"
                + "global planning preferences. Minimum buffer is enforced; Tomato only\n"
                + "adds advisory study suggestions when enabled. Activities keep their\n"
                + "stable IDs and original flexible windows.\n"
                + "\n"
                + "Examples:\n"
                + "  recommend\n"
                + "  recommend this week\n"
                + "  recommend date/2026-08-15\n"
                + "  recommend view\n"
                + "  recommend adopt\n"
                + "\n"
                + "Limitations: recommendation does not use route/facility data yet,\n"
                + "because activities still do not store facility/location bindings.");
        topics.put("preference", "Global planning preferences\n"
                + "Format: preference view\n"
                + "        preference set start/HH:mm [end/HH:mm] [buffer/MINUTES] [tomato/on|off]\n"
                + "        preference reset\n"
                + "\n"
                + "Stores one global profile for future planning features. Fields supplied to\n"
                + "preference set may appear in any order; omitted fields retain their current\n"
                + "values. start must be before end, buffer must be from 0 to 1440 minutes,\n"
                + "and tomato controls advisory Tomato/Pomodoro suggestions only.\n"
                + "\n"
                + "Defaults: start 08:00, end 20:00, buffer 15 minutes, tomato off.\n"
                + "\n"
                + "Examples:\n"
                + "  preference view\n"
                + "  preference set start/07:30 end/21:00 buffer/20 tomato/on\n"
                + "  preference set tomato/off\n"
                + "  preference reset\n"
                + "\n"
                + "Changes and reset require y/n confirmation. These settings are advisory\n"
                + "planning inputs for recommend. Tomato only affects advisory study\n"
                + "suggestions, not slot generation or activity timing.");
        topics.put("facility", "Accessible facilities\n"
                + "View and search the local read-only accessibility facility reference.\n"
                + "Related commands: connection (see guide connection for the route graph\n"
                + "between facilities)\n"
                + "\n"
                + "Examples:\n"
                + "  facility list\n"
                + "  facility view AS1\n"
                + "  facility find type/LIFT\n"
                + "  facility find type/LIFT status/NO\n"
                + "\n"
                + "If you have edited facilities.txt yourself and want to check it is still in a\n"
                + "format UniEnable can read, run facility validate. It only reports problems -\n"
                + "line by line, with no application restart needed - and never changes the file.\n"
                + "\n"
                + AccessibilityDisclaimer.TEXT);
        topics.put("connection", "Accessible connections\n"
                + "View and search the local read-only connection graph between facilities.\n"
                + "Related commands: facility (see guide facility for individual facility\n"
                + "lookups)\n"
                + "\n"
                + "Examples:\n"
                + "  connection list\n"
                + "  connection view 1\n"
                + "  connection find from/AS6\n"
                + "\n"
                + "If you have edited connections.txt yourself, run connection validate to check\n"
                + "it is still in a format UniEnable can read - including that every connection's\n"
                + "endpoints still match a known facility. It only reports problems and never\n"
                + "changes the file.\n"
                + "\n"
                + AccessibilityDisclaimer.TEXT);
        topics.put("route", "Accessible routes\n"
                + "Format: route from/FACILITY to/FACILITY\n"
                + "Related commands: facility (see guide facility), connection (see guide connection)\n"
                + "\n"
                + "Finds the shortest route between two known facilities using only connections\n"
                + "confirmed accessible (status YES) - connections marked NO or unknown are never\n"
                + "used, even as a last resort. from/ and to/ may appear in either order.\n"
                + "\n"
                + "Examples:\n"
                + "  route from/AS6 to/AS8\n"
                + "  route from/CLB to/AS7\n"
                + "  route from/AS1 to/AS1\n"
                + "\n"
                + "The source and destination being the same facility is a valid result: a\n"
                + "single-facility route with 0 m total distance, not an error.\n"
                + "\n"
                + "Limitations: this does not estimate travel time, and does not guarantee\n"
                + "real-world accessibility at the time of travel - only that the local dataset\n"
                + "records a confirmed-accessible path. If no confirmed-accessible path connects\n"
                + "two known facilities, route says so rather than suggesting an unconfirmed one.\n"
                + "\n"
                + AccessibilityDisclaimer.TEXT);
        topics.put("storage", "Data files and storage\n"
                + "Application data (activities, topics, your saved default order, and\n"
                + "global planning preferences)\n"
                + "is stored under data/. Do not edit data files while the application runs.\n"
                + "\n"
                + "data/academic-calendar.txt is different: it is a reference file you or\n"
                + "your school maintains yourself, only read by recur (see guide recur),\n"
                + "loaded once per run the first time you use recur, and never created,\n"
                + "repaired, or changed by UniEnable itself - including by reset all.");
        return topics;
    }
}
