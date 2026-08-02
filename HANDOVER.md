# UniEnable Handover

## 1. Current state (as of this update)

`main` is at `2cff382` locally and on `origin/main`
(`github.com/lukelouyu/SelfTeamProject`) — pushed. No unmerged work-in-progress branches remain;
`refactor/command-package-reorg` and `feature/recur-reset-v2` are both fully merged and safe to
delete; `v2-dashboard` is still empty (0 commits ahead), untouched.

Since the previous revision of this handover (written right after `feature/recur-reset-v2`
merged), four more things happened in the same continuous session — see Section 2 for full detail
on each:
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

**Verification as of this handover (commit `2cff382`):** `./gradlew clean test checkstyleMain
checkstyleTest` all green, **863 JUnit tests**, checkstyle clean, `text-ui-test/runtest.sh`
passes. `releaseZip` not re-run since the last two small commits landed — the underlying code
paths are unchanged (only `list`'s error messages and one doc line), so this is low-risk, but
worth a fresh run before trusting a new release artifact.

## 2. Condensed history (compressed — see `git log` for full detail)

Most recent sessions, newest first:

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
  requests, not unprompted redesigns.
- **PR flow:** `gh` CLI is installed and authenticated as `lukelouyu` (needs
  `export PATH="/c/Program Files/GitHub CLI:$PATH"` in the same Bash call on this Windows/Git-Bash
  environment).
- **Pacing:** one focused piece of work at a time, tested and committed, then report before
  continuing. Keep independent tasks genuinely independent — don't let one's cleanup absorb
  another's scope, and don't silently combine unrelated large workstreams into one giant branch.

## 5. Where to look for further bugs / risk, concretely

- **The "one error message covers two semantically different causes" family**: `DateTimeParser.
  parseTime()` still conflates "wrong shape" and "hour 25"/"minute 60" into one message, the same
  shape of issue `parseDate()` had before the latest fix (Section 2) — not flagged as a bug yet,
  worth a look if touching that file again.
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

## 6. Product context (condensed, mostly unchanged from prior sessions)

**UniEnable** is a single-user, offline, CLI-based Java 17 application (a simulated CS2113 team
project) helping two personas — Sam (ASD/ADHD, wants concise/predictable output) and Jordan
(wheelchair user, wants accurate accessibility data) — plan unfamiliar university/internship
routines. v1.0 command surface: activity add/list/view/find/edit/delete/mark/unmark/next/order,
topic add/list/rename/delete, read-only facility/connection lookups (plus `facility
validate`/`connection validate`), `guide`, `bye`. v2.0-so-far adds `recur TASK_ID week WEEK_SPEC`
and the three-option `reset all` (previously a single binary-confirmation v1.0 command, now
superseded by the menu — see Section 1). **No git tag or GitHub Release exists yet** — v1.0 has
been through a full 14-batch manual regression pass with all findings fixed (Sections 1-2), so
it's realistically tag-ready, but creating the tag is still the user's explicit call, not
something to do unprompted (see Section 1's last paragraph for exactly what's pending).

v2.0 scope was previously "not started, don't add unprompted" — recurrence and technical-debt
hardening have now both **shipped** (Section 2), so those two are done, not just approved.
Dashboard, timetable, preferences, recommendation, route, and CSV export remain
untouched/unapproved backlog — still don't start any of those unprompted.

Package root `seedu.unienable`: `app/` (`ApplicationRunner`, `CommandConfirmationHandler`,
`command.MenuConfirmable`/`MenuOutcome`), `command/` — `activity.crud` (Add/Delete/Edit/View) +
`activity.general` (Find/List/Mark/Next/OrderSet/OrderView/Unmark), `accessibility.facility` +
`accessibility.connection` (4 commands each) + `accessibility.common`
(`AccessibilityDisclaimer`/`ValidationReportFormatter`, both `public`), `topic/` and `recur/` left
flat (too few classes each to split), `general/` (Guide/Reset/Bye). `parser/` mirrors `command/`
one level up (still flat per domain — `ActivityCommandParser`/`FacilityCommandParser`/etc. were
*not* split when their command classes were), plus `common/` for
`FieldParser`/`DateTimeParser`/`RatingParser`/`Parser`/`ArgumentTokenizer`/`ArgumentMarker`, plus
`recur/`. `exception/` (flat, reused as-is by every feature including recur), `logic/` (the
`*Manager` classes, plus `graph/` and `recur/`), `model/` (`classes`/`enums`/`recur`), `storage/`
(plus `recur/` for the strictly-validated, read-only `AcademicCalendarStorage`), `ui/` (plus
`recur/` for `RecurrenceFormatter`), `accessibility/` (the read-only facility/connection domain
model — a different, older package tree from `command.accessibility.*` above, don't conflate the
two: this one is `seedu.unienable.accessibility.classes`/`.enums`, immutable read-only reference
data).

Other attached reference materials (requirements baseline, user stories, original draft user
guide, kickoff prompt, campus map PDF) are in the user's Downloads folder outside this repo, per
earlier sessions' notes — not re-listed here since nothing about them changed this session.
