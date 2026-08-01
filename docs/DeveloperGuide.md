# Developer Guide

## Acknowledgements

- Built from the [se-education.org](https://se-education.org) `Duke` project template (Gradle
  build setup, Checkstyle configuration, text-UI-test harness, and this Developer/User Guide
  skeleton).
- Architecture follows the AB3 (`se-edu/addressbook-level3`) convention of separating UI, logic,
  model, and storage responsibilities, adapted for a CLI-only, single-user application.
- The bundled `facilities.txt`/`connections.txt` sample dataset is digitised from a real NUS FASS
  accessible-route campus map (source PDF kept outside this repo).
- No other third-party libraries are used; the application depends only on the JDK 17 standard
  library and JUnit 5 for testing.

## Design & implementation

### Architecture

UniEnable is a single-user CLI application: it reads one line of input at a time, turns it into a
`Command` object, executes it, and prints the result, saving activity/topic data after every
mutating command.

```mermaid
flowchart TB
    Entry["UniEnable<br/>(main/run entry point)"]
    Runner["app.ApplicationRunner<br/>(startup, run loop, persistence)"]
    Confirm["app.CommandConfirmationHandler<br/>(y/n confirmation)"]
    UI["ui.Ui<br/>(console I/O, framing)"]
    Dispatcher["parser.CommandDispatcher"]
    Parsers["parser.activity / .topic / .accessibility / .common<br/>(one parser class per domain)"]
    Commands["command.activity / .topic / .accessibility / .general / .recur<br/>(one Command class per user action)"]
    Logic["logic.ActivityManager / TopicManager / FacilityManager / ConnectionManager<br/>logic.recur.RecurrencePlanner / ClassSchedulePolicy"]
    Model["model.classes / model.enums / model.recur<br/>(Activity, Topic, EnergyRating, AcademicCalendar, ...)"]
    Accessibility["accessibility.classes / .enums<br/>(Facility, Connection - read-only reference data)"]
    Storage["storage.*Storage / storage.recur.AcademicCalendarStorage<br/>(pipe-delimited text-file persistence)"]

    Entry --> Runner
    Runner --> UI
    Runner --> Confirm
    Confirm --> UI
    Runner --> Dispatcher
    Dispatcher --> Parsers
    Parsers --> Commands
    Commands --> Logic
    Logic --> Model
    Logic --> Accessibility
    Runner --> Storage
    Storage --> Model
    Storage --> Accessibility
```

Each box above is a package-level component:

| Component | Responsibility |
|---|---|
| `UniEnable` | The application's sole public entry point (`main`, and the `run(Path, InputStream)` test seam that end-to-end tests call directly). Delegates immediately to `app.ApplicationRunner`; holds no other logic, since Gradle and the Shadow JAR need this exact class name as the main class. |
| `app` | `ApplicationRunner` coordinates one full run: configuring startup (suppressing JDK logging, showing the welcome message), loading and populating stored data, running the read-execute-print command loop, and persisting activities/topics/settings after every executed command. `CommandConfirmationHandler` owns the confirmation step for any command that needs one, including EOF-as-cancel handling - a plain y/n via `Confirmable`, or a numbered menu with more than one outcome via `MenuConfirmable` (see Design considerations). Both hold their dependencies (UI, scanner, storage, managers, dispatcher) as fields rather than passing them through parameter lists. |
| `ui` | All console output framing (`Ui`) and activity-to-text formatting (`MessageFormatter`). `Ui` also formats the partial-load-warning block (`showLoadWarnings`); `ApplicationRunner` decides *when* to call it, but not what the warning text looks like. `ui.recur.RecurrenceFormatter` formats `recur`'s preview/no-op/success text, kept separate from `MessageFormatter` since it formats a *plan* (source, calendar week, to-create/skipped lists), not a single activity. No parsing or business logic. |
| `parser` | Turns one command line into a `Command` object. `CommandDispatcher` routes by command word to a domain-specific parser (`ActivityCommandParser`, `TopicCommandParser`, `FacilityCommandParser`, `ConnectionCommandParser`, `parser.recur.RecurCommandParser`), which all share small utilities in `parser.common` (`FieldParser`, `DateTimeParser`, `RatingParser`, and `ArgumentTokenizer`/`ArgumentMarker` - see below). `parser.recur.WeekSpecificationParser` is `RecurCommandParser`'s own private-to-the-package helper for the `WEEK_SPEC` grammar (`1 to 6; 7 to 13`) - not shared with any other command, since no other v1.0/v2.0 command has this grammar shape yet. |
| `command` | One class per user action (`AddCommand`, `EditCommand`, `FacilityFindCommand`, `command.recur.RecurCommand`, ...), each holding just the data it needs and an `execute()` method. Commands never parse raw text themselves. A command that needs a plain y/n confirmation implements `Confirmable`; a command whose confirmation is a numbered menu with more than one meaningful outcome (currently only `ResetCommand`) implements `MenuConfirmable` instead (see Design considerations). |
| `logic` | In-memory managers: `ActivityManager` (CRUD, duplicate/overlap validation, sorting, "next relevant activity", plus the atomic-batch and rollback support `recur`/`reset` need - see Design considerations), `TopicManager` (topic CRUD scoped per category, cascading rename/delete-guard, plus `retainTopicsUsedBy` for reset's "keep class schedules" option), `FacilityManager`/`ConnectionManager` (read-only lookups over the loaded accessibility dataset), plus `ActivityFilter` (a small value object bundling list/find's filter criteria). `logic.graph` (see Design considerations) is a preparatory addition, not used by any v1.0 command. `logic.recur.RecurrencePlanner` builds a complete, side-effect-free `RecurrencePlan` from calendar reference data before any activity is touched; `logic.recur.ClassSchedulePolicy` is the single shared eligibility rule `recur` and reset's "keep class schedules" option both call, so the two features can never silently disagree about what counts as a class session. |
| `model` | Mutable domain objects for user data: `Activity` (abstract base), `FixedActivity`/`FlexibleActivity`, `Topic`, `EnergyRating`/`SensoryRating` (validated 1-5 value objects), and enums (`ActivityCategory`, `ActivityOrder`, `CompletionStatus`, `ScheduleType`). `model.recur` holds the recurrence feature's own domain objects - `AcademicCalendar`/`AcademicWeek`/`NoClassDate` (an immutable snapshot of the external calendar file) and `RecurrencePlan` (an immutable, side-effect-free planning result, with nested `PlannedOccurrence`/`SkippedOccurrence`) - immutable throughout, unlike the mutable `Activity` hierarchy, since all of it is either read-only reference data or a disposable one-shot planning result. |
| `accessibility` | Immutable domain objects for the read-only reference dataset: `Facility`, `FacilityFeature`, `Connection`, and their enums (`AccessibilityStatus`, `ShelterStatus`, `TraversalType`). Immutable because, unlike activities, this data is never edited in-app. |
| `storage` | Loads/saves the pipe-delimited text files: `ActivityStorage`, `TopicStorage`, `SettingsStorage` (read-write), `FacilityStorage`, `ConnectionStorage` (read-only, from `data/facilities.txt`/`data/connections.txt`), all wrapped by the top-level `Storage` facade. `LoadResult<T>` pairs successfully loaded records with per-line warnings for malformed ones; `SettingsStorage` falls back to the documented default order with a warning rather than failing to start. `storage.recur.AcademicCalendarStorage` is a separate, strictly-validating, read-only loader for `data/academic-calendar.txt` - never wrapped by the `Storage` facade's own read-write model, since this file is never created, repaired, or written by the application (see Design considerations). |
| `exception` | A flat hierarchy under `UniEnableException`, each subtype naming a category shown in `[Error] <category>: <message>` (e.g. `MissingInputException` -> "Missing input", `InvalidActivityException` -> "Invalid input"). Kept flat rather than per-domain, since the categories are about the *kind* of problem, not which feature raised it. Reused as-is by `recur` - no new exception types were introduced for the feature. |

### A representative flow: editing an activity

`edit` is a good example because it touches every layer and exercises the confirmation step.
Editing activity `1`'s duration (`edit 1 dur/60`) proceeds as follows:

```mermaid
sequenceDiagram
    participant User
    participant Runner as ApplicationRunner
    participant Confirm as CommandConfirmationHandler
    participant Dispatcher as CommandDispatcher
    participant Parser as ActivityCommandParser
    participant Manager as ActivityManager
    participant Cmd as EditCommand
    participant Storage

    User->>Runner: "edit 1 dur/60"
    Runner->>Dispatcher: dispatch(input, now)
    Dispatcher->>Parser: parseEdit(activityManager, topicManager, "1 dur/60")
    Parser->>Manager: getById(1)
    Manager-->>Parser: old Activity
    Note over Parser: builds a complete new Activity,<br/>validating every changed field first
    Parser-->>Dispatcher: EditCommand(1, newActivity)
    Dispatcher-->>Runner: EditCommand
    Runner->>Confirm: confirmIfNeeded(command)
    Confirm->>Cmd: getConfirmation()
    Cmd->>Manager: getById(1)
    Manager-->>Cmd: old Activity
    Note over Cmd: builds diff via MessageFormatter;<br/>returns Confirmation.ask(diff + prompt)
    Cmd-->>Confirm: Confirmation
    Confirm->>User: show diff, "Save changes? (y/n)"
    User->>Confirm: "y"
    Confirm-->>Runner: true
    Runner->>Cmd: execute()
    Cmd->>Manager: replace(1, newActivity)
    Manager-->>Cmd: (validated, replaced)
    Cmd-->>Runner: CommandResult
    Runner->>Storage: saveActivities(activityManager.getAll())
    Runner->>User: framed feedback
```

The key design point: the parser builds a *complete, fully-validated* replacement `Activity`
before `EditCommand` ever touches the manager. If any supplied field fails validation (e.g. an
out-of-range energy rating), the parser throws before `EditCommand` is even constructed, so the
stored activity is never touched — this is what makes edit (and add, delete, topic rename/delete)
atomic: a rejected request always leaves prior state completely unchanged.

### A second flow: creating recurring class sessions

`recur` is the other representative flow: it shows the menu-style confirmation shape, a
multi-file external read, and a batch mutation instead of a single-activity one. Running
`recur 1 week 1 to 6; 7 to 13` against a fixed `CG3207 Lecture` proceeds as follows:

```mermaid
sequenceDiagram
    participant User
    participant Runner as ApplicationRunner
    participant Confirm as CommandConfirmationHandler
    participant Dispatcher as CommandDispatcher
    participant Parser as RecurCommandParser
    participant CalStorage as AcademicCalendarStorage
    participant Planner as RecurrencePlanner
    participant Manager as ActivityManager
    participant Cmd as RecurCommand
    participant Storage

    User->>Runner: "recur 1 week 1 to 6; 7 to 13"
    Runner->>Dispatcher: dispatch(input, now)
    Dispatcher->>Parser: parse(activityManager, "1 week 1 to 6; 7 to 13")
    Parser->>Manager: getById(1)
    Manager-->>Parser: source FixedActivity
    Note over Parser: checks ClassSchedulePolicy.isClassSchedule(source)
    Parser->>Parser: WeekSpecificationParser.parse("1 to 6; 7 to 13")
    Parser->>CalStorage: load(academicCalendarFile) [first recur call only; cached after]
    CalStorage-->>Parser: AcademicCalendar
    Parser->>Planner: plan(source, weeks, calendar, activityManager)
    Note over Planner: side-effect-free: resolves each target week's<br/>matching weekday, skips source/existing/no-class,<br/>preflights conflicts via activityManager.checkNoConflicts()
    Planner-->>Parser: RecurrencePlan
    Parser-->>Dispatcher: RecurCommand(activityManager, plan)
    Dispatcher-->>Runner: RecurCommand
    Runner->>Confirm: confirmIfNeeded(command)
    Confirm->>Cmd: getConfirmation()
    Cmd-->>Confirm: Confirmation.ask(preview) via RecurrenceFormatter
    Confirm->>User: show preview, "Continue? (y/n)"
    User->>Confirm: "y"
    Confirm-->>Runner: true
    Runner->>Cmd: execute()
    Cmd->>Manager: addAllAtomically(plannedActivities)
    Note over Manager: validates every candidate's ID and<br/>conflicts against existing + same-batch<br/>activities before mutating anything
    Manager-->>Cmd: (validated, all added)
    Cmd-->>Runner: CommandResult
    Runner->>Storage: saveAll(activities, topics, order)
    Runner->>User: framed success feedback
```

Two points this flow highlights that the edit flow above doesn't:

- **Planning is fully separated from mutation.** `RecurrencePlanner.plan()` never calls
  `ActivityManager.add`/`addAllAtomically` — it only *reads* the manager (via
  `checkNoConflicts`/`getAll`/`getNextId`) to preflight every occurrence, then returns an immutable
  `RecurrencePlan`. Nothing is added until `RecurCommand.execute()` runs, after confirmation.
- **The batch is atomic end to end, including against save failure.** `addAllAtomically` validates
  every candidate's expected ID and conflicts (against existing activities *and* earlier
  candidates in the same batch) before the stored list or next-ID counter changes at all. If the
  in-memory batch succeeds but the subsequent disk save fails, `ApplicationRunner` goes one step
  further than every other mutating command: for `RecurCommand`/`ResetCommand` specifically (see
  `needsFailureRollback` in Design considerations), it restores the exact pre-command in-memory
  snapshot, so a save failure never leaves a partially-applied batch sitting in memory only.

### Design considerations

- **v1.0 parsing stays on `FieldParser`; a declarative `ArgumentTokenizer` exists alongside it,
  not in place of it.** Every v1.0 command parser still calls the small stateless `FieldParser`
  (`extractField`/`indexOfMarker`) directly, exactly as before — this bullet's original tradeoff
  (self-contained, easy-to-follow parsers, at the cost of sharp edges like markers that are
  trailing substrings of each other, e.g. `c/` inside `topic/`, needing an explicit boundary rule
  cross-checked in `FieldParserTest.indexOfMarker_noKnownMarkerIsMistakenlyMatchedInsideAnother`)
  still holds for all of v1.0. Technical-debt hardening added `parser.common.ArgumentTokenizer` +
  `ArgumentMarker` as a **declarative alternative** for v2.0 commands with more complex grammars
  (`recommend`, `route`): a per-command marker registry (each marker declared `required`/
  `optional`) tokenized in one pass, reusing `FieldParser`'s own boundary rule so matching stays
  consistent, plus three things `FieldParser` callers previously had to hand-implement themselves:
  required-field checking, fail-fast rejection of an undeclared marker-shaped token anywhere in
  the input (not just leading text), and basic double-quote support so a value may contain spaces
  or another marker's prefix text (e.g. `n/"CG3207 lecture"`, `note/"see c/o front desk"`).
  `ArgumentTokenizerTest` proves field-by-field equivalence with `FieldParser.extractField` for
  representative v1.0 argument strings. No existing parser calls it yet — adopting it for a given
  v1.0 command is a deliberate future migration, not a side effect of it existing.
- **Blank optional fields are treated as absent, not as stored empty strings.** Free-text optional
  fields (`topic/`, `note/`, and `find`'s `k/`) run through a `blankToNull`-style normalisation so
  that `topic/   ` (whitespace only) behaves identically to omitting `topic/` entirely — both for
  storage and for "at least one filter is required" gates. This was fixed in three separate places
  after the same class of bug appeared repeatedly (add/edit's `topic/`/`note/`, `list`/`find`'s
  `topic/` filter, `connection find`'s `from/`/`to/`, and `find`'s `k/`), which is why it is called
  out explicitly here rather than left implicit: any new optional free-text field should apply the
  same normalisation from the start.
- **Storage delimiter is unescaped by design, so user input is validated against it up front.**
  `activities.txt`/`topics.txt` use `|` as a field delimiter with no escaping mechanism. Rather
  than escaping it, the parser layer rejects any `n/`, `topic/`, `note/`, or topic-name value
  containing `|` before the activity/topic is ever created — this avoids a command reporting
  success and then permanently failing to persist on every subsequent save.
- **Cross-field validation lives in the parser layer, not in model constructors.** For example,
  `FixedActivity`'s "end after start" and `FlexibleActivity`'s "duration fits window" checks are
  performed by `ActivityCommandParser` before construction, not inside the model classes. This
  keeps the model classes simple data holders and keeps all user-facing validation messages in one
  place.
- **An activity's topic is validated against `TopicManager`, not stored as a free-floating
  string.** `ActivityCommandParser.parseAdd`/`parseEdit` take a `TopicManager` and reject a
  non-null `topic/` that does not exist under the activity's resulting category, before the
  activity is built. This closes off two related failure modes that a plain string field would
  otherwise allow: referencing a topic that was never created, and an edit that changes category
  silently stranding the activity's existing topic outside the category it is registered under.
  The check runs during parsing, so a rejected request never reaches `confirmIfNeeded()`.
- **Confirmation is generic via a `Confirmable` interface, not a per-command `instanceof` chain.**
  `CommandConfirmationHandler.confirmIfNeeded()` previously named `DeleteCommand`, `EditCommand`,
  `TopicRenameCommand`, `TopicDeleteCommand`, and `ResetCommand` individually in an `instanceof`
  chain — readable at four or five branches, but every new confirmable command (v2.0's
  `recommend` "adopt this recommendation?" step, or `RecurCommand`'s confirmation) would have
  meant editing this class again, violating OCP. Technical-debt hardening replaced this with
  `command.Confirmable` (an interface with one method, `getConfirmation()`) and
  `command.Confirmation` (an immutable three-outcome value: `proceed()` — execute with no prompt,
  e.g. `reset all` with nothing to reset; `cancel(message)` — cancel with no prompt and a custom
  message, e.g. edit with no changed fields; or `ask(prompt)` — show the prompt and read y/n). The
  handler now only checks `instanceof Confirmable` generically; each of the five existing
  commands implements `getConfirmation()` itself, reusing the manager reference it already holds
  to build its own preview — no prompt text, ordering, or EOF/y/n behaviour changed for any
  existing command. `CommandConfirmationHandlerTest` proves this with a locally-defined test
  command that has no relationship to any real command class, demonstrating that a brand-new
  confirmable command works without the handler ever changing.
- **Every confirmation-requiring command validates before prompting, via a side-effect-free
  preflight check.** `delete`/`edit` validate that the target activity exists before showing the
  y/n prompt (`ActivityManager.getById()`, called from the parser); `edit` additionally preflights
  duplicate/overlap conflicts via `ActivityManager.checkNoConflicts()`; `topic rename`/`topic
  delete` preflight existence, duplicate-name, and still-in-use conditions via
  `TopicManager.checkCanRename()`/`checkCanDelete()`. Each preflight check is a thin wrapper that
  the corresponding mutating method (`replace()`, `rename()`, `delete()`) also calls internally,
  so the same validation runs again at execution time as defensive protection against state
  changes between the two calls. Any new confirmation flow should follow this pattern: validate
  everything that would cause a rejection in the parser layer, before a `Command` is ever returned
  for `confirmIfNeeded()` to act on. `RecurCommandParser` follows the same pattern one level
  further: it builds the entire `RecurrencePlan` - including every conflict check - during
  parsing, so a plan that would fail never even reaches `confirmIfNeeded()`.
- **A second confirmation shape, `MenuConfirmable`, sits alongside `Confirmable` rather than
  replacing it.** `reset all`'s redesign from a plain y/n into three explicit outcomes (delete
  all, keep class schedules, cancel) doesn't fit `Confirmable`'s `Confirmation` value, which only
  ever leads to one `execute()` behaviour. Rather than stretching `Confirmation` to carry a
  selectable outcome, `command.MenuConfirmable` adds a second, independent one-method-plus-one-method
  interface (`getMenuPrompt()` / `applyMenuAnswer(rawAnswer)` returning a `MenuOutcome`) that
  `CommandConfirmationHandler` also checks via `instanceof`, exactly like `Confirmable` - so a
  future menu-driven command needs neither interface's existing implementers nor the handler
  itself to change. `ResetCommand` is the only command implementing it so far;
  `applyMenuAnswer` mutates the command's own `Selection` field so a later `execute()` call knows
  which of the two mutating outcomes to perform.
- **`AcademicCalendarStorage` loads and validates its file once per run, lazily, and caches
  whatever it got - success or failure.** `RecurCommandParser` holds the cached
  `AcademicCalendar`/`StorageException` as instance state and only calls
  `AcademicCalendarStorage.load()` the first time `recur` is actually used, not at application
  startup - so a missing or malformed `academic-calendar.txt` never blocks any other command, and
  every `recur` call within the same run sees a consistent snapshot even if the file is edited
  mid-run (edits only take effect after a restart, matching the "reference data you maintain
  yourself" model). `AcademicCalendarStorage` itself never creates, repairs, or rewrites the file
  under any circumstance, including from `reset all`.
- **`ClassSchedulePolicy` is the single source of truth for "is this a class session?", shared by
  two otherwise-unrelated features.** Both `recur` (which activities can be recurred) and `reset
  all`'s "keep class schedules" option (which activities survive) need the identical answer to
  "is this activity a fixed lecture/tutorial/lab/section-teaching session?" - a
  `FIXED` `ACADEMIC` activity whose description contains one of a fixed set of whole-word,
  case-insensitive session terms, matched with a regex negative-lookaround so `lab` doesn't match
  inside `collaboration`. Keeping this in one class rather than duplicating the rule in
  `RecurCommandParser` and `ResetCommand` independently means the two features structurally cannot
  drift apart on what counts as a class.
- **`RecurrencePlanner` resolves each target week's matching weekday from the calendar record
  itself, never by adding seven days repeatedly.** `AcademicWeek.findDate(DayOfWeek)` scans that
  week's own inclusive date range for the requested weekday. This is deliberate: recess, reading,
  and examination gaps between instructional weeks mean a fixed seven-day stride would land on the
  wrong calendar date across a semester boundary (e.g. the source prompt's Semester 1 gap between
  week 6 ending and week 7 starting). Every generated occurrence's ID is assigned sequentially in
  chronological week order, and `ActivityManager.addAllAtomically()` (used by both `recur` and
  nowhere else) validates every candidate's expected consecutive ID and every conflict - against
  existing activities and against earlier candidates in the same batch - before mutating the
  stored list or the next-ID counter at all, so a rejection partway through a batch can never leave
  a partial series added.
- **A save failure after `recur`/`reset` rolls back the in-memory batch, not just the disk
  write.** Every other mutating command changes at most one activity/topic, so the existing
  behaviour - report the storage error, leave that one change sitting in memory until the next
  successful save or restart - was already acceptable. `recur` can add many activities in one
  command and `reset` can rewrite the entire collection, so `ApplicationRunner` captures an
  `ApplicationStateSnapshot` (activities, topics, next ID, default order) immediately before
  executing a command that `needsFailureRollback()` (currently `RecurCommand`/`ResetCommand`
  only), and restores it if the subsequent save fails - so a storage failure never leaves a
  partially-applied batch visible only in memory.
- **Three-state accessibility values.** `AccessibilityStatus`/`ShelterStatus` are `YES`/`NO`/
  `UNKNOWN`, not a boolean, so that "no information recorded" is never conflated with "confirmed
  not accessible" — a direct requirement from the project's accessibility principles.
- **The accessibility domain is immutable and entirely separate from activities.** `Facility` and
  `Connection` are read-only reference data loaded once at startup from external text files;
  activities never reference them. This keeps the "activities don't store locations, routes are a
  separate lookup" requirement structurally enforced rather than just documented.
- **`logic.graph.AccessibilityGraph` is a read-only, non-breaking prep layer for v2.0's `route`
  command.** `FacilityManager` and `ConnectionManager` stay completely independent in v1.0, by
  design (previous bullet) — but v2.0 needs Dijkstra-based shortest-path lookups over both
  together, and building that from scratch when v2.0 lands would risk duplicating data or
  destabilising the v1.0 loading path. `AccessibilityGraph` is built once, at the point something
  chooses to construct it, from an already-loaded `FacilityManager`/`ConnectionManager` (nodes =
  facilities, edges = two-way, distance-weighted connections); `getShortestPath(from, to)` runs a
  standard lazy-deletion Dijkstra (valid since every stored connection distance is a positive
  whole number, enforced at load time by `ConnectionStorage`) and returns the ordered facility
  names plus total distance, or `null` if no path exists. **Not referenced by any v1.0 command,
  `CommandDispatcher`, or `ApplicationRunner`** — v2.0's `route` command will be the first caller.
  `AccessibilityGraphTest` verifies both a small synthetic graph and the real bundled NUS FASS
  dataset, with two paths checked by hand against the dataset's own connection notes.
- **`facility validate`/`connection validate` re-run existing load-time checks on demand, and
  change nothing.** `facilities.txt`/`connections.txt` are human-editable, but the only integrity
  check used to run once at startup as an easy-to-miss warning; fixing a mistake meant noticing
  the warning, then restarting to see if the fix worked. Both new commands just call
  `Storage.loadFacilities()`/`loadConnections()` again on demand and report the returned
  `LoadResult`'s warnings (or "no issues found") — the exact same validation `FacilityStorage`/
  `ConnectionStorage` already perform, not a second implementation of it. Deliberately read-only:
  neither command writes to disk or replaces the already-loaded `FacilityManager`/
  `ConnectionManager`, matching the "validation only, no silent repair" requirement for
  accessibility-critical data. `CommandDispatcher` now holds a `Storage` reference (previously
  only `ApplicationRunner` did) so these commands can re-read the raw files without duplicating
  `Storage`'s path-resolution logic — the only wiring change this required outside the two new
  command classes and their parser methods. `facility reset-default` (this item's explicitly
  optional stretch goal, restoring the bundled defaults with confirmation) was not implemented in
  this pass.

## Product scope

### Target user profile

UniEnable serves two equally important personas, both entering an unfamiliar university,
internship, or entry-level work routine, both able to type comfortably and both preferring
predictable CLI interaction over a GUI:

- **Sam**, a tertiary student with ASD or ADHD, who can become overwhelmed by dense schedules,
  benefits from concise default output with optional detail, needs to retrieve "what's next"
  without reading a full day's itinerary, and needs specific (not vague) error messages.
- **Jordan**, a tertiary student who uses a wheelchair, who needs advance accessibility
  information (step-free entrances, lifts, ramps, rest points), a way to look up accessible routes
  between known facilities, and clear warnings when the local dataset is incomplete. Wheelchair use
  does not imply any difficulty typing or using a keyboard.

### Value proposition

Tertiary students with ASD or ADHD, and wheelchair users, entering unfamiliar routines often need
to weigh scheduling, sensory demands, recovery time, and accessible travel separately, making it
hard to build a practical daily plan around individual needs. UniEnable helps by combining
concise, preference-aware daily scheduling with separate, locally maintained accessibility and
route reference information, entirely offline in a fast CLI.

## User Stories

Priorities: `***` must have, `**` should have, `*` nice to have. Selected v1.0 stories (the
complete v1.0/v2.0 list lives in the team's prioritised user-story backlog, maintained outside
this repository):

| Priority | As a ... | I want to ... | So that I can ... |
|---|---|---|---|
| `***` | student managing an unfamiliar routine | add an activity with its essential scheduling information | include it in my daily itinerary |
| `***` | student managing multiple commitments | view activities in input, time, or chronological order | review my activities in the sequence that best matches my planning needs |
| `***` | student managing multiple commitments | find activities using keywords and narrow results by category, topic, or date | retrieve relevant activities without scanning my entire itinerary |
| `***` | student | change one or more details of an existing activity while keeping other details unchanged | update my itinerary with less typing |
| `***` | student | remove an activity whenever I consider it obsolete | reduce unnecessary schedule information and cognitive load |
| `***` | student | mark an activity as completed or change it back to incomplete | distinguish finished activities from unfinished ones |
| `***` | student | organise each activity under a fixed category | maintain a consistent structure for my commitments |
| `***` | student | view my next relevant activity in a concise format | know what to do next without reading my entire itinerary |
| `***` | student | record an activity's energy demand, sensory load, and optional notes | understand its demands when planning |
| `***` | student who uses a wheelchair | view pre-recorded accessibility information for known facilities | prepare for possible barriers |
| `***` | student who uses a wheelchair | view pre-recorded accessibility and distance information for connections | prepare for travel using locally maintained reference data |
| `**` | student | create and manage optional topics within a fixed category | organise related commitments at a level meaningful to me |
| `**` | student with a recurring weekly class | turn one lecture/tutorial/lab/section-teaching session into the whole semester's worth of sessions in one command, following my school's actual teaching-week calendar (including recess) | avoid manually re-entering the same class dozens of times and still manage each occurrence (mark, edit, delete) independently afterwards |
| `**` | student clearing out a semester | choose whether resetting keeps my recurring class schedule or wipes everything, instead of only ever being able to delete everything | avoid having to re-create my whole timetable after clearing out one-off tasks |

## Non-Functional Requirements

- Java 17, primarily object-oriented design.
- CLI as the sole interaction mode; single-user, offline operation with no dependency on a private
  remote server, external APIs, or a DBMS.
- Platform-independent behaviour on Windows, Linux, and macOS.
- Local, human-editable text-file storage; no installer required; ships as a standalone executable
  JAR.
- Fast startup and response, suitable for users who prefer direct keyboard interaction over a
  mouse-driven interface.

## Glossary

- **Activity** - A user-recorded item on the itinerary: either *fixed* (a confirmed start/end
  time) or *flexible* (an earliest-start/latest-end window plus a required duration).
- **Category** - One of four fixed top-level groupings (`ACADEMIC`, `CCA`, `WORK_INTERNSHIP`,
  `OTHERS`) every activity belongs to.
- **Topic** - An optional, user-created grouping within a category (e.g. a specific module or
  workplace); at most one per activity, one level deep.
- **Energy rating / Sensory rating** - A user-entered `1`-`5` planning value describing expected
  energy demand or sensory load; self-reported, not a medical assessment.
- **Facility** - A read-only reference record for a physical location (e.g. a building), including
  its accessibility features (lifts, ramps, step-free entrances, etc.).
- **Connection** - A read-only, weighted, two-way link between two facilities, carrying a distance
  in metres, a traversal type, and an accessibility status.
- **Accessibility status** - One of `YES`/`NO`/`UNKNOWN`; `UNKNOWN` is never treated as
  accessible.
- **Academic calendar** - The externally maintained `data/academic-calendar.txt` reference file:
  one or more academic years, each with numbered teaching weeks and no-class dates. The sole
  authority `recur` uses to resolve week numbers to real dates; UniEnable never creates, repairs,
  or embeds any of its contents.
- **Class schedule** - A `FIXED` `ACADEMIC` activity whose description contains a lecture/
  tutorial/lab/section-teaching session term (see `ClassSchedulePolicy`); the shared eligibility
  rule for both `recur` and `reset all`'s "keep class schedules" option.
- **Recurring session / occurrence** - One activity created by `recur` from a class-schedule
  source. Each occurrence is an ordinary, independent `FixedActivity` with its own permanent ID -
  there is no linked series object, so marking, editing, or deleting one occurrence never affects
  any other.

## Instructions for manual testing

These instructions cover exploratory manual testing on top of the automated suites described
below; they assume familiarity with the [User Guide](UserGuide.md)'s command reference.

### Automated tests (run these first)

```bash
./gradlew clean test checkstyleMain checkstyleTest
bash text-ui-test/runtest.sh
./gradlew releaseZip
```

- `./gradlew test` runs the full JUnit suite (see the test report for the current pass count;
  avoid quoting a specific number here, since it will drift as tests are added).
- `text-ui-test/runtest.sh` (or `.bat` on Windows) rebuilds the JAR, feeds it the scripted
  `text-ui-test/input.txt`, and diffs the output against `text-ui-test/EXPECTED.TXT`; it exercises
  the full v1.0 and v2.0-so-far command surface end-to-end, including many boundary cases and
  error paths (see the script itself for exactly what it covers — it grows as new scenarios are
  added, so treat any specific line count here as a snapshot, not a guarantee). It clears
  `text-ui-test/data/` before each run for a deterministic starting state, then copies
  `text-ui-test/academic-calendar-test.txt` in as `data/academic-calendar.txt` - a small synthetic
  calendar fixture (not the real, date-bound `data/academic-calendar.txt`) so the recur scenarios
  stay deterministic and don't expire as real time passes.
- `./gradlew releaseZip` depends on `shadowJar` and produces
  `build/distributions/unienable.zip`, containing `unienable.jar` plus
  `data/academic-calendar.txt` at the top level - the exact layout described in the User Guide's
  Quick Start. Verify the calendar file is absent from the jar itself (`jar tf
  build/libs/unienable.jar | grep academic-calendar` should find nothing) but present in the zip
  (`unzip -l build/distributions/unienable.zip`).

### Manual exploratory testing

1. **Start from a clean slate.** Run the JAR from an empty working directory so `data/` is created
   fresh:
   ```bash
   java -jar build/libs/unienable.jar
   ```
2. **Activity lifecycle.** Try `add` for both a `FIXED` and a `FLEXIBLE` activity, then `list`,
   `view ID`, `find k/KEYWORD`, `edit ID FIELD/VALUE`, `mark ID`/`unmark ID`, and `delete ID`
   (confirm with `y` and `n` on separate attempts to check both paths). See `guide add`/`guide
   find` for ready-to-copy examples.
3. **Validation and atomicity.** Deliberately submit an invalid edit (e.g. an out-of-range
   `energy/` value, or a `from/`/`to/` pair with the end before the start) and confirm the
   activity is completely unchanged afterwards (`view ID` again).
4. **Topics.** `topic add`, assign an activity to it via `add ... topic/NAME`, `topic rename` it
   (check the activity's topic updates too), then try `topic delete` while it is still in use
   (should be rejected) and again after reassigning/removing the activity.
5. **Accessibility reference data.** `facility list`, `facility view NAME`, `facility find
   type/TYPE`, `connection list`, `connection view ID`, `connection find from/NAME`. This data is
   read-only — there is no command that edits it. To test the malformed-data-file path, stop the
   application, edit `data/facilities.txt` or `data/connections.txt` to add an invalid line (e.g.
   an unknown feature type), and restart: the app should report a `[Warning] Partial data loaded`
   message naming the file and line, then continue with the rest of the data loaded normally. Then
   run `facility validate`/`connection validate` **without restarting** — they should report the
   exact same issue(s) on demand, without modifying either file (check with `git diff` or a hash
   of the file before/after).
6. **Persistence.** After making changes, exit with `bye` and restart the JAR; confirm activities,
   topics, and completion state survived.
7. **The built-in guide.** Run `guide` and try both a menu number (`guide 2` or bare `2` right
   after the menu) and a topic keyword (`guide add`) to confirm both resolve to the same topic.
8. **Recurring class sessions.** Confirm `data/academic-calendar.txt` exists (release ZIP layout
   above), then `add` a `FIXED` `ACADEMIC` activity whose description contains a session term
   (e.g. `n/CG3207 Lecture`) dated inside one of that file's instructional weeks. Run `recur ID
   week ...` with a sparse spec (e.g. `3;7;9`): check the preview lists the right dates and asks
   `Continue? (y/n)`, answer `n` and confirm nothing was added (`list`), then repeat and answer `y`
   and confirm every previewed date now exists as its own activity with its own ID. Run the exact
   same `recur` command a third time and confirm it reports nothing new to create instead of
   duplicating. Try `recur` on an activity that doesn't match the eligibility rule (wrong category,
   or no session term in the description) and confirm it's rejected before any preview. Try a week
   number the calendar file doesn't define, and a `WEEK_SPEC` that omits the source week, and
   confirm each gets its own specific error. `mark`/`edit`/`delete` one generated occurrence and
   confirm the others are unaffected. Then temporarily rename `data/academic-calendar.txt` and
   restart: confirm `recur` now reports the file is missing while `add`/`list`/every other command
   keeps working; rename it back and restart again to confirm `recur` recovers.
9. **Three-option reset.** With a mix of class-schedule and other activities present, run `reset
   all` and check the preview's four counts (activities, class schedules, other activities,
   topics) match what `list`/`topic list` show. Test all three numbered choices in separate runs
   (restart or re-`add` between them): `1` clears everything and restarts IDs at `[1]`; `2` keeps
   only the class-schedule activities with their original IDs/notes/completion status and prunes
   topics no longer referenced; `3`, a blank line, and an out-of-range number (e.g. `9`) each
   cancel with no change. After any reset, confirm `data/academic-calendar.txt`,
   `data/facilities.txt`, and `data/connections.txt` are byte-identical to before (`git diff` or a
   checksum).
