# CS2113 tP Requirements Baseline v0.2

**Status:** Working baseline based on decisions made so far.  
**Purpose:** Consolidate the agreed product direction before proceeding to detailed requirements gathering.

## 1. Confirmed Product Direction

- The product will be a single-user, CLI-based Java application.
- It will support unfamiliar daily routines across university, internships, and entry-level work.
- It will serve two equally important target groups:
  - tertiary students with ASD or ADHD;
  - tertiary students who use wheelchairs.
- Target users can type comfortably and prefer CLI interaction.
- The product will operate offline and will not depend on external data sources.
- The central product concept is an accessible itinerary planner, not a general calendar or full task-management system.

## 2. Revised Target Users

> Tertiary students with ASD or ADHD, and tertiary students who use wheelchairs, who are entering unfamiliar university, internship, or entry-level work routines, can type comfortably, and prefer CLI-based planning.

## 3. Confirmed Problem Statement

Tertiary students with ASD or ADHD and wheelchair users entering unfamiliar routines may need to consider schedules, sensory demands, recovery time, accessible travel and environmental barriers separately, making it difficult to create a practical daily plan that reflects their individual needs.

## 4. Confirmed Value Proposition

> Helps tertiary students with ASD or ADHD and wheelchair users prepare for unfamiliar university and work routines through concise, preference-based daily schedules and separate locally maintained accessibility and route references—all within a fast, offline CLI.

### 4.1 Confirmed CLI Suitability

The CLI suits the selected target users because they can type comfortably and prefer direct keyboard interaction. Predictable one-shot commands reduce mouse dependence and provide consistent workflows. Concise default output limits visual clutter, while optional detailed views and ASCII summaries provide additional information when requested. The application also remains fast, lightweight, and usable offline.

This suitability is specific to the selected personas. It does not imply that a CLI is suitable for every person with ASD, ADHD, or a physical disability.

### 4.2 Confirmed Core User Workflow

The user records fixed and flexible activities with timing, energy, sensory, category, topic, and completion information. The application presents the daily itinerary, identifies the next activity, recommends suitable times for flexible activities, and summarises user-recorded progress. A separate accessibility subsystem lets the user view locally stored facility and connection information and request a route between facilities supplied to the route command.

## 5. Equal Primary Personas

### 5.1 Sam — Tertiary Student with ASD/ADHD

**Context**

Sam is entering an unfamiliar university, internship, or workplace routine. Sam can type comfortably and prefers predictable CLI interaction.

**Interaction constraints and needs**

- Can become overwhelmed when schedules contain too much information.
- Needs concise default output with optional additional details.
- Benefits from predictable command and response formats.
- Needs to retrieve the next relevant activity without reading the full day.
- Benefits from advance information about noise, crowds, lighting, and other sensory conditions.
- Needs warnings and error messages to be clear and specific.
- May use categories, filters, and self-selected energy or sensory levels to organise activities.
- Needs sufficient preparation and recovery time around demanding activities.
- Benefits from viewing the demanding parts of a day without reading every itinerary item.
- Does not want the application to make assumptions or medical decisions based only on an ASD or ADHD label.

### 5.2 Jordan — Tertiary Student Who Uses a Wheelchair

**Context**

Jordan is entering an unfamiliar university, internship, or workplace environment. Jordan can type comfortably and prefers CLI interaction.

**Interaction constraints and needs**

- Needs advance information about physical accessibility.
- Needs to know whether locations and paths have step-free access.
- Benefits from information about accessible entrances, lifts, toilets, and rest points.
- Needs warnings about stairs, narrow paths, or other known barriers.
- May require additional travel time between activities.
- Benefits from a separate lookup of locally recorded wheelchair-accessible route distance.
- Benefits from accessibility reference data that can be refreshed through external local text files.
- Needs invalid input to be recoverable without losing valid information.

**Important non-assumption**

Wheelchair use does not automatically imply difficulty typing, using a mouse, or pressing key combinations. Those limitations are not part of Jordan’s persona unless later supported by a specific requirement.

## 6. Preliminary Release Scope

### 6.1 v1.0 — Minimum Viable Product

The first working version should provide a complete basic workflow:

- Add an itinerary item.
- List itinerary items in chronological order.
- Find itinerary items.
- Edit individual fields.
- Delete an itinerary item.
- Mark and unmark an item as completed.
- Assign every activity to a fixed top-level category.
- Create and manage optional user-defined topics within those categories.
- List or find activities by category or topic.
- View the next relevant activity in a concise format.
- Record basic sensory information.
- View a pre-populated sample of facility accessibility information.
- View pre-populated accessibility and travel-time information for connections between facilities.
- Record whether an activity has a fixed time or can be scheduled within a time window.
- Record the expected duration of a flexible activity.
- Record required, user-selected `LOW`, `MEDIUM`, or `HIGH` energy-demand and sensory-load levels.
- Save and load data using a local, human-editable text file.

The exact item fields and command formats remain to be specified.

#### Activity Classification

Every activity will belong to exactly one of four fixed top-level categories:

- Academic;
- CCA;
- Work and Internship;
- Others.

The fixed categories provide a predictable shared structure and cannot be renamed or deleted. Within a category, users may create an optional topic that reflects their own context, such as a module, CCA, workplace, or personal project. A topic name must be unique within its category, although the same name may appear in another category. Each activity may belong to at most one topic, and deeper nesting will not be supported in v1.0.

For example:

```text
Academic
└── CG3201
    └── Finish Assignment 1
```

Users will be able to create, list, rename, and delete topics. A topic that is still assigned to activities must not be deleted until those activities are reassigned or the user explicitly chooses another valid category or topic.

Illustrative commands:

```text
topic add c/Academic n/CG3201
add n/Finish Assignment 1 c/Academic topic/CG3201
list c/Academic topic/CG3201
```

The exact prefixes will be decided during command-design work. The classification feature will support concise filtered views and reduce the need to read unrelated academic, CCA, work, internship, and personal activities together. Categories and topics organise activities; they must not be used to infer a user's energy, sensory, or accessibility requirements.

### 6.2 v2.0 — Target Features for Peer Testing

The following final-product features should be substantially complete by v2.0:

#### Data Sufficiency and Clarification

Users may save basic records while leaving optional information unknown. However, before producing a timetable recommendation or an accessible-route suggestion, the application must check that the information required for that specific operation is available. Route requests obtain their starting and destination facilities directly from the route command; activities do not store locations.

For a timetable recommendation, if activity data is insufficient, the application will:

- avoid guessing missing sensory, energy, or timing values;
- decline to produce an unsupported recommendation;
- identify the affected activity;
- list only the missing or invalid fields required for the requested operation;
- offer a guided correction path that takes the user directly to the affected record and required fields;
- provide a direct one-shot edit command as an alternative to guided correction;
- preserve the interrupted timetable request while the correction is made;
- return to and retry the interrupted operation after the required information has been supplied;
- allow the user to cancel the correction and return without generating a recommendation;
- preserve all information that the user has already entered.

In the CLI, navigation means a short, explicit correction workflow rather than movement between graphical screens. The application will show which record requires attention and request only the fields needed for the interrupted operation. Users who prefer faster direct interaction can leave the guided workflow, use a one-shot edit command, and run the original operation again.

Guided changes will be staged and validated before being committed. If any supplied value is invalid, the original record will remain unchanged rather than being partially updated.

For example:

```text
Cannot recommend a timetable: Activity 3 is missing duration and latest end time.
Fix Activity 3 now? (y/n)
> y
Duration in minutes: 60
Latest end time: 16:00
Activity 3 updated. Resuming timetable recommendation...
```

One-shot alternative:

```text
edit 3 dur/60 latest/16:00
recommend
```

The route subsystem is read-only. If an accessibility file contains a malformed or inconsistent record, the application will identify the filename and line or record, exclude that invalid record, and report that the loaded dataset is partial. Users may correct the external file and restart the application. No in-application command will add, edit, or delete facility, feature, or connection records.

An accessible route must not be labelled as confirmed when a required connection has unknown accessibility information.

#### Wheelchair-Accessible Route Suggestion

Jordan can request an accessible route between two known locations.

The feature will:

- use a small, locally stored set of known locations and connections;
- use manually recorded accessibility data;
- avoid connections marked as inaccessible;
- avoid labelling connections with unknown accessibility as confirmed accessible;
- return an ordered route with relevant access notes;
- report clearly when no known accessible route exists;
- work without GPS, external APIs, or downloaded map information.

The routing cost will be locally recorded distance in metres. The route planner will use Dijkstra's algorithm to find the lowest-distance confirmed accessible route. BFS is not suitable for this cost model because it minimises the number of connections rather than their total weighted distance.

#### Local Accessibility Reference Dataset

The route feature will be usable immediately with a small, pre-populated sample dataset. The initial dataset will contain approximately 6–10 facilities and 10–15 connections, which is sufficient to demonstrate accessible routes, alternative routes, and cases where no accessible route is known.

The dataset will record:

- facilities and their typed accessibility features, such as lifts, ramps, sheltered ramps, accessible washrooms, step-free entrances, rest points, automatic doors, and relevant notes;
- two-way connections between facilities, including distance in metres, accessibility status, traversal type, known barriers, and notes.

Accessibility fields will distinguish `yes`, `no`, and `unknown`. Missing information must not be interpreted as accessible.

The initial accessibility-feature types will be `LIFT`, `RAMP`, `SHELTERED_RAMP`, `ACCESSIBLE_WASHROOM`, `STEP_FREE_ENTRANCE`, `REST_POINT`, `AUTOMATIC_DOOR`, and `OTHER`. Facility features and connection traversal types are related but distinct: a facility feature describes an amenity at a venue, while a connection traversal type describes how a route segment is travelled.

Viewing and searching the supplied facility and connection data are must-have capabilities. Accessibility-data CRUD commands are out of scope.

The data will use two simple, human-readable, line-based text files rather than requiring JSON or an external database:

- `facilities.txt` for facility records;
- `connections.txt` for weighted connection records.

For example:

```text
FACILITY|F01|COM3|Engineering building
FEATURE|F01|LIFT|YES|Level 1 lobby
FEATURE|F01|ACCESSIBLE_WASHROOM|YES|Level 2
FEATURE|F01|SHELTERED_RAMP|YES|Main entrance
CONNECTION|COM3|COM1|120|YES|SHELTERED_RAMP|NONE|Covered path
CONNECTION|CLB LEVEL 3|CLB LEVEL 6|20|YES|LIFT|NONE|Use the north lift
```

Facility names and structured enum values will be normalised to uppercase before storage and comparison. Natural-language notes will preserve the user's original letter case.

Default dataset templates will be packaged with the application. On first use, the application will create external `facilities.txt` and `connections.txt` copies. Those external files become the single source of accessibility data and may be edited manually outside the application; changes are validated and loaded on the next launch.

The dataset will be described as sample or locally maintained reference information. It will not be presented as verified real-time information or as a guarantee of actual route conditions.

Data-content preparation may be managed as a separate team workstream, but the application remains responsible for loading, validating, and displaying the files. Malformed records must be identified clearly and must not silently become route evidence.

The CLI will keep record types explicit:

```text
facility list
facility view COM3
facility find type/LIFT
connection list
connection view 12
connection find from/COM3
route from/COM3 to/E4
```

Activity commands remain unqualified by default. Facility commands operate on venues and their features, connection commands inspect individual stored graph edges, and the route command combines eligible connections into a calculated path.

A detailed route result will show every segment's starting and destination facilities, distance, accessibility type, barriers, and notes, followed by the complete ordered facility chain and total distance. Only connections marked `YES` are eligible for a confirmed route; `NO` and `UNKNOWN` connections are excluded. Estimated travel time will not be calculated or claimed.

#### Accessible Schedule Dashboard

Sam and Jordan can view an optional text-based dashboard that summarises metrics directly related to their daily planning needs.

The initial metrics will be:

- planned activity time;
- free or buffer time;
- total user-entered energy demand;
- total user-entered sensory load;
- number of activities marked as high sensory or high energy;
- completed activities divided by planned activities as a secondary progress metric.

Energy and sensory values are self-reported planning data. They are not medical assessments.

The dashboard will:

- show exact values beside every visual;
- use simple ASCII histograms;
- avoid relying on colour;
- use only information entered or confirmed by the user;
- allow a normal chronological text view instead;
- avoid medical conclusions or performance judgements;
- keep each view small enough to remain readable in a typical terminal.

Example:

```text
Wednesday overview

Planned time   | ######----  6.0 h
Buffer time    | ##--------  2.0 h
Energy demand  | #######---  7 points
Sensory load   | #####-----  5 points
Progress       | ######----  6/10 completed

High-load periods: 10:00-12:00, 15:00-16:00
```

A line graph is not part of the initial target. A histogram is easier to render, explain, and test in a CLI. A line graph can be reconsidered only if a clear time-series need appears later.

#### Optional Navigable ASCII Timetable View

Sam and Jordan can request an optional NUSMods-inspired timetable view that presents days as columns and time slots as rows. The feature will remain a command-driven CLI view rather than a full-screen terminal interface.

The timetable will:

- keep the concise chronological day view as the default;
- provide optional day and weekly grid views;
- use short activity identifiers in the grid and provide complete details below it;
- distinguish fixed activities, recommended flexible placements, and buffers using text markers;
- show exact start and end times in the accompanying details;
- avoid relying on colour, mouse input, arrow-key navigation, ANSI cursor control, or continuous screen redrawing;
- provide predictable one-shot commands for changing views and inspecting activities;
- fall back to a one-day chronological view when the terminal is too narrow;
- require explicit user confirmation before a recommended schedule is adopted.

Illustrative weekly view:

```text
WEEKLY TIMETABLE | 17-21 AUG 2026 | RECOMMENDATION

+-------+----------------+----------------+----------------+----------------+----------------+
| Time  | Monday         | Tuesday        | Wednesday      | Thursday       | Friday         |
+-------+----------------+----------------+----------------+----------------+----------------+
| 08:00 |                |                |                |                |                |
| 09:00 | A1 CG3201 [F]  |                | W1 Work [F]    |                |                |
| 10:00 | A1 continued   | A2 Study [R]   | W1 continued   | A3 Tutorial[F] |                |
| 11:00 | B1             | A2 continued   |                | B2             | A4 Assign. [R] |
| 12:00 | C1 Enablers[F] | B3             | O1 Lunch [F]   | O2 Lunch [F]   | A4 continued   |
| 13:00 | C1 continued   | A5 Reading [R] | B4             |                | B5             |
| 14:00 |                | A5 continued   | A6 Lab [F]     | W2 Meeting [F] |                |
| 15:00 | A7 Review [R]  |                | A6 continued   | W2 continued   | C2 Training[F] |
| 16:00 |                |                | B6             |                | C2 continued   |
+-------+----------------+----------------+----------------+----------------+----------------+

[F] Fixed activity
[R] Recommended placement of a flexible activity
B   Preparation or recovery buffer
```

Illustrative narrow-terminal fallback:

```text
MONDAY | 17 AUG 2026

09:00-11:00  [F]  CG3201 Lecture
11:15-11:45  [B]  Recovery buffer
12:00-14:00  [F]  Enablers Meeting
15:00-16:00  [R]  Assignment Review
```

Illustrative navigation commands:

```text
timetable week/17-08-2026
timetable day/mon
timetable item/A1
timetable compact
timetable details
schedule adopt
schedule reject
```

The exact command syntax and terminal-width threshold remain to be finalised. The project documentation will acknowledge NUSMods as design inspiration while making clear that the application does not reproduce the NUSMods GUI or use NUSMods data.

#### Preference-Based Schedule Recommender

Sam and Jordan can request one recommended daily schedule based on preferences they enter themselves.

The feature takes inspiration from the preference-driven idea of a timetable optimiser, but it will not reproduce the NUSMods GUI, import NUSMods data, or explore an unbounded number of combinations.

The user can:

- pin fixed activities;
- provide a time window and duration for each flexible activity;
- specify preferred earliest and latest times;
- specify desired minimum buffer time;
- record energy-demand and sensory-load levels;
- provide explicit planning preferences without configuring numerical scoring weights.

Every recommended schedule must satisfy these hard constraints:

- no overlapping activities;
- every flexible activity remains within its allowed time window;
- required buffer time exists between consecutive activities.

Activities do not store locations. The timetable recommender therefore does not infer travel requirements, calculate routes between activities, or claim that a recommended timetable is travel-feasible. Accessible-route requests remain a separate workflow in which the user supplies the starting and destination facilities.

After satisfying the hard constraints, the recommender will select one schedule using documented, deterministic soft-preference rules such as:

- reduce consecutive high-energy activities;
- reduce consecutive high-sensory activities;
- preserve break or recovery periods;
- respect preferred start and end times.

The output will:

- return one recommended schedule;
- show a concise chronological plan;
- provide exact dashboard metrics for the recommendation;
- explain which preferences the recommendation satisfies or compromises;
- require the user to choose whether to adopt the recommendation;
- remain deterministic and testable for the same data and preferences.

The recommender does not diagnose the user, infer needs from ASD, ADHD, or wheelchair use, or guarantee that a schedule is medically suitable.

Multiple ranked alternatives may be reconsidered only after the single-schedule version is stable, testable, and within the team's remaining capacity.

### 6.3 v2.1 — Final Release

The final release should contain the v2.0 target features, with emphasis on:

- fixing defects found during peer testing;
- preventing regressions;
- improving error messages and usability;
- finalising automated and manual tests;
- ensuring the UG and DG match the released product;
- completing the final executable and documentation.

Major new features should not first be introduced at v2.1.

The team will treat v2.1 as an internal feature freeze: changes should focus on defects, regressions, usability, documentation, packaging, and submission readiness.

## 7. Current Accessibility Principles

- Output should be concise and predictable.
- Additional details should be optional where practical.
- Common operations should use direct, one-shot commands.
- Any guided multi-step workflow should also provide a one-shot command alternative.
- Users should be able to edit one field without re-entering the whole item.
- Mutating commands should be atomic: if any requested change is invalid, the original stored record should remain unchanged.
- Errors should preserve previously stored information and explain the specific record, field, and problem.
- ASCII graphics must include exact textual values.
- No information should be communicated through colour alone.
- The default schedule view should remain a concise chronological text view.
- The wider timetable grid should remain optional and provide a narrow-terminal fallback.
- Schedule recommendations should be preference-based, explainable, and user-controlled.
- Metrics should relate directly to energy, sensory demand, time, accessible travel, or completion.
- Accessibility and sensory information should come from recorded data, not unsupported assumptions.
- Features should provide meaningful value to Sam, Jordan, or both without making the experience worse for the other persona.

## 8. Current Technical and tP Constraints

- Java 17.
- Primarily object-oriented design.
- CLI as the primary interaction mode.
- Single-user operation.
- Platform-independent behaviour on Windows, Linux, and macOS.
- Local, human-editable text-file storage.
- No DBMS.
- No dependency on a private remote server.
- No required installer.
- Standalone executable JAR.
- External libraries or services should be avoided for the current product direction.
- Development should be breadth-first, iterative, and incremental.
- Automated tests, manual-testing instructions, UG, DG, UML diagrams, releases, and project-tracking evidence will be required.

### 8.1 Preliminary Construction Principles

These principles guide later architectural design without prematurely fixing every class or interface:

- Follow an AB3-inspired separation of UI, logic, model, and storage responsibilities.
- Represent user operations as command objects where this keeps parsing separate from execution.
- Keep activity planning data separate from the accessibility-reference graph. Activities do not reference facilities; route commands receive their endpoints directly.
- Use three-state accessibility values (`yes`, `no`, `unknown`) where absence of information must remain distinguishable from a negative value.
- Finalise the v1.0 data fields, validation rules, and complete command syntax before v1.0 coding begins.
- Keep v2.0-only classes and interfaces provisional until their user stories, use cases, and acceptance criteria are agreed.
- Introduce an interface or design pattern only when it solves a demonstrated design problem; do not add patterns solely to showcase them.
- Treat the exact guided-correction implementation, including whether it requires a dedicated state-machine class, as an architectural decision rather than a confirmed requirement.
- Specify the line-based storage format precisely before implementing persistence, including delimiter escaping and malformed-line behaviour. A formal EBNF grammar is optional.

### 8.2 Testing and Team Workflow Principles

- Divide implementation primarily by complete user-visible features rather than isolated technical components.
- Deliver each feature as a vertical slice covering input, validation, logic, storage, output, tests, and relevant documentation.
- Integrate frequently through small reviewed pull requests while keeping the main product runnable.
- Set up the Java 17 build and executable JAR from the beginning.
- Require the build and relevant automated tests to pass before merging.
- Prioritise automated tests for important behaviour, boundaries, invalid input, file loading, routing, recommendation rules, and regression-prone code; no arbitrary coverage target is required.
- Derive integration and manual-testing scenarios from Sam's and Jordan's main use cases.
- Test valid and malformed human-editable data files, including clear identification of the affected line or record.
- Keep each release usable; do not deliver back-end-only fragments of features scheduled for that release.

## 9. Out of Scope

- Real-time GPS or turn-by-turn navigation.
- Downloading maps from external services.
- Live lift, crowd, weather, or barrier information.
- Automatic detection of inaccessible routes.
- In-application creation, editing, or deletion of facility, feature, or connection records.
- Public-transport route planning.
- External calendar synchronisation.
- NUSMods data import or integration.
- A full-screen terminal UI using mouse input, arrow-key navigation, or continuous screen redrawing.
- Chat, networking, user accounts, or multi-user collaboration.
- A graphical user interface.
- Automatic medical, psychological, or disability-based decisions.
- Diagnosis-driven or medically prescriptive schedule optimisation.
- Unbounded generation of timetable combinations.
- Guaranteed support for every disability group.
- Complete screen-reader compatibility as a core target for the current personas.

## 10. Open Decisions for Later Steps

The following have not yet been finalised:

- Product name.
- Exact itinerary-item fields.
- Mandatory versus optional fields.
- Exact scales used for energy demand and sensory load.
- Exact command syntax.
- Duplicate-item policy.
- Exact line-based storage schema, escaping rules, and malformed-record recovery.
- Treatment of skipped activities in progress calculations.
- Default progress period and chart scale.
- Terminal-width and chart-scaling rules.
- Maximum number of flexible activities handled in one recommendation request.
- Deterministic soft-preference priority order and tie-breaking rules.
- Exact guided-correction architecture and session behaviour.
- Exact architecture classes and interfaces.
- Detailed user stories and priorities.
- Measurable non-functional requirements.
- Use cases, extensions, and acceptance criteria.
- Final glossary.

## 11. Current Quality Principle

The project should not aim for the largest possible feature set. A smaller, cohesive, well-tested, well-documented product that clearly serves Sam and Jordan is the target.
