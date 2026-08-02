# UniEnable Handover

**Read this whole file before doing anything else.** It is the up-to-date continuity doc for this
project across sessions/tools — commit and push discipline, verification commands, and design
taste the user has been firm about are all in Section 4, and skipping them is the most common way
a new session repeats a mistake an earlier one already made and documented here.

**If you are picking this project up next (including a different tool, e.g. Codex):** v2.0
development has formally started, following
`UNIENABLE_V2_CLAUDE_CODEX_MASTER_PROMPT_UPDATED.md` (supplied 2026-08-02, outside the repo, in
Downloads). `feature/v2-route` (this update) is complete, committed, **not yet merged or pushed**
- it needs review/approval before merging, and before the next feature (`feature/v2-dashboard`
per the master prompt's required sequence) starts. See Section 1 for exactly what's on the branch
and Section 6 for the full v2.0 sequence/status.

## 1. Current state (as of this update)

**On branch `feature/v2-route`, 4 commits ahead of `main` (`59a9c4a`), not yet merged or pushed.**
`main`/`origin/main` are unchanged at `59a9c4a`, clean. `v2-dashboard` remains a stale local
branch with zero unique commits (confirmed again this session) - not touched, not deleted per
explicit instruction.

Commits on `feature/v2-route`, oldest first: `e1a54ac` (production code), `530bd98` (JUnit
tests), `9c4046e` (`text-ui-test`), `3f69338` (docs/diagrams). Implements
`route from/FACILITY to/FACILITY`: Dijkstra shortest path using only confirmed-accessible (`YES`)
connections, via a new `logic.route.AccessibleRouteGraphFactory` that filters
`ConnectionManager`'s list and builds the existing `logic.graph.AccessibilityGraph` (which gained
only a policy-neutral `List`-based constructor overload, not accessibility-status logic - see
`docs/tasks/v2/route/IMPLEMENTATION_NOTES.md` for the alternatives considered and why). Same
source/destination is a successful zero-length result, not an error; a disconnected known pair
gets a "No supported accessible route was found..." fallback that explicitly does not claim no
real-world route exists; an unrecognised facility is a `Not found` error. `GuideCommand` gained
"Route search" as numbered menu item **11** (the next available number - items 1-10 keep their
v1.0 numbers unchanged) and "Return" moved from 11 to 12; `CommandDispatcher`'s bare-number
shortcut switch was extended to match (`case "12"` added). All four of these design points -
where the `YES`-filter lives, the guide numbering, the same-endpoint-is-success decision, and the
no-route fallback wording - were explicit corrections/decisions the user gave in this session,
overriding this session's own initial (pre-correction) proposal on two of them (same-endpoint and
the graph-filter location); see `docs/tasks/v2/route/README.md`'s "Approved design decisions"
section for the full list with rationale.

**Verification (commit `3f69338`):** `./gradlew clean test checkstyleMain checkstyleTest` all
green, **950 JUnit tests** (up from 887 - 63 new, zero deleted/weakened), checkstyle clean (main +
test). `bash text-ui-test/runtest.sh` passes - `input.txt`/`EXPECTED.TXT` updated for new `route`
scenarios (against the real bundled dataset) and the guide 11/12/13 numbering ripple; every
changed line was diffed against the prior `EXPECTED.TXT` and confirmed intentional before
promoting, per Section 4's standing procedure. `./gradlew releaseZip` builds; a clean-extraction
smoke test (fresh temp folder, `java -ea -jar unienable.jar`, no repo `data/`) exercised `route`'s
direct/multi-hop/zero-length/unknown-facility/malformed-syntax paths plus `guide`/`bye` and all
matched expectations. `./gradlew javadoc` not re-run this session - flag if trusting a javadoc
claim, re-run before release.

**One real mistake this session, caught before it shipped, worth knowing about:** an early
implementation plan assumed the bundled `connections.txt`'s `AS1-AS2` connection (130 m, `RAMP`)
had `accessibility == NO`, and built a `text-ui-test` no-route-fallback scenario around it. Running
the actual harness showed a normal 130 m route instead - the connection's *shelter* status is
`NO`, not its accessibility (every bundled connection is `accessibility == YES`, confirmed by
re-reading `ConnectionStorage.parseLine`'s field order). Fixed by correcting the `text-ui-test`
scenario (now documents a real route whose one segment has both a barrier and shelter-`NO` notes)
and the design-notes/test-plan docs that had repeated the wrong claim - the no-route fallback
itself is still fully covered, just by synthetic JUnit fixtures instead of the real dataset (which
happens to have no `NO`/`UNKNOWN` edge to exercise it against). See
`docs/tasks/v2/route/IMPLEMENTATION_NOTES.md`'s "Bundled dataset detail" section for the corrected
account. **Lesson: when a connections.txt/facilities.txt field-position claim matters for a test
scenario, re-verify against `ConnectionStorage`/`FacilityStorage`'s actual parse order before
trusting a quick manual read of the pipe-delimited fields - it is easy to miscount which field is
which.**

Two diagram PNGs (`RouteClassDiagram.png`, `RouteSequence.png`) were rendered via the public
`www.plantuml.com` render service (Python: deflate-compress the `.puml` source, PlantUML's custom
base64 variant, GET the PNG) since no local PlantUML/Graphviz toolchain was found in this
environment - downloading them was flagged by the permission classifier as a file download and
explicitly confirmed with the user before fetching (matches the standing "explicit permission for
any file download" rule). `ArchitectureDiagram.png` was re-rendered the same way after a small,
justified update (one new `AccessibleRouteGraphFactory` component in the "Business logic"
package, since it's a real new dependency `Commands` now has, at the same granularity as the
existing `RecurrencePlanner` entry) - not a structural change to the diagram's package boundaries.

**Everything below this point, through the end of the "Verification at that earlier point (commit
`a4aa683`...)" paragraph, describes the state as of `804ce09` — the state at the start of this
session, before `feature/v2-route` (Section 1's opening paragraphs above). Kept as historical
record, per this file's established rolling convention, with one correction: point 4 of the
numbered list just below says the `ActivityConflictChecker` Phase-1 review is "the next task" —
that review happened and Phase 2 (the actual extraction) has since shipped directly on `main` at
`e44f660` ("refactor: extract activity conflict validation from ActivityManager"), in a session
this revision has no other record of. `logic.ActivityConflictChecker` exists in the current
codebase and Section 14 of `docs/DeveloperGuide.md` documents it, confirming it's real, not
aspirational.**

Since the previous revision of this handover (`a4aa683`, mid-review, not yet pushed), three more
small hardening batches landed and were pushed, each reviewed and approved before pushing, per the
user's standing "stop and report before pushing" instruction — full detail in Section 2's three
newest bullets:
1. Three isolated polish fixes flagged by the earlier review but deferred as out-of-scope for it:
   `serialVersionUID` on all 8 exception classes, one genuinely-misleading boolean method name,
   and `DateTimeParser.parseTime()`'s "wrong shape vs. out-of-range" message split (mirroring the
   equivalent `parseDate()` fix from a much earlier session).
2. Extended `ApplicationRunner`'s mutation-rollback mechanism (previously recur/reset-only) to
   **every** mutating command, after an audit found several commands - most seriously `topic
   rename`, which can cascade to an unbounded number of activities - had no rollback at all on a
   failed save.
3. Added Java assertions for a small set of internal programmer invariants, plus file-based
   logging (previously: none) for storage/rollback failures.

**No user-visible command syntax or successful-path output changed in any of the three batches.**
Test count grew from 867 to **887** as each batch added focused regression coverage; checkstyle
and javadoc stayed clean throughout (same 100 pre-existing javadoc warnings, zero new, in every
verification run across all three batches).

**Verification as of this handover (commit `804ce09`):** `./gradlew clean check` all green, **887
JUnit tests**, checkstyle clean (main + test), `./gradlew javadoc` succeeds with the same 100
pre-existing warnings as always, zero new. `text-ui-test/runtest.sh` passes. This was verified
separately at the end of each of the three batches before that batch was pushed (per the user's
standing "stop, report, wait for approval, then push" instruction — Section 4) — not re-verified
freshly for this documentation-only update, since no production code changed while adding
`docs/planning/` and editing this file. Re-run the full verification block (Section 4) before
trusting this claim if picking up work after any gap.

**New this update:** four external reference documents were added to the repo under
`docs/planning/` (previously kept outside the repo, in Downloads) so a different tool (Codex) can
read them without access to the user's local filesystem — see Section 6's last paragraph for what
each one actually is and, importantly, what it is **not**: three of them describe an earlier/wider
"Accessible Itinerary Planner" concept (dashboard, timetable, preferences, recommend, route,
export) that is **not** UniEnable's current approved scope - historical/traceability reference
only, not a task list. Only the fourth, `UniEnable_ActivityConflictChecker_Review_and_Extraction.
md`, is an actual work order: a read-only Phase-1 design review of whether `ActivityManager`'s
conflict-validation logic is worth extracting into a dedicated `ActivityConflictChecker` class,
which must stop and report before any Phase-2 implementation begins. **(As corrected above: both
phases are now done, at `e44f660`, before this session started - this was "the next task" as of
the `804ce09` revision this paragraph describes, not as of now.)**

The previous revision of this handover described what happened up to `a4aa683` — still accurate
history, kept below, prefixed with what came immediately before it:
1. `command.activity` and `command.accessibility` were split into responsibility-based
   sub-packages (`crud`/`general`, `facility`/`connection`/`common`) on
   `refactor/command-package-reorg`, merged into `main`.
2. A real CI failure (a checkstyle `MethodName` violation in a just-added test) was found and
   fixed directly on `main`, alongside finally committing this very `HANDOVER.md` file (left
   uncommitted on purpose in the previous revision).
3. A release JAR was built, relocated to
   `C:\Users\lukel\Downloads\AY2627_Sem1 Prep\CS2113\jar\v1.0.0\`, and put through the full 14-batch
   manual regression pass from `UniEnable_Post_Recur_ListNextWeek_Regression_Batches.md` (supplied
   in Downloads) — see `C:\Users\lukel\Downloads\AY2627_Sem1 Prep\CS2113\jar\v1.0.0\
   REGRESSION_REPORT.md` for the full batch-by-batch report. All 14 batches passed their
   documented checkpoints; 3 minor, non-functional findings turned up (2 message-clarity bugs, 1
   documentation overclaim) plus one new permanent JUnit test (`RecurNextWeekIntegrationTest`,
   B09 coverage — no existing test combined recur output with `list next week` under an injected
   clock).
4. All 3 findings from that report were fixed and pushed (2 small commits): the `list` relative-
   date rejection messages (stray trailing space on bare `next`/`this`; a specific message instead
   of a generic one when two relative-date phrases are combined, e.g. `list next week today`), and
   the User Guide's overclaim that every calendar error names a line number (only per-line checks
   can; the two whole-file structural checks — duplicate key, overlapping ranges — can't).

Also fixed along the way: `text-ui-test`'s "Far past overdue" scenario embedded the real
wall-clock date in its expected output (add's "date has passed... from `TODAY` onwards" message),
making `EXPECTED.TXT` silently go stale every day. Removed — the identical scenario is already
covered deterministically with an injected clock in `DateTimeParserTest`/`ActivityCommandParserTest`.

**The v1.0 git tag has not been created.** The user's condition was "tag v1.0 if no bugs found";
3 were found (all now fixed, all minor/non-functional) — flagged rather than auto-tagging, and the
user hasn't given an explicit go-ahead since the fixes landed. This is the most likely next
question a new session will be asked to resolve.

Any standalone `unienable.jar` copied out of the repo before this session predates recur entirely
and is stale. **The distributable is `./gradlew releaseZip`'s `build/distributions/unienable.zip`**
(jar + external `data/academic-calendar.txt`), not a bare jar — see the User Guide's Quick Start
and README's "Distribution" section. A copy already sits at
`C:\Users\lukel\Downloads\AY2627_Sem1 Prep\CS2113\jar\v1.0.0\unienable.jar` (built from `5988dcf`,
*before* the 3 message-quality fixes in point 4 above — rebuild if you need a JAR matching the
exact current `main` tip).

**Verification at that earlier point (commit `a4aa683`, historical, superseded by the current
verification above):** `./gradlew clean check` all green,
**867 JUnit tests** (863 + 4 new delegation-smoke tests from the parser-test split), checkstyle
clean (main + test), `./gradlew javadoc` succeeds with the same **100 pre-existing warnings** as
before this session — all in files this session didn't touch, all the permitted-omission kind
(missing `@return`/enum-constant comments), zero new warnings from any of the 7 commits.
`text-ui-test/runtest.sh` passes. `releaseZip` not re-run this session — none of the 7 commits
touch command syntax, output, or the release build itself (`build.gradle`'s only change was
adding explicit `sourceCompatibility`/`targetCompatibility`, additive), so this is low-risk, but
worth a fresh run before trusting a new release artifact or before tagging v1.0.

## 2. Condensed history (compressed — see `git log` for full detail)

Most recent sessions, newest first:

- **Assertions + logging batch** (2 commits, `797e29f`/`804ce09`, explicitly scoped to exclude
  `ActivityConflictChecker` decomposition, date/time parser changes, and new features): added Java
  `assert` statements for a small set of internal programmer invariants that should never be false
  if callers are behaving correctly (e.g. `FixedActivity`/`FlexibleActivity` constructor
  preconditions — `endTime.isAfter(startTime)`, duration fits the flexible window — see the two
  files' current content; these run only with `-ea`, which `build.gradle`'s `test {}`/`run {}` now
  both enable, with a comment documenting `java -ea -jar unienable.jar` for a manually-run jar).
  Built `app/LoggingConfig.java` (new, package-private): `configure(Path)`/`shutdown()` static
  methods, tracking *only* the single `FileHandler` it adds — deliberately not touching the root
  logger's other handlers, since an earlier version that did caused ~60 test failures (Windows
  couldn't delete the `@TempDir` because the log file handle was still open; see the `finally {
  LoggingConfig.shutdown(); }` wrapper this pushed into `ApplicationRunner.run()`). Added logging
  points at storage/rollback failure sites (`Storage.restore()` now logs `SEVERE` on an on-disk
  rollback failure) and trimmed field-mutation logs in `FixedActivity`/`FlexibleActivity` for
  privacy (log that a field changed, not the value). `ApplicationRunner.processCommand()` gained an
  entry assertion plus a `catch (RuntimeException e)` boundary, accepted as a defensive safety net,
  not a replacement for specific exception handling elsewhere. Checkstyle and javadoc clean, no
  user-visible output changed; this batch brought the running total to the 887 tests cited in
  Section 1.
- **Mutation persistence and rollback fix** (2 commits, `168eef1`/`d5a22b4`): extended
  `ApplicationStateSnapshot` rollback — previously wired only into `recur`/`reset` — to all 11
  mutating commands, after an audit found the others (most seriously `topic rename`, which cascades
  a name change across every activity under that topic) had no rollback at all on a failed save,
  meaning a failed persist could leave in-memory state ahead of what's on disk. Along the way, found
  and fixed a real pre-existing bug in the snapshot mechanism itself: it used to take snapshots via
  `List.copyOf(...)`, which only copies the *list*, not the mutable `Activity`/`Topic` objects
  inside it — since `mark()`/`unmark()`/`setName()`/topic-rename's cascade all mutate those objects
  **in place**, a snapshot taken before such a command would "restore" objects that were already
  mutated, silently defeating the rollback. Fixed with new `copyActivities()`/`copyOf(Activity)`/
  `copyTopics()` helpers that build genuinely independent copies. Also fixed `reset`'s no-op case
  (nothing to reset) to skip persistence entirely rather than writing an unchanged file. Two
  existing `ApplicationRunnerTest` tests (recur, reset) used an always-fails storage double for both
  their setup `add` and the command under test; once `add` itself started rolling back correctly,
  the setup `add` never persisted and broke the tests' premise — redesigned the double into
  `FailAfterStorage(dataDirectory, successesAllowed)` (succeeds N times, then fails) to fix. Also
  removed a `previouslyUnsaved` field that used to restore `hasUnsavedChanges` back to its
  pre-command value on rollback — that erased the fact a save had just failed, breaking `bye`'s
  "could not be saved" farewell message; `hasUnsavedChanges` now correctly stays `true` after a
  rollback until a later successful save clears it. Checkstyle/javadoc clean, no user-visible
  successful-path output changed; added focused regression coverage for the extended rollback.
- **Defensive-programming cleanup** (3 commits, `27746b8`/`da6e65c`/`8240a8f` — three items flagged
  by the earlier consolidated review, Section 2's next bullet, but deferred as out of scope for it
  at the time): added `serialVersionUID` to all 8 classes in `seedu.unienable.exception`; renamed
  `ListCommandParser`'s boolean-returning `parseViewMode()`/its `detail` local to predicate-style
  names; split `DateTimeParser.parseTime()`'s single "must be in HH:mm format" message into a
  wrong-shape case and a separate right-shape-but-out-of-range (`25:00`, `10:99`) case, mirroring
  the equivalent `parseDate()` split from an earlier session (Section 2's `fix/v1.0-guide-and-
  date-bugs` bullet below). No user-visible successful-path output changed; test count grew with
  new regression coverage for the message split.

- **Hardening-plan Phase 0 + parser/storage refactor + consolidated review** (7 commits directly
  on `main`, `567116f`..`a4aa683`, per an externally-supplied hardening-plan document driving the
  session): ran a Phase-0 baseline audit first (build/test/checkstyle/javadoc all green, release
  ZIP smoke-tested from a clean folder including calendar-unchanged/restart-persistence checks),
  which surfaced a small set of concrete findings, each turned into its own commit:
  1. `build.gradle` declares explicit Java 17 `sourceCompatibility`/`targetCompatibility` — the
     version constraint was previously enforced only by CI, not a local build.
  2. Removed the unedited AB3-template `CONTRIBUTORS.md` (unrelated se-edu maintainer names,
     referenced nowhere) — `docs/AboutUs.md`/`docs/team/lukelouyu.md` are the real contributor
     pages.
  3. Consolidated six small parsing helpers (`requireField`, category/status enum parsing,
     `validateNoDelimiter`, the "no arguments"/"no unrecognised leading text" guards,
     `extractPresentFields`) that had been copy-pasted near-identically across
     `ActivityCommandParser`/`TopicCommandParser`/`FacilityCommandParser`/
     `ConnectionCommandParser`/`CommandDispatcher` into `parser/common/FieldParser`, which already
     existed for exactly this. No behavior change (verified by every existing parser test passing
     unmodified).
  4. **Split `ActivityCommandParser`** (was ~800 lines covering all ten activity commands) into a
     thin router plus four new package-private classes for the commands with real grammar -
     `AddCommandParser`, `EditCommandParser`, `ListCommandParser`, `FindCommandParser` - matching
     the already-split `command/activity` packages. Delete/mark/unmark/view/next/order stayed
     inline in the router since each is just a bare ID, no arguments, or a trivial two-branch
     sub-command - deliberately **not** applying the split mechanically to every command. This
     **revises** the `refactor/command-package-reorg` session's decision (recorded further below)
     that `parser` should stay flat while `command` split - that decision is superseded for
     `parser.activity` specifically, on explicit user approval; `parser.topic`/`parser.accessibility`
     were deliberately left flat (each is small and cohesive enough that splitting further would
     just be tiny classes for no reason, the exact thing the hardening plan's own guidance warns
     against). `ActivityCommandParser`'s public method signatures are unchanged, so
     `CommandDispatcher` needed zero changes, and the entire pre-existing test suite passed against
     the new structure with zero modifications - the strongest available evidence that no behavior
     moved. A handful of small parsing/validation helpers whose rules are genuinely shared across
     2+ of the new classes (`blankToNull`, `parsePositiveInt`, `validateDurationFitsWindow`,
     `validateTopicExists`, `parseActivityOrder`, the `ALL_ACTIVITY_MARKERS` constant) stayed
     package-private in the router rather than being duplicated per class or pushed into a generic
     `ParserUtils` - reviewed afterward and judged not a "dumping ground" since every one is
     activity-domain-specific with 2+ real callers, not a generic grab-bag.
  5. **Decoupled `ActivityStorage` from `parser.common`.** `ActivityStorage` imported
     `parser.common.DateTimeParser`/`RatingParser` to parse persisted date/time/rating fields -
     storage reaching into the CLI-parsing package tree, the specific smell the hardening plan's
     package-dependency audit flags (§8.4). Corrected wording, checked during the later review:
     **there was no literal circular Java import** - `parser.common` itself never imported
     `storage`, so this was one-directional coupling crossing a responsibility boundary, not an
     actual A→B→A cycle. Gave `ActivityStorage` its own local `parseDate`/`parseTime`
     (same yyyy-MM-dd/HH:mm shapes, same `STRICT` resolver behavior, same exception types/messages)
     and `parseEnergyRating`/`parseSensoryRating` (delegating range validation to
     `EnergyRating.of`/`SensoryRating.of`, same as `RatingParser` did). `storage/` now imports
     nothing from `parser/` at all. **Known, accepted trade-off, not yet acted on:** the
     `parseDate`/`parseTime` formatter/resolver/shape-regex logic is now genuinely duplicated
     between `DateTimeParser` and `ActivityStorage` (the rating helpers are not - both sides still
     delegate the one thing worth centralizing, range validation, to
     `EnergyRating.of`/`SensoryRating.of`). Flagged in Section 5 as a live risk rather than fixed
     immediately, since fixing it means a genuine architectural call (where would a neutral
     shared date/time-format parser live?) that wants its own explicit go-ahead, not a
     reflexive re-fix.
  6. **Split `ActivityCommandParserTest`** (2152 lines, 157 tests in one file) to mirror the new
     production structure: `AddCommandParserTest`/`EditCommandParserTest`/`ListCommandParserTest`/
     `FindCommandParserTest` each test their command-specific class directly (48/34/40/17 tests,
     moved verbatim, zero duplicated or dropped); `ActivityCommandParserTest` kept its 18 tests for
     the six inline commands plus 4 new smoke tests (one per delegated command) proving the router
     wiring itself. 867 tests total.
  7. **Consolidated review pass** (a second supplied document) re-examined all of the above before
     allowing a push: confirmed via direct string-literal diffing (not just passing tests) that zero
     command syntax/message text changed across the parser split; confirmed `FieldParser` stayed
     cohesive (field-extraction mechanics plus a handful of small, genuinely-2+-caller domain-value
     parsers, not a dumping ground); confirmed no `storage→ui`/`model→parser`/`model→ui`/`logic→ui`
     dependencies exist; ran `./gradlew javadoc` and confirmed the same 100 pre-existing warnings,
     zero new ones. Found and fixed two small items directly: the four new command-parser classes'
     `parse()` methods were marked `public` on a package-private class (misleading - tightened to
     package-private); `AddCommandParser`'s `requireField` Javadoc cited a specific bug ID and test
     date, the only such reference in all of `src/main` - trimmed the traceability tag, kept the
     actual rationale. **Not pushed as of this handover** - awaiting an explicit go-ahead in the
     same session.

- **Post-recur bug-fix pass** (2 commits directly on `main`, `83eb6b6`/`2cff382`, per explicit
  user request "fix the bugs in this report" after being shown `REGRESSION_REPORT.md`): fixed the
  3 findings from the 14-batch regression pass below. `docs/UserGuide.md` no longer claims
  unconditionally that calendar errors name a line number (only per-line ones can).
  `ActivityCommandParser.extractRelativeDate()`: bare `list next`/`list this` used to render as
  `Unknown list option "next "` (literal trailing space baked into the string, unconditionally
  appended even with nothing to append) — now conditional. Combining two relative-date phrases
  (`list next week today`) used to fall through to the generic `Unknown list option "today"`
  message (the leading phrase was already consumed before the check ran) instead of a specific
  one, unlike combining with `date/` which already had one — both cases now get:
  `"today, tomorrow, this week, next week, and overdue cannot be combined with each other."` 5
  new regression tests lock in both fixes plus the still-generic case for genuinely unrelated
  trailing text. 863 tests, checkstyle clean, `text-ui-test` unaffected.
- **Release-JAR regression pass** (no branch — ran against the built jar, not the repo directly;
  fixes from it are the bullet above): built `./gradlew releaseZip`, copied
  `unienable.jar`+`data/academic-calendar.txt` to
  `C:\Users\lukel\Downloads\AY2627_Sem1 Prep\CS2113\jar\v1.0.0\`, then ran all 14 batches from
  `UniEnable_Post_Recur_ListNextWeek_Regression_Batches.md` (supplied in Downloads) in fresh
  scratch folders, per that file's own "testing task first, do not fix mid-batch" rule. Full
  batch-by-batch report at `...\jar\v1.0.0\REGRESSION_REPORT.md`. All 14 passed their documented
  checkpoints (sparse/mixed/full-semester recurrence, cancellation/idempotency, parser/eligibility
  rejection, atomic conflict detection, independent-occurrence management, the three reset
  options, previously-fixed-bug regression, restart persistence, and 10-step external-calendar
  single-source-of-truth checks including a synthetic AY2027/2028 term and Week-14 add/remove).
  B09 (clock-controlled `recur`+`list next week` integration) couldn't run against the real JAR
  since its wall clock can't be frozen — covered instead with a new permanent test,
  `RecurNextWeekIntegrationTest`. Found the 3 findings fixed in the bullet above; no repo file was
  touched by the batches themselves (all scratch-folder work).
- **`refactor/command-package-reorg` + CI hotfix** (merged into `main` at `5988dcf`, then two more
  direct-to-`main` commits `f77c1fa`/`d41c9dc`): split `command.activity` (11 classes) into
  `command.activity.crud` (Add/Delete/Edit/View) and `command.activity.general` (Find/List/Mark/
  Next/OrderSet/OrderView/Unmark), and `command.accessibility` (10 classes) into
  `command.accessibility.facility`/`.connection` (4 each) plus `.common` (`AccessibilityDisclaimer`,
  `ValidationReportFormatter` — the latter promoted package-private→public, a pure visibility
  change, since its two callers now live in sibling packages). `command.topic` (4 classes) and
  `command.recur` (1 class) deliberately left flat, per explicit user confirmation, since neither
  has enough classes to justify sub-packages. Also duplicated `data/academic-calendar.txt` into
  `src/main/resources/` (excluded from `shadowJar` via `exclude 'academic-calendar.txt'`, so the
  jar-absence guarantee from the original approved recur spec still holds) and updated the
  Developer Guide's package-layout description. Separately, CI caught a `checkstyleTest`
  `MethodName` violation in `ApplicationRunnerTest` (a just-added test method had 4
  underscore-separated segments; the rule allows 3) — fixed directly on `main`, and `HANDOVER.md`
  (left uncommitted in the previous revision, at the user's explicit request at the time) was
  finally committed alongside it. A concurrent README edit made directly on GitHub (removing the
  "simulated CS2113 team project" sentence) was merged in cleanly with no conflicts.
- **`feature/recur-reset-v2`** (merged into `main`, per explicit user instruction to
  merge and push): implemented the full "3b" spec the previous handover's Section 3 described —
  `recur TASK_ID week WEEK_SPEC`, the strictly-validated external `data/academic-calendar.txt`
  loader, and the three-option `reset all` menu (`[1]` delete all / `[2]` keep class schedules /
  `[3]` cancel). Most of the production code, tests, and package layout (`command.recur`,
  `logic.recur`, `model.recur`, `parser.recur`, `storage.recur`, `ui.recur`,
  `command.MenuConfirmable`/`MenuOutcome`) was already written and uncommitted on this branch
  before this session started — this session reviewed it file-by-file against the spec (no defects
  found), then closed the remaining gaps: wired the previously-missing `guide recur` topic and
  rewrote the stale `guide reset` topic in `GuideCommand`; added full `recur`/reset-v2 coverage to
  `text-ui-test` (parser errors, eligibility rejection, successful multi-week creation, cancel,
  idempotency, individual mark/edit/delete isolation, atomic conflict rejection, no-class skip)
  against a synthetic `text-ui-test/academic-calendar-test.txt` fixture (not the real, date-bound
  calendar); updated `README.md`/`docs/UserGuide.md`/`docs/DeveloperGuide.md` (new §6.12 `recur`,
  rewritten §6.11 `reset all`, a second DG sequence diagram, new design-consideration bullets, new
  glossary terms); and added `ApplicationRunnerTest` covering the save-failure rollback
  (`ApplicationStateSnapshot`) for both `recur` and `reset`, which previously had a constructor
  built for exactly this test but no test actually using it. Final state: 857 JUnit tests,
  checkstyle clean, `text-ui-test` passes, `./gradlew releaseZip` produces the correct
  jar+calendar ZIP layout with the calendar confirmed absent from the jar, and a full manual smoke
  test against a clean ZIP extraction (sparse recurrence against the real calendar, restart
  persistence, idempotency, all three reset options, missing/malformed calendar, and a real forced
  storage failure) all passed. No production logic was changed by this session — only the guide
  topics, test coverage, and documentation were added on top of the already-correct implementation.
- **`fix/technical-debt-hardening` + `fix/v1.0-manual-test-batch-2026-08-01`** (merged into `main`
  in a prior session not captured by this handover's previous revision): shipped the "3d" spec —
  `parser.common.ArgumentTokenizer`/`ArgumentMarker` as a declarative alternative to `FieldParser`
  for future complex-grammar commands (no v1.0 parser migrated to it); `command.Confirmable`
  replacing the `instanceof` chain in `CommandConfirmationHandler`; `logic.graph.AccessibilityGraph`
  as a non-breaking Dijkstra prep layer for v2.0's `route`, not wired into any command yet;
  `facility validate`/`connection validate` re-running existing load-time checks on demand
  (`facility reset-default` was the item's optional stretch goal and was **not** implemented). Also
  fixed BUG-01 (guide menu item 7 → facility/connection numbering), BUG-02/03 (reject a start time
  at/before now on today's date), BUG-04 (report the actually-missing marker), BUG-05 (reject 3+
  word `find k/` keywords), INVESTIGATION-01 (mixed rejected adds never consume an ID), and added
  FEATURE-01/02 (`list overdue`, `list next week`).
- **`fix/v1.0-guide-and-date-bugs`** (merged into `main` at `c148220`): fixed two bugs. (1)
  `guide facility` used to describe both facility *and* connection commands while
  `guide connection` returned "No guide topic named"; split into two independent topics (each
  keeping the `AccessibilityDisclaimer.TEXT` disclaimer), and menu item 7 now resolves to a new
  `accessibility` overview topic instead of straight to `facility`, so the numbered mapping still
  agrees with its own displayed description. (2) `DateTimeParser.parseDate()` used to report the
  same "must be in yyyy-MM-dd format" message for a wrong-shape string (`"2026-08:15"`) *and* a
  right-shape-but-nonexistent date (`"2026-02-30"`); now a regex shape check runs before calendar
  resolution, giving each its own message. New `DateTimeParser.parseNotBeforeDate(date, today)`
  additionally rejects a valid date earlier than `today`, threaded through the existing
  `dispatch(input, now)` seam into `add`/`edit` only (not `list`/`find` filters, not storage
  loading, both of which must keep accepting genuinely past dates) — validated before
  `AddCommand`/`EditCommand` is built, so a rejected `add` never consumes an ID and a rejected
  `edit` never reaches the confirmation prompt.

  **Known fragility this introduced, still live and worth knowing about:** `UniEnableTest.java`
  has ~60 pre-existing scenarios that `add` an activity with a hardcoded near-date (e.g.
  `date/2026-08-15`) purely as an arbitrary placeholder. Since `add` now rejects any date before
  the real wall-clock `today`, **every one of those tests will start failing once real time passes
  the hardcoded date it uses.** This is inherent to the fix working correctly, not a defect in it,
  but the test suite now has an expiry date it didn't have before. Don't "fix" this by injecting a
  full `Clock` DI seam unprompted (explicitly deferred territory, see Section 5); if it starts
  failing in CI, the fix is almost certainly "bump the hardcoded dates forward." Flag it to the
  user rather than silently patching it preemptively.

- **`fix/v1.0-rc-retest-2026-08-01`** (merged): fixed RC01–RC06 from an external retest report —
  `Storage.saveAll()` now backs up and rolls back every destination if a later commit in the
  sequence fails (not just cleans up temp files); `topics.txt`/`facilities.txt`/`connections.txt`/
  `activities.txt` now reject blank fields, wrong field counts, and duplicates at load time; every
  command parser (`add`, `find`, `edit`, `topic add/list/rename/delete`, `facility find`,
  `connection find`) now rejects unrecognised leading text via a new shared
  `FieldParser.leadingUnrecognisedText()` helper instead of silently discarding it; stale
  test-coverage claims in README/portfolio docs corrected.

- **`feature/v1.0-hardening-session2`** (merged): added `reset all` (with confirmation preview,
  skip-when-nothing-to-reset, and atomic multi-file save via the same `Storage.saveAll()`
  mechanism above), `list today`/`tomorrow`/`this week` (relative dates threaded through
  `dispatch(input, now)`, no new clock call site), completed the built-in `guide` for every
  implemented v1.0 command, added remaining load-time storage validation (activity topic
  cross-check, connection/facility positivity and duplicate checks), and documented that activity
  IDs are permanent (already true in code, just undocumented).

- **Earlier sessions** (`fix/v1.0-release-audit`, `refactor/application-runner`, and further back):
  see `git log` — persistence/save-before-success ordering, `ApplicationRunner` extraction, B01–B08
  bug-fix session, real Developer/User Guide content replacing AB3 template stubs. Only the
  *lessons* matter now; captured in Section 4 below.

## 3. Completed: recurrence + reset v2 (was "next session's task", now shipped — see Section 2)

The previous revision of this handover carried the full verbatim text of three planning documents
supplied 2026-08-01 evening (`claude_prompt_addup.md`,
`UniEnable_Approved_Recurrence_Implementation_Prompt.md`, `academic-calendar.txt`, and an
unverified candidate zip `UniEnable-c148-recurrence-reset-patched.zip`) as forward-looking spec for
three workstreams: 3a (the candidate zip — explicitly flagged as untrusted, never opened or
merged), 3b (recurrence + three-option reset — the actual feature spec), 3c (a documentation-update
strategy contingent on 3d landing first), and 3d (technical-debt hardening). All of that is now
either done or superseded:

- **3d (technical-debt hardening)** shipped in a prior session — see Section 2's
  `fix/technical-debt-hardening` bullet.
- **3c (post-hardening documentation)** was folded into 3d's own session (the Developer/User
  Guide/README updates documenting `Confirmable`, `logic.graph`, and the validate commands already
  happened together with 3d, per Section 2).
- **3b (recurrence + three-option reset)** shipped this session — see Section 2's
  `feature/recur-reset-v2` bullet for exactly what was implemented, tested, documented, and
  verified. The full original spec text is preserved verbatim in
  `UniEnable_Approved_Recurrence_Implementation_Prompt.md` in the user's Downloads folder (outside
  this repo) if a future session needs to re-check an implementation detail against it; it is not
  reproduced here again since the feature is done and the spec's authority has been superseded by
  the shipped code, tests, and docs themselves.
- **3a (the candidate zip)** was never opened, reviewed, or merged, by explicit design (it was
  flagged untrusted from the start) — the actual implementation on `feature/recur-reset-v2` was
  independently written and independently verified against the spec, not sourced from that zip.
  The zip can be deleted from Downloads; nothing in this repo depends on it.

If a **new** academic year needs to be added later (e.g. AY2027/2028), that is a `data/
academic-calendar.txt` edit plus an application restart — **not** a Java change. See the User
Guide's §11 (Data Storage) for the exact record schema.

## 4. Working conventions (still apply, established over many prior sessions)

- **Commit discipline:** small commits, one concept each. When two independent fixes are requested
  as **separate commits** but implemented in the same working-tree pass, split them properly
  before committing — save the diff for one, `git checkout main -- <its files>` to get a clean
  state for the other, build + regenerate `text-ui-test` against *that*, commit, then re-apply the
  first change on top, regenerate again, commit. `text-ui-test`'s `EXPECTED.TXT`/`input.txt` are
  the trickiest part to split since both fixes' output can land in the same file — verify the diff
  really is a clean, non-overlapping union before trusting a same-file split.
- Run `./gradlew test checkstyleMain checkstyleTest` (and usually `bash text-ui-test/runtest.sh`)
  before every commit.
- **Push discipline:** never push without the user explicitly asking in that turn; never merge
  without explicit approval either. True every session so far — commit on a branch, report, and
  wait, unless told otherwise. (`feature/recur-reset-v2` is the one exception on record: the user
  explicitly asked in-turn for the test to be added, `HANDOVER.md` updated, changes committed, and
  the branch merged and pushed, all in the same message — so all four happened together. This
  doesn't loosen the standing default for future sessions; keep asking first.)
- **`text-ui-test` changes:** after editing `input.txt`, regenerate `ACTUAL.TXT` for real (`bash
  text-ui-test/runtest.sh`), diff it against the previously committed `EXPECTED.TXT` to isolate
  exactly what changed, confirm every line of drift is *intentional*, and only then copy
  `ACTUAL.TXT` over `EXPECTED.TXT`. Never hand-write expected framed output. Anything depending on
  the real current date cannot go through this harness deterministically — keep those as JUnit
  tests with an injected fixed `now`/`today` instead.
- **Verification commands** (run all before considering any task done):
  ```bash
  ./gradlew clean test checkstyleMain checkstyleTest
  bash text-ui-test/runtest.sh
  ./gradlew releaseZip
  ```
  `text-ui-test/runtest.sh`/`.bat` now also copies `text-ui-test/academic-calendar-test.txt` (a
  small synthetic fixture, not the real calendar) into `data/academic-calendar.txt` before each
  run, so `recur` scenarios stay deterministic. `releaseZip` (not a bare `shadowJar`) is now the
  actual distributable — see Section 1.
  Then a manual smoke test against a **fresh `data/` folder** created by the built JAR (not the
  repo's tracked `data/`) — e.g. `mkdir -p /tmp/x/data && cd /tmp/x && printf '...\n' | java -jar
  path/to/unienable.jar`. The app resolves `data/` relative to the current working directory
  (`Paths.get("data")` in `UniEnable.java`), so fixture files must be placed under a `data/`
  subdirectory of wherever you `cd` to, not the working directory itself — this has tripped up a
  verification attempt before (malformed fixtures written directly into the temp dir were silently
  ignored in favour of freshly-copied bundled defaults, because `data/` itself didn't exist yet and
  got created fresh).
- **When a manual/smoke test surfaces an unplanned bug mid-task:** stop and ask whether to fix now
  or defer, with a concrete reproduction against the real JAR.
- **Design taste the user has been firm about:** no AB3-style generic `Prefix`/`ArgumentTokenizer`
  classes was the original standing preference — `parser.common.ArgumentTokenizer`/`ArgumentMarker`
  (technical-debt hardening) and `command.MenuConfirmable` (recur/reset-v2) are both deliberate,
  explicit exceptions the user approved for specific, scoped reasons, not contradictions to flag if
  seen; prefer reusing existing exception categories over introducing new ones (recur reuses the
  existing flat `exception` hierarchy, no new types); mutable model fields with getter/setter (not
  immutable value objects) for `Activity`/`Topic`, though the accessibility model and `model.recur`
  (`AcademicCalendar`/`AcademicWeek`/`NoClassDate`/`RecurrencePlan`) are both immutable by design,
  since both are either read-only reference data or a disposable planning result; each
  parser/storage class stays self-contained with its own small private helpers rather than sharing
  them broadly — but a *small*, narrowly-scoped shared helper (like
  `FieldParser.leadingUnrecognisedText()`, `DateTimeParser.parseNotBeforeDate()`, or
  `logic.recur.ClassSchedulePolicy`, deliberately shared by `recur` and reset's "keep class
  schedules" option so they can't silently disagree) is fine when explicitly scoped that way;
  confirmation handling moved from a plain `instanceof` chain to `command.Confirmable` (technical
  debt hardening) with `command.MenuConfirmable` added alongside it, not replacing it, for the
  reset menu's more-than-two-outcomes shape (recur/reset-v2) — both were explicit, approved
  requests, not unprompted redesigns. **Revised, explicit exception as of the hardening-plan
  session:** `parser.activity` is no longer flat — `ActivityCommandParser`/`FacilityCommandParser`/
  etc. being "not split when their command classes were" (`refactor/command-package-reorg`'s
  original call, Section 2 below) held until this session, when `ActivityCommandParser`
  specifically grew large enough (~800 lines, 10 commands) that the user approved splitting the
  four commands with real grammar into their own package-private classes
  (`AddCommandParser`/`EditCommandParser`/`ListCommandParser`/`FindCommandParser`), while
  deliberately keeping `parser.topic`/`parser.accessibility` flat (each too small/cohesive to
  benefit) and keeping trivial activity commands (delete/mark/unmark/view/next/order) inline in
  the router rather than splitting mechanically. Read as "split when a file's actual size and
  grammar complexity earns it, not as a blanket rule," not as silently abandoning the flat-parser
  preference elsewhere.
- **PR flow:** `gh` CLI is installed and authenticated as `lukelouyu` (needs
  `export PATH="/c/Program Files/GitHub CLI:$PATH"` in the same Bash call on this Windows/Git-Bash
  environment).
- **Pacing:** one focused piece of work at a time, tested and committed, then report before
  continuing. Keep independent tasks genuinely independent — don't let one's cleanup absorb
  another's scope, and don't silently combine unrelated large workstreams into one giant branch.

## 5. Where to look for further bugs / risk, concretely

- **`ActivityConflictChecker` decision — done, not a live risk.** Both the Phase-1 review and the
  Phase-2 extraction shipped at `e44f660`, before this session started (Section 1's transition
  note, Section 6). `logic.ActivityConflictChecker` is a real, current, package-private, stateless
  class - `ActivityManager` no longer owns duplicate/overlap logic inline. This entry is kept only
  so a stale local checkout or an older cached summary doesn't mislead a future session into
  redoing it; the actual next task is reviewing/merging `feature/v2-route`, then starting
  `feature/v2-dashboard` (Section 1, Section 6).
- **The "one error message covers two semantically different causes" family**: resolved for both
  `DateTimeParser.parseDate()` (earlier session) and `parseTime()` (Section 2's defensive-
  programming cleanup bullet, this update) — both now split "wrong shape" from "right shape but
  out of range" into separate messages. Worth re-checking this file if it grows a third date/time
  parsing entry point later, but not a live risk today.
- **The "hardcoded near-future date used as a placeholder" family**: still a real, live risk in
  `UniEnableTest.java`'s ~60 scenarios using `date/2026-08-15`-style placeholders, not yet
  mitigated. **This family already bit once this session** — `text-ui-test/input.txt`'s "Far past
  overdue" scenario (date/2000-01-01) triggered `add`'s "date has passed... from `TODAY` onwards"
  message, whose text embeds the real wall-clock date; since `text-ui-test` compares against a
  *static* committed `EXPECTED.TXT`, that was the only line in the whole suite whose expected
  output silently went stale every calendar day (or sooner, across a timezone boundary) — it broke
  in CI right after a push. Fixed by removing the line, since the identical scenario was already
  covered deterministically elsewhere (with an injected clock) in `DateTimeParserTest`/
  `ActivityCommandParserTest` — no coverage lost. **The lesson generalises: any text-ui-test line
  whose expected output would itself change based on real wall-clock time (not just a scenario
  that stops being "in the future" one day) must not exist in `text-ui-test` at all — move it to a
  JUnit test with an injected clock instead**, this is a stricter version of the plain
  "near-future placeholder" risk. The recur/reset-v2 JUnit fixtures using concrete 2026/2027 dates
  (`RecurrenceTestData`, `ApplicationRunnerTest`, `ScreenshotRecurrenceRegressionTest`) and
  `text-ui-test/academic-calendar-test.txt`'s 2027 dates are the plain version of this risk (not
  yet expired, but will be eventually) — those all use *injected* clocks/fixed dates, so they're
  fine architecturally; they'll just need bumping forward someday.
- **The "topic/command exists in name but not in the reachability graph" family**: the guide-topic
  bug was an instance of this, and it recurred once (`guide recur`/`guide reset` were missing/stale
  for one session despite being flagged in advance) before being fixed and wired into
  `GuideCommand`'s `TOPICS` map. Worth double-checking again for any *future* new command.
- **Documentation claims outliving what the code actually does**: the "always names the line
  number" overclaim (Section 2) is an instance of a general risk — this project's docs are written
  carefully and in detail, which is good, but a confident, specific claim ("always", "every") is
  exactly the kind of sentence that quietly goes false the next time the underlying code grows a
  second code path. Worth a light skeptical read of superlative doc claims when touching the
  code they describe.
- **`DateTimeParser`/`ActivityStorage` date-time-format duplication (new, hardening-plan
  session):** `ActivityStorage.parseDate()`/`parseTime()` now duplicate `DateTimeParser`'s
  formatter/`STRICT`-resolver/shape-regex logic verbatim, a deliberate trade-off accepted to
  decouple storage from the CLI-parser package (Section 2's newest bullet, point 5) — not fixed
  immediately since the actual fix (giving both a shared neutral home, e.g. under `model/`) is an
  architectural call that wants its own explicit go-ahead. If either side's date/time shape ever
  changes, **both `DateTimeParser` and `ActivityStorage` need updating together** — grep both
  before assuming a format change is complete. (The rating side of the same decoupling did *not*
  duplicate anything worth centralizing — both `RatingParser` and `ActivityStorage` still delegate
  the one real rule, the 1-5 range check, to `EnergyRating.of`/`SensoryRating.of`.)
- **`serialVersionUID` / boolean-naming nits — both resolved.** Both were flagged during the
  hardening-plan session's Javadoc/coding-standard audit and fixed in the defensive-programming
  cleanup batch (Section 2, this update): all 8 `seedu.unienable.exception` classes now declare
  `serialVersionUID`, and `ListCommandParser.parseViewMode()`/its `detail` local were renamed to
  predicate style. No longer live risks.

## 6. Product context (condensed, mostly unchanged from prior sessions)

**UniEnable** is a single-user, offline, CLI-based Java 17 application (a simulated CS2113 team
project) helping two personas — Sam (ASD/ADHD, wants concise/predictable output) and Jordan
(wheelchair user, wants accurate accessibility data) — plan unfamiliar university/internship
routines. v2.0 command surface so far: activity add/list/view/find/edit/delete/mark/unmark/next/
order, topic add/list/rename/delete, read-only facility/connection lookups (plus `facility
validate`/`connection validate`), `recur TASK_ID week WEEK_SPEC`, the three-option `reset all`,
`route from/FACILITY to/FACILITY` (Section 1, on `feature/v2-route`, not yet merged), `guide`,
`bye`. **No git tag or GitHub Release exists yet** — v1.0 has
been through a full 14-batch manual regression pass with all findings fixed (Sections 1-2), so
it's realistically tag-ready, but creating the tag is still the user's explicit call, not
something to do unprompted (see Section 1's last paragraph for exactly what's pending).

**v2.0 status, per the approved master prompt
(`UNIENABLE_V2_CLAUDE_CODEX_MASTER_PROMPT_UPDATED.md`, supplied 2026-08-02, now the authoritative
v2.0 spec):** the required branch sequence is route -> dashboard -> timetable -> preferences ->
recommend -> export -> final integration pass. Recurrence and technical-debt hardening already
shipped before this sequence started (Section 2). **Route is now implemented** on
`feature/v2-route` (Section 1) - the first v2.0 feature in the sequence, complete but awaiting
review/merge approval; do not start `feature/v2-dashboard` (the next required branch) until
`feature/v2-route` is merged and the user gives an explicit go-ahead for the next feature, per the
master prompt's own "one feature branch at a time... stop and present a review report" rule.
Dashboard, timetable, preferences, recommend, and export remain untouched backlog - approved in
principle by the master prompt, but each still needs its own audit-confirm-implement pass in its
own turn, not started proactively just because route is done.

Package root `seedu.unienable`: `app/` (`ApplicationRunner`, `CommandConfirmationHandler`,
`command.MenuConfirmable`/`MenuOutcome`), `command/` — `activity.crud` (Add/Delete/Edit/View) +
`activity.general` (Find/List/Mark/Next/OrderSet/OrderView/Unmark), `accessibility.facility` +
`accessibility.connection` (4 commands each) + `accessibility.common`
(`AccessibilityDisclaimer`/`ValidationReportFormatter`, both `public`) + `accessibility.route`
(new, `feature/v2-route`: `RouteCommand`), `topic/` and `recur/` left flat (too few classes each
to split), `general/` (Guide/Reset/Bye). `parser/` mostly mirrors `command/` one level up, still
flat per domain for `topic`/`accessibility` (`TopicCommandParser`/`FacilityCommandParser`/
`ConnectionCommandParser`/`RouteCommandParser` all live directly in `parser.accessibility`, none
split further), **except `parser.activity`**,
split as of the hardening-plan session (Section 2) into a thin `ActivityCommandParser` router plus
package-private `AddCommandParser`/`EditCommandParser`/`ListCommandParser`/`FindCommandParser` for
the four commands with real grammar — delete/mark/unmark/view/next/order stayed inline in the
router. Plus `common/` for
`FieldParser`/`DateTimeParser`/`RatingParser`/`Parser`/`ArgumentTokenizer`/`ArgumentMarker`, plus
`recur/`. `exception/` (flat, reused as-is by every feature including recur and route), `logic/`
(the `*Manager` classes, plus `graph/` [Dijkstra-prep, generic, policy-free - see Section 1] and
`recur/` and, new, `route/` [`AccessibleRouteGraphFactory`, the route-specific `YES`-only filter
that deliberately does *not* live in `logic.graph`]), `model/` (`classes`/`enums`/`recur` - no
`model/route`, route needs no new persistent domain type), `storage/` (plus `recur/` for the
strictly-validated, read-only `AcademicCalendarStorage`; `storage/` imports nothing from
`parser/` as of the hardening-plan session — see Section 2/5), `ui/` (plus `recur/` for
`RecurrenceFormatter` and, new, `accessibility/` for `RouteFormatter`), `accessibility/` (the
read-only facility/connection domain model — a different, older package tree from
`command.accessibility.*` above, don't conflate the two: this one is
`seedu.unienable.accessibility.classes`/`.enums`, immutable read-only reference data, unchanged
by route - route only reads it).

**`docs/planning/`** holds four reference documents, added so a different tool (e.g. Codex)
picking this project up doesn't need access to the user's local Downloads folder. Read what each
one actually is before treating any of them as a task list:

- `CS2113_tP_Requirements_Baseline_v0.2.md`, `CS2113_tP_Prioritised_User_Stories_v0.1.md`,
  `CS2113_tP_Primary_User_Guide_Draft.md` — a pre-development requirements baseline, user-story
  list, and draft user guide for a **broader, differently-scoped product concept** (working name
  undecided, described in these docs as an "Accessible Itinerary Planner") that predates
  UniEnable's actual implementation history. **Superseded as of this session** by
  `UNIENABLE_V2_CLAUDE_CODEX_MASTER_PROMPT_UPDATED.md` (outside the repo, in Downloads), which the
  user explicitly designated the authoritative v2.0 spec (see Section 1's opening paragraph), with
  these three treated as supplementary/historical background only where the master prompt is
  silent - not independent authority, and not a task list to start from unprompted.
- `UniEnable_ActivityConflictChecker_Review_and_Extraction.md` — **done.** Both the Phase-1 review
  and the Phase-2 extraction it approved shipped directly on `main` before this session started, at
  `e44f660` (Section 1's transition note has the detail). `logic.ActivityConflictChecker` is real,
  current code. This bullet previously described it as "the concrete next task"; it is not,
  anymore - the concrete next task is reviewing/merging `feature/v2-route` (Section 1), then
  `feature/v2-dashboard`, per the master prompt's required sequence.
