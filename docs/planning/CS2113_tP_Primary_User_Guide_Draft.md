# Accessible Itinerary Planner — Primary User Guide

**Status:** Pre-development command and behaviour draft  
**Target releases:** v1.0 and v2.0  
**Runtime:** Java 17, offline command-line application  
**Product name:** Working title; the final name has not been selected

All command outputs in this guide are representative pre-development examples. They define the intended information, tone, and interaction flow; minor wording may be refined during implementation without changing the documented behaviour.

## 1. Introduction

Accessible Itinerary Planner helps tertiary students with ASD or ADHD and tertiary students who use wheelchairs prepare for unfamiliar university, internship, and entry-level work routines.

The application combines:

- fixed and flexible activity planning;
- concise activity lists and search;
- category and topic organisation;
- user-entered energy-demand and sensory-load ratings;
- completion and daily-load summaries;
- preference-based daily schedule recommendations;
- locally maintained facility and accessible-route information; and
- CSV exports for historical records.

The application works offline. It does not provide real-time navigation, live accessibility information, medical advice, or external-calendar synchronisation.

### Guide Contents

- [Quick Start](#2-quick-start)
- [Command Overview](#5-command-overview)
- [Activity Commands](#6-activity-commands)
- [Topic Commands](#7-topic-commands)
- [Facility and Connection Commands](#8-facility-and-connection-commands)
- [Accessible Route Command](#9-accessible-route-command)
- [Dashboard Commands](#10-dashboard-commands)
- [Timetable Commands](#11-timetable-commands)
- [Recommendation Preferences](#12-recommendation-preferences)
- [Schedule Recommendation](#13-schedule-recommendation)
- [CSV Export Commands](#14-csv-export-commands)
- [Built-In Guide](#15-built-in-guide)
- [Data Storage](#17-data-storage)
- [Error Handling](#18-error-handling)
- [Frequently Asked Questions](#19-frequently-asked-questions)
- [Complete Command Summary](#20-complete-command-summary)

## 2. Quick Start

1. Install Java 17 or later.
2. Place the executable JAR in a folder where the application may create its `data` and `exports` folders.
3. Open a terminal in that folder.
4. Run:

   ```text
   java -jar app.jar
   ```

   Representative startup output:

   ```text
   ____________________________________________________________
   Hello! Welcome to Accessible Itinerary Planner.

   Plan activities, review your daily load, and check the
   local accessibility reference.

   Enter "guide" if you are unsure what to do next.
   ____________________________________________________________
   ```

5. Enter `guide` to open the built-in command guide.
6. Enter a command and press Enter.
7. Enter `bye` to exit.

The final JAR filename will replace `app.jar`.

## 3. Reading Command Formats

The guide uses these notation rules:

- `UPPER_CASE` represents a value that you must replace.
- `[OPTIONAL]` represents an optional part of a command.
- `A|B` means choose either `A` or `B`.
- `...` means that a value or field may be repeated.
- Do not type the square brackets shown in formats.

Example:

```text
view ID
```

For activity 12, enter:

```text
view 12
```

Representative output:

```text
____________________________________________________________
Activity [12]
Description : CG3207 lecture
Status      : Incomplete
Date        : 2026-08-15
Time        : 09:00–11:00
Category    : ACADEMIC
Topic       : CG3207
Energy      : 4/5
Sensory     : 3/5
____________________________________________________________
```

## 4. General Input Rules

- Command words, field prefixes, and structured values are case-insensitive.
- Natural-language descriptions and notes preserve the letter case entered by the user.
- Leading and trailing spaces around structured values are ignored.
- Dates use `YYYY-MM-DD`, for example `2026-08-15`.
- Times use 24-hour `HH:mm`, for example `09:30` or `17:45`.
- Durations and buffers use whole minutes.
- Energy demand and sensory load use whole numbers from `1` to `5`:
  - `1` — very low;
  - `2` — low;
  - `3` — moderate;
  - `4` — high;
  - `5` — very high.
- Top-level category values are:
  - `ACADEMIC`;
  - `CCA`;
  - `WORK_INTERNSHIP`; and
  - `OTHERS`.
- Descriptions, topic names, and notes may contain spaces.
- Stable activity IDs do not change when another activity is deleted.
- Multiple search keywords use AND: every keyword must match.
- Binary confirmation prompts accept only `y` or `n`; uppercase `Y` and `N` are also accepted.
- Numbered menus are used when more than two next actions are available.
- No information is communicated through colour alone.

## 5. Command Overview

| Area | Command | Release |
|---|---|---|
| General | `guide [TOPIC]` | v1.0 |
| General | `bye` | v1.0 |
| Activities | `add ...` | v1.0 |
| Activities | `list ...` | v1.0 |
| Activities | `view ID` | v1.0 |
| Activities | `find ...` | v1.0 |
| Activities | `order view` | v1.0 |
| Activities | `order set ORDER` | v1.0 |
| Activities | `edit ID ...` | v1.0 |
| Activities | `delete ID` | v1.0 |
| Activities | `mark ID` | v1.0 |
| Activities | `unmark ID` | v1.0 |
| Activities | `next` | v1.0 |
| Topics | `topic add ...` | v1.0 |
| Topics | `topic list ...` | v1.0 |
| Topics | `topic rename ...` | v1.0 |
| Topics | `topic delete ...` | v1.0 |
| Accessibility | `facility list` | v1.0 |
| Accessibility | `facility view FACILITY` | v1.0 |
| Accessibility | `facility find ...` | v1.0 |
| Accessibility | `connection list` | v1.0 |
| Accessibility | `connection view ID` | v1.0 |
| Accessibility | `connection find ...` | v1.0 |
| Dashboard | `dashboard PERIOD [detail]` | v2.0 |
| Timetable | `timetable day/DATE ...` | v2.0 |
| Timetable | `timetable week/DATE ...` | v2.0 |
| Timetable | `timetable item/ID` | v2.0 |
| Preferences | `preference view` | v2.0 |
| Preferences | `preference set ...` | v2.0 |
| Recommendation | `recommend PERIOD ...` | v2.0 |
| Route | `route from/FACILITY to/FACILITY` | v2.0 |
| Export | `export [activities|schedule|all]` | v2.0 |

## 6. Activity Commands

### 6.1 Add a Fixed Activity: `add`

Adds an activity with a confirmed start and end time.

Format:

```text
add n/DESCRIPTION c/CATEGORY date/DATE type/FIXED
    from/START to/END energy/1-5 sensory/1-5
    [topic/TOPIC] [note/NOTES]
```

Example:

```text
add n/CG3207 lecture c/ACADEMIC date/2026-08-15 type/FIXED from/09:00 to/11:00 energy/4 sensory/3 topic/CG3207 note/Bring laptop
```

Representative output:

```text
____________________________________________________________
Got it. Activity [12] has been added:
[ ][F] 2026-08-15 09:00–11:00 | CG3207 lecture
       ACADEMIC / CG3207 | Energy 4/5 | Sensory 3/5

You now have 12 activities.
____________________________________________________________
```

Required fields:

- `n/` — activity description;
- `c/` — one fixed category;
- `date/` — activity date;
- `type/FIXED`;
- `from/` — start time;
- `to/` — end time;
- `energy/` — energy-demand rating from `1` to `5`; and
- `sensory/` — sensory-load rating from `1` to `5`.

Optional fields:

- `topic/` — an existing topic under the selected category; and
- `note/` — preparation or contextual information.

The application rejects the activity when:

- a required field is missing or invalid;
- the end time is not later than the start time;
- an exact duplicate has the same description, date, and scheduling details; or
- it overlaps another fixed activity.

If the topic does not exist, the application displays:

```text
Topic "CG3207" does not exist under Academic.

1. Create "CG3207" under Academic
2. View and select an existing Academic topic
3. Continue without a topic
4. Cancel
```

A successfully added activity receives a stable ID and starts as incomplete.

### 6.2 Add a Flexible Activity: `add`

Adds an activity that may be scheduled anywhere within an allowed time window.

Format:

```text
add n/DESCRIPTION c/CATEGORY date/DATE type/FLEXIBLE
    earliest/TIME latest/TIME dur/MINUTES
    energy/1-5 sensory/1-5
    [topic/TOPIC] [note/NOTES]
```

Example:

```text
add n/Finish assignment 1 c/ACADEMIC date/2026-08-15 type/FLEXIBLE earliest/10:00 latest/18:00 dur/90 energy/5 sensory/2 topic/CG3207
```

Representative output:

```text
____________________________________________________________
Got it. Activity [13] has been added:
[ ][L] 2026-08-15 10:00–18:00 | Finish assignment 1
       Duration 90 min | ACADEMIC / CG3207
       Energy 5/5 | Sensory 2/5

You now have 13 activities.
____________________________________________________________
```

Rules:

- `earliest/` is the earliest allowed start.
- `latest/` is the latest allowed end.
- `dur/` is the complete required duration in positive whole minutes.
- The duration must fit inside the allowed window.
- Flexible windows may overlap each other because the recommender chooses placements later.

### 6.3 List Activities: `list`

Displays activities using the selected view, filters, and ordering.

Format:

```text
list [view/concise|detail]
     [status/all|completed|incomplete]
     [c/CATEGORY] [topic/TOPIC] [date/DATE]
     [order/input|time|chronological]
```

Examples:

```text
list
list view/detail status/incomplete
list c/ACADEMIC topic/CG3207
list date/2026-08-15 order/time
```

Representative output for `list`:

```text
____________________________________________________________
Here are all 3 activities in your saved chronological order:
[12][ ][F] 2026-08-15 09:00–11:00 | CG3207 lecture
             ACADEMIC / CG3207 | E4 | S3
[13][ ][L] 2026-08-15 10:00–18:00 | Finish assignment 1
             ACADEMIC / CG3207 | 90 min | E5 | S2
[14][X][F] 2026-08-15 14:00–15:00 | Project briefing
             CCA | E2 | S3
____________________________________________________________
```

Representative output for `list view/detail status/incomplete`:

```text
____________________________________________________________
Here are 2 incomplete activities:

[12] CG3207 lecture
Status: Incomplete | Type: FIXED | Date: 2026-08-15
Time: 09:00–11:00 | Category: ACADEMIC | Topic: CG3207
Energy: 4/5 | Sensory: 3/5 | Note: Bring laptop

[13] Finish assignment 1
Status: Incomplete | Type: FLEXIBLE | Date: 2026-08-15
Window: 10:00–18:00 | Duration: 90 min
Category: ACADEMIC | Topic: CG3207
Energy: 5/5 | Sensory: 2/5 | Note: None
____________________________________________________________
```

Representative output for `list c/ACADEMIC topic/CG3207`:

```text
____________________________________________________________
Here are 2 matching activities:
[12][ ][F] 2026-08-15 09:00–11:00 | CG3207 lecture
[13][ ][L] 2026-08-15 10:00–18:00 | Finish assignment 1
Filters: Category ACADEMIC AND Topic CG3207
____________________________________________________________
```

Representative output for `list date/2026-08-15 order/time`:

```text
____________________________________________________________
Activities on 2026-08-15, ordered by time:
[12][ ][F] 09:00–11:00 | CG3207 lecture
[13][ ][L] 10:00–18:00 | Finish assignment 1
[14][X][F] 14:00–15:00 | Project briefing

This one-command ordering did not change your saved default.
____________________________________________________________
```

Defaults:

- view: `concise`;
- completion status: `all`; and
- ordering: the saved default order.

Concise entries show:

- stable ID;
- completion status;
- date and timing;
- description;
- category and optional topic;
- energy demand; and
- sensory load.

Concise symbols use `[ ]` for incomplete, `[X]` for complete, `[F]` for fixed, and `[L]` for flexible. `E` and `S` abbreviate energy and sensory ratings only when the full labels would make the output difficult to scan.

Detailed entries additionally show:

- scheduling type;
- complete timing information; and
- notes.

A one-command ordering override does not change the saved default.

### 6.4 View One Activity: `view`

Displays every stored field for one activity.

Format:

```text
view ID
```

Example:

```text
view 12
```

Representative output:

```text
____________________________________________________________
Activity [12]
Description : CG3207 lecture
Status      : Incomplete
Type        : FIXED
Date        : 2026-08-15
Start       : 09:00
End         : 11:00
Category    : ACADEMIC
Topic       : CG3207
Energy      : 4/5
Sensory     : 3/5
Note        : Bring laptop
____________________________________________________________
```

If the ID does not exist, the application suggests using `list` or `find`.

### 6.5 Find Activities: `find`

Finds activities using keywords and structured filters.

Format:

```text
find [k/KEYWORD ...]
     [c/CATEGORY] [topic/TOPIC] [date/DATE]
     [order/input|time|chronological]
```

Examples:

```text
find k/assignment
find k/finish assignment
find c/ACADEMIC topic/CG3207
find k/project c/CCA date/2026-08-15
```

Representative output for `find k/assignment`:

```text
____________________________________________________________
Found 1 activity matching "assignment":
[13][ ][L] 2026-08-15 | Finish assignment 1
             ACADEMIC / CG3207 | E5 | S2
____________________________________________________________
```

Representative output for `find k/finish assignment`:

```text
____________________________________________________________
Found 1 activity matching ALL keywords: finish, assignment
[13][ ][L] 2026-08-15 | Finish assignment 1
____________________________________________________________
```

Representative output for `find c/ACADEMIC topic/CG3207`:

```text
____________________________________________________________
Found 2 activities:
[12][ ][F] CG3207 lecture
[13][ ][L] Finish assignment 1
Filters: Category ACADEMIC AND Topic CG3207
____________________________________________________________
```

Representative output for `find k/project c/CCA date/2026-08-15`:

```text
____________________________________________________________
Found 1 activity:
[14][X][F] 2026-08-15 14:00–15:00 | Project briefing
             CCA | E2 | S3
____________________________________________________________
```

Matching rules:

- At least one keyword or filter is required.
- Matching is case-insensitive.
- Partial matches are supported: `assign` matches `assignment`.
- Keywords search descriptions, topics, and notes.
- Every supplied keyword must match.
- Every supplied filter must match.
- Keywords may match different searchable fields.
- Filter-only searches are allowed.
- Approximate or unrelated results are never returned.

For example, `find k/finish assignment` returns only activities that match both `finish` and `assignment`.

### 6.6 View the Saved Ordering: `order view`

Displays the saved default activity-list order.

Format:

```text
order view
```

Representative output:

```text
____________________________________________________________
Saved default activity order: chronological
Activities are ordered by date, then start time, then ID.
____________________________________________________________
```

### 6.7 Set the Default Ordering: `order set`

Changes the default order used by `list` and `find` when no temporary override is supplied.

Format:

```text
order set input|time|chronological
```

Examples:

```text
order set input
order set time
order set chronological
```

Representative output for `order set input`:

```text
____________________________________________________________
Default activity order updated: input
Future list and find results will use creation order.
____________________________________________________________
```

Representative output for `order set time`:

```text
____________________________________________________________
Default activity order updated: time
Future results will use time, then stable activity ID.
____________________________________________________________
```

Representative output for `order set chronological`:

```text
____________________________________________________________
Default activity order updated: chronological
Future results will use date, time, then stable activity ID.
____________________________________________________________
```

Order meanings:

- `input` — order in which activities were added;
- `time` — start time within a selected date; and
- `chronological` — date followed by start time across dates.

Ties use stable activity IDs.

### 6.8 Edit an Activity: `edit`

Changes one or more fields without requiring unchanged information again.

Format:

```text
edit ID PREFIX/NEW_VALUE [PREFIX/NEW_VALUE ...]
```

Examples:

```text
edit 3 dur/60
edit 5 n/New activity name energy/4 sensory/2
edit 7 note/Bring headphones
```

Representative output for `edit 3 dur/60`:

```text
____________________________________________________________
Proposed changes to activity [3]:
Duration | Old: 90 min | New: 60 min

Save all changes? (y/n)
____________________________________________________________
```

After `y`:

```text
____________________________________________________________
Activity [3] has been updated.
Duration: 60 min
____________________________________________________________
```

Representative output for `edit 5 n/New activity name energy/4 sensory/2`:

```text
____________________________________________________________
Proposed changes to activity [5]:
Description | Old: Prepare slides | New: New activity name
Energy      | Old: 3/5            | New: 4/5
Sensory     | Old: 3/5            | New: 2/5

Save all changes? (y/n)
____________________________________________________________
```

Representative output for `edit 7 note/Bring headphones`:

```text
____________________________________________________________
Proposed changes to activity [7]:
Note | Old: None | New: Bring headphones

Save all changes? (y/n)
____________________________________________________________
```

Editable prefixes:

- `n/`;
- `c/`;
- `date/`;
- `type/`;
- `from/`;
- `to/`;
- `earliest/`;
- `latest/`;
- `dur/`;
- `energy/`;
- `sensory/`;
- `topic/`; and
- `note/`.

Rules:

- At least one field must be supplied.
- Completion status uses `mark` or `unmark`, not `edit`.
- The application validates all supplied fields before changing data.
- If one supplied value is invalid, the entire edit is rejected.
- Changing between `FIXED` and `FLEXIBLE` requires all timing fields for the new type.
- The proposed activity must not duplicate or overlap another fixed activity.
- The application displays old and proposed values.
- Enter `y` to save or `n` to cancel.
- The update is atomic: either every supplied change is saved or none is saved.

### 6.9 Delete an Activity: `delete`

Permanently removes one activity.

Format:

```text
delete ID
```

Example:

```text
delete 8
```

Representative output:

```text
____________________________________________________________
You selected activity [8]:
[ ][F] 2026-08-16 10:00–11:00 | Internship briefing

Delete this activity? (y/n)
____________________________________________________________
```

After `y`:

```text
____________________________________________________________
Activity [8] has been deleted.
You now have 12 activities.
____________________________________________________________
```

The application displays the selected activity and asks:

```text
Delete this activity? (y/n)
```

The activity is deleted only after `y`. Stable IDs of other activities do not change.

### 6.10 Mark an Activity Complete: `mark`

Format:

```text
mark ID
```

Example:

```text
mark 6
```

Representative output:

```text
____________________________________________________________
Nice! Activity [6] is now complete:
[X][L] Finish assignment 1
____________________________________________________________
```

The activity is stored as completed. Marking an already-completed activity is allowed and displays the normal success message.

No confirmation is required because `unmark` immediately reverses the change.

### 6.11 Mark an Activity Incomplete: `unmark`

Format:

```text
unmark ID
```

Example:

```text
unmark 6
```

Representative output:

```text
____________________________________________________________
Activity [6] is now incomplete:
[ ][L] Finish assignment 1
____________________________________________________________
```

Unmarking an already-incomplete activity is allowed and displays the normal success message.

### 6.12 View the Next Relevant Activity: `next`

Displays one deterministic next activity without requiring the full itinerary.

Format:

```text
next
```

Representative output:

```text
____________________________________________________________
Your next relevant activity is:
[12][ ][F] Today 09:00–11:00 | CG3207 lecture
             ACADEMIC / CG3207 | E4 | S3

Overdue incomplete activities: 1
____________________________________________________________
```

Selection order:

1. an incomplete fixed activity currently in progress;
2. otherwise, the nearest upcoming incomplete fixed activity;
3. otherwise, the incomplete flexible activity whose allowed window ends soonest.

Flexible-activity ties use:

1. earlier allowed-window start; and
2. lower stable activity ID.

Completed and overdue activities are not selected. Any overdue count appears separately.

## 7. Topic Commands

Topics are optional user-defined groupings inside one fixed category. A topic name is unique within its category. Deeper nesting is not supported.

### 7.1 Create a Topic: `topic add`

Format:

```text
topic add c/CATEGORY n/TOPIC
```

Example:

```text
topic add c/ACADEMIC n/CG3207
```

Representative output:

```text
____________________________________________________________
Topic created:
Category: ACADEMIC
Topic   : CG3207
____________________________________________________________
```

### 7.2 List Topics: `topic list`

Format:

```text
topic list [c/CATEGORY]
```

Examples:

```text
topic list
topic list c/ACADEMIC
```

Representative output for `topic list`:

```text
____________________________________________________________
Here are your topics:
ACADEMIC       : CG3207, CS2113
CCA            : Computing Club
WORK_INTERNSHIP: Summer Internship
OTHERS         : No topics
____________________________________________________________
```

Representative output for `topic list c/ACADEMIC`:

```text
____________________________________________________________
ACADEMIC topics:
1. CG3207
2. CS2113
____________________________________________________________
```

### 7.3 Rename a Topic: `topic rename`

Format:

```text
topic rename c/CATEGORY old/OLD_TOPIC new/NEW_TOPIC
```

Example:

```text
topic rename c/ACADEMIC old/CG3207 new/CS3207
```

Representative output:

```text
____________________________________________________________
Rename topic under ACADEMIC:
Old: CG3207
New: CS3207

2 activities will be updated to use CS3207.
Rename this topic? (y/n)
____________________________________________________________
```

After `y`:

```text
____________________________________________________________
Topic renamed from CG3207 to CS3207.
Updated linked activities: 2
____________________________________________________________
```

The application shows the affected activities and asks for `y/n` confirmation.

### 7.4 Delete a Topic: `topic delete`

Format:

```text
topic delete c/CATEGORY n/TOPIC
```

Example:

```text
topic delete c/ACADEMIC n/CS3207
```

Representative output when the topic is still in use:

```text
____________________________________________________________
[Error] Conflict: Topic CS3207 is used by 2 activities.
Reassign those activities with edit before deleting the topic.
____________________________________________________________
```

Rules:

- The topic must exist.
- A topic assigned to activities cannot be deleted.
- Reassign the affected activities with `edit` first.
- Deleting an unused topic requires `y/n` confirmation.

## 8. Facility and Connection Commands

The application includes a small local reference dataset. It is sample or locally maintained information, not an official or real-time guarantee.

Accessibility-data commands are read-only. Facility, feature, and connection records cannot be added, edited, or deleted inside the application.

### 8.1 List Facilities: `facility list`

Format:

```text
facility list
```

Displays every known facility's stable ID and name.

Representative output:

```text
____________________________________________________________
Known facilities in the local reference:
[F01] AS6
[F02] CLB LEVEL 3
[F03] CLB LEVEL 6
[F04] COM1
[F05] COM3
[F06] E4

This is local reference data, not a real-time guarantee.
____________________________________________________________
```

### 8.2 View a Facility: `facility view`

Format:

```text
facility view FACILITY
```

Examples:

```text
facility view COM3
facility view CLB LEVEL 3
```

Representative output for `facility view COM3`:

```text
____________________________________________________________
Facility: COM3 [F05]
STEP_FREE_ENTRANCE   | YES | Main entrance
LIFT                 | YES | Near the level 1 lobby
ACCESSIBLE_WASHROOM  | YES | Level 2
REST_POINT           | NO

Note: This is local reference data, not a real-time guarantee.
____________________________________________________________
```

Representative output for `facility view CLB LEVEL 3`:

```text
____________________________________________________________
Facility: CLB LEVEL 3 [F02]
STEP_FREE_ENTRANCE  | YES     | Via sheltered linkway
LIFT                | YES     | Connects to CLB LEVEL 6
ACCESSIBLE_WASHROOM | UNKNOWN | No confirmed local record
REST_POINT          | YES     | Near the entrance

Note: This is local reference data, not a real-time guarantee.
____________________________________________________________
```

Displays every recorded feature, its `YES`, `NO`, or `UNKNOWN` status, location details, notes, and the local-data disclaimer.

### 8.3 Find Facilities by Feature: `facility find`

Format:

```text
facility find type/FEATURE [status/YES|NO|UNKNOWN]
```

Examples:

```text
facility find type/LIFT
facility find type/ACCESSIBLE_WASHROOM status/YES
facility find type/REST_POINT status/UNKNOWN
```

Representative output for `facility find type/LIFT`:

```text
____________________________________________________________
Facilities with confirmed LIFT access:
[F02] CLB LEVEL 3  | Near the entrance
[F03] CLB LEVEL 6  | Central lift lobby
[F05] COM3         | Near the level 1 lobby
____________________________________________________________
```

Representative output for `facility find type/ACCESSIBLE_WASHROOM status/YES`:

```text
____________________________________________________________
Facilities where ACCESSIBLE_WASHROOM is YES:
[F03] CLB LEVEL 6 | Near lift lobby
[F05] COM3        | Level 2
____________________________________________________________
```

Representative output for `facility find type/REST_POINT status/UNKNOWN`:

```text
____________________________________________________________
Facilities where REST_POINT is UNKNOWN:
[F01] AS6
[F06] E4

UNKNOWN means the local dataset does not confirm the feature.
____________________________________________________________
```

If no status is supplied, `YES` is used.

Supported initial feature types:

- `LIFT`;
- `RAMP`;
- `SHELTERED_RAMP`;
- `ACCESSIBLE_WASHROOM`;
- `STEP_FREE_ENTRANCE`;
- `REST_POINT`;
- `AUTOMATIC_DOOR`; and
- `OTHER`.

### 8.4 List Connections: `connection list`

Format:

```text
connection list
```

Displays each connection's ID, endpoints, distance, accessibility status, traversal type, and shelter status.

Representative output:

```text
____________________________________________________________
Known two-way connections:
[12] COM3 <-> COM1 | 80 m  | ACCESSIBLE YES | RAMP
    Shelter: YES
[13] COM1 <-> AS6  | 120 m | ACCESSIBLE YES | PATH
    Shelter: YES
[14] AS6 <-> CLB LEVEL 3 | 140 m | ACCESSIBLE YES | RAMP
    Shelter: NO
[15] CLB LEVEL 3 <-> CLB LEVEL 6 | 40 m | ACCESSIBLE YES
    Type: LIFT | Shelter: YES
[16] CLB LEVEL 6 <-> E4 | 120 m | ACCESSIBLE YES | PATH
    Shelter: YES
____________________________________________________________
```

### 8.5 View a Connection: `connection view`

Format:

```text
connection view ID
```

Example:

```text
connection view 12
```

Representative output:

```text
____________________________________________________________
Connection [12]
From          : COM3
To            : COM1
Distance      : 80 m
Accessibility : YES
Type          : SHELTERED_RAMP
Shelter       : YES
Known barrier : None recorded
Notes         : Gentle slope beside the main entrance
____________________________________________________________
```

Displays endpoints, distance, accessibility status, traversal type, shelter status, known barrier, and notes.

### 8.6 Find Connections: `connection find`

Format:

```text
connection find [from/FACILITY] [to/FACILITY]
                [type/TYPE] [status/YES|NO|UNKNOWN]
                [shelter/YES|NO|UNKNOWN]
```

Examples:

```text
connection find from/COM3
connection find from/COM3 to/COM1
connection find type/LIFT status/YES
connection find shelter/YES
```

Representative output for `connection find from/COM3`:

```text
____________________________________________________________
Connections from COM3:
[12] COM3 <-> COM1 | 80 m  | SHELTERED_RAMP
[18] COM3 <-> AS6  | 210 m | PATH
____________________________________________________________
```

Representative output for `connection find from/COM3 to/COM1`:

```text
____________________________________________________________
Found 1 connection:
[12] COM3 <-> COM1 | 80 m | ACCESSIBLE YES
     Type: SHELTERED_RAMP | Shelter: YES
____________________________________________________________
```

Representative output for `connection find type/LIFT status/YES`:

```text
____________________________________________________________
Confirmed accessible LIFT connections:
[15] CLB LEVEL 3 <-> CLB LEVEL 6 | 40 m | Shelter: YES
____________________________________________________________
```

Representative output for `connection find shelter/YES`:

```text
____________________________________________________________
Connections with confirmed shelter:
[12] COM3 <-> COM1 | 80 m
[13] COM1 <-> AS6 | 120 m
[15] CLB LEVEL 3 <-> CLB LEVEL 6 | 40 m
[16] CLB LEVEL 6 <-> E4 | 120 m
____________________________________________________________
```

At least one filter is required. Multiple filters use AND.

Every stored connection is treated as two-way.

## 9. Accessible Route Command

### 9.1 Find One Best Route: `route`

Format:

```text
route from/START_FACILITY to/DESTINATION_FACILITY
```

Example:

```text
route from/COM3 to/E4
```

Representative output:

```text
____________________________________________________________
Best confirmed accessible route:
COM3 -> COM1 -> AS6 -> CLB LEVEL 3
-> CLB LEVEL 6 (via lift) -> E4

1. COM3 -> COM1             | 80 m  | SHELTERED_RAMP | Sheltered
2. COM1 -> AS6              | 120 m | PATH           | Sheltered
3. AS6 -> CLB LEVEL 3       | 140 m | RAMP           | Unsheltered
4. CLB LEVEL 3 -> LEVEL 6   | 40 m  | LIFT           | Sheltered
5. CLB LEVEL 6 -> E4        | 120 m | PATH           | Sheltered

Total distance: 500 m
Sheltered distance: 360 m

This route uses local reference data, not live navigation.
____________________________________________________________
```

The route planner:

- considers only connections with accessibility `YES`;
- accepts shelter status `YES` or `NO`;
- excludes accessibility `NO` or `UNKNOWN`;
- excludes shelter `UNKNOWN`;
- minimises total distance in metres using Dijkstra's algorithm;
- prefers more sheltered distance when total distances are equal;
- then prefers fewer segments;
- then uses the alphabetically earlier facility sequence; and
- returns only one route.

Example result:

```text
COM3 -> COM1 -> AS6 -> CLB LEVEL 3
-> CLB LEVEL 6 (via lift) -> E4

Total distance: 500 m
```

The detailed result also shows each segment's distance, traversal type, shelter status, barriers, and notes.

If a facility name is not recognised exactly, the application shows possible matches and asks the user to enter a complete new route command. It never guesses.

If the start and destination are identical:

```text
Start and destination facilities are the same.
```

If no confirmed route exists:

```text
Sorry, we could not find a confirmed accessible route from
COM3 to E4.

We are still improving our local database.
Please ask someone nearby for assistance.
```

The command is read-only and requires no confirmation.

## 10. Dashboard Commands

### 10.1 View a Dashboard: `dashboard`

Format:

```text
dashboard today [detail]
dashboard tomorrow [detail]
dashboard YYYY-MM-DD [detail]
dashboard this week [detail]
```

Examples:

```text
dashboard today
dashboard tomorrow detail
dashboard 2026-08-15
dashboard this week detail
```

Representative output for `dashboard today`:

```text
____________________________________________________________
Dashboard — Today (2026-08-15)

Completion | ######----  3/5 (60.0%)
Time       | Planned: 6 h 30 min | Completed: 4 h 00 min
Energy     | Average: 3.4/5      | Highest: 5/5
Sensory    | Average: 2.2/5      | Highest: 4/5
____________________________________________________________
```

Representative output for `dashboard tomorrow detail`:

```text
____________________________________________________________
Dashboard — Tomorrow (2026-08-16)

Completion | ###-------  1/3 (33.3%)
Time       | Planned: 4 h 00 min | Completed: 1 h 00 min
Energy     | Average: 3.0/5      | Highest: 4/5
Sensory    | Average: 2.7/5      | Highest: 4/5

Energy distribution  | 1:0  2:1  3:1  4:1  5:0
Sensory distribution | 1:0  2:2  3:0  4:1  5:0
____________________________________________________________
```

Representative output for `dashboard 2026-08-15`:

```text
____________________________________________________________
Dashboard — 2026-08-15

Completion | ######----  3/5 (60.0%)
Time       | Planned: 6 h 30 min | Completed: 4 h 00 min
Energy     | Average: 3.4/5      | Highest: 5/5
Sensory    | Average: 2.2/5      | Highest: 4/5
____________________________________________________________
```

Representative output for `dashboard this week detail`:

```text
____________________________________________________________
Dashboard — 2026-08-15 to 2026-08-21

Date       Progress       Completed   Planned time
2026-08-15 ######----     3/5 (60.0%) 6 h 30 min
2026-08-16 ###-------     1/3 (33.3%) 4 h 00 min
2026-08-17 ----------     0/2 (0.0%)  2 h 00 min
2026-08-18 N/A            0/0 (N/A)   0 min
2026-08-19 ##########     2/2 (100%)  3 h 00 min
2026-08-20 #####-----     1/2 (50.0%) 2 h 30 min
2026-08-21 ----------     0/1 (0.0%)  1 h 00 min

Energy distribution  | 1:1  2:4  3:3  4:4  5:3
Sensory distribution | 1:2  2:5  3:4  4:3  5:1
____________________________________________________________
```

Period meanings:

- `today` — current date;
- `tomorrow` — following date;
- a date — that specific date; and
- `this week` — today and the following six calendar days.

The concise view shows:

- completion count and percentage;
- planned time;
- completed planned time;
- average and highest energy-demand rating; and
- average and highest sensory-load rating.

Concise output structure:

```text
15 Aug 2026

Completion | ######----  3/5 (60.0%)
Time       | Planned: 6 h 30 min | Completed: 4 h 00 min
Energy     | Average: 3.4/5      | Highest: 5/5
Sensory    | Average: 2.2/5      | Highest: 4/5
```

`detail` additionally displays the number of activities at every rating from `1` to `5`.

Completed planned time means the planned duration belonging to completed activities. It does not claim to measure actual time spent.

A date with no activities shows:

- planned time: `0`;
- completed planned time: `0`; and
- completion percentage and load ratings: `N/A`.

## 11. Timetable Commands

The concise chronological list remains the default. The timetable is an optional NUSMods-inspired text view and does not use NUSMods data.

### 11.1 View One Day: `timetable day`

Format:

```text
timetable day/DATE [view/compact|detail]
```

Examples:

```text
timetable day/2026-08-15
timetable day/2026-08-15 view/detail
```

Representative output for `timetable day/2026-08-15`:

```text
____________________________________________________________
Timetable — 2026-08-15

08:00 | ........
09:00 | [F12 CG3207 lecture================]
10:00 | [F12 CG3207 lecture================]
11:00 | ........
12:00 | ........
13:00 | ........
14:00 | [F14 Project briefing=]
15:00 | ........
16:00 | ........
17:00 | ........
18:00 | ........

[F12] 09:00–11:00 | CG3207 lecture
[F14] 14:00–15:00 | Project briefing

Flexible but not placed:
[13] 10:00–18:00 | 90 min | Finish assignment 1

Enter timetable item/ID to inspect an activity.
____________________________________________________________
```

Representative output for `timetable day/2026-08-15 view/detail`:

```text
____________________________________________________________
Timetable — 2026-08-15 — Detailed

[F12] 09:00–11:00 | CG3207 lecture
      ACADEMIC / CG3207 | Energy 4/5 | Sensory 3/5
      Note: Bring laptop
[F14] 14:00–15:00 | Project briefing
      CCA | Energy 2/5 | Sensory 3/5

[13] Flexible, not placed | Window 10:00–18:00 | 90 min
     Finish assignment 1 | Energy 5/5 | Sensory 2/5
____________________________________________________________
```

### 11.2 View Seven Days: `timetable week`

Format:

```text
timetable week/START_DATE [view/compact|detail]
```

Example:

```text
timetable week/2026-08-15
```

Representative output:

```text
____________________________________________________________
Timetable — 2026-08-15 to 2026-08-21

Time  Sat 15     Sun 16     Mon 17     Tue 18     Wed 19
08:00 ........   ........   ........   ........   ........
09:00 [F12]      ........   [F21]      ........   [F31]
10:00 [F12]      [F18]      [F21]      ........   [F31]
11:00 ........   [F18]      ........   [F25]      ........
12:00 ........   ........   ........   [F25]      ........
13:00 ........   ........   ........   ........   ........
14:00 [F14]      ........   [F22]      ........   ........

Time  Thu 20     Fri 21
08:00 ........   ........
09:00 ........   [F36]
10:00 [F33]      [F36]
11:00 [F33]      ........
12:00 ........   ........
13:00 ........   ........
14:00 ........   ........

Use timetable day/DATE for a clearer daily view.
____________________________________________________________
```

The weekly view covers the supplied date and the following six calendar days.

The grid uses:

- `[F]` for fixed activities;
- `[R]` for recommended placements in a preview;
- `[B]` for buffers; and
- short activity labels linked to stable IDs.

Exact start and end times appear below the grid. If the terminal is too narrow, the application displays a chronological one-day fallback.

The timetable does not use mouse input, arrow-key navigation, cursor control, or continuous screen redrawing.

### 11.3 Inspect a Timetable Activity: `timetable item`

Format:

```text
timetable item/ID
```

Example:

```text
timetable item/12
```

Representative output:

```text
____________________________________________________________
Timetable item [12]
Description : CG3207 lecture
Status      : Incomplete
Placement   : Fixed
Date        : 2026-08-15
Time        : 09:00–11:00
Category    : ACADEMIC / CG3207
Energy      : 4/5
Sensory     : 3/5
Note        : Bring laptop
____________________________________________________________
```

Displays the complete details associated with that timetable entry.

## 12. Recommendation Preferences

### 12.1 View Preferences: `preference view`

Format:

```text
preference view
```

Representative output when a profile exists:

```text
____________________________________________________________
Saved recommendation preferences:
Preferred day    : 09:00–18:00
Minimum buffer  : 15 min
Peak period     : MORNING
Tomato suggestion: ON
Focus / break   : 25 min / 5 min
____________________________________________________________
```

If no profile exists, the application displays the documented recommendation defaults:

```text
start/08:00
end/20:00
buffer/15
peak/NONE
tomato/OFF
focus/25
break/5
```

### 12.2 Save or Update Preferences: `preference set`

Initial profile format:

```text
preference set start/TIME end/TIME buffer/MINUTES
               peak/MORNING|AFTERNOON|EVENING|NONE
               [tomato/ON|OFF] [focus/MINUTES] [break/MINUTES]
```

Example:

```text
preference set start/09:00 end/18:00 buffer/15 peak/MORNING tomato/ON focus/25 break/5
```

Representative output:

```text
____________________________________________________________
Recommendation preferences saved:
Preferred day    : 09:00–18:00
Minimum buffer  : 15 min
Peak period     : MORNING
Tomato suggestion: ON
Focus / break   : 25 min / 5 min
____________________________________________________________
```

After a complete profile exists, one or more values may be updated:

```text
preference set buffer/20 peak/AFTERNOON
```

Representative output:

```text
____________________________________________________________
Recommendation preferences updated:
Minimum buffer | Old: 15 min  | New: 20 min
Peak period    | Old: MORNING | New: AFTERNOON
____________________________________________________________
```

Validation:

- start must be earlier than end;
- buffer must be a non-negative whole number of minutes;
- peak must be one accepted value;
- focus must be a whole number from `10` to `60`; and
- break must be a whole number from `1` to `30`.

The first saved profile requires `start`, `end`, `buffer`, and `peak`. Tomato Co-work-style settings are optional.

## 13. Schedule Recommendation

### 13.1 Generate a Recommendation: `recommend`

Format:

```text
recommend today [PREFERENCE_OVERRIDES]
recommend tomorrow [PREFERENCE_OVERRIDES]
recommend YYYY-MM-DD [PREFERENCE_OVERRIDES]
```

Possible overrides:

```text
start/TIME end/TIME buffer/MINUTES
peak/MORNING|AFTERNOON|EVENING|NONE
tomato/ON|OFF focus/MINUTES break/MINUTES
```

Examples:

```text
recommend today
recommend tomorrow buffer/20
recommend 2026-08-15 peak/MORNING tomato/ON
```

Representative output for `recommend today`:

```text
____________________________________________________________
Recommended schedule — Today (2026-08-15)

09:00–11:00 [F12] CG3207 lecture
11:00–11:15 [B]   Recovery buffer
11:15–12:45 [R13] Finish assignment 1
14:00–15:00 [F14] Project briefing

Not scheduled: None

Satisfied:
- no overlaps;
- all flexible activities remain within their windows;
- minimum 15-minute buffer preserved; and
- high-energy work placed in the morning.

Compromised:
- 75 minutes remain between the recommended activity and
  the project briefing because fixed activities cannot be moved.

Completion | ###-------  1/3 (33.3%)
Time       | Planned: 4 h 30 min | Completed: 1 h 00 min
Energy     | Average: 3.7/5      | Highest: 5/5
Sensory    | Average: 2.7/5      | Highest: 3/5

Choose an action:
1. View ASCII timetable
2. View detailed explanations
3. Revise preferences
4. Adopt recommendation
5. Cancel
____________________________________________________________
```

Representative output for `recommend tomorrow buffer/20`:

```text
____________________________________________________________
Recommended schedule — Tomorrow (2026-08-16)
Temporary override: Minimum buffer 20 min

10:00–11:00 [F18] Internship briefing
11:00–11:20 [B]   Recovery buffer
11:20–12:20 [R19] Prepare interview notes

Not scheduled:
[20] Gym session
Reason: No 60-minute placement fits its 10:30–12:00 window
after preserving the requested buffer.

Saved preferences were not changed.

Choose an action: 1 Timetable | 2 Explanations | 3 Revise
                  4 Adopt     | 5 Cancel
____________________________________________________________
```

Representative output for `recommend 2026-08-15 peak/MORNING tomato/ON`:

```text
____________________________________________________________
Recommended schedule — 2026-08-15
Temporary overrides: Peak MORNING | Tomato suggestion ON

09:00–11:00 [F12] CG3207 lecture
11:00–11:15 [B]   Recovery buffer
11:15–12:45 [R13] Finish assignment 1
14:00–15:00 [F14] Project briefing

Preference explanation:
- The high-energy flexible activity was placed in the morning.
- Consecutive high-sensory activities were avoided.
- The 15-minute minimum buffer was preserved.

Suggested focus approach for [13], based on Tomato Co-work:
25 min focus | 5 min break | 25 min focus
5 min break  | 25 min focus | 5 min wrap-up

Choose an action: 1 Timetable | 2 Explanations | 3 Revise
                  4 Adopt     | 5 Cancel
____________________________________________________________
```

A temporary override does not modify the saved profile.

The recommender:

1. keeps fixed activities unchanged;
2. processes the least-flexible activity first;
3. considers only placements inside the activity's allowed window;
4. preserves the activity's complete duration;
5. rejects overlapping placements;
6. prefers the minimum buffer;
7. prefers the selected daily start and end range;
8. avoids consecutive sensory ratings of `4` or `5`;
9. avoids consecutive energy ratings of `4` or `5`;
10. places high-energy activities near the selected peak; and
11. uses the earlier valid start as the final tie-breaker.

The recommender is deterministic but does not claim to find a mathematical global optimum.

If an activity cannot be placed, it remains flexible and appears under `Not scheduled` with one concrete reason.

The output provides:

- a chronological recommended plan;
- unscheduled activities and reasons;
- satisfied and compromised preferences;
- exact dashboard metrics; and
- optional advisory focus suggestions.

Example action menu:

```text
Choose an action:
1. View ASCII timetable
2. View detailed explanations
3. Revise preferences
4. Adopt recommendation
5. Cancel
```

If option `4` is selected:

```text
Adopting this recommendation will convert scheduled flexible
activities into fixed activities. Unscheduled activities will
remain flexible.

Adopt this recommended schedule? (y/n)
```

After `y`:

- scheduled flexible activities become fixed at their recommended times;
- unscheduled activities remain flexible;
- all changes are saved atomically; and
- `data/recommended_schedule.txt` is replaced with the latest adopted snapshot.

Representative output after a successful adoption:

```text
____________________________________________________________
Recommendation adopted successfully.
Converted to fixed activities: [13]
Still flexible: None
Latest snapshot: data/recommended_schedule.txt
____________________________________________________________
```

If any required save fails, every activity change is rolled back and the previous snapshot remains unchanged.

Activities do not store locations. A recommendation does not calculate travel time, call the route planner, or claim that the day is travel-feasible.

### 13.2 Tomato Co-work-Style Suggestion

When enabled, the output clearly states:

```text
Suggested focus approach
Based on the Tomato Co-work style:

25 minutes focus
5 minutes break
25 minutes focus
5 minutes wrap-up

This is an optional suggestion. Adjust or ignore it according
to your needs.
```

The suggestion:

- is advisory;
- stays within the activity's scheduled duration;
- may shorten its final focus or wrap-up block;
- does not start a countdown;
- does not send notifications;
- does not change scheduling feasibility; and
- is not medical advice.

## 14. CSV Export Commands

### 14.1 Export Activities: `export activities`

Format:

```text
export activities
```

Creates a UTF-8 CSV containing the complete current activity list.

Representative output:

```text
____________________________________________________________
Activity history exported successfully.
File: exports/activities_2026-08-15_143000.csv
Records exported: 13
____________________________________________________________
```

Example filename:

```text
exports/activities_2026-08-15_143000.csv
```

### 14.2 Export the Latest Adopted Timetable: `export schedule`

Format:

```text
export schedule
```

Creates a CSV from the latest `recommended_schedule.txt` snapshot.

Representative output:

```text
____________________________________________________________
Latest adopted timetable exported successfully.
File: exports/recommended_schedule_2026-08-15_143000.csv
Scheduled activities exported: 3
____________________________________________________________
```

If no timetable has been adopted, no file is created and the application explains why.

### 14.3 Export Both: `export all`

Format:

```text
export all
```

Both files use the same timestamp. If no adopted timetable exists, the activity export still succeeds and the schedule export is reported as skipped.

Representative output:

```text
____________________________________________________________
Export completed.
Activities: exports/activities_2026-08-15_143000.csv
Schedule  : exports/recommended_schedule_2026-08-15_143000.csv

Both files use the same historical-record timestamp.
____________________________________________________________
```

### 14.4 Select an Export Interactively: `export`

Entering:

```text
export
```

displays:

```text
Choose what to export:
1. Current activity list
2. Latest adopted timetable
3. Both
4. Cancel
```

Representative output after selecting `1`:

```text
____________________________________________________________
Activity history exported successfully.
File: exports/activities_2026-08-15_143000.csv
Records exported: 13
____________________________________________________________
```

Export rules:

- Every export creates a new file.
- Existing historical exports are never overwritten.
- A numerical suffix is added if a generated filename already exists.
- Commas, quotation marks, and line breaks are escaped correctly.
- Exported CSV files are historical records.
- CSV importing is not supported.
- Exporting is read-only and requires no confirmation.

## 15. Built-In Guide

### 15.1 Open the Main Guide: `guide`

Format:

```text
guide
```

The application displays:

```text
Application Guide

1. Getting started
2. Add, edit and delete activities
3. List, find and view activities
4. Categories and topics
5. Completion and dashboard
6. Recommended timetable
7. Accessible facilities and routes
8. CSV export
9. Data files and storage
10. Return
```

Representative output:

```text
____________________________________________________________
Application Guide

1. Getting started
2. Add, edit and delete activities
3. List, find and view activities
4. Categories and topics
5. Completion and dashboard
6. Recommended timetable
7. Accessible facilities and routes
8. CSV export
9. Data files and storage
10. Return

Enter a number from 1 to 10.
____________________________________________________________
```

### 15.2 Open a Guide Topic Directly

Examples:

```text
guide getting-started
guide add
guide edit
guide find
guide topic
guide dashboard
guide timetable
guide recommend
guide facility
guide route
guide export
guide storage
```

Representative outputs:

```text
guide getting-started
____________________________________________________________
Getting started
Run the JAR, enter guide when needed, and use bye to exit.
Related commands: guide, bye
____________________________________________________________

guide add
____________________________________________________________
Add activities
Use add with FIXED timing or a FLEXIBLE window and duration.
Related commands: topic add, list, view
____________________________________________________________

guide edit
____________________________________________________________
Edit an activity
Format: edit ID PREFIX/NEW_VALUE [PREFIX/NEW_VALUE ...]
The application validates all changes before asking for y/n.
____________________________________________________________

guide find
____________________________________________________________
Find activities
Format: find [k/KEYWORD ...] [FILTERS]
Multiple keywords and filters use AND.
____________________________________________________________

guide topic
____________________________________________________________
Categories and topics
Topics are optional one-level groupings inside fixed categories.
Related commands: topic add, topic list, topic rename, topic delete
____________________________________________________________

guide dashboard
____________________________________________________________
Completion and daily load
Use dashboard today, tomorrow, YYYY-MM-DD, or this week.
Add detail to display the full 1-to-5 rating distribution.
____________________________________________________________

guide timetable
____________________________________________________________
Text timetable
Use timetable day/DATE or timetable week/START_DATE.
Use timetable item/ID to inspect one entry.
____________________________________________________________

guide recommend
____________________________________________________________
Recommended timetable
Use recommend PERIOD [PREFERENCE_OVERRIDES].
Review the plan before choosing whether to adopt it.
____________________________________________________________

guide facility
____________________________________________________________
Accessible facilities
Use facility list, facility view, or facility find.
The local data is read-only and is not a real-time guarantee.
____________________________________________________________

guide route
____________________________________________________________
Accessible routes
Format: route from/START_FACILITY to/DESTINATION_FACILITY
The planner returns one best confirmed route from local data.
____________________________________________________________

guide export
____________________________________________________________
CSV exports
Use export activities, export schedule, export all, or export.
Each export creates a timestamped historical record.
____________________________________________________________

guide storage
____________________________________________________________
Data files and storage
Application data is stored under data/. CSV history is under
exports/. Do not edit data files while the application runs.
____________________________________________________________
```

Each topic contains:

- a short purpose;
- the command format;
- one valid example;
- important restrictions;
- related commands; and
- a return option.

The guide is packaged inside the executable JAR and requires no internet connection.

## 16. Exit the Application: `bye`

Format:

```text
bye
```

The application saves pending application-managed data and exits normally.

Representative output:

```text
____________________________________________________________
Your data has been saved.
Bye! Take care and see you again.
____________________________________________________________
```

## 17. Data Storage

The planned local files are:

```text
data/
├── activities.txt
├── topics.txt
├── preferences.txt
├── recommended_schedule.txt
├── facilities.txt
└── connections.txt

exports/
├── activities_TIMESTAMP.csv
└── recommended_schedule_TIMESTAMP.csv
```

Storage roles:

- `activities.txt` — authoritative current activity data;
- `topics.txt` — user-defined topic records;
- `preferences.txt` — saved recommendation profile;
- `recommended_schedule.txt` — latest adopted recommendation snapshot;
- `facilities.txt` — locally maintained facility and feature reference data;
- `connections.txt` — locally maintained weighted route connections; and
- `exports/` — timestamped historical CSV records.

The application saves application-managed data after successful mutating commands.

Default facility and connection templates are copied outside the JAR on first use. Users may manually update those two accessibility files while the application is closed. Changes are validated and loaded at the next launch.

For an invalid accessibility record, the application:

- identifies the filename and line number;
- explains the validation problem;
- ignores only that record;
- loads the remaining valid records; and
- never uses the invalid record as route evidence.

Do not edit data files while the application is running. Back up the `data` folder before manual maintenance.

## 18. Error Handling

Common command failures include:

- an unknown command;
- a missing required field;
- an unsupported prefix;
- an invalid date or time;
- an invalid `1`–`5` rating;
- a non-positive duration;
- an unknown stable ID;
- an unknown category or topic;
- an exact duplicate;
- an overlapping fixed activity;
- a failed confirmation;
- an unavailable export folder; or
- a storage failure.

Error messages identify the affected command, record, or field where possible.

Example categories:

```text
[Error] Invalid input: ...
[Error] Missing input: ...
[Error] Not found: ...
[Error] Conflict: ...
[Error] Storage error: ...
[Warning] Partial data loaded: ...
```

Representative error examples:

Invalid activity rating:

```text
edit 12 energy/7
____________________________________________________________
[Error] Invalid input: energy must be a whole number from 1 to 5.
Activity [12] was not changed.
Example: edit 12 energy/4
____________________________________________________________
```

Missing required field:

```text
add n/Project meeting c/ACADEMIC date/2026-08-15 type/FIXED from/14:00
____________________________________________________________
[Error] Missing input: a FIXED activity requires to/, energy/,
and sensory/.
No activity was added.
____________________________________________________________
```

Unknown stable ID:

```text
delete 999
____________________________________________________________
[Error] Not found: Activity [999] does not exist.
Use list or find to locate an activity ID.
____________________________________________________________
```

Overlapping fixed activity:

```text
add n/Consultation c/ACADEMIC date/2026-08-15 type/FIXED from/10:30 to/11:30 energy/3 sensory/2
____________________________________________________________
[Error] Conflict: This timing overlaps activity [12],
CG3207 lecture (09:00–11:00).
No activity was added.
____________________________________________________________
```

Invalid route facility:

```text
route from/COM33 to/E4
____________________________________________________________
[Error] Not found: Facility "COM33" is not recognised.
Possible match: COM3
Enter a complete new route command using the intended facility.
____________________________________________________________
```

Invalid commands do not partially change stored data. Mutating multi-field commands are atomic.

## 19. Frequently Asked Questions

### What is the difference between a fixed and flexible activity?

A fixed activity already has a start and end time. A flexible activity has an allowed window and required duration, allowing the recommender to choose a placement.

### Can activity descriptions and notes contain spaces?

Yes. The next recognised prefix marks the end of a value.

### Why does an edit require confirmation?

Editing changes stored schedule information. The old and proposed values are displayed before the application accepts `y` or `n`.

### Why do mark and unmark not require confirmation?

They are immediately reversible and use separate commands.

### Does the recommender use accessible-route data?

No. Activities do not contain locations. Route requests are separate and require explicit starting and destination facilities.

### Does the recommender guarantee the optimal timetable?

No. It uses a documented deterministic greedy strategy to create one practical recommendation.

### Is the Tomato Co-work-style output a live timer?

No. It is an optional text suggestion only.

### Is the accessibility dataset official or live?

No. It is a small local reference dataset and may not reflect current real-world conditions.

### Can I update accessibility information inside the application?

No. Accessibility CRUD commands are out of scope. The external `facilities.txt` and `connections.txt` files may be maintained manually while the application is closed.

### Why was no route returned?

The application uses only connections with accessibility `YES` and shelter `YES` or `NO`. Inaccessible, unknown-accessibility, unknown-shelter, invalid, or disconnected records cannot form a confirmed route.

### Can I import an exported CSV?

No. CSV files are external historical records only.

### What happens to an older adopted recommendation?

`recommended_schedule.txt` keeps only the latest adopted snapshot. Use `export schedule` before a later adoption if you want a permanent CSV history.

### What does `this week` mean?

For the dashboard, it means today and the following six calendar days.

## 20. Complete Command Summary

### General

```text
guide
guide TOPIC
bye
```

### Activities

```text
add n/DESCRIPTION c/CATEGORY date/DATE type/FIXED from/START to/END energy/1-5 sensory/1-5 [topic/TOPIC] [note/NOTES]

add n/DESCRIPTION c/CATEGORY date/DATE type/FLEXIBLE earliest/TIME latest/TIME dur/MINUTES energy/1-5 sensory/1-5 [topic/TOPIC] [note/NOTES]

list [view/concise|detail] [status/all|completed|incomplete] [c/CATEGORY] [topic/TOPIC] [date/DATE] [order/input|time|chronological]

view ID

find [k/KEYWORD ...] [c/CATEGORY] [topic/TOPIC] [date/DATE] [order/input|time|chronological]

order view
order set input|time|chronological

edit ID PREFIX/NEW_VALUE [PREFIX/NEW_VALUE ...]
delete ID
mark ID
unmark ID
next
```

### Topics

```text
topic add c/CATEGORY n/TOPIC
topic list [c/CATEGORY]
topic rename c/CATEGORY old/OLD_TOPIC new/NEW_TOPIC
topic delete c/CATEGORY n/TOPIC
```

### Accessibility References

```text
facility list
facility view FACILITY
facility find type/FEATURE [status/YES|NO|UNKNOWN]

connection list
connection view ID
connection find [from/FACILITY] [to/FACILITY] [type/TYPE] [status/YES|NO|UNKNOWN] [shelter/YES|NO|UNKNOWN]
```

### Route

```text
route from/START_FACILITY to/DESTINATION_FACILITY
```

### Dashboard and Timetable

```text
dashboard today [detail]
dashboard tomorrow [detail]
dashboard YYYY-MM-DD [detail]
dashboard this week [detail]

timetable day/DATE [view/compact|detail]
timetable week/START_DATE [view/compact|detail]
timetable item/ID
```

### Preferences and Recommendation

```text
preference view
preference set start/TIME end/TIME buffer/MINUTES peak/MORNING|AFTERNOON|EVENING|NONE [tomato/ON|OFF] [focus/MINUTES] [break/MINUTES]
preference set PREFIX/NEW_VALUE [PREFIX/NEW_VALUE ...]

recommend today [PREFERENCE_OVERRIDES]
recommend tomorrow [PREFERENCE_OVERRIDES]
recommend YYYY-MM-DD [PREFERENCE_OVERRIDES]
```

### CSV Export

```text
export
export activities
export schedule
export all
```

## 21. Current Draft Limitations

This guide defines the current canonical command proposal. Before coding begins, the team must still finalise:

- the product name and JAR filename;
- exact storage escaping and malformed activity-file recovery;
- exact CSV column order;
- terminal-width thresholds for timetable fallback; and
- measurable non-functional requirements.

Any later command change must be updated consistently in:

- this User Guide;
- the requirements baseline;
- the user stories and use cases;
- automated and manual tests; and
- implementation help text.
