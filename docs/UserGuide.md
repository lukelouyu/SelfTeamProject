# UniEnable — User Guide

**Status:** v1.0, plus v2.0's accessible route search (`route`). Every command in Sections 5–14,
including recurring class sessions, the three-option `reset all`, and `route`, is implemented.
Section 15 lists remaining v2.0 work that is explicitly **not** implemented yet.

## 1. Introduction

UniEnable is a single-user, offline, CLI-based Java 17 application that helps tertiary students
with ASD or ADHD, and tertiary students who use wheelchairs, prepare for unfamiliar university,
internship, or entry-level work routines.

v1.0 combines:

- fixed and flexible activity planning, with completion tracking;
- concise activity lists and keyword/filter search;
- category and topic organisation;
- user-entered energy-demand and sensory-load ratings;
- a deterministic "next relevant activity" lookup; and
- read-only local facility and accessible-route reference information; and
- academic-calendar recurrence plus a three-option reset workflow.

The application works fully offline. It does not provide real-time navigation, live
accessibility information, or medical advice.

## 2. Quick Start

1. Install Java 17 or later.
2. Extract the UniEnable release ZIP. Keep `unienable.jar` beside the supplied `data` folder,
   which contains `academic-calendar.txt`.
3. Open a terminal in the extracted folder.
4. Run:

   ```text
   java -jar unienable.jar
   ```

   Startup output:

   ```text
   ____________________________________________________________
   Hello! Welcome to UniEnable.
   Your Uni Friend for planning accessible university routines.

   Enter "guide" if you are unsure what to do next.
   ____________________________________________________________
   ```

5. Enter `guide` to open the built-in command guide.
6. Enter a command and press Enter.
7. Enter `bye` to exit.

## 3. Reading Command Formats

- `UPPER_CASE` represents a value that you must replace.
- `[OPTIONAL]` represents an optional part of a command.
- `A|B` means choose either `A` or `B`.
- `...` means that a value or field may be repeated.
- Do not type the square brackets shown in formats.

## 4. General Input Rules

- Command words, field prefixes, and structured values are case-insensitive.
- Descriptions and notes preserve the letter case you enter.
- Leading and trailing spaces around structured values are ignored.
- Dates use `YYYY-MM-DD`, e.g. `2026-08-15`. A `date/` you supply to `add` or `edit` is checked
  in three stages, each with its own specific message: the text must match the `YYYY-MM-DD`
  shape (`date must be in yyyy-MM-dd format.`); it must name a calendar date that actually exists
  (`date does not exist. Please enter a valid calendar date in yyyy-MM-dd format.` — e.g.
  `2026-02-30` or `2027-02-29` are rejected here, not described as "wrong format"); and it must
  not be earlier than today (`date has passed. Please enter a date from TODAY onwards.`). This
  only applies to a date you are actively supplying through `add`/`edit` — `list`/`find`'s
  `date/` filter and previously-saved activities may still refer to a genuinely past date.
- Times use 24-hour `HH:mm`, e.g. `09:30` or `17:45`.
- Durations use whole minutes.
- Energy demand and sensory load use whole numbers from `1` (very low) to `5` (very high).
- Top-level categories are `ACADEMIC`, `CCA`, `WORK_INTERNSHIP`, and `OTHERS`.
- Multiple search keywords use AND: every keyword must match.
- Binary confirmation prompts accept only `y` or `n` (uppercase `Y`/`N` also accepted); anything
  else is treated as `n` (cancel).
- Activity IDs are permanent. After deletion, remaining IDs do not change. Use `reset all` to
  restart from ID `[1]`.
- Commands documented as taking no arguments (`next`, `bye`, `order view`, `facility list`,
  `connection list`) reject any trailing text rather than silently ignoring it.

## 5. Command Overview

| Area | Command | Status |
|---|---|---|
| General | `guide [TOPIC]` | v1.0 |
| General | `bye` | v1.0 |
| Activities | `add ...` | v1.0 |
| Activities | `list ...` | v1.0 |
| Activities | `view ID` | v1.0 |
| Activities | `find ...` | v1.0 |
| Activities | `order view` / `order set ORDER` | v1.0 |
| Activities | `edit ID ...` | v1.0 |
| Activities | `delete ID` | v1.0 |
| Activities | `mark ID` / `unmark ID` | v1.0 |
| Activities | `next` | v1.0 |
| Activities | `recur TASK_ID week WEEK_SPEC` | v1.0 |
| General | `reset all` (3-option menu) | v1.0 |
| Topics | `topic add/list/rename/delete ...` | v1.0 |
| Accessibility | `facility list/view/find ...` | v1.0 |
| Accessibility | `facility validate` / `connection validate` | v1.0 |
| Accessibility | `connection list/view/find ...` | v1.0 |
| Accessibility | `route from/FACILITY to/FACILITY` | v2.0 |
| Dashboard, timetable, preferences, recommendation, export | various | **Coming soon (v2.0)** |

## 6. Activity Commands

### 6.1 Add a Fixed Activity: `add`

```text
add n/DESCRIPTION c/CATEGORY date/DATE type/FIXED
    from/START to/END energy/1-5 sensory/1-5
    [topic/TOPIC] [note/NOTES]
```

Example:

```text
add n/CG3207 lecture c/ACADEMIC date/2026-08-15 type/FIXED from/09:00 to/11:00 energy/4 sensory/3
```

Output:

```text
____________________________________________________________
Got it. Activity [1] has been added:
[ ][F] 2026-08-15 09:00 -> 11:00 | CG3207 lecture
       ACADEMIC | Energy 4/5 | Sensory 3/5

You now have 1 activities.
____________________________________________________________
```

If a topic is supplied, it appears after the category (e.g. `ACADEMIC / CG3207`).

Required fields: `n/`, `c/`, `date/`, `type/FIXED`, `from/`, `to/`, `energy/`, `sensory/`.
Optional: `topic/`, `note/`.

The application rejects the activity when a required field is missing or invalid, the end time
is not later than the start time, an exact duplicate exists, or it overlaps another fixed activity
on the same date. Fixed activities are exact scheduling duplicates when their descriptions match
exactly (including letter case), and their date, start, and end are the same. Category, topic,
note, ratings, completion, and ID do not make otherwise identical scheduling details distinct.
Two fixed activities are accepted when one starts exactly when the other ends. If `topic/` is
supplied, that topic must already exist under the given category (create it first with `topic add`)
— otherwise the activity is rejected with a "does not exist" error rather than silently accepting
an unregistered topic name.

### 6.2 Add a Flexible Activity: `add`

```text
add n/DESCRIPTION c/CATEGORY date/DATE type/FLEXIBLE
    earliest/TIME latest/TIME dur/MINUTES
    energy/1-5 sensory/1-5
    [topic/TOPIC] [note/NOTES]
```

Example:

```text
add n/Finish assignment 1 c/ACADEMIC date/2026-08-15 type/FLEXIBLE earliest/10:00 latest/18:00 dur/90 energy/5 sensory/2
```

Output:

```text
____________________________________________________________
Got it. Activity [2] has been added:
[ ][L] 2026-08-15 10:00 -> 18:00 | Finish assignment 1
       Duration 90 min | ACADEMIC
       Energy 5/5 | Sensory 2/5

You now have 2 activities.
____________________________________________________________
```

`dur/` must be a positive whole number of minutes that fits inside the `earliest/`–`latest/`
window. As with a fixed activity, a supplied `topic/` must already exist under the given category.
Flexible windows may overlap each other and may overlap fixed activities. However, an exact
flexible scheduling duplicate is rejected: description (including letter case), date, earliest
start, latest end, and duration must not all match an existing flexible activity.

Conflict checks include completed activities. Marking an activity complete therefore does not
free its time or allow an exact scheduling duplicate. When an activity is both an exact duplicate
and an overlap, the duplicate error is reported first.

### 6.3 List Activities: `list`

```text
list [today|tomorrow|this week|next week|overdue]
     [view/concise|detail]
     [status/all|completed|incomplete]
     [c/CATEGORY] [topic/TOPIC] [date/DATE]
     [order/input|time|chronological]
```

Every marker field is optional and may appear in any order. With no fields, `list` shows every
activity in the saved default order. Header wording is `"Here are N matching activity/activities:"`
regardless of whether filters were supplied. If `view/` is supplied, its value must be exactly
`concise` or `detail`; any other value is rejected rather than silently falling back to concise.

Concise entries show: ID, completion `[ ]`/`[X]`, type `[F]`/`[L]`, date/time, description,
category (and topic if set), and `E`/`S` energy/sensory ratings.

`view/detail` shows scheduling type, complete timing, and the note field (`None` if unset)
instead.

An optional relative-date word or phrase may appear right after `list`, before any marker fields:

```text
list today
list tomorrow
list this week status/incomplete c/ACADEMIC order/time
list tomorrow view/detail
list next week
list overdue
```

- `today` — activities on the current local date.
- `tomorrow` — activities on the current local date plus one day.
- `this week` — activities from Monday through Sunday of the week containing today.
- `next week` — activities from Monday through Sunday of the week immediately after `this week`.
- `overdue` — incomplete activities whose scheduled time has already fully passed. This is a
  separate view, not a replacement: plain `list` (and every other selector above) keeps showing
  every activity regardless of overdue status. A completed activity, and any activity that hasn't
  started yet, never appears here. `overdue` cannot be combined with `status/`, since it already
  means "incomplete" by definition; it can still be combined with `c/`, `topic/`, `date/`,
  `view/`, and `order/`.

A relative-date phrase can be freely combined with the other filters above (except `overdue` with
`status/`, see above), but not with `date/YYYY-MM-DD` (which still works on its own) or with
another relative-date phrase; either combination, or unrecognised trailing text after a
relative-date phrase, is rejected with a clear error rather than silently falling back to plain
`list`:

```text
list today date/2026-08-15
list today tomorrow
list this month
list today extra
list overdue status/completed
```

### 6.4 View One Activity: `view`

```text
view ID
```

Output:

```text
____________________________________________________________
Activity [1]
Description : CG3207 lecture
Status      : Incomplete
Type        : FIXED
Date        : 2026-08-15
Start       : 09:00
End         : 11:00
Category    : ACADEMIC
Energy      : 4/5
Sensory     : 3/5
____________________________________________________________
```

`Topic` and `Note` lines only appear if those fields are set.

### 6.5 Find Activities: `find`

```text
find [k/KEYWORD ...] [c/CATEGORY] [topic/TOPIC] [date/DATE] [order/input|time|chronological]
```

At least one keyword or filter is required. Matching is case-insensitive and partial (`assign`
matches `assignment`); keywords search description, topic, and note. `k/` accepts exactly one or
two words (e.g. `k/lecture` or `k/finish assignment`, both combined with AND); three or more
words is rejected rather than silently searched. Leading, trailing, and repeated whitespace
within `k/` don't count toward the word limit. Multiple keywords and filters all combine with
AND. Header wording is `"Found N activity/activities:"`.

### 6.6 View/Set the Default Ordering: `order`

```text
order view
order set input|time|chronological
```

`order view` shows the saved default order used by `list`/`find` when no one-shot `order/`
override is given. `order set` changes that saved default.

### 6.7 Edit an Activity: `edit`

```text
edit ID PREFIX/NEW_VALUE [PREFIX/NEW_VALUE ...]
```

Editable prefixes: `n/`, `c/`, `date/`, `type/`, `from/`, `to/`, `earliest/`, `latest/`, `dur/`,
`energy/`, `sensory/`, `topic/`, `note/`. At least one is required. Changing `type/` between
`FIXED` and `FLEXIBLE` requires supplying every timing field the new type needs.

Topics are one-level groupings inside a fixed category, so the application enforces that
invariant on every edit: the activity's resulting topic (whether left unchanged or newly
supplied) must exist under its resulting category. Changing `c/` to a category that does not have
the activity's current topic is rejected — supply a valid `topic/NEW_TOPIC` for the new category
in the same edit, or clear the topic with a blank `topic/`, before the category change is applied.
This check runs before the confirmation prompt, so a rejected edit never asks "Save changes?" and
never changes the stored activity.

Before applying any change, the application shows a concise before/after diff of only the fields
that actually differ, then asks for confirmation:

```text
____________________________________________________________
Before: energy = 4/5
After : energy = 5/5
Save changes? (y/n)
____________________________________________________________
```

If the schedule type itself changes, a single `schedule` line summarises the old and new
timing instead of comparing individual fields that no longer correspond (e.g.
`Before: schedule = 09:00-11:00` / `After : schedule = 10:00-18:00 (90 min)`).

If every supplied field's new value is identical to the stored value, no confirmation is asked
and the application reports `"No changes to activity [ID]."` instead.

After `y`:

```text
____________________________________________________________
Activity [1] has been updated.
____________________________________________________________
```

The update is atomic: if any supplied value is invalid, or the resulting activity would exactly
duplicate or (for a fixed activity) overlap another activity, the entire edit is rejected before
any confirmation is shown.

### 6.8 Delete an Activity: `delete`

```text
delete ID
```

```text
____________________________________________________________
You selected activity [8]:
[8][ ][F] 2026-08-16 10:00 -> 11:00 | Internship briefing
             WORK_INTERNSHIP | E3 | S2

Delete this activity? (y/n)
____________________________________________________________
```

After `y`:

```text
____________________________________________________________
Activity [8] has been deleted.
You now have 7 activities.
____________________________________________________________
```

Activity IDs are permanent: deleting activity `[8]` does not renumber any other activity, and a
future `add` never reuses `[8]`. The only way to restart ID assignment from `[1]` is `reset all`
(section 6.11), which clears every activity and topic.

### 6.9 Mark/Unmark Completion: `mark` / `unmark`

```text
mark ID
unmark ID
```

Neither requires confirmation, since both are immediately reversible.

```text
____________________________________________________________
Nice! Activity [6] is now complete:
[X][L] Finish assignment 1
____________________________________________________________
```

```text
____________________________________________________________
Activity [6] is now incomplete:
[ ][L] Finish assignment 1
____________________________________________________________
```

### 6.10 View the Next Relevant Activity: `next`

```text
next
```

```text
____________________________________________________________
Your next relevant activity is:
[12][ ][F] 2026-08-15 09:00 -> 11:00 | CG3207 lecture
             ACADEMIC | E4 | S3

Overdue incomplete activities: 1
____________________________________________________________
```

Selection order: an incomplete fixed activity currently in progress; otherwise the nearest
upcoming incomplete fixed activity; otherwise the incomplete flexible activity whose window ends
soonest. Completed and overdue activities are never selected.

### 6.11 Reset All User Data: `reset all`

```text
reset all
```

`reset all` is the only accepted form; `reset`, `reset all extra`, and any other option after
`reset` are rejected. Instead of a single yes/no question, it shows a preview of exactly what's
currently stored and a menu with three explicit choices — nothing is deleted until you type a
number:

```text
____________________________________________________________
Reset user data

Activities      : 24
Class schedules : 6
Other activities: 18
Topics          : 3

[1] Delete all user data
[2] Delete other activities but keep class schedules
[3] Do not delete anything

Facility, connection, and academic-calendar reference data will be kept.
Enter 1, 2, or 3:
____________________________________________________________
```

"Class schedules" counts the same fixed lecture/tutorial/lab/section-teaching activities that
`recur` (Section 6.12) can build on — see Section 6.12 for the exact eligibility rule. Facility,
connection, and `data/academic-calendar.txt` reference data are always kept, whichever choice you
pick; they are never counted or affected by this menu.

**Option `1` — delete all user data:**

```text
____________________________________________________________
All user data has been reset.
Your next activity will use ID [1].
____________________________________________________________
```

Clears every activity (including every class-schedule occurrence created by `recur`) and every
user-created topic, resets your saved default order back to `chronological`, and resets the next
activity ID back to `[1]`.

**Option `2` — delete other activities but keep class schedules:**

```text
____________________________________________________________
Reset complete. Kept 6 class-schedule activities and deleted 18 other activities.
Your next activity will use ID [25].
____________________________________________________________
```

Keeps every activity eligible under the same rule `recur` uses (Section 6.12), with its original
ID, note, and completion status unchanged, and deletes everything else. Only the topics still
referenced by a kept activity survive; the next activity ID continues from the highest kept ID
plus one, so kept IDs are never reused.

**Option `3` — do not delete anything:**

```text
____________________________________________________________
Cancelled. No changes were made.
____________________________________________________________
```

Entering anything other than `1`, `2`, or `3` — including a blank line or the end of input —
cancels the same way, with a message telling you to enter `1`, `2`, or `3`. If there is nothing to
reset at all (no activities, no topics, and the default order is already chronological), the menu
is skipped entirely and the reset succeeds immediately as if you had picked option `1`.

### 6.12 Create Recurring Class Sessions: `recur`

```text
recur TASK_ID week WEEK_SPEC
```

`WEEK_SPEC` is one or more week numbers or inclusive ranges, separated by semicolons — for
example `1 to 6; 7 to 13` or `3;7;9;11`. Week numbers, whitespace around `;`/`to`, and the word
`to` itself are all case-insensitive and flexible; zero/negative numbers, a reversed range (e.g.
`5 to 3`), blank items, commas, hyphens, duplicate or overlapping weeks, and trailing text are all
rejected. There is no fixed maximum week number in the application itself — every week you list is
checked against `data/academic-calendar.txt` (see Section 11), so what's valid depends entirely on
what that file defines for the target activity's academic year and semester. The specification
must include the instructional week containing the source activity; omitting it is rejected.

Only a `FIXED` activity in the `ACADEMIC` category is eligible, and only if its description
contains one of these whole-word, case-insensitive session terms: `lecture`/`lec`,
`tutorial`/`tut`, `lab`/`laboratory`, or `section teaching`/`sectional teaching`/`sec` (a substring
like the "lab" inside "collaboration" does not count). This is the exact same rule `reset all`'s
"keep class schedules" option (Section 6.11) uses.

Example:

```text
add n/CG3207 Lecture c/ACADEMIC date/2026-08-14 type/FIXED from/16:00 to/18:00 energy/2 sensory/2
recur 1 week 1 to 6; 7 to 13
```

Output — a preview listing every date to be created and every date skipped, before anything is
changed:

```text
____________________________________________________________
Create recurring sessions from activity [1]?

Source       : CG3207 Lecture
Calendar     : AY2026/2027 SEM1
Day and time : FRIDAY, 16:00 -> 18:00
Weeks        : 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13

To create:
Week 2 | 2026-08-21 | Activity [2]
...

Skipped:
Week 1 | 2026-08-14 | source activity [1]

12 new activities will be created.
Continue? (y/n)
____________________________________________________________
```

A requested date is skipped, not created, for three reasons: it is the original activity's own
week (which you must include in `WEEK_SPEC`); a fixed activity with the same description, date,
start, and end already exists (so running the same `recur` command again creates nothing new); or
`data/academic-calendar.txt` lists it as a no-class date. If **any** other target date would
conflict with an existing activity, the entire command is rejected before the preview is shown —
never a partial batch. After confirmation, all planned sessions are checked together once more;
if that final check fails, none is added and no activity ID is consumed. If every requested week
turns out to be the source, already existing, or a no-class date, `recur` reports that there is
nothing new to create and skips the confirmation prompt entirely.

After `y`, every planned session is added as its own ordinary, independent activity with its own
permanent ID, starting incomplete regardless of the source activity's own completion status, and
copying the source's description, category, topic, timing, ratings, and note. From that point on,
`mark`, `unmark`, `edit`, and `delete` (Sections 6.7–6.9) affect only the one occurrence you target
— there is no linked series to keep in sync.

If `data/academic-calendar.txt` is missing or cannot be parsed, `recur` reports the problem and
every other command keeps working normally (see Section 11).

## 7. Topic Commands

Topics are optional user-defined groupings inside one fixed category. A topic name is unique
within its category (case-insensitively); deeper nesting is not supported.

### 7.1 Create a Topic: `topic add`

```text
topic add c/CATEGORY n/TOPIC
```

```text
____________________________________________________________
Topic created:
Category: ACADEMIC
Topic   : CG3207
____________________________________________________________
```

### 7.2 List Topics: `topic list`

```text
topic list [c/CATEGORY]
```

Unfiltered, every category is shown on one line (names comma-joined, or `No topics`):

```text
____________________________________________________________
Here are your topics:
ACADEMIC       : CG3207, CS2113
CCA            : No topics
WORK_INTERNSHIP: No topics
OTHERS         : No topics
____________________________________________________________
```

Filtered by category, a numbered list is shown instead:

```text
____________________________________________________________
ACADEMIC topics:
1. CG3207
2. CS2113
____________________________________________________________
```

### 7.3 Rename a Topic: `topic rename`

```text
topic rename c/CATEGORY old/OLD_TOPIC new/NEW_TOPIC
```

The old topic name must already exist under the category, and the new name must not already be
used by a different topic in that category — both are checked before any confirmation is shown,
so a rejected rename never asks "Save changes?".

Renaming shows a before/after confirmation, same style as edit:

```text
____________________________________________________________
Before: topic = CG3207
After : topic = CS3207
Save changes? (y/n)
____________________________________________________________
```

After `y`:

```text
____________________________________________________________
Topic renamed from CG3207 to CS3207.
Updated linked activities: 2
____________________________________________________________
```

Every activity currently assigned to the old name (under that category) is updated to the new
name.

### 7.4 Delete a Topic: `topic delete`

```text
topic delete c/CATEGORY n/TOPIC
```

```text
____________________________________________________________
Delete topic "CG3207" under ACADEMIC? (y/n)
____________________________________________________________
```

After `y`:

```text
____________________________________________________________
Topic CG3207 has been deleted.
____________________________________________________________
```

If the topic does not exist under the category, or is still assigned to any activity, deletion is
rejected before any confirmation is shown, rather than asking "Delete topic ...?" first:

```text
____________________________________________________________
[Error] Conflict: Topic CG3207 is used by 2 activities.
____________________________________________________________
```

Reassign the affected activities with `edit ... topic/NEW_TOPIC` first.

## 8. Facility and Connection Commands

The application includes a small local reference dataset digitised from the NUS Student
Accessibility Unit's FASS Access Route map. Facility and connection commands are **read-only** —
no in-app command adds, edits, or deletes these records.

**Every facility and connection command's output ends with:**

```text
Sample local accessibility reference data. Distances are estimates and may be incomplete. Please verify with current campus information when needed.
```

This dataset is a digitised sample, not verified real-time information. Connection distances in
particular are estimated from the source map's grid spacing, since the map has no printed scale.

### 8.1 List Facilities: `facility list`

```text
facility list
```

Displays every known facility's stable ID and name (e.g. `[F01] AS1`), followed by the
disclaimer above.

### 8.2 View a Facility: `facility view`

```text
facility view FACILITY
```

Example: `facility view AS1`. Displays every recorded feature as `TYPE | STATUS | NOTES` (notes
omitted when none is recorded), followed by the disclaimer.

### 8.3 Find Facilities by Feature: `facility find`

```text
facility find type/FEATURE [status/YES|NO|UNKNOWN]
```

If `status/` is omitted, `YES` is used. Supported feature types: `LIFT`, `RAMP`,
`SHELTERED_RAMP`, `ACCESSIBLE_WASHROOM`, `STEP_FREE_ENTRANCE`, `REST_POINT`, `AUTOMATIC_DOOR`,
`OTHER`. When `status/UNKNOWN` is used, an extra line clarifies that `UNKNOWN` means the local
dataset does not confirm the feature.

### 8.4 List Connections: `connection list`

```text
connection list
```

Displays every connection as `[ID] FROM <-> TO | DISTANCE m | ACCESSIBLE STATUS | TYPE` followed
by a `Shelter: STATUS` line, then the disclaimer.

### 8.5 View a Connection: `connection view`

```text
connection view ID
```

Displays every stored field: endpoints, distance, accessibility, traversal type, shelter,
known barrier, and notes (`None recorded` if unset), then the disclaimer.

### 8.6 Find Connections: `connection find`

```text
connection find [from/FACILITY] [to/FACILITY]
                [type/TYPE] [status/YES|NO|UNKNOWN]
                [shelter/YES|NO|UNKNOWN]
```

At least one filter is required; all supplied filters combine with AND. Since every stored
connection is two-way, `from/` and `to/` each match either endpoint, not a specific direction.

### 8.7 Check `facilities.txt` for Problems: `facility validate`

```text
facility validate
```

If you've edited `data/facilities.txt` by hand, this re-checks it for the same problems the
application already looks for every time it starts — a blank or duplicate facility ID, a
duplicate facility name, or an invalid feature type/status — and lists them, one per line, so you
don't have to close and reopen the application (or scroll back to catch a startup warning) just to
see whether your edit was accepted. It only reports; it never changes `facilities.txt` and never
affects what's currently loaded, so running it is always safe. If nothing is wrong, it says so:

```text
facility validate
____________________________________________________________
facilities.txt: no issues found.
____________________________________________________________
```

### 8.8 Check `connections.txt` for Problems: `connection validate`

```text
connection validate
```

Works the same way as `facility validate`, for `data/connections.txt` — including checking that
every connection's `from`/`to` endpoint still names a facility that actually exists in
`facilities.txt`. Read-only; changes nothing.

### 8.9 Find an Accessible Route: `route`

```text
route from/FACILITY to/FACILITY
```

`from/` and `to/` may appear in either order. Finds the shortest path between the two named
facilities by total distance, using only connections whose accessibility status is confirmed
`YES` — a connection marked `NO` or `UNKNOWN` is never used, not even as a last resort when no
`YES` path exists. Names are matched case-insensitively; the report always shows the canonical
stored name.

Example:

```text
route from/AS6 to/AS8
```

Output shows the ordered facility chain, each segment's own distance, traversal type, shelter
status, and any recorded barrier/notes, plus the total distance — followed by the same disclaimer
every facility/connection command ends with. `from`/`to` naming the same known facility is a
**successful** zero-length result (single-facility chain, `0 m`, no travel required), not an
error. Two known facilities with no confirmed-accessible path between them get a clear message
beginning "No supported accessible route was found..." rather than suggesting an unconfirmed one;
this means UniEnable's local dataset has no confirmed path, not that no real-world accessible
route exists. An unrecognised facility name is reported as an error.

`route` never estimates travel time and never claims real-time verification, a guarantee of
accessibility, or current usability — only that the local dataset records a confirmed-accessible
path. It is read-only: it never adds, edits, or deletes a facility or connection record.

## 9. Built-In Guide: `guide`

```text
guide
guide TOPIC
guide NUMBER
```

`guide` alone shows a 12-item numbered menu. `guide TOPIC` shows one topic's text directly.
Implemented topics: `getting-started`, `activities`, `browse`, `add`, `view`,
`list`, `edit`, `delete`, `completion`, `mark`, `unmark`, `find`, `next`, `order`, `reset`,
`recur`, `topic`, `facility`, `connection`, `route`, `storage`. Topics for v2.0-only features
still awaiting implementation (`timetable`, `recommend`, `export`) are still listed but end with
`(Coming soon in a future release.)` where the underlying command isn't built yet; `dashboard`
explains that completion tracking itself is already available (see `completion`) while the
aggregate dashboard view is still coming.

Each menu item can also be selected by its number, either as `guide NUMBER` or by entering the
bare number as its own command right after the menu is shown (e.g. `1` selects "Getting
started"). Menu items that span more than one command topic use a dedicated overview topic
instead of picking just one: item `2` ("Add, edit and delete activities") is the `activities`
topic, and item `3` ("List, find and view activities") is the `browse` topic; each overview
points onward to the individual command's own topic (`add`, `edit`, `delete`, `list`, `find`,
`view`) for full detail. `facility` (item `7`), `connection` (item `8`), and `route` (item `11`,
added when v2.0's `route` shipped) are separate, independently and uniquely numbered topics -
each describes only its own command family, with no combined item grouping them together. Item
`12` ("Return") is not a topic; it just acknowledges the selection and returns to the command
prompt — it moved from `11` to `12` solely because `route` took the next available number; every
other item's number is unchanged from v1.0.

## 10. Exit: `bye`

```text
bye
```

```text
____________________________________________________________
Your data has been saved.
Bye! Take care and see you again.
____________________________________________________________
```

## 11. Data Storage

```text
data/
├── activities.txt
├── topics.txt
├── facilities.txt
├── connections.txt
├── settings.txt
└── academic-calendar.txt
```

`activities.txt` and `topics.txt` are your saved planning data, created empty on first run.
`facilities.txt` and `connections.txt` are copied from the bundled sample dataset on first run if
they don't already exist — an existing file (including one you've edited manually) is never
overwritten. `settings.txt` stores your saved default activity order (see `order set` in Section
6); a missing or malformed file safely falls back to the documented `chronological` default.

`academic-calendar.txt` is different from every file above: it is a reference file you (or your
school) maintain yourself, listing each semester's teaching weeks and no-class dates. UniEnable
never creates, repairs, or overwrites it — if it's missing, only `recur` (Section 6.12) is
affected; every other command keeps working. It's read once, the first time you use `recur` in a
run, not at startup, and any edit you make while the application is running only takes effect the
next time you start it. `reset all` (Section 6.11) never touches it, no matter which option you
choose. If you want to plan for a new academic year or add a week the file doesn't yet have, edit
the file yourself and restart — no application update is needed. See the release download for the
exact record format and a worked example.

The application only saves activities, topics, and settings after a command that actually changes
them: `add`, `edit`, `delete`, `mark`, `unmark`, `order set`, `recur`, `reset all`, `topic add`,
`topic rename`, and `topic delete`. `topic list` and other read-only commands such as `list`,
`find`, `view`, and `next` never write to disk.
`settings.txt` is therefore created the first time you run any such data-changing command, not
only when you first use `order set`; `reset all` also writes it, since it resets the saved default
order back to `chronological`. Activities, topics, and settings are always saved together as one
unit: if any of the three files cannot be written, none of them are updated, so a failure never
leaves the files disagreeing with each other on disk. If a save ever fails (for example, a
read-only or locked file), the application restores activities, topics, IDs, and ordering to how
they were before that command. It reports the storage error instead of a false success message,
and `bye` will say so plainly rather than claiming your data was saved.

Every data file is validated line-by-line when the application starts, not just parsed for shape.
A line that fails validation is skipped with a warning (see `[Warning] Partial data loaded` below)
instead of silently loading bad data:

- `activities.txt` — positive/unique IDs, non-blank descriptions, end-after-start timing, a
  flexible window/duration that fits, no exact duplicate, no fixed-activity overlap, and a topic
  field that matches a topic actually recorded in `topics.txt` under the same category.
- `connections.txt` — positive/unique IDs, a positive distance, and `from`/`to` endpoints that
  both match a known facility's name in `facilities.txt`.
- `facilities.txt` — unique facility IDs and unique facility names (facilities are looked up by
  name, so a duplicate name would otherwise make that lookup ambiguous).
- `academic-calendar.txt` — validated separately, only when `recur` first needs it (not at
  startup): schema version, field counts, real calendar dates, non-blank fields, non-negative week
  numbers, a matching `SOURCE` record for every week/no-class entry, unique academic-year/
  semester/week combinations, unique no-class dates, and non-overlapping instructional week
  ranges. Any problem disables `recur` only — every other command keeps working normally. A
  problem confined to one line (a bad field count, an invalid date, an unknown record type) names
  that exact line number; a problem that only becomes visible once the whole file is read (a
  duplicate academic-year/semester/week combination, or two instructional weeks overlapping)
  names the conflicting records themselves instead of a single line.

If you'd rather check a hand-edited `facilities.txt`/`connections.txt` without restarting the
application, `facility validate`/`connection validate` (Sections 8.7–8.8) run the exact same
checks on demand.

## 12. Error Handling

```text
[Error] Invalid input: ...
[Error] Missing input: ...
[Error] Not found: ...
[Error] Conflict: ...
[Error] Storage error: ...
[Warning] Partial data loaded: FILENAME
```

Example:

```text
delete 999
____________________________________________________________
[Error] Not found: Activity [999] does not exist.
____________________________________________________________
```

## 13. Frequently Asked Questions

**What is the difference between a fixed and flexible activity?**
A fixed activity has a confirmed start and end time. A flexible activity has an allowed window
(`earliest/`–`latest/`) and a required duration.

**Why does `edit`/`topic rename` ask for confirmation but `mark`/`unmark` don't?**
Edits and renames change stored information non-trivially and show a before/after diff so you can
catch a mistake before it's saved. Marking/unmarking is immediately reversible with the opposite
command, so no confirmation is needed.

**Will `recur` or `reset all` ever delete or repair `data/academic-calendar.txt`?**
No. Neither command, nor anything else in UniEnable, creates, repairs, or overwrites that file —
it's entirely yours to maintain. `reset all` always leaves it untouched, whichever of the three
options you pick.

**If I run `recur` twice by mistake, will I get duplicate sessions?**
No. The second run recognises every session it already created (same description, date, and
timing) and reports that there is nothing new to create instead of adding a duplicate.

**Is the accessibility dataset official or live?**
No. It's a small, sample local reference dataset digitised from a real campus map, with
estimated (not measured) distances. See the disclaimer in every facility/connection command's
output.

**Can I edit `facilities.txt`/`connections.txt` manually?**
Yes, while the application is closed. Changes are validated and loaded the next time you start
the application. If you want to check your edit is well-formed without restarting, run `facility
validate`/`connection validate` (Sections 8.7–8.8) any time while the application is running.

## 14. Complete Command Summary

```text
guide
guide TOPIC
guide NUMBER
bye

add n/DESCRIPTION c/CATEGORY date/DATE type/FIXED from/START to/END energy/1-5 sensory/1-5 [topic/TOPIC] [note/NOTES]
add n/DESCRIPTION c/CATEGORY date/DATE type/FLEXIBLE earliest/TIME latest/TIME dur/MINUTES energy/1-5 sensory/1-5 [topic/TOPIC] [note/NOTES]
list [today|tomorrow|this week|next week|overdue] [view/concise|detail] [status/all|completed|incomplete] [c/CATEGORY] [topic/TOPIC] [date/DATE] [order/input|time|chronological]
view ID
find [k/KEYWORD ...] [c/CATEGORY] [topic/TOPIC] [date/DATE] [order/input|time|chronological]
order view
order set input|time|chronological
edit ID PREFIX/NEW_VALUE [PREFIX/NEW_VALUE ...]
delete ID
mark ID
unmark ID
next
recur TASK_ID week WEEK_SPEC
reset all

topic add c/CATEGORY n/TOPIC
topic list [c/CATEGORY]
topic rename c/CATEGORY old/OLD_TOPIC new/NEW_TOPIC
topic delete c/CATEGORY n/TOPIC

facility list
facility view FACILITY
facility find type/FEATURE [status/YES|NO|UNKNOWN]
facility validate
connection list
connection view ID
connection find [from/FACILITY] [to/FACILITY] [type/TYPE] [status/YES|NO|UNKNOWN] [shelter/YES|NO|UNKNOWN]
connection validate

route from/FACILITY to/FACILITY
```

## 15. Coming in a Future Release (v2.0 — not yet implemented)

The following were planned in the original product scope but have no working command yet: a
completion/workload dashboard, a text-based weekly timetable, recommendation preferences, a
deterministic schedule recommender, and CSV export. `guide` will point this out if you ask about
one of these topics directly. Accessible route search (`route`) shipped in v2.0 — see Section
8.9.
