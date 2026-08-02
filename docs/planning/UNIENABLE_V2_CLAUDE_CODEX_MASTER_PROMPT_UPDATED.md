# UniEnable v2.0 — Claude/Codex Master Implementation Prompt

You are acting as the senior software engineer and documentation maintainer for the **UniEnable** CS2113 team-project codebase. **v1.0 has already been released and is the protected baseline.** Your task is to implement the approved v2.0 features incrementally, without regressing v1.0 behaviour, while maintaining CS2113 code quality, testing, Git, documentation, and UML standards.

## 1. Non-negotiable working rules

1. Begin with a **read-only repository audit**. Do not edit anything until you have reported:
   - current branch, `HEAD`, and working-tree status;
   - current package/test/docs structure;
   - all existing v1.0 commands and public behaviour;
   - the current v2.0 placeholders/"Coming soon" wording in `GuideCommand`, `docs/UserGuide.md`, `docs/DeveloperGuide.md`, and `README.md`;
   - reusable foundations already present, especially the accessibility graph/Dijkstra preparation, rollback mechanism, parser structure, storage facade, and recurrence package organization.
2. Treat the released v1.0 behaviour as a compatibility contract. Do not rename commands, alter storage formats, renumber stable activity IDs, weaken validations, remove confirmations, or change established output wording unless the relevant v2.0 specification explicitly requires it.
3. Implement **one feature branch at a time**. Do not develop all v2.0 features on one large branch.
4. Before starting a feature, create/update its task specification under:

   ```text
   docs/tasks/v2/<feature>/
   ├── README.md
   ├── ACCEPTANCE_CRITERIA.md
   ├── TEST_PLAN.md
   └── IMPLEMENTATION_NOTES.md
   ```

   Use the existing recurrence organization as the model: feature-specific command, parser, logic, model, storage, UI, tests, and diagrams should be grouped coherently when those layers are actually needed. Do not create empty or artificial packages merely for symmetry.
5. Each feature must be delivered through a focused PR-sized branch with meaningful commits. Recommended branch names:
   - `feature/v2-route`
   - `feature/v2-dashboard`
   - `feature/v2-timetable`
   - `feature/v2-preferences`
   - `feature/v2-recommend`
   - `feature/v2-export`
   - `docs/v2-guide-refresh` only if a final cross-feature documentation pass is still needed
6. Do not merge, push, tag, or delete branches unless explicitly instructed. At the end of each branch, stop and present a review report.
7. Apply small-step refactoring only where needed to support the feature. Refactoring must preserve external behaviour and be followed immediately by regression testing.

## 2. CS2113 engineering standards to preserve

Apply these throughout every branch:

- **SRP and Separation of Concerns:** parsing, command orchestration, domain logic, persistence, and presentation remain separate.
- **Low coupling/high cohesion:** reuse existing facades and interfaces; do not let UI or parsers reach into storage internals.
- **OCP where appropriate:** extend command dispatch and domain behaviour without turning central routers into large condition-heavy classes.
- Prefer clear domain objects and enums over primitive obsession and magic strings.
- Avoid long methods, deep nesting, complicated expressions, duplicated validation, hidden side effects, and speculative abstractions.
- Use exceptions for invalid user/environment conditions and Java assertions only for internal programmer invariants. Never use assertions to perform required work.
- Preserve file-based logging for exceptional storage/rollback failures; do not expose stack traces to normal users.
- Add JavaDoc only for public APIs or non-obvious contracts. Comments should explain **why**, constraints, or design decisions—not restate obvious code.
- All command output must remain text-only, deterministic, readable without colour, and suitable for narrow terminals where specified.
- Any state-changing command must remain atomic: validate first, preview where applicable, confirm where required, mutate once, save once, and roll back fully on persistence failure.
- Keep external accessibility data read-only from the application.
- Keep activity-planning data separate from the accessibility graph. Activities do not gain facility/location fields.

## 3. Required branch sequence and dependencies

Use this order unless the audit proves a different dependency is necessary:

1. `feature/v2-route`
2. `feature/v2-dashboard`
3. `feature/v2-timetable`
4. `feature/v2-preferences`
5. `feature/v2-recommend`
6. `feature/v2-export`
7. final v2.0 integration/documentation/regression pass

Rationale: route can reuse the existing graph preparation; dashboard and timetable provide reusable views/metrics; preferences should exist before recommendation; recommendation can then consume preferences and timetable formatting; export should serialize stable, already-finalized views/data.

For every branch, first rebase/branch from the latest approved `main`, not from another unmerged feature branch. If a feature needs code from another branch, state that dependency and wait until the prerequisite branch is merged.

---

# FEATURE 1 — Accessible route search

## Branch and package organization

Branch: `feature/v2-route`

Suggested production layout, adjusted only if the current architecture justifies a better fit:

```text
src/main/java/seedu/unienable/
├── command/accessibility/route/RouteCommand.java
├── parser/accessibility/RouteCommandParser.java
├── logic/graph/                  # reuse existing AccessibilityGraph/GraphPath
└── ui/accessibility/RouteFormatter.java
```

Mirror the same hierarchy under `src/test/java`.

## Required behaviour

Implement:

```text
route from/FACILITY to/FACILITY
```

The route must:

- use Dijkstra's algorithm with stored distance in metres as the weight;
- use only connections whose accessibility status is `YES`;
- exclude `NO` and `UNKNOWN` connections;
- return the lowest-total-distance confirmed accessible path;
- show each ordered segment with start, destination, distance, traversal type, barriers, and notes;
- show the complete facility chain and total distance;
- not estimate travel time;
- not claim real-time verification or guaranteed accessibility;
- clearly report unknown facilities, same-origin/destination cases, malformed input, and "no known confirmed accessible route".

Do not add/edit/delete facility or connection records through the CLI.

## Tests

Cover at minimum:

- direct route;
- multi-edge shortest weighted route;
- a fewer-edge route that is longer than another route, proving Dijkstra rather than BFS behaviour;
- exclusion of `NO` and `UNKNOWN` edges;
- disconnected graph;
- unknown endpoint;
- same endpoint;
- deterministic tie handling;
- case-normalized facility lookup;
- malformed command syntax;
- partial accessibility dataset behaviour;
- dispatcher-to-parser-to-command integration;
- Text UI happy path and error path.

## Documentation and diagrams

Update in the same branch:

- `GuideCommand`: replace route "Coming soon" content with complete syntax, examples, limitations, and disclaimer;
- `docs/UserGuide.md`: full route section and command summary;
- `docs/DeveloperGuide.md`: route design, algorithm rationale, error handling, data eligibility rules;
- root `README.md` and `docs/README.md`: feature/status summary;
- architecture diagram only if a new component boundary is introduced;
- class diagram for route/graph interactions;
- sequence diagram for `route from/... to/...` execution.

Keep `.puml` and generated `.png` synchronized.

---

# FEATURE 2 — Accessible planning dashboard

## Branch and package organization

Branch: `feature/v2-dashboard`

Suggested layout:

```text
command/dashboard/
parser/dashboard/
logic/dashboard/
model/dashboard/
ui/dashboard/
```

Only create layers/classes that contain real responsibility.

## Required behaviour

Implement a command surface consistent with the approved requirements and current parser conventions, for example:

```text
dashboard today
dashboard tomorrow
dashboard this week
dashboard date/2026-08-17
dashboard PERIOD detail
```

During the audit, confirm the exact approved syntax from planning documents before coding and record it in the task folder.

Dashboard metrics must use only user-entered activity data:

- planned activity time;
- free/buffer time using a clearly documented calculation boundary;
- total energy demand;
- total sensory load;
- count of high-energy and high-sensory activities;
- completed/planned activity count as secondary progress.

Presentation requirements:

- exact values beside every ASCII bar;
- no colour dependency;
- no medical conclusions or performance judgement;
- energy/sensory labels must explicitly remain self-reported planning data;
- deterministic widths and rounding;
- chronological text fallback/details view;
- readable on a typical terminal.

Define and document how flexible activities contribute before recommendation exists. Do not silently invent scheduled durations beyond data the activity already contains.

## Tests

Cover empty periods, fixed-only, flexible-only, mixed activities, completed activities, boundary times, zero-duration/free-time edge cases, exact histogram scaling, high-load thresholds, deterministic formatting, invalid periods, and injected-clock tests for relative dates.

## Documentation and diagrams

Update guide/UG/DG/READMEs in the same branch. Add a dashboard class diagram and sequence diagram if the design is non-trivial. Replace dashboard "Coming soon" guidance with implemented commands only.

---

# FEATURE 3 — Optional ASCII timetable

## Branch and package organization

Branch: `feature/v2-timetable`

Suggested layout:

```text
command/timetable/
parser/timetable/
logic/timetable/
model/timetable/
ui/timetable/
```

## Required behaviour

Implement the approved one-shot CLI timetable views. Confirm exact grammar from the requirements, expected examples include:

```text
timetable week/2026-08-17
timetable day/mon
timetable item/A1
timetable compact
timetable details
```

Resolve any date-format discrepancy in the planning documents during the audit and use the repository's established `yyyy-MM-dd` convention unless an approved requirement says otherwise.

The timetable must:

- preserve chronological list as the normal/default activity view;
- provide optional day and week rendering;
- use short stable display identifiers in the grid and full details below;
- distinguish fixed `[F]`, recommended `[R]`, and buffer entries using text markers;
- show exact start/end times in details;
- avoid colour, mouse input, arrow-key navigation, ANSI cursor control, and screen redrawing;
- provide a narrow-terminal fallback to a one-day chronological rendering;
- handle overlaps clearly rather than silently overwriting cells;
- not adopt recommendation output without explicit confirmation.

Do not prematurely implement recommendation logic in this branch. Provide interfaces/data structures that recommendation can later consume without overengineering.

## Tests

Cover day/week boundaries, activities spanning multiple slots, overlaps, empty days, weekend handling, fixed/flexible distinction, stable IDs, narrow width fallback, deterministic alignment, long names, Unicode/ASCII safety as supported by the project, and exact output snapshots.

## Documentation and diagrams

Update guide/UG/DG/READMEs and relevant UML. Add a sequence diagram for a weekly timetable request and a focused class diagram for timetable rendering/model transformation.

---

# FEATURE 4 — User planning preferences

## Branch and package organization

Branch: `feature/v2-preferences`

Suggested layout:

```text
command/preference/
parser/preference/
logic/preference/
model/preference/
storage/preference/
ui/preference/
```

## Required behaviour

Read the requirements/user stories and define the smallest approved preference set needed by the recommendation feature. Do not invent health scoring or medical categories.

The command set should support viewing, setting, and resetting preferences with explicit syntax and validation. Before implementation, write the exact grammar and defaults into `docs/tasks/v2/preferences/ACCEPTANCE_CRITERIA.md`.

Requirements:

- human-readable persistence separate from activity records;
- backward-compatible startup when the preferences file does not yet exist;
- strict validation and helpful errors;
- atomic save and rollback on failure;
- stable documented defaults;
- no hidden inferred preferences;
- no use of disability or medical claims as algorithmic facts.

## Tests

Cover default state, set/view/reset, invalid values, duplicate fields, missing file, malformed file, persistence/restart, rollback on save failure, and compatibility with existing v1.0 data.

## Documentation and diagrams

Update all user/developer guidance and add storage/class/sequence diagrams where needed. Remove only the preference-related "Coming soon" text.

---

# FEATURE 5 — Deterministic schedule recommendation

## Branch and package organization

Branch: `feature/v2-recommend`

Suggested layout:

```text
command/recommend/
parser/recommend/
logic/recommend/
model/recommend/
ui/recommend/
```

Reuse timetable and dashboard abstractions from merged prerequisite branches. Do not duplicate formatting or metrics logic.

## Required behaviour

Implement one deterministic recommended daily schedule based on user-entered preferences and existing fixed/flexible activities.

Hard constraints:

- no activity overlap;
- fixed activities remain fixed;
- flexible activities are placed only inside their allowed windows;
- each flexible placement satisfies its full duration;
- existing activity conflict rules remain authoritative;
- recommendation does not infer travel feasibility because activities have no location field;
- route planning remains a separate explicit workflow.

Soft rules must be explicit, documented, deterministic, and tie-broken predictably. Do not claim global optimality unless the implemented algorithm proves it. Prefer "recommended schedule" over "best schedule".

The workflow must:

1. validate required activity fields and preferences;
2. identify exactly which activity/fields prevent recommendation;
3. offer a guided correction path only if that workflow is approved and can be implemented without destabilising CLI control flow;
4. otherwise provide a precise one-shot edit alternative;
5. generate a preview without mutating stored activities;
6. show timetable and exact dashboard metrics for the preview;
7. require explicit confirmation before adoption;
8. commit all adopted placements atomically;
9. save once and roll back fully on failure;
10. allow cancellation with no state change.

If recommendation creates placements for flexible activities, define whether it updates those activities, creates derived records, or stores a separate recommendation plan. Select one design, justify it in the DG, and ensure IDs, completion state, and persistence semantics remain unambiguous.

## Tests

Cover:

- no flexible activities;
- one feasible placement;
- multiple feasible placements with deterministic tie-breaking;
- impossible schedule;
- window boundary fit;
- fixed conflicts;
- preference effects;
- missing required fields;
- cancellation;
- acceptance and restart persistence;
- save failure rollback;
- timetable/dashboard preview consistency;
- repeated recommendation/idempotency semantics;
- no route/travel inference.

## Documentation and diagrams

This branch requires substantial DG work: algorithm, constraints, tie-breaking, object model, adoption transaction, limitations, and alternatives considered. Add focused class and sequence diagrams for planning and confirmed adoption. Replace recommendation "Coming soon" guide content only after the feature is complete.

---

# FEATURE 6 — CSV export

## Branch and package organization

Branch: `feature/v2-export`

Suggested layout:

```text
command/export/
parser/export/
logic/export/
ui/export/
```

Use storage utilities only where appropriate; export is not normal application persistence.

## Required behaviour

Confirm the approved export command grammar and exported datasets from the requirements before coding. Export must:

- write a documented CSV schema with a header row;
- preserve stable IDs;
- escape commas, quotes, line breaks, and empty values correctly;
- use deterministic ordering;
- avoid silently overwriting an existing file unless explicit confirmation/approved naming policy permits it;
- report the exact output path;
- fail safely without altering application state;
- avoid exporting internal implementation-only fields;
- document spreadsheet formula-injection mitigation for cells beginning with `=`, `+`, `-`, or `@`.

Use Java standard-library facilities or a justified lightweight implementation. Do not add a large dependency for a small CSV need without documenting licence, maintenance, and security implications.

## Tests

Cover empty export, normal records, commas/quotes/newlines, Unicode, formula-like content, deterministic ordering, invalid path, existing file policy, permission failure, and output round-trip parsing in tests.

## Documentation and diagrams

Update guide/UG/DG/READMEs. A sequence diagram is required if export crosses command/logic/filesystem boundaries; a class diagram is optional if the structure is simple.

---

# 4. Guide interface migration

The current built-in guide contains v2.0 "Coming soon" placeholders. Change it incrementally:

- A feature remains labelled **Coming soon** until its implementation, tests, docs, and diagrams are complete on that branch.
- When a feature is completed, replace only that feature's placeholder with:
  - command syntax;
  - concise purpose;
  - constraints/limitations;
  - 2–4 copyable examples;
  - related commands;
  - relevant safety/disclaimer text.
- Keep the numbered guide menu coherent and stable where possible. If numbering must change, update `GuideCommandTest`, UG screenshots/examples, Text UI expected output, and all cross-references in the same commit.
- Do not advertise commands that are not actually dispatched and tested.
- At final v2.0 release, there must be no stale "Coming soon" wording for shipped features and no v1.0 status text claiming those features are absent.

# 5. Comprehensive v2.0 QA and regression requirements

The following requirements are mandatory for every applicable v2.0 feature branch. Treat them as part of the implementation definition of done, not as a separate optional testing exercise.

You are an expert Java QA Automation Engineer and Software Architect specializing in JUnit 5 and Text-UI regression testing.

Your task is to write comprehensive, deterministic test suites for UniEnable v2.0 based strictly on the product specification requirements.

### 1. General Testing Rules & Constraints
When writing any test cases or code, you must adhere to these structural mandates:
- **No Real-Time/System Clock**: All tests must use the injectable time source/fixed clock fixture. They must be completely independent of real-world time.
- **No External/Network Dependencies**: All tests must be 100% offline, isolated, and self-contained using synthetic, small fixtures. Never depend on the real NUS dataset or current semester calendars.
- **Determinism**: Ensure no random elements or flaky behaviors exist. Equal-cost tie-breakers must resolve deterministically (e.g., using permanent activity IDs).
- **Isolation & Parallel Safety**: Tests must run safely in parallel and must not leak state. Any temporary files created during storage tests must be automatically cleaned up.

### 2. Implementation Deliverables
For the requested feature, generate the tests in the following structure:

#### A. Unit Tests (JUnit 5)
Provide clean JUnit 5 test classes utilizing standard assertions. Ensure you cover:
- Boundary conditions (e.g., zero-activity handling, week/month/year transitions, exact-fit slots vs. 1-minute-too-short slots).
- Exceptional states (e.g., malformed file lines, missing fields, negative buffer values, disconnected graph nodes).
- Happy paths with exact validation rules.

#### B. Integration Tests (JUnit 5)
Provide multi-component integration workflows proving that:
- Core domains correctly read/write from local human-editable storage without mutational side effects.
- Persistence transaction rollbacks function properly (if a write fails, in-memory state remains completely unchanged).

#### C. Text-UI Regression Batch File
Provide a plain text script matching the UniEnable CLI command structure (e.g., `batch-04-dashboard-timetable.txt`) along with its accompanying `expected` text transcript.

### 3. Immediate Task
Please generate the comprehensive test suites (Unit, Integration, and Text-UI batch scripts) for the following feature area:
[INSERT TARGET FEATURE HERE - e.g., "5. V2-F01 — Dashboard and data analysis" OR "10. V2-F06 — Accessible route search"]

Ensure every edge case listed in Section 16 & 17 of the product spec for this specific feature is accounted for explicitly in your code comments or test assertions. Do not truncate the code.

For the Dashboard feature, you must write specific test cases that explicitly implement and assert the following conditions:
- Unit: 'No activities' period handles potential division-by-zero safely by outputting a helpful message.
- Unit: Perfect completion (100%), absolute zero completion (0%), and fractional mixed completion (e.g., 6/10 or 60%).
- Unit: Fixed vs flexible activity calculations and accurate category grouping counts.
- Unit: Correct mathematical rounding for energy-demand and sensory-load averages.
- Unit: Boundary transitions ensuring future activities are excluded from being counted as incomplete unless explicitly stated.
- Integration: Simulating a malformed line in the activity save file to ensure it is skipped and reported without mutating data or crashing.
- Text-UI: The exact ASCII chart output structure (using '#' and '-') matching the spec example concept.

For the Weekly Timetable feature, you must write specific test cases that explicitly implement and assert the following conditions:
- Unit: Monday through Sunday chronological grouping, ensuring an identical start-time tie-break resolves deterministically.
- Unit: Cross-midnight policy handling (explicitly documenting how activities spanning past 00:00 behave).
- Unit: Flagging and visibly rendering overlapping fixed activities with a clear text overlap warning.
- Unit: Separately listing flexible activities that are currently unscheduled.
- Integration & Text-UI: Verifying that fixed commitments plus adopted recommendations render seamlessly on a plain text terminal layout without omitting any activities.

For the Preference Profile feature, you must write specific test cases that explicitly implement and assert the following conditions:
- Parser: Exception handling for missing values, invalid times (e.g., 25:00), negative buffers, unknown fields, and duplicate markers.
- Storage: Absolute fallback to documented backward-compatible defaults if the local text file is completely missing.
- Command Atomic Rules: Testing a 'set multiple preferences atomically' command, proving that if one setting fails validation, the entire transaction rolls back and leaves existing settings untouched.

For the Schedule Recommender feature, you must write specific test cases that explicitly implement and assert the following conditions:
- Slot Generation: An exact-fit slot vs a slot that is precisely 1 minute too short for the required duration.
- Hard Constraints: Immediate rejection of a candidate flexible slot if the required travel route is explicitly marked inaccessible.
- Soft Rule Ordering: Proving deterministic resolution via the exact sequence: (1) Fewest valid slots, (2) Earlier slots preference, (3) Buffer preservation, (4) High-energy spreading, (5) High-sensory spreading, (6) Preferred daily start/end, (7) Sheltered travel tie-break, (8) Permanent-ID final tie-breaker.
- Adoption Isolation: Asserting that viewing/previewing a recommendation performs zero in-memory or on-disk mutations, and that canceling leaves user data completely unmodified.
For the Route Search feature, you must write specific test cases that explicitly implement and assert the following conditions:
- Graph Validation: Ensuring duplicate nodes, duplicate edges, negative distances, or malformed lines in the reference file do not crash the application.
- Dijkstra Edge Cases: Verifying a direct path vs a longer direct path that loses to a shorter multi-edge indirect path.
- Special Graph Topologies: Successful handling of a zero-length route (source equals destination), completely missing nodes, and a disconnected graph (returning the exact text fallback: "No supported accessible route was found...").

Generate JUnit 5 integration tests that programmatically execute these exact multi-step user scenarios:

### Scenario A: Typical Student Week Integration Workflow
1. Programmatically inject fixed lectures.
2. Add multiple flexible study activities.
3. Apply user profile preferences.
4. Call RecommendationService to generate a deterministic schedule suggestion.
5. Confirm and adopt the proposal.
6. Assert that the Weekly Timetable and Dashboard data update accurately and match.
7. Execute a simulated app restart and verify all adopted data completely persists.

### Scenario B: Accessibility-Aware travel Verification
1. Bind sequential activities to different locations/nodes.
2. Load an explicit synthetic route graph with an inaccessible edge obstacle.
3. Assert that the recommender rejects the flexible candidate attempting to cross that edge.
4. Assert that a critical warning prints for an unsupported route, while keeping the original fixed activity unchanged.

### Scenario C: Atomic Transaction Persistence Failure
1. Prepare a recommendation with several multi-entity file adjustments.
2. Force an IOException on the final file persistence layer.
3. Assert that zero activities changed in-memory and no partial changes leaked onto the storage text files.

## Applying the target-feature placeholder

When working on a specific branch, replace `[INSERT TARGET FEATURE HERE]` with that branch's feature and generate only the tests relevant to the current approved branch. Do not implement or test later branches prematurely, except for narrowly scoped synthetic integration fixtures needed to prove an already-approved cross-component contract.

Store each feature's test-planning material under:

```text
docs/tasks/v2/<feature>/TEST_PLAN.md
```

Store executable tests in the repository's established test-source hierarchy, mirroring production packages. Store Text-UI input and expected-output files in the existing Text-UI regression-test location and follow its naming convention. Do not create a second competing test framework or duplicate test directory tree.

For each test requirement, record traceability from:
- product-spec section and acceptance criterion;
- test method or Text-UI batch name;
- expected outcome;
- implementation status and result.

# 6. Testing and quality gate for every branch

Before declaring a branch complete, run and report:

```text
./gradlew clean test
./gradlew checkstyleMain checkstyleTest
./gradlew textUiTest        # or the repository's actual Text UI command/script
./gradlew shadowJar
./gradlew releaseZip        # if configured
```

Also perform a clean-extraction smoke test using the produced release ZIP/JAR where the feature touches resources or persistence.

Required test layers:

- focused unit tests for new domain/parsing/formatting classes;
- command tests;
- parser/dispatcher integration tests;
- storage/restart tests where persistence is involved;
- rollback tests for state-changing commands;
- injected-clock tests for date-relative behaviour;
- Text UI regression tests for public CLI output;
- full existing regression suite.

Do not weaken, delete, or rewrite existing tests merely to make a new implementation pass. If an old test is genuinely obsolete because of an approved requirement change, explain the requirement change first.

# 7. UML and documentation rules

For every feature, inspect whether the change affects architecture, static design, or runtime interaction.

- **Architecture diagram:** update only when high-level components/dependencies change.
- **Class diagram:** add/update a focused diagram showing important classes, relationships, multiplicities, and public operations—not every private field.
- **Sequence diagram:** show one representative successful workflow and important alternatives such as invalid input, cancellation, no-route/no-schedule, or save rollback where useful.
- Use correct UML arrow directions and activation bars; do not use implementation detail that contradicts code.
- Store both `.puml` and generated `.png` in the existing diagram folders.
- Keep image widths/readability consistent with the current DG.
- The DG must explain design decisions and alternatives; it must not merely paste diagrams.
- The UG must describe user-visible behaviour only and provide copyable commands.
- README files must accurately state the current release and feature set.

# 8. Per-branch completion report

At the end of each branch, stop and provide:

1. branch name and base commit;
2. commits created, with one-line purpose;
3. files added/changed grouped by production, tests, docs, and diagrams;
4. exact implemented command syntax and observable behaviour;
5. design decisions and CS2113 principles applied;
6. test commands and results;
7. compatibility/regression assessment;
8. unresolved risks or decisions;
9. whether the branch is ready for review/merge;
10. confirmation that you did **not** push, merge, tag, or begin the next feature.

# 9. Start now

Perform only the read-only audit first. Compare the actual repository against this plan and the planning documents. Report any conflict, stale requirement, already-implemented foundation, or ambiguous command grammar. Then propose the exact first-branch plan for `feature/v2-route` and wait for approval before editing files.
