# Package Hierarchy Review and Refactor Plan

Date: 2026-08-06
Scope: `src/main/java/seedu/unienable/**`, `src/test/java/seedu/unienable/**`
Baseline: HEAD `07cf8a7` ("feat: complete UniEnable v2.0 release prep"), 144 main
Java files, 118 test Java files, 1141 `@Test` methods, `./gradlew clean test` green.

## 1. Current package tree

```text
seedu.unienable
├── UniEnable.java                          (entry point)
├── app/                                     ApplicationRunner, CommandConfirmationHandler, LoggingConfig
├── accessibility/
│   ├── classes/                             Facility, Connection, FacilityFeature
│   └── enums/                               AccessibilityStatus, ShelterStatus, TraversalType
├── command/
│   ├── (root)                               Command, CommandResult, Confirmable, Confirmation,
│   │                                         MenuConfirmable, MenuOutcome  (shared command contracts)
│   ├── accessibility/
│   │   ├── common/                          AccessibilityDisclaimer, ValidationReportFormatter
│   │   ├── connection/                      Connection{Find,List,Validate,View}Command
│   │   ├── facility/                        Facility{Find,List,Validate,View}Command
│   │   └── route/                           RouteCommand
│   ├── activity/
│   │   ├── crud/                            Add/Delete/Edit/ViewCommand
│   │   └── general/                         Find/List/Mark/Next/OrderSet/OrderView/UnmarkCommand
│   ├── dashboard/                           DashboardCommand
│   ├── general/                             Exit/Guide/ResetCommand
│   ├── preference/                          Preference{Set,View,Reset}Command
│   ├── recommend/                           Recommend{Generate,View,Adopt,Cancel}Command
│   ├── recur/                               RecurCommand
│   ├── timetable/                           TimetableCommand
│   └── topic/                               Topic{Add,List,Rename,Delete}Command
├── exception/                                UniEnableException + 7 specific subclasses (flat)
├── logic/
│   ├── (root)                               ActivityManager, ActivityFilter, ActivityConflictChecker,
│   │                                         ConnectionManager, FacilityManager, TopicManager
│   ├── dashboard/                            DashboardService
│   ├── graph/                                AccessibilityGraph, GraphPath
│   ├── preference/                           PreferenceManager
│   ├── recommend/                            RecommendationManager, RecommendationService
│   ├── recur/                                ClassSchedulePolicy, RecurrencePlanner
│   ├── route/                                RouteService (if present)
│   └── timetable/                            TimetableService
├── model/
│   ├── classes/                               Activity, FixedActivity, FlexibleActivity, Topic,
│   │                                          EnergyRating, SensoryRating
│   ├── dashboard/                             DashboardPeriod
│   ├── enums/                                 ActivityCategory, ActivityOrder, CompletionStatus, ScheduleType
│   ├── preference/                             PreferenceProfile, TomatoSuggestion
│   ├── recommend/                              RecommendationProposal, RecommendedPlacement
│   ├── recur/                                  AcademicCalendar, AcademicWeek, NoClassDate, RecurrencePlan
│   └── timetable/                              TimetableMode, TimetablePeriod
├── parser/
│   ├── (root)                                  CommandDispatcher
│   ├── accessibility/                          Connection/Facility/RouteCommandParser
│   ├── activity/                               ActivityCommandParser, Add/Edit/Find/ListCommandParser
│   ├── common/                                 ArgumentMarker, ArgumentTokenizer, DateTimeParser,
│   │                                            FieldParser, Parser, RatingParser
│   ├── dashboard/                              DashboardCommandParser
│   ├── preference/                             PreferenceCommandParser
│   ├── recommend/                              RecommendCommandParser
│   ├── recur/                                  RecurCommandParser, WeekSpecificationParser
│   ├── timetable/                              TimetableCommandParser
│   └── topic/                                  TopicCommandParser
├── storage/
│   ├── (root)                                  Activity/Connection/Facility/Settings/TopicStorage,
│   │                                            LoadResult, Storage (facade)
│   ├── preference/                             PreferenceStorage
│   └── recur/                                  AcademicCalendarStorage
└── ui/
    ├── (root)                                  Ui, MessageFormatter
    ├── accessibility/                          RouteFormatter (etc.)
    ├── dashboard/                               DashboardFormatter
    ├── preference/                               (formatter, if any)
    ├── recommend/                                RecommendationFormatter
    ├── recur/                                    RecurrenceFormatter
    └── timetable/                                 TimetableFormatter
```

`src/test/java` mirrors this tree package-for-package (verified: every production subpackage listed
above has a same-named test subpackage, plus `testutil/recur` for shared recurrence fixtures).

## 2. Assessment against the master-prompt design rules

The master prompt asks for: feature-oriented subpackages, mirrored test structure, discoverable
command/parser/model/service/storage/exception/utility groupings, no catch-all `misc`/`general`/
`common`-as-dumping-ground packages, no excessive nesting, no packages holding a single trivial
class without architectural justification, and no aggressive splitting that makes imports harder to
follow.

The tree above already satisfies these:

1. **Feature-oriented already.** Every non-trivial feature (`activity`, `accessibility`, `topic`,
   `recommend`, `recur`, `dashboard`, `timetable`, `preference`) has its own `command`, `parser`,
   `model`, `logic`/`service`, and (where relevant) `storage`/`ui` subpackage, consistently named.
2. **Tests mirror production 1:1.** No test-package audit finding was needed beyond confirming the
   mirror holds, which it does.
3. **No dumping grounds.** `parser/common` holds five genuinely shared, feature-agnostic parsing
   primitives (`ArgumentMarker`, `ArgumentTokenizer`, `DateTimeParser`, `FieldParser`, `RatingParser`)
   that every feature parser depends on — this is a legitimate shared-utility package, not a place
   things were dropped for lack of a better name. `command/accessibility/common` is the same pattern
   scoped to one feature (`AccessibilityDisclaimer`, `ValidationReportFormatter`, shared by the
   facility/connection/route commands). Neither is named `misc`/`general`-as-catch-all; `command/general`
   holds exactly three cross-cutting, feature-less commands (`exit`, `guide`, `reset`), which is what
   that package is for.
4. **`exception/` is intentionally flat.** `Ui`/`CommandConfirmationHandler` catch `UniEnableException`
   uniformly regardless of subclass (see `app/CommandConfirmationHandler.java` and the top-level
   command loop) — handling does not differ by domain, so per-domain exception subpackages
   (`activity.exception`, `recur.exception`, ...) would add navigation depth without adding meaning.
   This matches the master prompt's own instruction to subpackage exceptions "only when behaviour or
   handling differs."
5. **No excessive nesting.** Maximum depth is `command/accessibility/connection` (3 levels under the
   base package), well short of the "excessive" threshold the prompt warns against.
6. **No circular dependencies found.** `parser.*` depends on `command.*`, `logic.*`, and `model.*`;
   `command.*` depends on `logic.*` and `model.*`; `logic.*` depends on `model.*`; `model.*` and
   `exception.*` depend on nothing else in the tree. This is a clean one-directional layering
   (parser → command → logic → model), confirmed by `grep`-ing imports in each layer for any
   back-reference into a higher layer; none were found.

## 3. Candidate move considered and rejected

The only structure that superficially looks like the master prompt's "crowded general package"
warning is `command/activity/general` (7 classes: `Find`, `List`, `Mark`, `Next`, `OrderSet`,
`OrderView`, `Unmark`), which a naive reading of the prompt's suggested `query`/`completion`/
`scheduling` split would break into three even-smaller packages of 2–3 classes each.

This move was evaluated and **rejected**:

- 7 classes in one package is not "crowded" by this codebase's own calibration — no existing
  package holds more than ~9 classes (`command/topic`-sibling comparison: `parser/common` has 6,
  `command/accessibility` subpackages have 4 each). Splitting a 7-class package into three
  2–3-class packages produces packages this codebase would otherwise avoid creating from scratch,
  which conflicts with rule 6 in the master prompt itself ("avoid packages containing only one
  trivial class unless the responsibility is architecturally important") applied at a slightly
  larger scale, and with rule 9 ("do not split packages so aggressively that imports become harder
  to understand").
- The corresponding parser package (`parser/activity`, 5 classes) is *not* split into crud/general
  at all, and is not being asked to be — meaning enforcing a 3-way split only on the command side
  would make commands and parsers for the same feature structurally inconsistent with each other,
  which is a worse outcome than the current crud/general split.
- No behavioural, testing, or dependency benefit was identified — this would be a purely cosmetic,
  non-zero-risk change (7 production files + 7 test files + their imports) for a codebase where the
  build is currently green. The master prompt explicitly says: "Do not move classes solely to make
  folders look symmetrical."

**Decision: no package moves are made in this refactor.** The existing hierarchy already meets the
stated design goals. Effort is redirected to the functional defects (Sections 4–5 below) and to
test/documentation work, which is where this codebase actually has gaps.

## 4. Movement table

| Current class | Current package | Proposed package | Reason | Dependencies affected | Tests affected | Diagram/doc affected |
|---|---|---|---|---|---|---|
| *(none)* | — | — | See Section 3 — no move met the bar for a justified, non-cosmetic change. | — | — | — |

## 5. Risks

Not applicable — no moves are being made, so there is no risk of circular dependencies, visibility
changes, package-private test breakage, reflection/resource-path breakage, Checkstyle regressions,
or Text-UI/storage incompatibility from this phase. These risk categories will instead be evaluated
per-change for the functional fixes below, where they are relevant (e.g. a new shared date-selector
class touches several parser packages' imports).

## 6. Functional work carried out instead

Since the hierarchy audit found no refactor debt, the review budget was spent verifying the two
functional defects named in the master prompt against current HEAD, plus the "previously identified
defects to re-verify" list:

1. **Relative date-selector inconsistency (Defect A)** — confirmed real. See
   `docs/planning/DATE_SELECTOR_SUPPORT_MATRIX.md` for the full audit and the fix.
2. **Recurrence unable to span a full semester in one command (Defect B)** — audited and **not
   reproduced** on current HEAD. `WeekSpecificationParser` already accepts one inclusive range
   (e.g. `1 to 13`) as a single `WEEK_SPEC` item, and `RecurrencePlanner` resolves each requested
   week number against the academic-calendar file via `AcademicCalendar.findWeek(year, semester,
   weekNumber)` rather than adding `7 * plusWeeks()` to the source date, so a recess gap between
   Week 6 and Week 7 is already handled correctly by the file-driven date lookup, in one command.
   This is locked in with new regression tests (`RecurCommandParserTest`,
   `RecurrencePlannerTest`) rather than a behavioural change. See the final report for detail.
3. **Recommender scheduling in the past / stale adoption** (master prompt Section 6, items 1–2) —
   confirmed real and reproduced: `RecommendationService` does not thread `now` into slot
   generation, and `RecommendCommandParser`'s `adopt` branch never checks staleness. Fixed; see the
   final report.
