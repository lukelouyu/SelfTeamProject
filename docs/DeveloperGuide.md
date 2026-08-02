# UniEnable Developer Guide

## 1. Introduction

UniEnable is a single-user, offline Java 17 command-line application for planning activities and
consulting local accessibility reference data. This guide describes the implemented v1.0 system,
with emphasis on its activity-conflict refactor, recurrence workflow, persistence guarantees, and
read-only accessibility features, plus v2.0's first two shipped features: accessible route search
(`route`, Section 12) and the accessible planning dashboard (`dashboard`, Section 13).

The project began from the se-education.org Duke template. Its Gradle setup, Checkstyle rules, and
text-UI harness follow that ecosystem; the application design and domain behaviour described here
are UniEnable-specific. Production code uses only the JDK. JUnit 5 is used for tests.

## 2. Setting up and getting started

Prerequisites:

- JDK 17;
- Git;
- an IDE with Gradle support, or a terminal.

Clone the repository and import it as a Gradle project. Useful commands are:

```bash
./gradlew clean check
./gradlew javadoc
./gradlew run
./gradlew releaseZip
```

On Windows, use `gradlew.bat`. The release task creates
`build/distributions/unienable.zip`; the ZIP contains `unienable.jar` and
`data/academic-calendar.txt`. Run the extracted JAR from the directory that contains that `data`
folder.

## 3. Architecture

The entry point delegates to `ApplicationRunner`, which owns startup, loading, the command loop,
confirmation, saving, and shutdown. Parsing produces command objects; commands call managers;
managers operate on domain objects; storage translates between domain objects and local text files.

<p align="center"><img src="diagrams/architecture/ArchitectureDiagram.png" width="760" alt="UniEnable architecture diagram"></p>

The academic calendar is deliberately outside the main `Storage` facade. It is a supplied,
read-only recurrence resource loaded lazily by `RecurCommandParser`, whereas the facade coordinates
application-owned files and the accessibility reference dataset.

## 4. Main components

| Component | Responsibility |
|---|---|
| `UniEnable` | Public entry point; delegates immediately to `ApplicationRunner`. |
| `app` | Configures logging, loads state, runs the read-confirm-execute-save loop, restores failed mutations, and formats lifecycle errors. |
| `ui` | Frames console output and formats activities and recurrence previews; it contains no business rules. |
| `parser` | Routes a line through `CommandDispatcher`, validates syntax and values, and constructs a command. |
| `command` | Represents one user action through `execute()`; confirmable commands also provide a prompt or menu. |
| `logic` | Owns in-memory activity/topic state, read-only accessibility lookups, filters, conflict policy, and recurrence planning. |
| `model` | Defines activities, topics, ratings, enums, academic-calendar records, and recurrence plans. |
| `accessibility` | Defines immutable facility, feature, connection, and three-state accessibility records. |
| `storage` | Loads validated text records and atomically saves activities, topics, and settings. |
| `exception` | Provides user-facing checked exception categories such as invalid input, conflict, and storage error. |

`ActivityManager` owns the activity list, permanent-ID counter, saved ordering, CRUD operations,
queries, and atomic batch mutation. It delegates scheduling-conflict policy to
`ActivityConflictChecker`. `TopicManager` owns topics and maintains their relationship to
activities. `FacilityManager` and `ConnectionManager` expose immutable loaded records through
read-only lookup operations.

## 5. Activity model

Every activity has a permanent ID, description, category, date, ratings, optional topic and note,
and completion status. A `FixedActivity` has a start and end time. A `FlexibleActivity` has an
earliest start, latest end, and required duration.

<p align="center"><img src="diagrams/class/ActivityDomainClassDiagram.png" width="760" alt="Activity domain class diagram"></p>

Activities are mutable because edits, completion changes, and topic renames update existing user
state. Ratings are validated value objects. Constructor assertions protect the internal
end-after-start and duration-fits-window invariants; callers validate untrusted input before
construction, as described in Section 14.

## 6. Command and parser design

`CommandDispatcher.dispatch(input, now)` separates the first command word from its arguments and
routes to a domain parser. Parsers validate raw text and construct complete command objects;
commands never parse their own input. `now` is injected so date-sensitive parsing is deterministic
in tests.

Adding an activity illustrates the normal path. The parser obtains the next permanent ID,
validates fields, and constructs the correct subtype. The command asks `ActivityManager` to add it,
the manager delegates conflict policy, and `ApplicationRunner` saves before showing success.

<p align="center"><img src="diagrams/sequence/AddActivitySequence.png" width="760" alt="Add activity sequence diagram"></p>

Plain confirmations use `Confirmable`; the three-choice reset uses `MenuConfirmable`.
`CommandConfirmationHandler` owns input handling for both shapes, keeping confirmation mechanics
out of commands and the runner.

## 7. Conflict validation

`ActivityConflictChecker` is a package-private, final, stateless helper owned by
`ActivityManager`. Its single package-visible operation receives a candidate, a permanent ID to
exclude, and the list to inspect. The manager exposes a public facade for recurrence preflight and
retains ownership of all mutation and ID rules.

<p align="center"><img src="diagrams/class/ActivityConflictValidationClassDiagram.png" width="760" alt="Activity conflict validation class diagram"></p>

Conflict rules are applied in this order:

1. Check exact scheduling duplicates. Description comparison is exact and case-sensitive; the
   date and schedule type must match. Fixed activities additionally compare start and end;
   flexible activities compare earliest start, latest end, and duration. ID, category, topic,
   note, ratings, and completion are ignored.
2. If no duplicate exists and the candidate is fixed, find the first fixed activity on the same
   date that overlaps in list order. The half-open predicate is
   `existing.start < candidate.end && candidate.start < existing.end`.

Consequently, adjacent fixed activities are accepted, flexible-window overlaps are accepted, and
fixed/flexible pairs never overlap for this policy. Exact flexible duplicates are still rejected.
Completed activities participate exactly like incomplete ones. Duplicate failure takes precedence
over overlap failure.

Edit validation excludes every activity whose permanent ID equals the supplied exclusion ID. This
prevents an activity from conflicting with itself without relying on list position. For batches,
`ActivityManager.addAllAtomically` also validates that candidate IDs begin at `nextId` and increase
by one; permanent-ID validation therefore remains a manager responsibility.

## 8. Editing activities

`ActivityCommandParser.parseEdit` loads the current activity, merges supplied fields into a
complete replacement object, validates type-specific requirements and topic membership, and calls
`ActivityManager.checkNoConflicts(replacement, id)` before a confirmation is shown. A doomed edit
therefore does not ask the user to approve it.

After confirmation, `EditCommand.execute` calls `ActivityManager.replace`. `replace` finds the
target by permanent ID and repeats the same conflict check immediately before replacing the list
entry. This defensive execution-time validation protects against state changes between parsing and
execution.

<p align="center"><img src="diagrams/sequence/EditActivitySequence.png" width="760" alt="Edit activity sequence diagram"></p>

Because the replacement is complete and no field of the stored object is changed before the
manager accepts it, a validation failure leaves the previous activity unchanged. A later save
failure is handled by the broader rollback mechanism in Section 10.

## 9. Recurring activities

`recur TASK_ID week WEEK_SPEC` is implemented in v1.0 for eligible fixed academic class sessions.
The parser obtains the source activity, applies the shared class-schedule eligibility rule, parses
the week specification, loads one cached academic-calendar snapshot, and asks
`RecurrencePlanner` for a side-effect-free plan.

<p align="center"><img src="diagrams/class/RecurrenceClassDiagram.png" width="760" alt="Recurrence class diagram"></p>

The source must fall in an instructional week, and the requested weeks must include that source
week. For each requested week, the planner finds the matching weekday from the calendar's own date
range rather than adding seven-day offsets. It skips the source date, a no-class date, or the first
existing fixed occurrence with the same description, date, start, and end. Generated copies retain
the source metadata but start incomplete.

Each remaining candidate is preflighted through `ActivityManager.checkNoConflicts`. The immutable
`RecurrencePlan` records both planned and skipped occurrences so `RecurCommand` can present a full
preview without changing state.

<p align="center"><img src="diagrams/sequence/RecurrencePlanningSequence.png" width="760" alt="Recurrence planning sequence diagram"></p>

After confirmation, `RecurCommand.execute` extracts the planned fixed activities and calls
`ActivityManager.addAllAtomically`. The manager builds a temporary validated list. Candidate by
candidate, it checks the expected permanent ID and conflicts against existing activities plus all
earlier candidates in that batch. Only after every candidate passes does it append the complete
batch and advance `nextId`.

<p align="center"><img src="diagrams/sequence/RecurrenceExecutionSequence.png" width="760" alt="Recurrence execution sequence diagram"></p>

This final revalidation is deliberate: the preview is informative, but execution remains safe if
state changed after planning. Any failure leaves the real activity list and ID counter unchanged.

## 10. Atomic mutation and rollback

`ApplicationRunner` classifies these as state-changing commands: activity add, edit, delete, mark,
unmark, `order set`, recurrence, topic add/rename/delete, and `reset all` when reset would actually
change state. Before executing one, it captures deep copies of activities and topics plus the exact
`nextId` and saved order.

Success feedback is withheld until `Storage.saveAll` succeeds. If saving fails, the runner restores
the snapshot through `ActivityManager.restoreState` and `TopicManager.loadAll`; the attempted
command is therefore not left visible only in memory. The unsaved-change flag remains set so `bye`
retries and cannot falsely claim that data was saved.

<p align="center"><img src="diagrams/sequence/MutationRollbackSequence.png" width="760" alt="Mutation rollback sequence diagram"></p>

There are two complementary rollback layers:

- in-memory rollback in `ApplicationRunner` restores the pre-command object graph and counters;
- file rollback in `Storage` restores the prior persisted files if a staged multi-file commit
  fails partway through.

## 11. Storage

The `Storage` facade owns loaders for activities, topics, settings, facilities, and connections.
Application-owned planning state is pipe-delimited in `activities.txt`, `topics.txt`, and
`settings.txt`. `saveAll` stages all three temporary files, checks destinations, retains backups,
and commits them as one operation; a later failure triggers restoration of earlier file states.

<p align="center"><img src="diagrams/class/StorageClassDiagram.png" width="760" alt="Storage class diagram"></p>

`ActivityStorage` treats persisted lines as untrusted. It validates each accepted record against
earlier accepted records for duplicate IDs, exact scheduling duplicates, and fixed overlap, in
addition to field, timing, rating, status, and topic-reference checks. Invalid lines become
line-specific load warnings rather than entering the manager.

Storage deliberately does not depend on `ActivityConflictChecker`. The checker is a logic-layer
policy for proposed mutations, while storage is an independent trust boundary responsible for
rejecting corrupt persisted records before `ActivityManager.loadAll` receives them. This avoids a
storage-to-logic dependency and allows each boundary to report errors appropriate to its caller.

`FacilityStorage` and `ConnectionStorage` load read-only reference files. The separate
`AcademicCalendarStorage` strictly validates the supplied calendar only when recurrence first
needs it; UniEnable never creates, repairs, or writes that file.

## 12. Accessibility functionality

Facilities contain immutable feature records. Connections are immutable two-way reference links
with distance, traversal type, shelter status, accessibility status, and optional barrier/notes.
`YES`, `NO`, and `UNKNOWN` are separate states so missing knowledge is never presented as confirmed
accessibility.

<p align="center"><img src="diagrams/class/AccessibilityClassDiagram.png" width="700" alt="Accessibility class diagram"></p>

`FacilityManager` supports list, case-insensitive name lookup, and feature/status filtering.
`ConnectionManager` supports list and permanent-ID lookup; connection commands perform their own
documented filters. `facility validate` and `connection validate` rerun the same storage checks on
demand without replacing loaded records or modifying either file.

### Accessible route search (`route`)

`route from/FACILITY to/FACILITY` finds the shortest confirmed-accessible path between two known
facilities, using each connection's `distanceInMetres` as the Dijkstra weight and only connections
with `accessibility == YES` as eligible edges.

<p align="center"><img src="diagrams/class/RouteClassDiagram.png" width="700" alt="Route class diagram"></p>

**Why the `YES`-only filter lives in `logic.route.AccessibleRouteGraphFactory`, not in
`logic.graph.AccessibilityGraph`.** `AccessibilityGraph` was built during v1.0 hardening as
generic, policy-free Dijkstra-prep infrastructure over *any* facility/connection dataset - its own
class Javadoc says so, and its existing test suite builds graphs from datasets that mix accessible
and non-existent connections without any status opinion. Folding "only `YES` connections are
usable" into it would collapse a reusable graph algorithm and one command's business rule into a
single class, and would silently narrow what any future caller of `AccessibilityGraph` could do
with it. `AccessibleRouteGraphFactory.build(FacilityManager, ConnectionManager)` instead filters
`connectionManager.list()` down to `AccessibilityStatus.YES` connections and builds the existing
graph over exactly that filtered list - route policy and graph algorithm stay in separate classes,
and `AccessibilityGraph` itself gained only a policy-neutral `(List<Facility>, List<Connection>)`
constructor overload (the manager-based constructor now delegates to it) so the factory never
needs to manufacture a throwaway `ConnectionManager` purely to hold a filtered list. Every existing
`AccessibilityGraphTest` case is unaffected, since the manager-based constructor's own behaviour is
unchanged.

Because `FacilityStorage`/`ConnectionStorage` already reject non-positive/duplicate IDs, malformed
lines, and unknown-facility endpoints at load time (Section 11), and `ApplicationRunner` only ever
builds `FacilityManager`/`ConnectionManager` from an already-validated `LoadResult`, the route
graph factory performs no second validation pass of its own - by the time any command runs, the
managers can only contain trusted, self-consistent records. `route`'s source and destination
naming the same known facility is a successful zero-length result (single-facility chain, `0 m`),
not an error, since `AccessibilityGraph.getShortestPath` already returns that result correctly for
matching endpoints. Two known facilities with no confirmed-accessible path between them get a
"No supported accessible route was found..." message rather than an exception, worded to state
only that UniEnable's local dataset has no confirmed path - never that no real-world accessible
route exists. Only an unrecognised facility name raises `InvalidIndexException`.

`RouteCommand` resolves each consecutive pair in the returned path's facility chain against the
same `YES`-only connection list the graph was built from, to recover each segment's own distance,
traversal type, shelter status, and optional barrier/notes for display - segment display direction
always follows the path's own travel direction, not a connection's stored (and irrelevant, since
every connection is two-way) `from`/`to` order. `ui.accessibility.RouteFormatter` is pure text
formatting with no routing decisions of its own. `route` never estimates travel time and never
claims real-time verification or a guarantee of real-world accessibility.

<p align="center"><img src="diagrams/sequence/RouteSequence.png" width="760" alt="Route sequence diagram"></p>

## 13. Accessible planning dashboard

`dashboard today|tomorrow|date/YYYY-MM-DD|this week [detail]` calculates a read-only planning-load
summary directly from `ActivityManager`'s current in-memory state every time it runs - it is a
**derived view, not a stored entity**: there is no dashboard persistence file, no dashboard model
saved to disk, and nothing about it survives between commands except what `ActivityManager` itself
already owns. This keeps it consistent with the rest of the read-only accessibility commands
(`facility`/`connection`/`route`) and avoids a second source of truth that could drift from the
activities it summarises.

<p align="center"><img src="diagrams/class/DashboardClassDiagram.png" width="760" alt="Dashboard class diagram"></p>

Responsibilities are split the same way `route` splits parsing/policy/formatting:

- `parser.dashboard.DashboardCommandParser` - syntax only. Validates the grammar, resolves the
  selected period via `DashboardService`'s `resolve*` methods (using the `now` it was given, per
  the existing `list`/`next`/`edit` convention - `dashboard` never calls
  `LocalDate.now()`/`LocalDateTime.now()` directly in production code or tests), and captures the
  `detail` flag.
- `command.dashboard.DashboardCommand` - orchestration only: calls `DashboardService.summarize`,
  then `DashboardFormatter.format`. Implements neither `Confirmable` nor `MenuConfirmable`, and is
  not in `ApplicationRunner.mutatesState`'s recognised set, so it never triggers a snapshot or a
  save - a `dashboard` command is architecturally incapable of persisting anything.
- `logic.dashboard.DashboardService` - stateless (all-static, mirroring
  `logic.route.AccessibleRouteGraphFactory`); owns period resolution and every metric calculation.
- `model.dashboard.DashboardPeriod`/`RatingSummary`/`DashboardSummary` - immutable calculated
  results. `RatingSummary` is reused identically for energy and sensory, since the two are
  structurally identical metrics (total, high-rating count, average, highest, 1-5 distribution)
  over different ratings, rather than duplicating six fields twice.
- `ui.dashboard.DashboardFormatter` - pure text formatting; makes no calculation of its own.

<p align="center"><img src="diagrams/sequence/DashboardSequence.png" width="760" alt="Dashboard sequence diagram"></p>

**Period boundaries** are always half-open (`[start, end)`), computed with `java.time` so
month/year transitions and leap days are handled for free. `this week` reuses `list this week`'s
exact Monday-Sunday boundary (`TemporalAdjusters.previousOrSame(MONDAY)`/`nextOrSame(SUNDAY)`),
not a rolling seven-day window - two now-superseded planning drafts disagreed with each other and
with `list`'s own shipped behaviour on this point; `list`'s precedent is authoritative.

**Activity inclusion** uses the same half-open intersection test for both activity types: a fixed
activity's `[start, end)` or a flexible activity's `[earliestStart, latestEnd)` must intersect the
period. **Planned-workload contribution** differs by type: a fixed activity's contribution is
*clipped* to the period (`clip(activityStart, activityEnd, periodStart, periodEnd)`, a
package-private `DashboardService` helper using `min`/`max` on the boundaries); a flexible
activity's contribution is its full requested `durationMinutes`, counted once, never clipped to
however much of its window overlaps the period - stated and tested as a limitation, not silently
invented data.

**Cross-midnight clipping - generic by construction, not reachable by real data today.** Both
`FixedActivity` and `FlexibleActivity` constrain start/end to one calendar date
(constructor-enforced assertions), so no activity in this codebase can actually span midnight.
`clip(...)` is nonetheless implemented generically over raw `LocalDateTime` boundaries with no
same-day assumption - exactly as correct for a hypothetical midnight-spanning interval as for an
ordinary one. `DashboardServiceTest` exercises this directly with synthetic
`LocalDateTime` pairs (a simulated Monday 23:00 -> Tuesday 01:00 interval clipped against a
Monday-only, a Tuesday-only, and a Monday-Sunday-week period, asserting 60/60/120-minute
contributions), rather than constructing an activity the model's own invariants would reject. No
cross-date activity support was added to `FixedActivity`/`FlexibleActivity`, their parsers,
storage format, or conflict-checking - that would be a v1.0 data-model change, outside this
feature's scope. See `docs/tasks/v2/dashboard/IMPLEMENTATION_NOTES.md` for the full account.

**Nominal capacity** is derived, not hardcoded per period "kind":
`Duration.between(period.start, period.end).toMinutes()` gives 1440 for any single day and 10080
for the week automatically. **Buffer/overload**:
`nominalBuffer = max(0, capacity - workload)`, `overload = max(0, workload - capacity)` - never
both non-zero. The metric is always labelled "Nominal buffer", documented everywhere as arithmetic
capacity minus planned workload, not a promise of actually-usable free time (overlapping fixed
activities count individually toward workload rather than merging, so a double-booked day reduces
nominal buffer accordingly without silently hiding the conflict).

**Energy/sensory**: totals are a plain sum of included activities' ratings; a rating counts as
"high" at a single named threshold, `DashboardService.HIGH_RATING_THRESHOLD = 4` (ratings 4-5).
Detail-mode averages use `BigDecimal`/`RoundingMode.HALF_UP` to one decimal place rather than raw
`double` formatting, so the documented rounding rule can't silently vary by platform or JDK
version; an empty rated set reports itself as having no data (`RatingSummary.hasData() == false`)
rather than a misleading `0`.

**Completion denominator**: an activity counts toward completion only once its own time has fully
passed - `!endTime.isAfter(now)` for fixed, `!latestEnd.isAfter(now)` for flexible - using the same
`now` the period itself was resolved against. A future or currently-in-progress activity is
excluded from both the completed and incomplete counts entirely, not folded into "incomplete".
Completion percentage is `Math.round(completed * 100.0 / eligible)`, computed once in
`DashboardService.summarize` and stored on `DashboardSummary`, matching every documented rounding
example (`2/3 -> 67%`) exactly.

**Deterministic ordering**: category-grouped counts iterate `ActivityCategory.values()` (fixed
enum declaration order), looking up each category's count from a `Map` rather than iterating the
map itself, so the result is independent of the map implementation's own iteration order. Rating
distributions are `int[5]` indexed by `rating - 1`, not a `Map<Integer,Integer>` - ascending order
is structural.

**Design alternatives considered:** a separate `DashboardDetail` model type (rejected - detail
metrics are cheap to always compute, so gating by a boolean at *format* time avoids a second
model's own emptiness story); six separate energy/sensory fields inline on `DashboardSummary`
(rejected in favour of the shared `RatingSummary`); exposing `clip(...)` as public API purely for
testability (rejected - it stays package-private, tested from a same-package test class, matching
`ActivityConflictChecker`'s precedent); raw `double` averages with `String.format` (rejected for
`BigDecimal`/`HALF_UP`, for exact, JVM-independent rounding).

Test strategy mirrors `route`'s: `DashboardServiceTest` (period resolution, the clipping
calculation directly, inclusion, workload, buffer/overload, ratings, completion eligibility and
percentage, detail-mode ordering - all synthetic fixtures, injected fixed clocks, no
`LocalDateTime.now()`), `DashboardCommandParserTest` (full accept/reject grammar),
`DashboardCommandTest` and `DashboardFormatterTest` (orchestration and exact-text formatting
respectively), and `DashboardIntegrationTest` (reads through the real `Storage`/`ActivityManager`
path, restart consistency, a malformed persisted line skipped safely, and a full
`ApplicationRunner` session proving no data file changes across several `dashboard` commands).
`text-ui-test` covers only `dashboard date/...` scenarios (deterministic regardless of wall-clock
date, using a far-future date so completion resolves to "not yet due" deterministically) -
`dashboard today`/`tomorrow`/`this week` are excluded from `text-ui-test` for the same reason
`list today`/`tomorrow`/`this week` already are (the harness runs the real jar against the real
wall clock, with no fixed-clock injection point), covered instead by `DashboardServiceTest`'s
injected-`now` tests.

## 14. Logging and assertions

`LoggingConfig` removes default console handlers and attaches one append-mode `FileHandler` at
`data/unienable.log`. It records `INFO` and above with `SimpleFormatter`; operational logs do not
pollute the text UI. If file logging cannot be initialised, startup continues with application
logging disabled. Shutdown detaches and closes the handler.

Activity and topic mutations log at `INFO`. The runner logs startup/load warnings and save failures
at `WARNING`, and unexpected runtime failures at `SEVERE`. A storage rollback failure is also
`SEVERE` because the persisted state may need manual inspection.

Java assertions protect programmer-only invariants:

- `ApplicationRunner.processCommand` requires its collaborators to have been initialised;
- a fixed activity's end must be after its start;
- a flexible activity's latest end must be after its earliest start and its duration must fit.

Assertions are not used for user or persisted input. Parsers and storage validate those inputs and
raise checked domain exceptions or load warnings, so behaviour does not depend on whether `-ea` is
enabled.

## 15. Design considerations

### Extracting conflict validation

**Alternative 1: keep conflict validation in `ActivityManager`.** This keeps fewer classes and
avoids one delegation, but combines collection ownership, IDs, queries, sorting, mutation, and
scheduling policy in a growing manager. Focused policy tests must then construct the broader
manager state.

**Alternative 2: extract `ActivityConflictChecker`.** This introduces a small collaborator while
keeping it package-private and stateless. The checker has one cohesive responsibility, and the
manager remains the only public mutation boundary.

**Chosen: Alternative 2.** Extraction improves single-responsibility separation and cohesion,
reduces the manager's policy burden, and enables focused checker tests. Coupling stays narrow
because only `ActivityManager` owns the checker; parsers and recurrence still call the manager
facade. Existing duplicate precedence, overlap selection, messages, and manager behaviour are
preserved.

### Intentional predicate duplication

Similar scheduling predicates exist at three boundaries: logic checks general mutations,
recurrence identifies an already-generated occurrence before general preflight, and storage
validates untrusted records independently. This creates a maintenance risk: a future rule change
may require coordinated updates and boundary-specific tests. Consolidation was intentionally
outside this refactor because recurrence's identical-occurrence skip has different semantics and
storage must not depend on logic. A later change should first define one boundary-neutral policy
contract and preserve each caller's distinct error and recovery behaviour.

### Other decisions

- Academic weeks are resolved from explicit calendar ranges so recess and other gaps do not break
  recurrence dates.
- Accessibility records remain separate from activities; activities do not store route state.
- `reset all` and recurrence use the same class-schedule eligibility policy.
- `parser.common.ArgumentTokenizer`/`ArgumentMarker` remain prepared but unused by any shipped
  command as of this guide. `logic.graph.AccessibilityGraph`, the other preparatory utility from
  the same hardening session, is no longer unused - v2.0's `route` is its first real caller, via
  `logic.route.AccessibleRouteGraphFactory` (Section 12).

## 16. Testing

The automated strategy has four layers:

- unit tests for parsers, models, managers, the conflict checker, recurrence, and storage;
- integration tests for `ApplicationRunner`, confirmations, persistence, and rollback;
- the Gradle `check` lifecycle for JUnit and Checkstyle;
- the Git Bash text-UI regression for end-to-end command/output compatibility.

Conflict tests should cover both activity types, ignored metadata, completed activities,
duplicate-before-overlap precedence, adjacency, permanent-ID exclusion, and first-overlap order.
Recurrence tests should cover source-week inclusion, no-class and identical skips, planning
preflight, final batch revalidation, candidate-to-candidate validation, IDs, and no partial
mutation. Storage tests should independently exercise malformed records and three-file commit
rollback. Runner tests should force save failures for each category of mutation. Route tests
should cover Dijkstra correctness (direct vs. multi-edge, distance-optimal vs. hop-optimal),
`YES`-only edge exclusion, disconnected pairs, the same-facility zero-length case, unknown
facilities, and malformed/duplicate/negative-distance reference data - all against small
synthetic fixtures (`docs/tasks/v2/route/TEST_PLAN.md`), never the real bundled dataset, so
algorithmic edge cases stay independent of what the shipped `facilities.txt`/`connections.txt`
happen to contain. Dashboard tests should cover period-boundary math (day/week, month/year/leap
transitions), interval inclusion and clipping (including the cross-midnight calculation via
synthetic `LocalDateTime` boundaries - see Section 13), workload/buffer/overload arithmetic,
completion eligibility and percentage rounding under an injected clock, and detail-mode
deterministic ordering - all in `docs/tasks/v2/dashboard/TEST_PLAN.md`, with `text-ui-test`
restricted to `dashboard date/...` scenarios for the same real-wall-clock-determinism reason
`list today`/`tomorrow`/`this week` are.

## 17. Product scope

UniEnable serves tertiary students with ASD or ADHD and tertiary students who use wheelchairs as
they prepare for unfamiliar university, internship, or entry-level work routines. It prioritises
predictable keyboard interaction, concise output, local data ownership, energy/sensory planning,
and explicit uncertainty in accessibility information.

Implemented v1.0 scope includes activity and topic management, completion and ordering, next-item
selection, academic-calendar recurrence, reset choices, read-only facility/connection lookup, and
reference-file validation. v2.0 has so far added accessible route search (`route`, Section 12) and
the accessible planning dashboard (`dashboard`, Section 13). CSV export, a timetable, recommendation
preferences, and a schedule recommender remain future work.

## 18. User stories

| Priority | As a ... | I want to ... | So that ... |
|---|---|---|---|
| Must | student managing an unfamiliar routine | record fixed and flexible activities | I can plan confirmed and movable work. |
| Must | student sensitive to workload and environment | record energy and sensory ratings | I can anticipate an activity's demands. |
| Must | student | see one next relevant activity | I need not scan a dense itinerary. |
| Must | wheelchair user | inspect local facility and connection information | I can prepare for known barriers. |
| Should | student with weekly classes | generate semester occurrences from a supplied calendar | I avoid repetitive entry while retaining independent activities. |
| Should | student starting a new period | reset all data or retain class schedules | I can clear obsolete planning state safely. |
| Should | wheelchair user | find the shortest confirmed-accessible route between two facilities | I do not have to guess whether a shorter-looking path is actually usable. |
| Should | student sensitive to workload and environment | see a summary of how full a day or week is | I can gauge my planning load without manually adding it up myself. |

## 19. Use cases

### UC01: Add an activity

**Main success scenario:** The user enters a valid `add` command. UniEnable validates the fields,
checks conflicts, adds the activity, saves all planning state, and reports the new permanent ID.

**Extensions:** Invalid fields or conflicts are reported without mutation. If saving fails, the
addition is restored to the prior state and no success message is shown.

### UC02: Edit an activity

**Main success scenario:** The user supplies one or more changes. UniEnable builds and preflights a
replacement, shows the actual differences, receives `y`, revalidates, replaces, saves, and reports
success.

**Extensions:** No effective change ends without confirmation. Invalid input or a conflict ends
before confirmation. `n` cancels. A save failure restores the original activity.

### UC03: Create recurring class sessions

**Main success scenario:** The user selects an eligible source and weeks including its source
week. UniEnable plans dates, shows planned/skipped occurrences, receives `y`, atomically inserts the
batch, saves, and reports success.

**Extensions:** Ineligible sources, calendar errors, missing source week, or conflicts reject the
request without mutation. No new occurrences skips confirmation. `n` cancels. Final revalidation or
saving failure leaves no partial batch.

### UC04: Consult accessibility data

The user lists, views, finds, or validates facilities/connections. UniEnable returns local
reference data plus its disclaimer and never modifies the dataset.

### UC05: Find an accessible route

**Main success scenario:** The user supplies two known facility names. UniEnable computes the
shortest path using only confirmed-accessible connections and reports the ordered chain, each
segment's detail, and the total distance.

**Extensions:** The same facility for both ends succeeds with a zero-length result. An
unrecognised facility name is rejected before any pathing occurs. Two known facilities with no
confirmed-accessible path between them get an explicit no-route message rather than a suggested,
unconfirmed route.

### UC06: View a planning dashboard

**Main success scenario:** The user selects a period (today, tomorrow, a date, or this week).
UniEnable calculates planned workload, nominal buffer, energy/sensory totals and high-rating
counts, and completion, from currently stored activities only, and displays the summary.

**Extensions:** An empty period shows a distinct "no activities found" message. A period with
activities but none yet due shows "no activities are due yet" instead of a percentage.
`detail` additionally shows fixed/flexible counts, a category breakdown, and full rating
distributions. Every parser rejection (missing/invalid selector, malformed date, duplicate
`detail`, unexpected trailing text) leaves all activity data unchanged - `dashboard` never
mutates, saves, or prompts for confirmation.

## 20. Non-functional requirements

- The application must run on Java 17 and use primarily object-oriented design.
- It must operate offline as a single-user CLI without a DBMS or private remote service.
- Behaviour must be portable across Windows, Linux, and macOS terminals supported by Java 17.
- User-owned data must remain in local, human-readable text files.
- A release must be a ZIP containing the executable JAR and required external resources,
  including `data/academic-calendar.txt`.
- Normal commands should respond promptly for the intended personal dataset size.
- User errors must be specific and must not terminate the command loop.
- Unknown accessibility status must remain distinguishable from confirmed inaccessibility.

## 21. Glossary

- **Activity:** A fixed or flexible user planning item with a permanent ID.
- **Exact scheduling duplicate:** Same exact description, date, type, and that type's timing fields.
- **Half-open interval:** An interval whose end does not overlap another interval beginning at the
  same instant; used for fixed activity conflicts.
- **Topic:** An optional user-created grouping within one fixed category.
- **Class schedule:** An eligible fixed academic lecture, tutorial, lab, or section-teaching
  activity used by recurrence and the reset keep option.
- **Occurrence:** One independent fixed activity planned from a recurrence source.
- **Academic calendar:** The supplied read-only file mapping teaching weeks and no-class dates.
- **Snapshot:** A deep in-memory copy used to restore state after a failed save.
- **Reference data:** Facility, connection, and calendar information that commands do not mutate.
- **Confirmed-accessible connection:** A connection whose `accessibility` field is `YES`; `route`
  uses only these as graph edges, never `NO` or `UNKNOWN`.
- **Nominal buffer:** `dashboard`'s arithmetic capacity-minus-workload metric; not a guarantee of
  actually-usable free time.
- **Completion-eligible:** An activity whose own scheduled time has fully passed as of the
  injected `now`; only eligible activities count toward `dashboard`'s completion percentage.

## 22. Instructions for manual testing

Build and extract the release ZIP, then run `java -jar unienable.jar` from its extracted root.
Start with a separate test directory if existing personal data must be preserved.

1. Add one fixed and one flexible activity; verify `list`, `view`, `find`, `order`, `mark`,
   `unmark`, `edit`, and `delete` follow the User Guide.
2. Add adjacent fixed activities and confirm acceptance. Try an overlapping fixed activity and an
   exact duplicate and confirm rejection. Try overlapping flexible windows with different timing
   details and confirm acceptance; try an exact flexible duplicate and confirm rejection. Repeat
   conflict checks against a completed activity.
3. Edit scheduling fields and inspect the before/after confirmation. Submit an invalid or
   conflicting edit and confirm the prompt is not shown and the activity is unchanged.
4. Create, list, rename, and delete topics. Verify a rename updates linked activities and an
   in-use topic cannot be deleted.
5. Use an eligible fixed academic class dated inside `academic-calendar.txt`. Run `recur` with a
   week specification that includes the source week, cancel once, then confirm. Verify preview
   dates, skipped source/no-class/identical occurrences, consecutive IDs, and independent editing
   of generated activities. Confirm an omitted source week is rejected.
6. Exercise all three `reset all` choices and confirm reference files are unchanged.
7. Run every facility and connection list/view/find/validate command and verify the disclaimer.
   Introduce a malformed reference line while the app is closed, restart, and confirm a warning;
   restore the file afterward.
8. Run `route` between two facilities with a known confirmed-accessible path and verify the
   segment detail and total distance. Run it with the same facility twice and confirm a
   zero-length success, not an error. Run it with an unrecognised facility name and confirm the
   error. `route` never mutates state, so no restart check is needed for it.
9. Add a few fixed and flexible activities on today's date and run `dashboard today` and
   `dashboard today detail`. Verify planned workload, nominal buffer, energy/sensory totals and
   high-rating counts, and the detail section's category/rating breakdown. Run `dashboard` on a
   date with no activities and confirm "No activities found for the selected period." Run
   `dashboard this week` and confirm the range matches `list this week`'s own Monday-Sunday week.
   `dashboard` never mutates state, so no restart check is needed for it.
10. Force a save failure in a disposable copy by making a planning-state destination unwritable.
    Confirm the error is shown, the attempted mutation is absent from subsequent views, and `bye`
    does not falsely claim it was saved.
11. Restart after successful mutations and confirm activities, topics, completion, and default
    order were persisted together.

Before a release, also run:

```bash
./gradlew clean check --console=plain
./gradlew javadoc --console=plain
./gradlew releaseZip --console=plain
bash text-ui-test/runtest.sh
```
