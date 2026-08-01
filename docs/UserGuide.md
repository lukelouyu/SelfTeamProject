# UniEnable — User Guide

**Status:** v1.0 — the commands below are implemented and match the running application's actual
output. Section 15 lists what's planned for a future v2.0 release and is explicitly **not**
implemented yet.

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
- read-only local facility and accessible-route reference information.

The application works fully offline. It does not provide real-time navigation, live
accessibility information, or medical advice.

## 2. Quick Start

1. Install Java 17 or later.
2. Place the executable JAR in a folder where the application may create its `data` folder.
3. Open a terminal in that folder.
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
| General | `reset all` | v1.0 |
| Topics | `topic add/list/rename/delete ...` | v1.0 |
| Accessibility | `facility list/view/find ...` | v1.0 |
| Accessibility | `connection list/view/find ...` | v1.0 |
| Dashboard, timetable, preferences, recommendation, route, export | various | **Coming soon (v2.0)** |

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
is not later than the start time, an exact duplicate exists (same description/date/timing), or it
overlaps another fixed activity on the same date. If `topic/` is supplied, that topic must already
exist under the given category (create it first with `topic add`) — otherwise the activity is
rejected with a "does not exist" error rather than silently accepting an unregistered topic name.

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

### 6.3 List Activities: `list`

```text
list [today|tomorrow|this week]
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
```

- `today` — activities on the current local date.
- `tomorrow` — activities on the current local date plus one day.
- `this week` — activities from Monday through Sunday of the week containing today.

A relative-date phrase can be freely combined with the other filters above, but not with
`date/YYYY-MM-DD` (which still works on its own) or with another relative-date phrase; either
combination, or unrecognised trailing text after a relative-date phrase, is rejected with a clear
error rather than silently falling back to plain `list`:

```text
list today date/2026-08-15
list today tomorrow
list this month
list today extra
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
matches `assignment`); keywords search description, topic, and note. Multiple keywords and
filters all combine with AND. Header wording is `"Found N activity/activities:"`.

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

Clears every activity and user-created topic, resets your saved default order back to
`chronological`, and resets the next activity ID back to `[1]`. Facility and connection reference
data (the read-only accessibility dataset) is always kept.

```text
____________________________________________________________
Reset all user data?

Activities to delete: 3
Topics to delete   : 1
Default order      : reset to chronological

Facility and connection reference data will be kept.
This action cannot be undone.
Continue? (y/n)
____________________________________________________________
```

After `y`:

```text
____________________________________________________________
All user data has been reset.
Your next activity will use ID [1].
____________________________________________________________
```

`reset all` is the only accepted form; `reset`, `reset all extra`, and any other option after
`reset` are rejected. If there is nothing to reset (no activities, no topics, and the default
order is already chronological), the confirmation prompt is skipped and the reset succeeds
immediately.

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

## 9. Built-In Guide: `guide`

```text
guide
guide TOPIC
guide NUMBER
```

`guide` alone shows a 10-item numbered menu. `guide TOPIC` shows one topic's text directly.
Implemented topics: `getting-started`, `activities`, `browse`, `accessibility`, `add`, `view`,
`list`, `edit`, `delete`, `completion`, `mark`, `unmark`, `find`, `next`, `order`, `reset`,
`topic`, `facility`, `connection`, `storage`. Topics for v2.0-only features (`timetable`,
`recommend`, `route`, `export`) are still listed but end with `(Coming soon in a future
release.)` where the underlying command isn't built yet; `dashboard` explains that completion
tracking itself is already available (see `completion`) while the aggregate dashboard view is
still coming.

Each menu item can also be selected by its number, either as `guide NUMBER` or by entering the
bare number as its own command right after the menu is shown (e.g. `1` selects "Getting
started"). Menu items that span more than one command topic use a dedicated overview topic
instead of picking just one: item `2` ("Add, edit and delete activities") is the `activities`
topic, item `3` ("List, find and view activities") is the `browse` topic, and item `7`
("Accessible facilities and routes") is the `accessibility` topic; each overview points onward to
the individual command's own topic (`add`, `edit`, `delete`, `list`, `find`, `view`, `facility`,
`connection`) for full detail. `facility` and `connection` are separate, independently reachable
topics - each describes only its own command family. Item `10` ("Return") is not a topic; it
just acknowledges the selection and returns to the command prompt.

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
└── settings.txt
```

`activities.txt` and `topics.txt` are your saved planning data, created empty on first run.
`facilities.txt` and `connections.txt` are copied from the bundled sample dataset on first run if
they don't already exist — an existing file (including one you've edited manually) is never
overwritten. `settings.txt` stores your saved default activity order (see `order set` in Section
6); a missing or malformed file safely falls back to the documented `chronological` default.

The application only saves activities, topics, and settings after a command that actually changes
them (`add`, `edit`, `delete`, `mark`, `unmark`, `order set`, `reset all`, and the `topic`
commands) — read-only commands such as `list`, `find`, `view`, and `next` never write to disk.
`settings.txt` is therefore created the first time you run any such data-changing command, not
only when you first use `order set`; `reset all` also writes it, since it resets the saved default
order back to `chronological`. Activities, topics, and settings are always saved together as one
unit: if any of the three files cannot be written, none of them are updated, so a failure never
leaves the files disagreeing with each other on disk. If a save ever fails (for example, a
read-only or locked file), the application reports the storage error instead of a false success
message, and `bye` will say so plainly rather than claiming your data was saved.

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

**Is the accessibility dataset official or live?**
No. It's a small, sample local reference dataset digitised from a real campus map, with
estimated (not measured) distances. See the disclaimer in every facility/connection command's
output.

**Can I edit `facilities.txt`/`connections.txt` manually?**
Yes, while the application is closed. Changes are validated and loaded the next time you start
the application.

## 14. Complete Command Summary

```text
guide
guide TOPIC
guide NUMBER
bye

add n/DESCRIPTION c/CATEGORY date/DATE type/FIXED from/START to/END energy/1-5 sensory/1-5 [topic/TOPIC] [note/NOTES]
add n/DESCRIPTION c/CATEGORY date/DATE type/FLEXIBLE earliest/TIME latest/TIME dur/MINUTES energy/1-5 sensory/1-5 [topic/TOPIC] [note/NOTES]
list [today|tomorrow|this week] [view/concise|detail] [status/all|completed|incomplete] [c/CATEGORY] [topic/TOPIC] [date/DATE] [order/input|time|chronological]
view ID
find [k/KEYWORD ...] [c/CATEGORY] [topic/TOPIC] [date/DATE] [order/input|time|chronological]
order view
order set input|time|chronological
edit ID PREFIX/NEW_VALUE [PREFIX/NEW_VALUE ...]
delete ID
mark ID
unmark ID
next
reset all

topic add c/CATEGORY n/TOPIC
topic list [c/CATEGORY]
topic rename c/CATEGORY old/OLD_TOPIC new/NEW_TOPIC
topic delete c/CATEGORY n/TOPIC

facility list
facility view FACILITY
facility find type/FEATURE [status/YES|NO|UNKNOWN]
connection list
connection view ID
connection find [from/FACILITY] [to/FACILITY] [type/TYPE] [status/YES|NO|UNKNOWN] [shelter/YES|NO|UNKNOWN]
```

## 15. Coming in a Future Release (v2.0 — not yet implemented)

The following were planned in the original product scope but have no working command yet:
accessible route search (`route`), a completion/workload dashboard, a text-based weekly
timetable, recommendation preferences, a deterministic schedule recommender, and CSV export.
`guide` will point this out if you ask about one of these topics directly.
