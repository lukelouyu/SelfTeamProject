# UniEnable Handover

## 1. Current state (as of this update)

`main` now includes the recurring-class-sessions feature and the three-option `reset all` redesign
(the "3b" workstream from the previous handover's Section 3 — see Section 2 below for what
shipped). It was developed on `feature/recur-reset-v2`, branched from `c148220`, then merged into
`main` and pushed to `origin/main` (`github.com/lukelouyu/SelfTeamProject`) this session, per
explicit user instruction to merge and push. `fix/technical-debt-hardening` (the "3d" workstream)
was already merged into `main` **before** `feature/recur-reset-v2` was branched, so both are now
in `main` together; `fix/technical-debt-hardening` itself is safe to ignore or delete. No other
unmerged work-in-progress branches remain; `v2-dashboard` is still empty (0 commits ahead),
untouched.

Any standalone `unienable.jar` copied out of the repo in an earlier session (e.g. to
`C:\Users\lukel\Downloads\AY2627_Sem1 Prep\CS2113\unienable.jar`) is now stale — it predates recur
entirely. **The distributable is now `./gradlew releaseZip`'s
`build/distributions/unienable.zip`** (jar + external `data/academic-calendar.txt`), not a bare
jar — see the User Guide's Quick Start and README's "Distribution" section.

**Verification as of this handover:** `./gradlew clean test checkstyleMain checkstyleTest` all
green, **857 JUnit tests**, checkstyle clean, `text-ui-test/runtest.sh` passes,
`./gradlew releaseZip` produces the correct ZIP layout with the calendar confirmed absent from the
jar. Full manual smoke test (sparse recurrence against the real AY2026/2027 calendar, restart
persistence, repeat-is-idempotent, all three reset options, missing calendar, malformed calendar
with line-numbered error, and a real forced storage failure via a read-only `activities.txt`) all
passed against a clean extraction of the release ZIP. See Section 2's newest entry for exactly what
was verified and how.

## 2. Condensed history (compressed — see `git log` for full detail)

Most recent sessions, newest first:

- **`feature/recur-reset-v2`** (merged into `main` this session, per explicit user instruction to
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
- **The "hardcoded near-future date used as a placeholder" family**: see Section 2's "Known
  fragility" note — real, live risk in `UniEnableTest.java`, not yet mitigated. Same shape of risk
  now also applies to the recur/reset-v2 JUnit fixtures that use concrete 2026/2027 dates
  (`RecurrenceTestData`, `ApplicationRunnerTest`, `ScreenshotRecurrenceRegressionTest`) and to
  `text-ui-test/academic-calendar-test.txt`'s 2027 dates — none are expected to expire soon, but
  it's the same category of fragility if real time ever catches up to them.
- **The "topic/command exists in name but not in the reachability graph" family**: the guide-topic
  bug (Section 2) was an instance of this, and it recurred once more — `guide recur` and the
  rewritten `guide reset` were both still missing/stale when this session started, despite this
  exact risk having been flagged in the previous handover revision. Both are now fixed and wired
  into `GuideCommand`'s `TOPICS` map. Worth double-checking again for any *future* new command.

## 6. Product context (condensed, mostly unchanged from prior sessions)

**UniEnable** is a single-user, offline, CLI-based Java 17 application (a simulated CS2113 team
project) helping two personas — Sam (ASD/ADHD, wants concise/predictable output) and Jordan
(wheelchair user, wants accurate accessibility data) — plan unfamiliar university/internship
routines. v1.0 command surface: activity add/list/view/find/edit/delete/mark/unmark/next/order,
topic add/list/rename/delete, read-only facility/connection lookups (plus `facility
validate`/`connection validate`), `guide`, `bye`. v2.0-so-far adds `recur TASK_ID week WEEK_SPEC`
and the three-option `reset all` (previously a single binary-confirmation v1.0 command, now
superseded by the menu — see Section 1). No git tag or GitHub Release exists yet; that's the
user's call, not something to do unprompted.

v2.0 scope was previously "not started, don't add unprompted" — recurrence and technical-debt
hardening have now both **shipped** (Section 2), so those two are done, not just approved.
Dashboard, timetable, preferences, recommendation, route, and CSV export remain
untouched/unapproved backlog — still don't start any of those unprompted.

Package root `seedu.unienable`: `app/` (`ApplicationRunner`, `CommandConfirmationHandler`,
`command.MenuConfirmable`/`MenuOutcome`), `command/` (`activity`/`topic`/`accessibility`/`general`
subpackages, plus `recur/`), `parser/` (mirrors `command/`, plus `common/` for
`FieldParser`/`DateTimeParser`/`RatingParser`/`Parser`/`ArgumentTokenizer`/`ArgumentMarker`, plus
`recur/`), `exception/` (flat, reused as-is by every feature including recur), `logic/` (the
`*Manager` classes, plus `graph/` and `recur/`), `model/` (`classes`/`enums`/`recur`), `storage/`
(plus `recur/` for the strictly-validated, read-only `AcademicCalendarStorage`), `ui/` (plus
`recur/` for `RecurrenceFormatter`), `accessibility/` (the read-only facility/connection domain
model).

Other attached reference materials (requirements baseline, user stories, original draft user
guide, kickoff prompt, campus map PDF) are in the user's Downloads folder outside this repo, per
earlier sessions' notes — not re-listed here since nothing about them changed this session.
