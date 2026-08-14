# UniEnable Handover

**Read this whole file before doing anything else.** It is the up-to-date continuity doc for this
project across sessions/tools — commit and push discipline, verification commands, and design
taste the user has been firm about are all in Section 4, and skipping them is the most common way
a new session repeats a mistake an earlier one already made and documented here.

## 0o. `v2.1.0` retagged/republished for the quality-hardening pass, verified (2026-08-14, Claude Code) — read this first

Follow-up to Section 0n, same session: records the push and retag once the user explicitly
requested them, per the same two-commit pattern Sections 0i/0k/0m/0m-follow-up already established
(the commit that gets tagged records the fixes; a separate, later, untagged commit records the
retag/publish narrative once it actually happened).

**Push.** `git fetch origin` confirmed `origin/main` unchanged at `eb1a23f` (this session's own
starting HEAD) immediately before pushing; `git push origin main` was a clean fast-forward
(`eb1a23f..62fac62`, the seven commits from Section 0n). Verified after: `git rev-parse HEAD` and
`git rev-parse origin/main` both `62fac62`.

**Fresh JAR/ZIP**, built clean from this exact HEAD (`./gradlew clean releaseZip
verifyReleaseZip`), manifest confirmed (`Main-Class: seedu.unienable.UniEnable`), bare `java -jar`
startup smoke test passed, plus a fresh-directory smoke test against the actual `unienable.zip`
directly reproducing the Section 0n bug-fix scenario (seeded `data/activities.txt` with a completed
`09:00-10:00` fixed activity, `-Dunienable.fixedNow=2026-08-19T15:00`): both `dashboard today` and
`recommend today` showed `Completion [##########] 100% (1/1)`. SHA-256:
- `unienable.jar`: `e1c34029493daf4890dda63df83e4b9147b4391aae91f304e6feab8c3c0744f8`
- `unienable.zip`: `98061fa8369aebbb8ed96098aba391228e06c0cfdba7f8e08eb0e4ad889cf1e2`

**`v2.1.0` retagged in place** (not a new `v2.1.1` - explicit user instruction was to retag
`v2.1.0`, matching this project's `v2.0.1`/`v2.1`/Section 0m retag precedent). Old target recorded
first (`git rev-parse v2.1.0^{commit}` = `451fd93`, Section 0m's HEAD), then `git tag -d v2.1.0` +
`git tag -a v2.1.0 ... 62fac62` + `git push origin :refs/tags/v2.1.0` + `git push origin v2.1.0`.
Deleting the underlying tag turned the GitHub release into an orphaned `draft` (confirmed via
`gh release view v2.1.0 --json isDraft` -> `true` immediately after) - `gh release edit v2.1.0
--tag v2.1.0 --target main --draft=false --latest --notes-file <updated notes>` reattached,
republished, and updated the release notes in one call. `gh release upload v2.1.0 <jar> <zip>
--clobber` replaced both assets - confirmed exactly two assets afterward (no stale duplicates),
server-computed digests matching the checksums above exactly.

**Release notes updated in place** (appended, not replaced from scratch - same discipline as every
prior retag in this file): a new "Additional pass: code-quality hardening (independent QA review)"
section summarizing the Section 0n now-bug fix, the exhaustive-search performance fix, and the
code-quality changes, plus updated verification numbers (1,319 tests) and the new checksums in the
"Release assets" section at the bottom (old checksums removed, not left stale alongside the new
ones, matching how every earlier retag in this file has handled the same section).

**Published-asset verification.** `gh release download v2.1.0 --dir <fresh temp dir>` then
`sha256sum` matched the freshly-built checksums above exactly; the downloaded jar was smoke-tested
directly with the exact fixed-clock/seeded-activity reproduction above and showed the corrected
`100% (1/1)` completion line for both `dashboard today` and `recommend today` - the published asset
is confirmed to be the corrected build.

**Final state.** `git rev-parse HEAD` / `origin/main` / `v2.1.0^{commit}` all `62fac62`; `v2.1`
unchanged. `git status`: clean except the same two pre-existing untracked presentation files
flagged since Section 0f. No stale release JAR/ZIP, temporary fixture, or copied GitHub asset left
inside the repository - all verification artifacts were written under this session's external
scratch directory.

## 0n. Code-quality hardening pass after independent QA review: bug fix, perf fix, architecture, docs, tests (2026-08-14, Claude Code) — read this first

This session was **quality hardening, not feature development**, per an independent QA review
supplied as this session's task. Scope: (1) fix a newly confirmed recommendation/dashboard bug,
(2) reduce the recommendation algorithm's factorial performance cliff, (3) fix two genuine package
dependency cycles, (4) remove misleading names/stale comments/dead code, (5) strengthen regression
coverage, (6) keep CS2113-style code quality, (7) update this file. No release/tag action was in
scope and none was taken - see "Final state" below.

**Starting state.** Date 2026-08-14. Branch `main`. Starting HEAD `eb1a23f` (Section 0m). Working
tree clean except the same two pre-existing untracked presentation files flagged since Section 0f
(`.codex-presentations/`, `UniEnable_NUS_Enablers_Presentation.pptx.inspect.ndjson`) - confirmed
still unrelated to this project's source and left untouched.

**1. Confirmed defect: recommendation preview dashboard used the wrong `now` (fixed).**
`RecommendationFormatter.formatPreview` called `DashboardService.summarize(previewActivities,
proposal.getDashboardPeriod(), proposal.getDashboardPeriod().getStart())` - the third argument is
the completion-eligibility basis, and the period's own `start` (midnight) was passed instead of the
real injected `now`. Reproduction: `now = 2026-08-14 15:00`, a `09:00 -> 10:00` activity marked
`COMPLETE`; `dashboard today` correctly showed `Completion [##########] 100% (1/1)`, but
`recommend today`'s embedded dashboard showed `Completion: No activities are due yet.` - the two
views disagreed about which activities were already due.

Root cause confirmed by reading the formatter before writing any fix: the UI formatter was also
doing business calculation (`DashboardService.summarize`/`TimetableService.build` called directly
from `ui.recommend.RecommendationFormatter`), which is how a period boundary ended up standing in
for `now` in the first place - nothing forced the actual injected `now` to flow that far down. Fix:
a new `RecommendationService.buildPreview(activityManager, proposal, now)` applies the proposal's
placements to copied activities and builds the preview timetable/dashboard from them using the real
`now`, bundled into a new immutable `model.recommend.RecommendationPreview` (proposal + timetable +
dashboard). `RecommendGenerateCommand` and `RecommendViewCommand` (which did not previously receive
`now` at all - now threaded through from `RecommendCommandParser`, which already had it) call
`buildPreview` and hand the result straight to `RecommendationFormatter.formatPreview(preview)`,
which now does no calculation of its own - this fixes the bug and the UI/logic layering violation in
one change, since the layering violation was the root cause. Regression tests (all
`ApplicationRunnerTest`, fixed injected `now`, no wall-clock dependence):
`recommendToday_previewDashboard_usesActualNow`,
`recommendToday_previewDashboard_matchesDashboardCompletion`,
`recommendThisWeek_previewDashboard_countsAlreadyDueActivities`,
`recommendTomorrow_previewDashboard_hasNoDueActivitiesYet`. Commit `59a50b8`.

**2. Performance: recommendation exhaustive search's factorial cliff at `PERMUTATION_CAP` (fixed).**
`RecommendationService.optimizeDay`'s exhaustive path materialized every permutation of a day's
remaining flexible activities into a `List<List<FlexibleActivity>>` (via `permute`), then evaluated
each independently from a fresh copy of the base commitments (`placeInOrder`). Benchmarked locally
(this machine, synthetic same-day flexible activities with wide overlapping windows so every
ordering is a genuine candidate - the true adversarial case): ~34 ms at 5 activities, ~342 ms at 7,
**~2.7 s at 8** (8! = 40,320 orderings), dropping back to ~5 ms at 9 once the heuristic fallback took
over - a real, reproducible cliff, matching the independent QA benchmark's reported numbers closely.

Fix, in two parts. First, `exhaustiveSearch`/`searchOrderings` replaced `permute`/`placeInOrder`:
the identical in-place swap permutation-generation algorithm, but fused directly with greedy
placement so no ordering is ever materialized as a full list, plus a branch-and-bound prune once
`placedSoFar.size() + remainingDepth` can no longer reach the best item count already found
(`DaySchedule.betterThan`'s top-priority criterion - a safe bound, since a completed branch's item
count can never exceed already-placed items plus however many candidates remain unplaced). Measured
in isolation (cap still 8, to isolate this effect from the cap change below): n=8 went from ~2.7 s to
~2.65 s - a real but small improvement, because the pruning bound never triggers when every candidate
activity is mutually compatible (the exact adversarial benchmark scenario has no branch to prune).
Second, `PERMUTATION_CAP` was lowered from 8 to 7 (5,040 worst-case orderings, consistently well
under a second even unpruned) specifically because the benchmark showed pruning alone does not bound
the true worst case. With both changes: n=7 (still exhaustive) ~316 ms, n=8 (now heuristic) ~7 ms -
no more cliff. Scheduling output is unchanged for every `n <= PERMUTATION_CAP`: identical orderings
explored, identical greedy placement logic, identical `DaySchedule.betterThan` winner selection -
confirmed by every pre-existing `RecommendationServiceTest` case passing unmodified. Tests:
`recommend_eightFlexibleActivities_returnsDeterministically`,
`recommend_largeFlexibleSet_doesNotMaterializeAllPermutations` (generous 5 s CI-safe bound, not a
brittle near-actual-runtime threshold - chosen because the old cap-8 defect measured multiple
seconds for a strictly *smaller* 40,320-ordering search, so 5 s leaves wide margin while still
catching a regression back toward unbounded materialization),
`recommend_optimization_preservesExpectedSchedule` (a two-activity scenario where only a
multi-ordering search, not a single greedy pass, finds the schedule that fits both). Commit
`ae9e2fc`.

**3. Architecture: two genuine package dependency cycles removed.**
- `command.activity.crud.EditCommand` imported `parser.common.DateTimeParser` for
  `requireNotPastIfToday` (a pre-execution re-check run after the confirmation prompt), while
  `parser` already depends on `command` everywhere (parsers construct commands) - `command ->
  parser -> command`. Fixed by moving the rule into a new neutral `logic.validation.ActivityTimeValidator`;
  `DateTimeParser.requireNotPastIfToday` now delegates to it (zero behavioural or call-site change
  for its many existing parser-internal callers - `parseNotBeforeNow`, `AddCommandParser`,
  `EditCommandParser`, `RecommendCommandParser` all still call `DateTimeParser` exactly as before),
  and `EditCommand` calls `ActivityTimeValidator` directly instead of reaching into `parser`.
- `ui.accessibility.RouteFormatter` imported `command.accessibility.common.AccessibilityDisclaimer`,
  while `command` already depends on `ui` broadly (formatters) - `ui -> command -> ui`. Fixed by
  moving `AccessibilityDisclaimer` to `ui.accessibility` (it is presentation content - text appended
  to formatted output - not command logic, and `RouteFormatter`, also in `ui.accessibility`, needs it
  too); every command that displays it (`ConnectionFindCommand`, `ConnectionListCommand`,
  `ConnectionViewCommand`, `FacilityFindCommand`, `FacilityListCommand`, `FacilityViewCommand`,
  `GuideCommand`) now imports it from there instead - `command -> ui` for this content, same
  direction as every other formatter dependency, no new cycle introduced.

Dependency direction is now the documented one: `parser -> command`, `command -> logic/ui`, no path
back from either into `parser`, and no path from `ui` into `command` for this content.
`command.accessibility.common.ValidationReportFormatter` was checked and found *not* to be part of a
cycle (only used within `command` itself) - left in place, not moved speculatively. Commit `d3e6298`.

**4. Readability: `TimetableView.getFixedEntries()` renamed to `getScheduledEntries()`.**
The returned list contains both fixed activities and adopted flexible activities (the backing field
was already named `scheduledEntries`); the accessor name was misleading about what the collection
actually holds. Renamed the accessor plus the local-variable and helper-method names that referred
to the same concept (`TimetableService.build`'s `scheduledEntries` local,
`TimetableFormatter.appendFixedEntries`/`appendFixedEntry` -> `appendScheduledEntries`/
`appendScheduledEntry`), and updated every production caller, test caller
(`TimetableServiceTest`, `TimetableIntegrationTest`), and the stale "fixed entry"/"fixed start"
wording in `TimetableEntry`'s JavaDoc to match. Developer-readability refactor only - confirmed via
the full test suite and a manual `timetable today`/`timetable this week` smoke test that output is
byte-for-byte unchanged. Commit `7a31050`.

**5. Stale JavaDoc/comments corrected; one dead constant removed.**
- `ActivityManager.add`/`replace`/`checkNoConflicts`, `ActivityCommandParser.parseEdit`,
  `EditCommandParser.parse`, and `ActivityStorage.validateAgainstAlreadyLoaded` all still said
  "(for a `FixedActivity`) overlaps another fixed activity" - stale since conflict detection was
  widened (Section 0j finding C) to cover any two activities' *occupied intervals* (fixed or adopted
  flexible), not just fixed-vs-fixed. Reworded to the precise current rule throughout.
- `UniEnable.java`'s `run(...)` JavaDoc said "the directory containing... the five data files" - the
  application persists more than five files now and the exact count was never load-bearing to the
  parameter's meaning; reworded to "the application data directory" without embedding a count, per
  this file's own "avoid hardcoding facts that will drift" discipline.
- `GuideCommand.COMING_SOON_NOTE` was an unused constant (confirmed via full-codebase search - zero
  references anywhere outside its own declaration) - removed. A codebase-wide search for
  `TODO`/`FIXME`/`XXX`/"coming soon"/"not yet implemented"/"future release" found nothing else.
- `ActivityManager.sort`'s `TIME`/`CHRONOLOGICAL` comment narrated a past bug fix ("TIME used to
  compare only time-of-day, which caused...", from Section 0m) instead of stating the current
  invariant; reworded to explain *why* the two cases intentionally share one comparator, leaving the
  bug narrative in git history/Section 0m where it already lives, per this file's own Section 10
  discipline (own instruction in this session's task spec).

Not swept codebase-wide: a broader grep for "used to"/"previously"/"in v1./v2." across all of
`src/main` returned 19 files, nearly all false positives (ordinary present-tense "used to reject...",
not historical narration) - only the one genuine case above was rewritten, per the task's own "do
this selectively" instruction; the rest were read and confirmed not to need it, not skipped
unreviewed. Commit `92fd841`.

**6. Overdue exact-end-instant semantics: confirmed correct, documented, not changed.**
`ActivityManager.isOverdue` uses `end.isBefore(now)` (strictly exclusive) while
`DashboardService`'s completion eligibility uses `!eligibleFrom.isAfter(now)` (inclusive of the
exact end instant) - so an activity becomes completion-eligible/"due" exactly at its own end time,
but does not become overdue until one instant later. Confirmed both the implementation and an
existing exact-boundary regression test (`countOverdueIncomplete_nowExactlyAtEndTime_isNotYetOverdue`,
already present before this session) are correct and unchanged - per the task's explicit instruction
not to change this behaviour absent a specification/test contradiction, and none was found. Only the
documentation was underspecified: `docs/UserGuide.md`'s `list overdue` bullet gained an explicit
sentence stating the exact-end-instant rule; `docs/DeveloperGuide.md`'s glossary gained
`Completion-eligible`/`Overdue` entries spelling out the two boundary conditions and why they
intentionally differ by one instant. Included in commit `2effa59`.

**Test-count change:** 1312 -> **1319** (7 net new tests: 4 bug-fix regressions + 3 performance
regressions), zero deleted or weakened. `RecommendationServiceTest`'s `ArrayList` import was added
alongside the new performance tests (no other change to that file's existing cases).

**Verification, this session's own HEAD (`2effa59`):**
```
JUnit tests:      1319, 0 failures (./gradlew clean test)
Checkstyle:       clean (checkstyleMain, checkstyleTest)
Javadoc:          clean (./gradlew javadoc, 0 errors)
text-ui-test:     Test passed! (bash text-ui-test/runtest.sh - zero EXPECTED.TXT changes needed,
                  confirming none of this session's changes altered any existing scripted CLI output)
releaseZip:       clean (./gradlew releaseZip)
verifyReleaseZip: clean (./gradlew verifyReleaseZip)
```
Also ran a codebase-wide search for `TODO`/`FIXME`/`XXX`/`System.out`/`catch (Exception`/
`catch (Throwable`/`@SuppressWarnings`: the only `System.out` hit is `ui.Ui`'s own legitimate CLI
output boundary; nothing else matched anywhere in `src/main`.

**Manual CLI smoke test**, fresh-extracted `unienable.zip`, fixed clock
(`-Dunienable.fixedNow=2026-08-19T15:00`, a Wednesday), two runs:
- **Bug-fix reproduction (the primary deliverable):** seeded `data/activities.txt` directly with a
  `FIXED|1|Morning briefing|ACADEMIC|2026-08-19|09:00|10:00|2|2|COMPLETE` activity (bypassing `add`'s
  own not-in-the-past validation, which correctly rejects constructing a fresh past-dated activity -
  a real activity would already exist in storage from before now). `dashboard today` showed
  `Completion [##########] 100% (1/1)`; `recommend today`'s embedded dashboard showed the *identical*
  `Completion [##########] 100% (1/1)` line - matching, not the old "No activities are due yet."
- **Flexible recommend/view/adopt end to end:** added two `FLEXIBLE` activities, `recommend today`
  proposed both (`Reading 16:00->16:45`, `Essay draft 17:00->18:00`), `recommend view` re-displayed
  identically, `recommend adopt` succeeded and `timetable today` showed both as `[R]` adopted-flexible
  entries at those exact times.
- **`getScheduledEntries` rename regression check:** `timetable today`/`timetable this week` output
  inspected directly - unaffected by the internal rename.
- **Adopted-flexible edit placement preservation:** `edit 2 note/Bring laptop` showed `Before: note =
  None` / `After: note = Bring laptop`; the following `timetable today` still showed activity 2 at
  its original adopted `17:00-18:00`, confirming the placement survived a non-scheduling edit
  (pre-existing Section 0j behaviour, re-confirmed not regressed by this session's changes).
- **Duplicate marker rejection:** `find k/Study k/Assignment` -> `[Error] Invalid input: Duplicate
  option "k/".` (pre-existing Section 0j behaviour, re-confirmed).
- **Preference change invalidates a stale proposal:** generated a `recommend date/2026-08-27`
  proposal with one real placement, then `preference set start/12:00` (confirmed `y`), then
  `recommend adopt` -> `[Error] Invalid input: This recommendation proposal no longer fits your
  current preferred daily start/end - preferences changed since it was generated. Generate a new
  recommendation with recommend.` (pre-existing Section 0j behaviour, re-confirmed).

No new functional defect was found during this session beyond the one already supplied as this
session's task.

**Documentation and diagrams synced:** `docs/UserGuide.md` (overdue exact-boundary wording),
`docs/DeveloperGuide.md` (recommend preview responsibility split, performance-hardening benchmark
paragraph, `EditCommand`'s validator-move paragraph, `Completion-eligible`/`Overdue` glossary
entries), `RecommendationClassDiagram` and `RecommendationGenerationSequence` (`.puml` + regenerated
`.png`, via the same local `plantuml.jar` toolchain prior sessions used - still present in this
environment at `C:\Users\lukel\AppData\Local\Temp\unienable-plantuml-tool\tp-master\tools\plantuml.jar`,
reused without a fresh download; both diagrams visually verified rendered correctly, not error
placeholders, before committing).

**A note on commit/file overlap:** `RecommendationService.java`'s bug-fix change (`buildPreview`) and
performance change (`exhaustiveSearch`/`searchOrderings`, lowered `PERMUTATION_CAP`) both landed in
the same file at different points in this session; the bug-fix commit (`59a50b8`) happened to be
staged first and so carries both hunks together (the perf commit `ae9e2fc` only added the new test
file, since the production file was already fully committed) - same reasoning this file's Section 0j
already documented for exactly this kind of unavoidable single-file overlap under non-interactive
git tooling: both commits' own test suites still pass standalone at that point in history, and
splitting the file's hunks would need interactive patching this environment doesn't support.
Similarly, `GuideCommand.java`'s `AccessibilityDisclaimer` import-path update and its unrelated dead
`COMING_SOON_NOTE` removal both landed in the dependency-cycle commit (`d3e6298`) rather than the
JavaDoc-cleanup commit, for the same reason.

**Commits, in order:**
| Commit | Message |
|---|---|
| `59a50b8` | fix: use actual current time in recommendation dashboard preview |
| `ae9e2fc` | perf: avoid factorial recommendation ordering materialization |
| `d3e6298` | refactor: remove parser/command package dependency cycle |
| `7a31050` | refactor: rename timetable fixed entries to scheduled entries |
| `92fd841` | docs: align JavaDoc and comments with occupied-schedule conflict semantics |
| `2effa59` | docs: clarify overdue boundary and architecture updates |

**Release/tag rule at the time these fixes were committed.** No release action was taken until the
user separately, explicitly requested it after reviewing this work - per this file's own "ask
before publishing" discipline. See the follow-up note immediately above this section (Section 0o)
for the push/retag/republish that happened once that explicit instruction arrived, in the same
two-commit pattern this file has used for every prior retag.

**Final state as of this section's own commit (`2effa59`).** `git status`: clean except the same
two pre-existing untracked presentation files flagged since Section 0f - left untouched, out of
this session's scope. `origin/main` had not yet been updated at this point - see Section 0o for the
push and retag that followed once requested.

## 0m. `v2.1.0` retagged/republished for the `order/time` fix, verified (2026-08-14, Claude Code) — read this first

Follow-up to Section 0l, same session: records the actual retag/republish once the fix there was
fully green, per the same two-commit pattern Sections 0i/0j/0k already established.

**Push.** `git fetch origin` confirmed `origin/main` unchanged at `f3fdd1e` before pushing;
`git push origin main` was a clean fast-forward (`f3fdd1e..451fd93`, the two commits from Section
0l - `f5df5f3` fix, `451fd93` this file's own entry). Verified: `git rev-parse HEAD`/`origin/main`
both `451fd93`.

**Fresh JAR/ZIP**, built clean from this exact HEAD (`./gradlew clean releaseZip
verifyReleaseZip`), manifest confirmed (`Main-Class: seedu.unienable.UniEnable`), startup smoke
test passed, plus a fresh-directory smoke test against the actual `unienable.zip` reproducing the
exact `find k/QA Lecture order/time` scenario one more time. SHA-256:
- `unienable.jar`: `1fc3737755a972bd39cdd919068f04c87976bfe0ee6f6dcc7055a4819e02372f`
- `unienable.zip`: `cd7f4268da1ae03a6a9f71b303a6434ba840abc6127d7ed80d6587d543feea97`

**`v2.1.0` retagged in place** (not a new `v2.1.1` - the user's explicit instruction was to
"ensure the final `v2.1.0` tag points to the corrected `main` HEAD," matching this project's own
`v2.0.1`/`v2.1` retag precedent). Old target recorded first (`git rev-parse v2.1.0^{commit}` =
`fb40b03`, Section 0k's HEAD), then `git tag -d v2.1.0` + `git tag -a v2.1.0 ... 451fd93` +
`git push origin :refs/tags/v2.1.0` + `git push origin v2.1.0`. Exactly as Section 0i's own
retag documented for `v2.1`, deleting the underlying tag turned the GitHub release into an
orphaned `draft` - `gh release edit v2.1.0 --tag v2.1.0 --target main --draft=false --latest`
reattached and republished it (note: `--target` needed the branch name `main`, not a commit SHA -
`gh` rejected a short SHA with `Release.target_commitish is invalid`; a full 40-character SHA
would likely also have worked, `main` was simplest since it already pointed at the right commit).
`gh release upload v2.1.0 <jar> <zip> --clobber` replaced both assets - confirmed exactly two
assets afterward (no stale duplicates), server-computed digests matching the checksums above
exactly. Release notes updated in place (not replaced from scratch) with a new "Additional fix:
`order/time` ordering semantics" section - deliberately framed as an ordering-semantics defect,
not "another parser fix" (`order/time` already parsed and applied correctly per its own
definition; that definition was what was wrong), per explicit instruction - plus updated
verification numbers (1312 tests) and the new checksums.

**Published-asset verification.** `gh release download v2.1.0 --dir <fresh temp dir>` then
`sha256sum` matched the freshly-built checksums above exactly; the downloaded jar was smoke-tested
directly with the exact `find k/QA Lecture order/time` reproduction and showed the corrected
ordering - the published asset is confirmed to be the corrected build.

**Final state.** `git rev-parse HEAD` / `origin/main` / `v2.1.0^{commit}` all `451fd93`; `v2.1`
unchanged at `d2c7557`. `git status`: clean except the same two pre-existing untracked
presentation files flagged since Section 0f. No stale release JAR/ZIP, temporary fixture, or
copied GitHub asset left inside the repository - all verification artifacts were written under
this session's external scratch directory.

## 0l. Independent manual QA pass on published `v2.1.0`: one new ordering defect found and fixed (2026-08-14, Claude Code) — read this first

Follow-up, same day, after `v2.1.0` (Section 0k) was already tagged and published: a **second,
independent manual QA pass** - a 14-batch, 52-case interactive regression script run by hand
against a freshly built `v2.1.0` jar and a real, pre-existing 131-activity baseline dataset (not
the bundled sample data) - re-verified every one of Section 0j's six fixes end-to-end and found
them all genuinely fixed, but surfaced **one new defect** the six-bug audit's own test suite had
not covered: `find k/QA Lecture order/time` did not sort chronologically.

**Initial QA result: 51 PASS / 1 FAIL** (52 total cases across the 14 batches, plus an exploratory
pass that found nothing further). The one failure:

```
find k/QA Lecture order/time
Expected: [156] 2026-08-18 18:30 -> 19:00, then [155] 2026-08-25 18:30 -> 19:00
Actual:   [155] 2026-08-25 18:30 -> 19:00, then [156] 2026-08-18 18:30 -> 19:00
```

Two activities sharing an identical `18:30` start time on different dates were not sorted by
date at all.

**Root cause, confirmed by reading `ActivityManager.sort()` before writing any fix (not assumed
from the QA report's own "find vs list" framing).** `find` and `list` (and `listOverdue`) have
**always** called the exact same `sort()` method - there is no find-specific ordering code
anywhere, so this was never a find-vs-list inconsistency at the implementation level. The real
defect: the `TIME` case's comparator was `Comparator.comparing(getSortTime).thenComparingInt(id)`
- **no date term at all** - while the separate `CHRONOLOGICAL` case correctly compared
`date, then getSortTime, then id`. Two activities sharing a start time on different dates
therefore fell through the date-blind `TIME` comparator straight to the `id` tie-break, which is
exactly why it looked like "order/time toggles ID direction" in the QA report - it wasn't toggling
ID order, it was falling back to it, and only in the specific edge case of a tied time-of-day
across dates. Confirmed this affects `list order/time` identically, not just `find`: the existing
scripted `text-ui-test/input.txt` already had a `list order/time` (line 163) and a
`find k/finish assignment order/time` (line 189) case spanning many dates with several repeated
times, and its previously-committed `EXPECTED.TXT` had been silently asserting the old
time-of-day-only order the entire time - undetected until this fix's own `runtest.sh` run
surfaced the diff. Neither `docs/UserGuide.md`, `docs/DeveloperGuide.md`, nor the in-app
`guide order` had ever actually explained what `time` vs `chronological` meant, which plausibly
let this go unnoticed for as long as it did.

**Fix.** `ActivityManager.sort()`'s `TIME` case now falls through to `CHRONOLOGICAL`'s exact
comparator instead of maintaining a second, subtly different one - matching this file's own
"do not create two subtly different date/time comparators" discipline. Both `order/time` and
`order/chronological` remain independently valid input (parsed to distinct `ActivityOrder` enum
values, both still required so existing scripts/muscle memory keep working) and now simply resolve
to the identical ordering. `docs/UserGuide.md` §6.6 and `guide order` were updated to explicitly
document the (now singular) ordering rule, since its previous complete silence on the distinction
was itself part of how this went unnoticed.

**Regression tests:** `find_orderTime_sortsAcrossDifferentDatesChronologically`,
`find_orderTime_sortsSameDateByStartTime`,
`find_orderTimeMixedFixedFlexible_usesCanonicalOrdering` (adopted-flexible start time, not
earliest/latest window, correctly compared), `find_withoutOrderTime_preservesExistingDefaultOrdering`
(guards that the null-order/saved-default path was untouched) - all in `FindCommandParserTest`,
written and confirmed **failing** against the pre-fix code first, then fixed, then re-confirmed
green - plus `find_orderTime_sortsChronologicallyAcrossDatesEndToEnd` in `ApplicationRunnerTest`,
reproducing the exact reported command text end to end.

**Final rerun result: 52/52 (all original cases) + all new regression tests green.** The fix was
independently re-verified against the **same real 131-activity baseline dataset** the original QA
pass used (found still on disk at `unienable (3)/data/`, confirmed pristine - 131 records, highest
ID 146, no `QA`-prefixed activities yet - matching the QA report's own description exactly), not
just synthetic JUnit fixtures: a fresh isolated copy was seeded with that exact data plus the
newly-built jar, and every batch was re-run (reconstructed from the QA report's own detailed
per-batch descriptions, since this session did not have the literal original 52-line script text)
- adopted-placement survival across a note-only edit, fixed-vs-adopted-flexible conflict rejection
via both `add` and `edit`, an unadopted flexible window not blocking a fixed activity, the
recommender correctly proposing a slot around an existing fixed commitment, the preference-change
proposal-lifecycle rejection with its own distinct "no active proposal" control case, three
duplicate-marker rejections (`find k/`, `route from/`, `topic rename new/`), full restart
persistence (including of the adopted placement and the now-correct `find order/time` result),
zero `[OVERLAP]` timetable markers, zero phantom activities (activity count exactly matched every
successful `add`), and zero activity-ID leakage across seven distinct rejected `add`/`edit`/`recur`
attempts in the session. The originally-failing case specifically now returns
`[154] 2026-08-18 18:30 -> 19:00` before `[153] 2026-08-25 18:30 -> 19:00` (fresh IDs in this
re-run's own session, not the original 155/156 - the original script's exact intermediate state
that produced those specific IDs was not available to replay verbatim). Full JUnit suite: 1312
tests, 0 failures (up from 1307 before this fix). Checkstyle, javadoc, `verifyReleaseZip` all
clean. `text-ui-test/runtest.sh` required one `EXPECTED.TXT` regeneration - confirmed via the
standard `ACTUAL.TXT` diff-before-promote procedure that every changed line was a pure reordering
of the identical entry set (same IDs/dates/times/descriptions, no additions, removals, or content
changes), consistent with a sort-order-only fix.

**Commit:** `f5df5f3` ("fix: sort find order/time results chronologically").

`v2.1.0` was **not** retagged for this fix until the full pipeline above was green, per explicit
instruction - see the follow-up note immediately below this section for the retag itself, once it
had actually happened (same two-commit pattern as Sections 0i/0j/0k: this section documents the
fix; a further, separate, untagged commit documents the retag/republish that came after it).

## 0k. `v2.1.0` tagged, released, and verified (2026-08-14, Claude Code) — read this first

Follow-up to Section 0j below, same session, same day: records what actually happened once the six
fixes described there were verified, committed, and pushed, per this file's own established
two-commit pattern (Section 0i itself was written the same way - the commit that gets tagged
records the fixes; a separate, later, untagged commit records the tag/publish narrative once it has
actually happened, not predicted in advance).

**Full pipeline re-run clean, fresh, at the exact commit that got tagged:** `./gradlew clean test
checkstyleMain checkstyleTest javadoc verifyReleaseZip` all green (1307 tests, 0 failures);
`bash text-ui-test/runtest.sh` → `Test passed!` (after promoting the one intentional `guide recur`
`EXPECTED.TXT` line, verified via the standard `ACTUAL.TXT` diff-before-promote procedure - no other
scripted scenario changed); `releaseZip`/`verifyReleaseZip` re-run once more after `runtest.sh`'s own
`clean` step, per this file's standing note that a separate `gradlew` invocation's `clean` doesn't
survive across processes.

**Push.** `git fetch origin` immediately before pushing confirmed `origin/main` was still `1f042ee`
(Section 0j's starting point, no drift). `git push origin main` succeeded as a clean fast-forward
(`1f042ee..fb40b03`, the 9 commits listed in Section 0j). Verified after: `git rev-parse HEAD` and
`git rev-parse origin/main` both `fb40b03`.

**`v2.1.0` tag.** Verified before tagging: `v2.1.0` did not exist, locally or on
`origin` (`git tag --list` / `git ls-remote --tags origin`, both showed only `v2.1`). Per Section
0j's naming reconciliation, this meant creating a **new** tag, not moving or deleting `v2.1`.
Created as an annotated tag at the verified `fb40b03`: `git tag -a v2.1.0 -m "..." fb40b03`, then
`git push origin v2.1.0` (`* [new tag] v2.1.0 -> v2.1.0`). Verified after: `git rev-parse
v2.1.0^{commit}` is `fb40b03`, matching `HEAD`/`origin/main` exactly; `git tag --list` now shows
both `v2.1` (still `d2c7557`, untouched) and `v2.1.0` (`fb40b03`).

**Fresh release JAR/ZIP.** `./gradlew clean releaseZip verifyReleaseZip` from the freshly-tagged
commit (not reusing any earlier build). `unzip -p build/libs/unienable.jar META-INF/MANIFEST.MF`
confirmed `Main-Class: seedu.unienable.UniEnable`; a bare `java -jar` startup smoke test (`bye`)
showed the normal welcome/goodbye banners. SHA-256 (`sha256sum`):
- `unienable.jar`: `5a52f94a109ba6fa37212ac8d72c5511644d9498fa4f6fda0583694332ae3779`
- `unienable.zip`: `fa2cbd2db3f559482522e17403fd89d9ee8cbd41ff1301d9b380e02298f472c6`

**Fresh-directory smoke test against the actual release ZIP** (not the repo's tracked `data/`,
not a bare jar copy): extracted `unienable.zip` into a clean external temp directory and re-ran, in
one scripted session, the exact reported reproductions for bugs A/C/F together (add a flexible
activity, adopt it via `recommend`, edit a non-scheduling field, add an overlapping fixed activity,
run a duplicate-keyword `find`) - every one showed the fixed behaviour: `Adopted     : 10:00 ->
11:00` survived the edit; the overlapping `add` was rejected with `This timing overlaps activity
[1], Study (10:00 -> 11:00).`; `find k/Study k/Assignment` was rejected with `Duplicate option
"k/".`. (Bugs B/D/E were already smoke-tested against an earlier identically-built jar during
Section 0j's own verification pass, before this final rebuild - not re-run a second time here since
nothing in the source changed between that build and this one, only the fresh compile itself.)

**`v2.1.0` GitHub release published.** No pre-existing `v2.1.0` release existed
(`gh release view v2.1.0` → `release not found`, confirmed before creating), so this was a plain
`gh release create v2.1.0 build/libs/unienable.jar build/distributions/unienable.zip --title
"UniEnable v2.1.0" --notes-file <notes> --latest` - not a `--clobber`/asset-replacement case, since
there was no old `v2.1.0` JAR to remove. Published at
[https://github.com/lukelouyu/SelfTeamProject/releases/tag/v2.1.0](https://github.com/lukelouyu/SelfTeamProject/releases/tag/v2.1.0),
not a draft, not a prerelease, marked `--latest`. Release notes cover the six fixes, regression
hardening, documentation updates, and verification results in the structure requested for this
session, with the two checksums above quoted verbatim in the notes body itself.

**Published-asset verification (not just the local build).** `gh release download v2.1.0
--dir <fresh temp dir>` then `sha256sum` on both downloaded files matched the freshly-built
checksums above **exactly**, byte for byte - confirmed independently via `gh release view v2.1.0
--json assets`, whose own server-computed `digest` fields (`sha256:5a52f94a...`/`sha256:fa2cbd2d...`)
also match. A `java -jar` startup + the same add/adopt/edit/overlap/duplicate-keyword scripted
session as above, re-run directly against the **downloaded** jar (not the local build artifact),
showed identical correct output (`Adopted     : 10:00 -> 11:00` present after edit) - the
user-visible GitHub asset is confirmed to actually be the corrected build, not merely the local one.

**Final cleanliness check.** `git status`: clean except the same two pre-existing untracked
presentation files this file has flagged as out-of-scope since Section 0f
(`.codex-presentations/`, `UniEnable_NUS_Enablers_Presentation.pptx.inspect.ndjson`) - left
untouched, not part of this session's scope. No stale release JAR/ZIP, temporary test fixture,
copied GitHub asset, or modified generated diagram was left inside the repository - every build/
smoke-test artifact (including the fresh downloads used for asset verification above) was written
under the session's own external scratch directory, never inside the repo tree.
`git rev-parse HEAD` / `origin/main` / `v2.1.0^{commit}` all `fb40b03`; `v2.1` unchanged at
`d2c7557`.

## 0j. Independent testing-engineering audit: six confirmed correctness bugs fixed, `v2.1.0` corrective release (2026-08-14, Claude Code) — read this first

This session originated from an **independent testing-engineering audit** of the codebase (a bug
report supplied as this session's task, structurally similar to the prior review reports that
drove Sections 0f/0i - a fresh set of findings, not a re-check of those already-fixed ones). It
listed six reported bugs (labelled A-F below); this session's own discipline (matching Section 0f's
"trust but verify") was to reproduce each one against current source *before* writing any fix, not
implement the report's prose on faith. **All six reproduced and were confirmed real defects** -
none turned out to be already fixed or unreproducible. Starting HEAD: `1f042ee` (tip of Section
0i's retag work). Ending HEAD before this note: `e27cbee`.

**A. Editing an adopted flexible activity silently lost its placement (confirmed, fixed).**
`EditCommandParser.buildFlexible` always constructed a brand-new `FlexibleActivity` via the
unadopted-state constructor - so `edit 1 note/Bring notes` on an activity already adopted from a
recommendation (e.g. `Adopted: 10:00 -> 11:00`) silently reverted it to unscheduled, even though
nothing about its schedule was touched. Root cause: the method never read
`((FlexibleActivity) old).getAdoptedStartTime()` at all. Fixed: after building the replacement's
window/duration, the old adopted start is re-applied via `FlexibleActivity.canAdoptAt`/
`setAdoptedStartTime` whenever it still fits (covering both non-scheduling edits, where it always
fits, and scheduling edits that widen or shift the window without excluding the old start); it is
left cleared, never re-applied, only when a scheduling edit genuinely invalidates it -
`MessageFormatter.appendTimingChanges` gained its own `adopted` Before/After diff line so the
confirmation preview states this explicitly (`Before: adopted = 10:00 -> 11:00` /
`After : adopted = None`) rather than losing state silently. Tests:
`editAdoptedFlexible_nonSchedulingEdit_preservesAdoptedPlacement`,
`editAdoptedFlexible_validWindowEdit_preservesAdoptedPlacement`,
`editAdoptedFlexible_invalidatingScheduleEdit_handlesPlacementExplicitly` (all in
`EditCommandParserTest`), plus an `ApplicationRunnerTest` end-to-end case verifying the placement
survives a save + restart reload. Commit `d053d2b`.

**B. `route` could display a different connection than the one Dijkstra actually chose (confirmed,
fixed).** With two parallel accessible connections between the same facility pair (e.g. a 100 m
`PATH` and a 50 m `RAMP`), `route from/A to/B` printed `[1] A -> B | 100 m | PATH ...` alongside
`Total distance: 50 m` - a real, user-visible inconsistency. Root cause:
`AccessibilityGraph`'s Dijkstra only ever recorded a predecessor *facility name*
(`Map<String, String> previous`); `GraphPath` carried only names + total distance, no edge
identity; `RouteCommand.resolveSegments`/`findConnectionBetween` then re-derived a connection per
hop by scanning for the *first* endpoint match in `ConnectionManager`'s load order, with no way to
know which of several parallel connections Dijkstra had actually used. Fixed: `Edge` now carries
the originating `Connection`; the relaxation loop records the winning edge's connection into a
parallel `viaConnection` map at the same moment it updates `bestDistance`; `GraphPath` gained a
`connections` field/`getConnections()` built from that map; `RouteCommand` reads it directly -
`resolveSegments`/`findConnectionBetween`/`connects` are deleted outright, not patched. Tests:
`getShortestPath_parallelEdges_retainsExactChosenConnection` and a load-order-reversed companion
(`AccessibilityGraphTest`), `route_parallelEdges_displaysChosenEdgeAndMatchingDistance` and its
companion (`RouteCommandTest`), plus `GraphPathTest`/`RouteFormatterTest` updates for the widened
constructor. `RouteClassDiagram.puml`/`.png` and `RouteSequence.puml`/`.png` regenerated. Commit
`68762cf`.

**C. A new fixed activity could overlap an already-adopted flexible activity (confirmed, fixed).**
`add`/`edit` accepted a fixed activity overlapping an adopted flexible activity's real committed
time slot, only for the timetable's independent `[OVERLAP]` marker (a completely separate
detection path, `TimetableService.findOverlapIds`) to notice afterward. Root cause:
`ActivityConflictChecker.checkNoConflicts`'s overlap check only ever ran `if (candidate instanceof
FixedActivity)` and only ever compared against other `FixedActivity` instances - an adopted
`FlexibleActivity` was invisible to it entirely, despite representing a real scheduled commitment
exactly like a fixed activity does. Fixed: a new `effectiveInterval(Activity)` helper returns the
activity's occupied `(date, start, end)` - always present for `FixedActivity`, present only once
`hasAdoptedPlacement()` for `FlexibleActivity`, absent (empty) for an unadopted one - and the
overlap check now compares any two activities that both have one, regardless of type combination.
`storage.ActivityStorage.validateAgainstAlreadyLoaded` got the identical, independently-duplicated
fix (storage deliberately does not depend on `logic`, same layering rule Section 11/18 already
document for date/time parsing), so a hand-edited `activities.txt` with an impossible persisted
schedule is now rejected at load time too. An unadopted flexible window still never blocks
anything, confirmed by dedicated tests. Tests (all in `ActivityConflictCheckerTest` unless noted):
`addFixed_overlappingAdoptedFlexible_isRejected`, `editFixed_overlappingAdoptedFlexible_isRejected`,
`scheduledInterval_unadoptedFlexible_doesNotBlockNormalWindowOverlap`,
`scheduledInterval_adoptedFlexible_blocksRealOverlap`, plus adopted-flexible-vs-adopted-flexible
cases, `ActivityStorageTest`'s load-time equivalents, `AddCommandParserTest`/`EditCommandParserTest`
parser-level cases, and `ApplicationRunnerTest` end-to-end. `ActivityConflictValidationClassDiagram.puml`/
`.png` regenerated. Commit `5059868`.

**D. `recur` could create occurrences in the past (confirmed, fixed).** `RecurrencePlanner.plan`
never received `now` at all and did zero past-date checking, so a requested week resolving to an
already-passed calendar date was silently created like any other. Fixed: `now` is now threaded
through `CommandDispatcher.dispatch` -> `RecurCommandParser.parse` -> `RecurrencePlanner.plan` (the
same seam every other time-sensitive command already uses); a new `requireNotPast` check - run
inline in the same per-week loop that already builds the plan entirely before `RecurCommand` even
exists - rejects any target date (other than the source's own, already-skipped date) that is before
`now`'s date, or is `now`'s own date at a time not after the source's inherited start time, mirroring
`DateTimeParser.parseNotBeforeDate`/`requireNotPastIfToday`'s exact two-tier shape. Because this
lives inside `plan()`, the existing "nothing is created until `RecurCommand.execute()` runs, and
that never happens if `plan()` throws" structure gives whole-batch atomicity for free - no
restructuring needed. Error format: `Week 1 resolves to 2026-08-11, which has already passed.\nNo
recurring activities were created.` Tests (all in `RecurrencePlannerTest` unless noted):
`recur_occurrenceBeforeToday_rejectedAtomically`, `recur_todayFixedOccurrenceAlreadyStarted_rejected`,
`recur_invalidOccurrence_createsNothing`, `recur_allFutureOccurrences_stillSucceeds`, plus an
`ApplicationRunnerTest` end-to-end case. A new `RecurrenceTestData.NOW` fixed-clock constant was
added and threaded through every existing recur test (`RecurrencePlannerTest`, `RecurCommandTest`,
`RecurCommandParserTest`, `RecurrenceFormatterTest`, `RecurNextWeekIntegrationTest`,
`ScreenshotRecurrenceRegressionTest`) since `plan()`'s signature gained the required parameter -
zero behavioural change to any of those, confirmed by every one passing unmodified in substance.
`guide recur` and the User Guide's recur section updated; `text-ui-test/EXPECTED.TXT` regenerated
for that one guide-text line only (verified via `runtest.sh`/`ACTUAL.TXT` diff - no other scripted
scenario changed, confirming none of the other five fixes altered any existing scripted output).
`RecurrencePlanningSequence.puml`/`.png` regenerated. Commit `b739a7d`.

**E. Recommendation proposal lifecycle contradicted its own documentation (confirmed - a real
inconsistency between code and docs, not a guessed product rule; fixed).** Per this file's own
"do not guess the product rule" instruction, the *documented* contract was established first, not
assumed: `docs/UserGuide.md` §11/§12.4, `docs/DeveloperGuide.md` §16, and `guide preference`
(`"An existing unadopted proposal is also unaffected until you act on it"`) all unambiguously say a
`preference set`/`preference reset` must **not** discard an active recommendation proposal - only
that `recommend adopt` re-validates it against the *current* profile at adopt time
(`RecommendationService.hasOutOfPreferredRangePlacement`, already implemented and already called
from `RecommendCommandParser`). The actual code contradicted this: `ApplicationRunner.processCommand`
called `recommendationManager.clear()` after **every** successful mutating command with zero
exceptions, including `preference set`/`preference reset` - so by the time `recommend adopt` ran,
the proposal was already gone, and `hasOutOfPreferredRangePlacement` was unreachable through the
real CLI (only reachable in isolated unit tests that construct `RecommendCommandParser` directly,
bypassing `ApplicationRunner` entirely - which is exactly why this shipped unnoticed). Fixed: `code
now matches docs`, not the other way around - `preference set`/`preference reset` succeeding is the
one deliberate, narrow exception to the generic clear-on-mutation rule; every other mutating command
(`add`, `edit`, `delete`, `mark`, `recur`, `reset all`, every `topic` mutation, `recommend adopt`
itself) still clears the proposal exactly as before, since no documented contract says otherwise for
them and re-litigating that per command was out of this fix's scope. Tests: three new
`ApplicationRunnerTest` end-to-end cases -
`recommend_thenPreferenceChangeInvalidatesProposal_adoptRejectedNotLost` (the exact reported
reproduction, asserting the previously-unreachable revalidation message now appears instead of "no
active proposal"), `recommend_thenPreferenceChangeStillFits_adoptStillSucceeds`, and
`recommend_thenUnrelatedMutation_stillClearsProposal` (guard confirming the fix's scope is exactly
the two preference commands). Commit `407cdbe`.

**F. Duplicate command markers rejected inconsistently across parsers (confirmed, fixed).**
`FieldParser.rejectDuplicateMarkers` already existed (Section 0f finding 3) and was already used by
`add`/`edit`, but `find`, `list`, `route`, `connection find`, `facility find`, and every `topic`
subcommand extracted marker fields without it - so `find k/Study k/Assignment` silently mis-parsed
into a garbled two-word keyword search instead of being rejected, `route from/A from/B to/C` and
`connection find from/A from/B` silently absorbed the repeated `from/` into a garbled facility name
(the latter then matching zero connections with no error at all), and `topic rename ... new/Baz
new/Qux` silently renamed to the garbled literal string `"Baz new/Qux"`. Fixed: all six parsers now
call the same `FieldParser.rejectDuplicateMarkers` guard, reusing its existing `Duplicate option
"..."` wording - deliberately **not** migrated onto `ArgumentTokenizer` (same reasoning Section 0f
already established: that would additionally reject any undeclared marker-shaped token found
unquoted in free text, breaking e.g. `n/Meeting w/ friends`, a materially larger change than this
finding calls for). Confirmed safe for every affected parser's own free-text-ish fields (`k/`,
`topic/`, topic names) via `FieldParserTest`'s existing `rejectDuplicateMarkers_undeclaredSlashBearingText_isIgnored`
guarantee - re-verified live: `add n/Meeting w/ friends c/ACADEMIC ...` still accepts the incidental
`w/` unchanged. Tests: two new cases per parser (twelve total) in `FindCommandParserTest`,
`ListCommandParserTest`, `RouteCommandParserTest`, `ConnectionCommandParserTest`,
`FacilityCommandParserTest`, `TopicCommandParserTest`, plus two `ApplicationRunnerTest` end-to-end
cases (`find`, `topic rename`). `dashboard`/`timetable`/`recommend` were reviewed and found already
safe (whole-word-token parsers that already reject a second `date/`/`day/` token, just with
different wording) - deliberately left as-is, not a silent gap. Commit `155570d`.

**Cross-feature regression review (Section 8 of the audit spec).** Beyond each bug's own unit/
parser-level tests, `ApplicationRunnerTest` (commit `c4f5815`) covers every scenario the spec's
Section 8 checklist named end-to-end through the real `CommandDispatcher`/
`CommandTransactionExecutor`/`Storage` pipeline - adopted-flexible lifecycle across a save+restart,
every conflict-lifecycle combination (Fixed↔Fixed already covered by pre-existing tests, Fixed↔
adopted-Flexible and adopted-Flexible↔adopted-Flexible newly covered), routing with parallel edges,
recurrence's past-boundary atomic rejection, duplicate-marker rejection for a representative subset,
and the full recommendation lifecycle across a preference change. All 1307 tests pass (up from 1261
before this session - 46 net new), `checkstyleMain`/`checkstyleTest` clean, `javadoc` builds clean
(0 errors; this session's `./gradlew javadoc` run reported 0 warnings via the default Gradle task
output, differing from the "100 pre-existing warnings" figure earlier sessions cited from a more
verbose invocation - not chased further since it is not a release gate either way, and no error was
present), `bash text-ui-test/runtest.sh` passes (one intentional `EXPECTED.TXT` line for the `guide
recur` text change, confirmed via the standard `ACTUAL.TXT` diff-before-promote procedure - no other
scripted scenario's output changed, direct evidence none of the six fixes altered any existing
command's successful-path output), `./gradlew verifyReleaseZip` passes.

**Manual JAR smoke tests, fresh `data/` directories, real system clock (2026-08-14):** all six
reported reproductions were re-run verbatim (or as close as the real clock allowed) against a
freshly built, freshly extracted `unienable.jar` + `data/`, not just asserted by JUnit -
A: adopted-flexible edit-then-view showed `Adopted     : 10:00 -> 11:00` surviving `edit 1
note/Bring notes`. B: a synthetic two-facility/two-connection dataset (`100 m PATH` id 1, `50 m
RAMP` id 2) printed `[1] A -> B | 50 m | RAMP ...` / `Total distance: 50 m` - matching, not the old
mismatched 100 m/50 m pair. C: adding a fixed activity over the same adopted slot from test A was
rejected with `[Error] Conflict: This timing overlaps activity [1], Study (10:00 -> 11:00).` D: **not
reproducible live against the real wall clock** - the bundled `data/academic-calendar.txt`'s
earliest instructional week (Week 1, `2026-08-10`-`2026-08-14`) had not yet fully elapsed as of this
session's real system date (`2026-08-14`), so there was no genuinely past instructional week to
request; covered instead by the deterministic injected-clock JUnit suite above, which is strictly
more rigorous than a real-clock manual repro could be for a date-boundary bug and is this project's
own standing convention for exactly this reason (Section 5's "hardcoded near-future placeholder"
risk family). E: the exact reported scenario (`recommend` -> `preference set start/10:30` -> `y` ->
`recommend adopt`) produced `[Error] Invalid input: This recommendation proposal no longer fits your
current preferred daily start/end - preferences changed since it was generated. Generate a new
recommendation with recommend.` - not `"No recommendation proposal is currently active."` F: `find
k/Study k/Assignment`, `route from/A from/B to/C`, and `topic rename ... new/Baz new/Qux` all
produced `Duplicate option "..."` for their respective repeated marker; `topic add c/ACADEMIC
n/Foo` (control) succeeded normally in between, confirming the fix didn't over-reject.

**No reported issue was found not reproducible.** All six were real, confirmed defects.

**Documentation and diagrams synced:** `docs/UserGuide.md` (duplicate-marker scope widened past
add/edit; edit's adopted-placement carry-over/clear rule; fixed-activity overlap wording now
covers adopted flexible activities; `route`'s parallel-edge connection-identity guarantee; `recur`'s
past-occurrence rejection rule), `docs/DeveloperGuide.md` (§7 conflict-validation rules rewritten
for effective-occupied-interval; §8 edit section gained the adopted-placement carry-over paragraph;
§9 recur section gained the `now`-threading/`requireNotPast` paragraph; §11 storage section's
overlap description corrected; §12 route section rewritten for `Edge`/`GraphPath` connection
tracking and `RouteCommand`'s simplified segment reading, with the old buggy `resolveSegments`
approach explicitly called out as removed, not just superseded silently; §16 recommend section
gained a "Proposal lifecycle vs. other mutating commands" paragraph naming the
`isPreferenceMutation` exception explicitly; §18 duplicate-marker bullet widened to the six newly
covered parsers, plus a new bullet on the `ScheduledInterval` duplication mirroring the existing
date/time-parsing-duplication discussion), `command.general.GuideCommand`'s `recur` topic text.
Four diagrams regenerated from their already-updated `.puml` sources via the same local
`plantuml.jar` toolchain prior sessions used (`C:\Users\lukel\AppData\Local\Temp\
unienable-plantuml-tool\tp-master\tools\plantuml.jar` - still present in this environment, reused
without a fresh permission prompt since it is neither new nor freshly downloaded this session):
`RouteClassDiagram`, `RouteSequence`, `ActivityConflictValidationClassDiagram`,
`RecurrencePlanningSequence` (all `.puml`+`.png` pairs, visually verified rendered correctly, not
error placeholders). 22 `.puml`/22 `.png` files remain 1:1 paired. `EditActivitySequence.puml` and
the recommendation-lifecycle diagrams were reviewed and found to already be accurate at their
existing level of abstraction (the adopted-placement carry-over and the `ApplicationRunner`-level
proposal-clearing exception are both internal details one level below what those specific diagrams
show) - deliberately left unregenerated, not overlooked.

**Product-lifecycle decision made:** Bug E above - "code now matches docs" was the applied fix
direction, established by reading the existing documented contract first (per this file's own "do
not guess the product rule" instruction), not by picking whichever direction seemed more
defensible in isolation.

**Test-count change:** 1261 → **1307** (46 net new tests), zero deleted or weakened.

**Commits, in order:**
| Commit | Message |
|---|---|
| `d053d2b` | fix: preserve adopted placement across flexible edits |
| `68762cf` | fix: retain exact connection edges in route paths |
| `5059868` | fix: include adopted flexible activities in conflict checks |
| `b739a7d` | fix: reject past recurrence occurrences |
| `407cdbe` | fix: align recommendation proposal lifecycle with documented contract |
| `155570d` | fix: reject duplicate command markers consistently |
| `c4f5815` | test: add cross-feature end-to-end regression coverage for QA fixes |
| `e27cbee` | docs: synchronize guides after second-pass QA fixes |

(`EditCommandParserTest.java`'s changes landed in the Bug C commit `5059868` rather than Bug A's
`d053d2b`, since the file carries one Bug-C-only regression test alongside Bug A's three - splitting
a single file's hunks across two commits via non-interactive tooling was judged not worth the
fragility for one test method; every commit's test suite still passes standalone at that point in
history, since Bug C's own production fix is what that one test needs, not Bug A's.)

**`v2.1` vs. `v2.1.0` naming, reconciled for future sessions:** this file's history (Sections
0h/0i) records prior release work under the tag name `v2.1` - that tag exists, at `d2c7557`, and is
**left untouched by this session**, preserving historical release integrity exactly as instructed.
The user explicitly requested this corrective release be published as `v2.1.0`. Verified before any
tag operation: `v2.1.0` did not exist, locally or on the remote, before this session. Per instruction,
since only `v2.1` existed and `v2.1.0` did not, `v2.1` was **not** deleted, moved, or reinterpreted -
`v2.1.0` is a **new** tag, cut fresh at this session's own verified HEAD (see the follow-up note
below for the exact commit and verification results). Any future session should treat `v2.1` and
`v2.1.0` as two distinct, both-real tags: `v2.1` is the original v2.1 line (including its own 0i
retag for three earlier fixes), `v2.1.0` is this session's corrective pass on top of it. Do not
assume they are the same tag under two names, and do not silently delete either without a fresh
explicit instruction.

Verification, commit, push, tag, and publish results for this pass are completed and recorded in
the follow-up note immediately below this section (mirroring Section 0i's own two-commit pattern:
this section's own commit is the one that gets tagged; the tag/publish narrative is recorded
afterward, once it has actually happened, rather than predicted here).

## 0i. Second-pass re-audit of v2.1: three new correctness fixes (2026-08-10/11, Claude Code) — committed, pushed, and `v2.1` retagged/re-released

An independent re-audit of the tagged/released v2.1 snapshot (Section 0h below) found three real
new-or-remaining correctness defects and one diagram-accuracy issue, on top of confirming every
Section 0f finding was still correctly fixed. All were verified against current source before
fixing (same discipline as 0f), fixed, regression-tested, and validated end to end against a
rebuilt JAR. Full detail: `CODEBASE_AUDIT_REPORT.md` Section 12 (which now supersedes Section 11's
original `READY` verdict - Section 11 is kept, marked superseded, not deleted).

**1. ID exhaustion silently disabled every unrelated mutating command (confirmed regression from
0f's own fix, fixed).** 0f's `getNextId()` throwing once exhausted was correct in isolation, but
`CommandTransactionExecutor.Snapshot`'s field initializer called it unconditionally for *every*
mutating command's pre-execution snapshot - so exhaustion transitively blocked `mark`, `delete`,
`edit`, `reset all`, topic/preference mutations, everything, not just `add`/`recur`'s own
allocation. `ResetCommand.hasAnythingToReset()` had the identical problem via its own `getNextId()`
call, blocking even the reset *menu* from being shown - the one command that should be able to
recover from this state could not run either. Fixed: `ActivityManager` gained a non-throwing
`snapshotIdAllocation()` (returning a new `IdAllocationState(nextId, idSpaceExhausted)` record)
for `Snapshot`'s exclusive use, `hasAllocatedAnyId()` for `ResetCommand`, and `tryGetNextId():
OptionalInt` for a narrower related gap in `ResetCommand`'s post-reset success message.
`restoreState()` now takes an `IdAllocationState` and restores the exhaustion flag from it, instead
of unconditionally clearing it to `false` (a related bug: a snapshot taken while genuinely
exhausted used to silently un-exhaust the manager on rollback). Tests: 7 new in
`ActivityManagerTest`, 2 new in `ApplicationRunnerTest` reproducing the exact reported end-to-end
scenario (`mark 2147483647` then `delete`+`reset all` with the ID space exhausted). Commit
`2aee025`.

**2. Recur's ID-capacity exhaustion surfaced as a generic internal error (confirmed, fixed).**
0f's `Math.addExact` fix in `RecurrencePlanner` was arithmetically correct but throws a raw
`ArithmeticException`, not a `UniEnableException` - an anticipated capacity limit reported as
"[Error] An unexpected internal error occurred" instead of a clean validation message. Fixed:
explicit `long` computation + `InvalidActivityException("not enough activity IDs remain...")`,
matching every other early-rejection condition already in that method. The existing regression test
updated to expect the domain exception instead of the raw one. Commit `2a64f1d`.

**3. Time-sensitive validation went stale during the confirmation wait (confirmed, fixed - the
most realistic-in-ordinary-use of the three).** `now` was sampled exactly once per command, at
dispatch time, before a `Confirmable` command's y/n prompt was even shown. A user can take a real
amount of time to answer; if the basis of a "not in the past" check elapsed during that wait, the
stale value was still silently accepted. Reproduced for both `recommend adopt` (a proposed
placement elapsing while answering "Proceed with adoption?") and `edit` (a newly-requested start
time elapsing while answering "Save changes?"), using a `Supplier<LocalDateTime>` in tests that
advances between dispatch and post-confirmation execution - no real elapsed time needed to
reproduce deterministically. Fixed with a new `PreExecutionValidatable` interface
(`validateBeforeExecution(now)`), called by `ApplicationRunner` with a freshly-sampled `now`
immediately after confirmation and immediately before transaction execution; only
`RecommendAdoptCommand` and `EditCommand` implement it (the only two `Confirmable` commands with a
time-sensitive check that can go stale during the wait - `add` isn't `Confirmable` at all).
`EditCommand`'s re-check only applies when the edit actually supplied a new date/start-time value
(the boolean is computed once by `EditCommandParser`, mirroring its own existing parse-time
condition exactly, and passed through the constructor - re-deriving it by diffing old vs. new
activity was considered and rejected, since it would subtly change semantics for a user
re-supplying an unchanged value). Tests: 2 new end-to-end `ApplicationRunnerTest` cases
reproducing both exact scenarios; `EditCommandTest`'s existing 8 cases updated for the new
constructor parameter (defaulting to `false`, i.e. no behavioural change for any of them). Commit
`cd87438`.

**4. Adoption diagram's loop didn't match the two-phase source (confirmed, fixed).**
`RecommendationAdoptionSequence.puml` (from 0g) showed one interleaved validate-then-mutate loop
per placement; `RecommendAdoptCommand.execute()` actually validates every placement first
(collecting `AdoptionTarget`s), then mutates every placement second, in two separate loops - so a
bad later placement can never leave earlier ones partially adopted. Split the loop to match, added
a note explaining why, relabelled the snapshot note as an explicitly simplified view of the
`CommandTransactionExecutor`-owned flow (pointing to the Mutation rollback sequence diagram for the
full picture, rather than redrawing that participant into this diagram), and added the new
finding-3 `validateBeforeExecution(now)` step with its own stale-during-confirmation-wait branch,
since it postdates when this diagram was first drawn. `DeveloperGuide.md` Sections 8 and 16
updated in prose for the same reason. Commit `dc62a95`.

**Validation:** full pipeline re-run clean (1261 tests, up from 1250; Checkstyle; Javadoc;
`verifyReleaseZip`'s extracted-distribution smoke test), `text-ui-test/runtest.sh` passes with no
fixture changes needed, plus three manual smoke tests against a freshly rebuilt JAR (seeded
`activities.txt`/`academic-calendar.txt` fixtures) directly reproducing and then confirming the fix
for the ID-exhaustion and recur-capacity scenarios end to end.

**Release decision:** since this session's work was prompted by a bug report rather than an
explicit release request, whether to publish anything was asked rather than assumed (same
discipline as Section 4's "explicit permission required" rule for anything user-visible). The user
chose to **retag `v2.1` in place** (not cut a separate `v2.1.1`), matching the project's own
`v2.0.1` retag precedent (Section 0h's own history: `git show v2.0.1` shows it was moved once
already, from its original cut to `dca0675`). Executed as: `git tag -d v2.1` + `git tag -a v2.1`
at the new HEAD (`d2c7557`) + force-replace the remote tag
(`git push origin :refs/tags/v2.1` then `git push origin v2.1`) - deleting the underlying tag
briefly turned the existing GitHub release into an orphaned `draft` under a synthetic
"untagged-..." URL, which needed `gh release edit v2.1 --tag v2.1 --target d2c7557 --draft=false
--latest` to reattach and republish, plus `gh release upload v2.1 <jar> <zip> --clobber` to
replace the assets. Release notes were extended in place (not replaced) with a "Retagged
(2026-08-11)" section covering the three fixes, following the same append-don't-rewrite pattern
`v2.0.1`'s own notes already used for its own retag. Verified after the fact: `gh release
download` + `sha256sum` on both assets matched the freshly-built
`d75e7be9.../ea4e68fe...` checksums exactly, and `git rev-parse v2.1^{commit}` matches
`git rev-parse HEAD`.

## 0h. v2.1 tagged and released (2026-08-10, Claude Code)

Cut immediately after Section 0g below, at commit `e8276f0` (0f's audit fixes + 0g's diagram
split, nothing else). Full pipeline re-run fresh before tagging: `clean test checkstyleMain
checkstyleTest javadoc verifyReleaseZip`, then the Unix text-UI harness separately (its own
`clean` step meant `build/distributions/unienable.zip` had to be rebuilt with `verifyReleaseZip`
afterward, since `clean` doesn't survive across separate `gradlew` invocations) - 1,250 tests,
Checkstyle, Javadoc, release-ZIP verification (extracted-distribution smoke test), and the text-UI
harness all passed. Annotated tag `v2.1` created and pushed;
[GitHub release](https://github.com/lukelouyu/SelfTeamProject/releases/tag/v2.1) published with
both `unienable.jar` and `unienable.zip` attached (SHA-256: `af50d5...` / `0d606a...`, full
strings in the release body) and detailed notes covering every fix from Section 0f plus the
Section 0g diagram split, matching v2.0.1's release-notes format/tone. No CHANGELOG.md exists in
this repo - release notes live only in the GitHub release body, consistent with v1.0/v2.0/v2.0.1.

## 0g. Split RecommendationSequence into generation/adoption diagrams (2026-08-10, Claude Code) — committed and pushed

Follow-up to Section 0f below, same day: the user (reviewing 0f's work) asked for
`RecommendationSequence.puml` to be split, pointing out it combined two conceptually different
workflows in one 80+ interaction diagram - the read-only proposal lifecycle (generate/view/cancel)
and the state-changing adoption transaction (validate/confirm/mutate/persist/rollback) - and gave a
precise spec: split at the existing `== Adopt proposal ==` marker, drop `Storage`/
`RecommendAdoptCommand` from the generation diagram's participants (neither is ever touched by that
half), restart `autonumber` at 1 in each, and title them "Generating and Managing a Recommendation
Proposal" / "Validating and Adopting a Recommendation".

Implemented exactly as specified: `RecommendationSequence.puml` renamed/split into
`RecommendationGenerationSequence.puml` (generate/view/cancel, 44 numbered steps) and
`RecommendationAdoptionSequence.puml` (the full adopt flow — both `alt` guards, confirmation
prompt, placement `loop`, save/rollback `alt`, 42 numbered steps), both regenerated to `.png` via
the same `/tmp/plantuml.jar` toolchain used throughout 0f and visually verified to render
correctly before committing. `DeveloperGuide.md` §16 updated: the old single diagram embed is now
two embeds, each preceded by a short bold lead-in sentence (matching this section's existing
prose style of bold-lead-in subsections like "**Whole-day optimization.**" rather than introducing
new `###` subheadings, which aren't this section's convention) explaining what that half of the
flow does and doesn't touch. `CODEBASE_AUDIT_REPORT.md`'s Finding 6 discussion (which had said the
`RecommendationSequence` diagram was "left untouched" during 0f) got a parenthetical noting the
later split, so the report doesn't dangle a reference to a file that no longer exists. Commit
`699080e`.

## 0f. Full codebase audit remediation pass: overflow, rollback, parser, ID, and doc-drift fixes (2026-08-10, Claude Code) — read this first, committed and pushed

Starting point: `a84146d` (already includes 0e's recommender rewrite, already committed and
pushed by the time this session started — see the note at the top of Section 0e; it is stale).
An independently-written, detailed audit (eight findings, P1–P4) was supplied as this session's
task. Every finding was re-verified against current source before any change, per this file's own
"trust but verify" discipline, using eight parallel research passes (one per finding) reading full
source + full tests + git history before touching anything. Two findings turned out to be only
*partially* still open — see below. Full narrative, root-cause analysis, and a
recommendation→implementation traceability table for all eight live in
`CODEBASE_AUDIT_REPORT.md` at the repo root (new file, this session); this section is a condensed
pointer to it, not a replacement.

**1. `AccessibilityGraph` cumulative-distance overflow (confirmed, fixed).** Dijkstra's running
distance was plain `int` end to end (`bestDistance` map, `QueueEntry.distanceInMetres`, the
`candidate` local, `GraphPath.totalDistanceInMetres`), while `ConnectionStorage` only rejects
`distanceInMetres <= 0` — no upper bound. A multi-hop route whose true total exceeds
`Integer.MAX_VALUE` could wrap to negative and get wrongly preferred by Dijkstra's comparison.
Fixed: `bestDistance`/`QueueEntry`/`GraphPath.totalDistanceInMetres` now `long`; individual edge
distances stay `int` (already bounded by storage). `reconstructPath` also gained a visited-set
cycle guard (`IllegalStateException` on a repeated facility) as defence against any future
corruption reaching it, and was made package-private specifically so the guard is directly
testable without needing to first corrupt a live Dijkstra run. Tests:
`AccessibilityGraphTest.getShortestPath_cumulativeDistanceExceedsIntegerMaxValue_choosesTrueShortestPath`,
`..._multiHopAboveIntegerMaxValue_reportsExactUnwrappedTotal`,
`reconstructPath_cyclicPredecessorMap_throwsInsteadOfLoopingIndefinitely`,
`GraphPathTest.getTotalDistanceInMetres_valueAboveIntegerMaxValue_preservedExactly`. Verified
end-to-end against the built shadow JAR with a synthetic three-facility dataset (two 1.2B-metre
hops vs. a 2B-metre direct edge) — correctly chose the direct edge. Commit `3b036d6`.

**2. Rollback gap on unchecked persistence failure (partially confirmed — the checked-exception
path was already fixed by an earlier commit not yet reflected in this file; only the unchecked
path was still open — fixed).** `ApplicationRunner.trySave()` only caught `StorageException`; an
unanticipated `RuntimeException` from `Storage.saveAll()` propagated past the
`if (!trySave()) { execution.rollback(); ... }` call site entirely and was swallowed by the outer
boundary catch with no rollback, leaving the in-memory model mutated while disk still reflected
the pre-command state. Fixed by widening `trySave()`'s catch clause to also catch
`RuntimeException` and report it the same way — the existing rollback-on-failed-save call site
then covers both cases automatically, no restructuring needed. Tests:
`ApplicationRunnerTest.add_saveFailsWithRuntimeException_rollsBackAndReportsNoFalseSuccess`,
`..._saveFailsWithRuntimeExceptionThenRetrySucceeds_doesNotConsumeIdOnFailedAttempt`, plus two new
test doubles (`RuntimeExceptionAfterStorage`, `RuntimeExceptionOnceThenSucceedStorage`) mirroring
the existing `StorageException`-based ones. Commit `7562104`.

**3. Duplicate Add/Edit field prefixes silently absorbed (confirmed, fixed — deliberately
*not* via the "migrate to `ArgumentTokenizer`" approach the supplied audit recommended).**
`add n/A n/B c/...` silently merged the second `n/` into the first field's value ("A n/B") instead
of being rejected, while `ArgumentTokenizer` (used only by `preference set`) already rejects a
repeated marker. Fixed via a new `FieldParser.rejectDuplicateMarkers(text, markers...)`, called by
both `AddCommandParser`/`EditCommandParser`, reusing `ArgumentTokenizer`'s `Duplicate option "..."`
wording for consistency. **Deliberately did not migrate `add`/`edit` onto `ArgumentTokenizer`
itself**, even though that was the externally-suggested "best solution": `ArgumentTokenizer`
additionally rejects any *undeclared* marker-shaped token found unquoted anywhere in the text,
which would force `add`/`edit`'s free-text description/note fields to require quoting any
incidental `word/` (e.g. `n/Meeting w/ friends` would need quoting) — a materially larger
behavioural change than the finding called for. The two prior pinning tests
(`parseAdd_markerSuppliedTwice_firstOccurrenceValueAbsorbsTheSecond`,
`parseEdit_markerSuppliedTwice_firstOccurrenceValueAbsorbsTheSecond`) were replaced with rejection
tests, not merely deleted. `text-ui-test/input.txt`'s scripted `edit 1 n/New name n/Another name`
case, which depended on the old absorb behaviour for its downstream assertions, was updated (added
a follow-up `edit 1 n/New name` so the rest of the script's activity-1-renamed assumptions still
hold) and `EXPECTED.TXT` regenerated from verified-correct output. Commits `17bba67`, `6952cda`.

**4. `UniEnableTest` E2E suite used the real wall clock (confirmed, fixed — time-critical: today
is 2026-08-10, the suite's earliest hardcoded "not before today" date literal is 2026-08-15, so
this genuinely had days left before it would have started failing).** All three
`UniEnable.run(...)` call sites in the file used the 2-arg overload (real
`LocalDateTime::now`) instead of the existing 3-arg injectable-time overload already used
correctly elsewhere (e.g. `ApplicationRunnerTest`). Fixed: added
`private static final LocalDateTime TEST_NOW = LocalDateTime.of(2026, 8, 10, 12, 0);` (strictly
before every hardcoded date literal in the file) and threaded it into all three call sites via the
3-arg overload. All 47 existing `UniEnableTest` cases re-verified passing unchanged with the fixed
clock. No other file in `src/test` was found calling a real-clock API. Commit `b4ba68f`.

**5. `ActivityManager` next-ID counter overflow (confirmed, fixed).** `loadAll`'s
`Math.max(nextId, activity.getId() + 1)` silently overflowed for a loaded `id ==
Integer.MAX_VALUE`, and — worse than a simple wraparound — `Math.max` then reset `nextId` back
down to `1`, masking that the ID was ever taken and risking a duplicate-ID collision on the very
next `add()`. The same unchecked-arithmetic shape existed in `restoreState`, `add` (`nextId++`),
`addAllAtomically` (`nextId + index`, `nextId += candidates.size()`), and
`RecurrencePlanner.plan`'s own `nextId + toCreate.size()`. Fixed: `ActivityManager` gained a
`boolean idSpaceExhausted` flag; `loadAll`/`restoreState` compute the candidate next ID as `long`
(so the comparison itself can't overflow) and set the flag once that value would exceed
`Integer.MAX_VALUE`; `getNextId()`/`add()`/`addAllAtomically()` all throw `IllegalStateException`
once exhausted instead of wrapping or silently reusing an ID, and a batch call is rejected
atomically upfront if it needs more IDs than remain. `RecurrencePlanner` uses `Math.addExact`.
IDs stay plain `int` throughout — only the bookkeeping arithmetic was widened/checked, not the ID
type. Tests: seven new `ActivityManagerTest` cases (load-time exhaustion, final-ID consumption,
post-exhaustion rejection for single/batch add, exact-fit batch, reset recovery) plus
`RecurrencePlannerTest.plan_nextIdNearIntegerMaxValue_throwsArithmeticExceptionInsteadOfWrappingCandidateId`.
Commit `b82c644`.

**6. Developer Guide recommender section drift (partially confirmed — the supplied audit's
premise assumed the whole section was stale; re-verification against current source found only
one paragraph actually was, since 0e's own rewrite above had already fixed the rest — fixed).**
`grep` confirmed `preferredRangePenalty`/`chooseBestSlot`/`chooseNextActivity` don't exist anywhere
in `src/main`; the DG's whole-day-optimization narrative (added by 0e / commit `d9d89fe`) already
correctly says so. What was actually still stale: one earlier paragraph, never revisited since
`d9d89fe` deleted the field it describes, claiming `preferredRangePenalty` was "kept rather than
removed... always 0... a no-op safety net" — directly contradicted by the very next paragraph in
the same section, which correctly says the whole scoring apparatus was removed. Corrected only
that one paragraph; the rest of the section, and the recommendation class/sequence diagrams, were
confirmed accurate and left untouched. Commit `3b19069`.

**7. Date/time-parsing duplication between `DateTimeParser` and `ActivityStorage` (confirmed real,
declined — diverges from the supplied audit's "extract a shared codec" recommendation).**
Real, verbatim duplication of format constants and strict-parse logic exists. `ActivityStorage`
already carries a comment explaining this is *intentional*: a persistence codec must keep reading
the exact bytes it already wrote regardless of how the CLI parser's input rules evolve, so a
shared codec would risk exactly the coupling that comment warns against (a future parser-ergonomics
relaxation silently changing what storage can reload). No code change; the existing rationale was
elevated into a new `DeveloperGuide.md` §18 bullet so it's discoverable without reading the comment
in isolation. Commit `797bf07`.

**8. Large-class review (partially justified — diverges from the supplied audit's specific
suggestion to decompose `GuideCommand`).** All eight originally-flagged classes were read in full.
`GuideCommand` (585 lines) was judged **not** to need decomposition: ~35 lines are command logic,
the rest is one static help-text lookup table, not branching logic — splitting a map literal
across five new files (`GuideTopic`/`GuideContentProvider`/etc., as the supplied audit suggested)
would relocate complexity, not reduce it. Two *different* extractions were independently judged
genuinely justified and were **not implemented, only documented as deferred** (correctness fixes
took priority within this session's scope): a `NextActivityFinder` from `ActivityManager`'s
~150-line stateless "next relevant activity" selector (mirroring the existing
`ActivityConflictChecker` precedent), and an `AtomicFileTransaction` from `Storage`'s ~150-line
generic atomic multi-file commit/rollback engine (zero domain knowledge, independently testable) —
the latter wasn't called out by the supplied audit at all. Recorded in `DeveloperGuide.md` §18
under "Maintainability review (2026-08 codebase audit)". No commit beyond the doc note (`ec528a3`).

**Docs synced:** `UserGuide.md` §4 (duplicate-prefix rejection rule);
`DeveloperGuide.md` §5 (ID-overflow guard), §10 (rollback, now naming
`CommandTransactionExecutor` explicitly), §12 (route distance `long`/`int` split + cycle guard),
§16 (Finding 6 above), §18 (four new/updated design-decision bullets), §19 (`UniEnableTest`'s
deterministic time). Diagrams regenerated: `MutationRollbackSequence.puml`/`.png` (the "file
commit fails" branch now also covers an unexpected `RuntimeException`),
`RouteClassDiagram.puml`/`.png` (`GraphPath.totalDistanceInMetres` corrected from `int` to `long`
— was stale even before this session).

**Validation:** `./gradlew clean test checkstyleMain checkstyleTest javadoc shadowJar build` all
green (1250 tests, 0 failures — up from 1236 before this session's 14 net-new test methods);
`text-ui-test/runtest.sh` passes after the Finding 3 fixture update; a manual release smoke test
against the built JAR verified both the duplicate-prefix rejection and the route-overflow fix
end-to-end (not just via unit tests). 14 commits, each independently reviewed and tested before the
next; pushed to `origin/main` as a clean fast-forward (`a84146d..e7bb8d5`). Working tree clean
except three pre-existing untracked presentation files unrelated to this session's scope, left
untouched throughout.

**One correction to Section 0e above:** its header still says "not yet committed" as of this
writing, but git history shows its content (commit `d9d89fe` and neighbours) was committed and
pushed well before this session started (`a84146d`, this session's starting HEAD, already includes
it). Left Section 0e's own text unedited since verifying and rewriting historical entries was out
of this session's scope — flagging it here so the next session doesn't repeat the "not yet
committed" assumption.

## 0e. Whole-day recommender rewrite, command aliases, and boundary-enforcement guard (2026-08-06, Claude Code) — read this first, not yet committed

**Not committed as of this note.** Everything below is verified in the working tree on top of
`93c545a` (Section 0d's merge commit `2897709` plus one later commit) but has not been committed or
pushed - a session doing further work here should commit deliberately rather than assuming this is
already on `origin/main`.

**1. Greedy recommender defect (confirmed, fixed).** A PE-style review reproduced Section 0c's
flagged `chooseBestSlot` concern as an *observable* defect, not just theoretical: `chooseNextActivity`
picked one activity at a time (fewest-valid-slots-first, each placed at its own earliest slot), so a
short activity could claim a slot that was the only way a longer activity later the same day could
fit - `recommend` reported activities unscheduled even when a valid whole-day schedule containing
them existed. Two concrete review examples reproduced this (three activities on one day; four
activities under a restrictive 60-minute-buffer profile).

**Fix:** `RecommendationService` now groups each period's eligible flexible activities by date and
solves each date independently. For up to 8 remaining activities on a date it exhaustively tries
every ordering (`permute`, 8! = 40320 worst case) and places each ordering's activities at their own
earliest feasible slot given what's already placed earlier in that ordering (`placeInOrder`/
`earliestValidSlot`) - provably at least as good as any other placement of that ordering, so trying
every ordering explores every combination of which activities end up schedulable together. Beyond 8,
`heuristicOrderings` substitutes four fixed orderings (tightest window, longest duration, earliest
window, stable ID) to keep the search bounded. Candidate `DaySchedule`s are ranked by
`DaySchedule.betterThan`: most activities scheduled, then most total duration, then lowest total
window slack among scheduled activities, then earliest aggregate placement, then lowest sorted
activity-ID list as the final deterministic tie-break. This entirely replaces
`chooseNextActivity`/`chooseBestSlot` and the buffer-slack/energy-spread/sensory-spread/
preference-penalty scoring fields that Section 0d's release notes called out as known technical
debt - they are gone, not just documented as dead, since the one-activity-at-a-time design they
existed for no longer exists.

**Tests added:** `RecommendationServiceTest`'s `recommendDate_threeActivitiesFitTogether...`,
`..._restrictiveProfileFourActivitiesFitTogether...`, and
`..._restrictiveProfileTwoActivitiesFitTogether...` reproduce the review's two examples plus a third
(Thursday) scenario directly and assert every activity in them is now scheduled.

**2. `dashboard`/`timetable`/`recommend` marker inconsistency (confirmed, fixed).** The same review
flagged that `timetable day/DATE` and `dashboard date/DATE` used different, non-interchangeable
markers, and that the natural `dashboard day/DATE` guess was rejected - "documented behaviour" but
still the kind of inconsistency that causes user errors. `dashboard` now also accepts `day/`,
`timetable` now also accepts `date/`, and `recommend` now also accepts `day/` (it already had
`date/`); every command resolves either marker identically. `DashboardCommandParser`,
`TimetableCommandParser`, and `RecommendCommandParser` were updated, plus their parser tests
(including `timetable`'s existing `...resolvesSameAsDayMarker` equivalence-assertion pattern,
extended to the new alias) and `guide dashboard`/`guide timetable`/`guide recommend` text.

**3. Preferences don't retroactively move adopted placements (confirmed usability gap, minimum fix
applied - not the full `recommend replan` feature the review sketched).** The review's "at minimum"
fallback ask was applied: `preference set` and `preference reset`'s confirmation prompt now states
that activities already adopted from a recommendation keep their existing scheduled times and that
the change only affects future `recommend` proposals. A full replan/unadopt feature was
**deliberately not built** - there is no product-exposed way today to revert a single flexible
activity's adopted placement back to "pending," and inventing one was out of scope for this pass.

**4. Boundary-enforcement re-investigation (explicitly requested by the user; found NOT reproducible
on this code, defense-in-depth guard added anyway per explicit instruction).** After the above
landed, the user separately asked to re-verify Section 0c's preferred-start/end fix end-to-end,
supplying a specific repro (`preference set start/07:30 end/21:00 buffer/30`, one flexible activity
windowed `06:00-09:00`, another `20:30-23:00`, both 60 minutes) and claiming placements still landed
outside the preferred range. **This was investigated before writing any fix**, per this file's own
standing "trust but verify" discipline: a fresh `shadowJar` was built and the exact repro run
end-to-end through the real CLI. Result matched the *expected*, non-buggy behaviour exactly - the
early activity clamped to `07:30`, the late one was left unscheduled (only 30 preferred-range
minutes available against its 60-minute duration). Reading the code confirmed why: `recommend
date/...`, `recommend this week`, and `recommend next week` all funnel through the single
`earliestValidSlot` search-bound computation from Section 0c's fix, and neither `recommend view` nor
`recommend adopt` ever recompute or override a placement's time - there is structurally only one
place a candidate time is ever produced. **No live defect was found.** The user was told this
directly, with the repro transcript as evidence, and asked how to proceed; they chose to add the
requested defense-in-depth guard anyway rather than stop.

**Guard added (belt-and-suspenders, not a bug fix):** `RecommendationService.withinPreferredRange`
is the single boundary predicate. `build()` re-checks every `ScheduledItem` against it immediately
before adding a `RecommendedPlacement` to a proposal (discarding to unscheduled on failure, though
this is unreachable in normal operation since `earliestValidSlot`'s search bounds already guarantee
it). Separately, and with genuine new value (not just redundant defense): `recommend adopt` now also
rejects a proposal via the new `RecommendationService.hasOutOfPreferredRangePlacement(proposal,
preferenceManager.getProfile())` if `preference set`/`preference reset` ran *after* the proposal was
generated and the new preferred range no longer contains one or more of its placements - this is a
real gap the stale-time check alone didn't cover, since a preference change doesn't advance `now`.

**Tests added:** `RecommendationServiceTest`'s
`recommendDate_earlyWindowClampsToPreferredStart_lateWindowLeftUnscheduled` (the exact repro),
`recommendThisWeek_everyPlacementStaysWithinPreferredRange`,
`recommendNextWeek_everyPlacementStaysWithinPreferredRange`,
`recommendDate_todayClampAndPreferredEndBothApply`, and three `hasOutOfPreferredRangePlacement_*`
cases; `RecommendCommandParserTest`'s
`parse_adopt_rejects/acceptsProposalThatNoLongerFitsPreferredRangeAfterPreferenceChange`.

**Docs:** `DeveloperGuide.md` §16 rewritten for the whole-day search (replacing the stale
`chooseBestSlot`/known-technical-debt description) and given a new "Boundary-enforcement pipeline"
subsection naming all four stages (generation/selection/final guard/adoption). `UserGuide.md`
§9/§10/§12.2 document the marker aliases; §11 documents the adopted-placements note; §12.4/§12.6 add
the preference-change adoption rejection and a concrete boundary example. `guide preference`/`guide
recommend` updated to match. `RecommendationClassDiagram.puml`/`RecommendationSequence.puml` sources
updated (new method, new dependency edges, new alt-block in the adopt flow) - **their `.png` renders
were not regenerated, since no PlantUML renderer is available in this environment; whoever next has
the toolchain should re-render both before this is considered fully closed.**

**Result:** every recommendation output (any period, any command) is now guaranteed within the
preferred range at three independent points, and adoption specifically re-validates against
whatever the profile is *at adopt time*, not the one active when the proposal was generated.

**Limitations:**
- The `.puml` diagram sources are updated but their `.png` renders are not (see above).
- No `recommend replan`/`unadopt` feature exists; Section 0e.3's gap is documented, not resolved.
- The CG2028 missing-fixed-session data issue the same review flagged was **not** investigated or
  fixed - it was confirmed to be an artifact of the reviewer's own interactive test session (no
  checked-in fixture reproduces it: `text-ui-test/data/activities.txt` and `data/activities.txt`
  have no CG2028 entries, and `text-ui-test/batches/v2/batch-01-sem1-setup.txt` already has the
  correct two-part CG2028 entry), not a repo defect.
- Full validation run for this section: `./gradlew clean test` all green; `checkstyleMain`/
  `checkstyleTest` clean; `javadoc` unchanged at 100 pre-existing warnings (none in touched files);
  `bash text-ui-test/runtest.sh` → `Test passed!` after regenerating `EXPECTED.TXT` from a fresh
  `ACTUAL.TXT` (CRLF preserved via `unix2dos`) to reflect the new guide text and the
  preference-confirmation note. `shadowJar`/`releaseZip` were not separately re-verified after the
  guard/doc changes in this same pass - do that before tagging any release from this tree.

## 0d. Divergence resolved, pushed, and v2.0.1 retagged (2026-08-06, Claude Code) — read this first

**Supersedes Section 0c's "push blocked" status below - the divergence it describes is resolved
and everything from that section is now on `origin/main`.**

**Merge.** Section 0c's blocker (`origin/main` at `9dca6a9`, a sibling of local `HEAD` rather than
an ancestor) was resolved on explicit user instruction ("merge"): `git fetch origin` confirmed
`9dca6a9` unchanged, then `git merge origin/main --no-edit` produced a clean merge commit
(`2897709`) with **zero conflicts** - `9dca6a9` only touched `docs/AboutUs.md`, no overlap with
Section 0c's three commits. Full validation was re-run fresh on the merged tree before pushing:
`./gradlew clean test` **1199 passed, 0 failed**; `checkstyleMain`/`checkstyleTest` clean;
`bash text-ui-test/runtest.sh` → `Test passed!`. `git fetch origin` immediately before pushing
confirmed `origin/main` was still `9dca6a9` (no further drift), so the fast-forward
`git push origin main` succeeded cleanly - no force-push, no rebase. Verified after push:
`git rev-parse HEAD` and `git rev-parse origin/main` both `2897709`, `git status` "up to date,"
clean tree.

**v2.0.1 retagged, explicit user instruction ("retag v2.0.1 with release notes").** The `v2.0.1`
tag/release created in Section 0b (before Section 0c's preference-boundary fix existed) pointed at
`b07e577` - meaning the originally-published v2.0.1 asset **never actually contained the
preference-boundary fix**, the exact defect that release's own notes didn't yet know about. Per
explicit instruction, this was corrected by moving the tag rather than leaving two overlapping
patch releases:
1. `gh release delete v2.0.1 --yes` (removed the GitHub Release; the underlying tag is not deleted
   by this alone).
2. `git push origin :refs/tags/v2.0.1` + `git tag -d v2.0.1` (removed the tag both remotely and
   locally).
3. Rebuilt fresh from `HEAD` (`./gradlew clean shadowJar releaseZip`) and smoke-tested the exact
   asset before publishing, per this file's own established release discipline (Section 4/Section
   2's `v1.0.1` lesson): extracted to a clean temp directory, ran `recur 1 week 1 to 13` against
   the packaged calendar, set `preference set start/07:30 end/21:00`, added a flexible "Jogging"
   activity with window `06:30-09:30`, and confirmed `recommend date/2026-08-08` placed it at
   `07:30 -> 08:15` (not `06:30`) - live proof the shipped jar actually contains the fix, not just
   the source tree.
4. `git tag -a v2.0.1 -m "..." HEAD` (now points at `2897709`, the merge commit) + `git push origin
   v2.0.1`.
5. `gh release create v2.0.1 build/distributions/unienable.zip --title "UniEnable v2.0.1"
   --notes-file ...` with release notes rewritten to describe what's actually in this build: the
   preference-boundary fix (headlined, with an explicit "this tag was moved" note explaining why),
   the past-time recommend fix, the relative date-selector consistency work, `recur`'s improved
   conflict messages, the documentation repairs, and the known `chooseBestSlot` technical debt.

**Known consequence, worth knowing if this ever comes up again:** anyone who fetched the tag
`v2.0.1` or downloaded its release asset before this retag has a stale reference - `git fetch
--tags --force` (or a fresh clone) is needed to see the moved tag; there is no way to make an
already-downloaded asset self-update. This is the same trade-off Section 2's `v1.0`/`v1.0.1`
history already made once before under the same "don't keep two tags pointing at variously-fixed
states of identical code" reasoning - not a new precedent.

## 0c. Preference-boundary fix verification (2026-08-06, Claude Code) — read this first, push blocked

**`origin/main` has diverged and this commit was NOT pushed as of this note** - see the last
paragraph before starting any push. Everything else below (the fix, tests, docs) is verified and
committed locally.

**Approved fix verified:** commit `a378643` ("fix: enforce preferred daily start/end as a hard
boundary in recommend") was independently re-inspected end-to-end, not just re-summarized -
`git show --stat`/`--check`, `git diff origin/main...HEAD --check` (both clean), and a fresh read
of the actual `RecommendationService.java` content confirmed it touches only
`RecommendationService.java` (production fix), `RecommendationServiceTest.java` (109 lines, purely
additive - 0 lines removed, no `@Test`/`@Disabled` touched), `GuideCommand.java` (guide text),
`docs/UserGuide.md`/`docs/DeveloperGuide.md` (docs), and `text-ui-test/EXPECTED.TXT` (fixture
sync). `chooseBestSlot` and `SlotScore.betterThan` are byte-for-byte untouched by this commit,
confirmed via `git diff b07e577 a378643 -- RecommendationService.java | grep chooseBestSlot` (no
output) - the "do not redesign chooseBestSlot in this batch" instruction was already honoured
before this instruction even arrived, since it was never touched in the first place.

**Root cause (re-confirmed by reading, not re-derived from memory):** `preferredRangePenalty` was
only ever consulted inside `SlotScore.betterThan` as the *lowest*-priority tie-break field, and
that method's very first check (`if (!candidate.equals(current)) { return candidate.isBefore(current); }`)
compares the two candidates' clock times and returns immediately whenever they differ - which they
always do for one activity's own `validSlots` (strictly increasing, distinct one-minute steps). So
the preferred-range penalty could only ever matter when comparing two candidates with the *exact
same* start time, which never happens within one activity's own slot list. `validSlots()`/`fits()`
never excluded an out-of-range candidate either. Net effect: preferred start/end had no real
scheduling effect at all, confirmed by the PE-style review's exact reproduction (jogging placed at
06:30 when preferred start was 07:30 and 07:30 itself was reachable).

**Fix:** `validSlots()` now searches only `effectiveEarliestStart(activity, preferences, now)` to
`effectiveLatestEnd(activity, preferences)` - the intersection of the activity's own window and
`preferences.getPreferredStart()`/`getPreferredEnd()`, with `now` composing on top as a further
lower bound on today's date (`max(activity.earliestStart, preferences.preferredStart, now rounded
up to the next whole minute)`). A `Duration.between(earliestStart, effectiveEnd).toMinutes() <
durationMinutes` guard runs before any `LocalTime.minusMinutes()` arithmetic, so a too-narrow (or
inverted) intersection returns an empty slot list - the activity surfaces as unscheduled - instead
of risking a wrapped-past-midnight range. The original `FlexibleActivity`'s stored window is never
mutated; only `RecommendedPlacement`/preview copies carry a start time (unchanged from the
pre-existing `copyOf`/`applyPreview` design).

**7 new regression tests + 2 edge cases**, all in `RecommendationServiceTest`, matching the
review's exact scenarios: `recommendDate_windowStartsBeforePreferredStart_placementClampedToPreferredStart`,
`..._windowEntirelyAfterPreferredEnd_leavesActivityUnscheduled`,
`..._narrowerPreferredWindow_placementClampedToPreferredStart`,
`..._narrowerPreferredWindow_windowAfterPreferredEndLeavesUnscheduled`,
`..._todayNowLaterThanPreferredStart_clampsToNowNotPreferredStart`,
`..._preferredWindowNarrowerThanDuration_leavesUnscheduledWithoutCrashing`, and
`..._windowFullyInsidePreferredRange_isUnaffectedByPreference`.

**Full validation, run fresh this pass, not reused:**
- `./gradlew clean test`: **1199 passed, 0 failed** (1192 + 7 new; matches exactly)
- `checkstyleMain`/`checkstyleTest`: clean (one `LineLengthCheck` violation from an over-long test
  method name was found and fixed - `recommendDate_preferredWindowNarrowerThanDuration_leavesUnscheduledWithoutCrashing`
  - before this pass, confirming the gate was actually exercised, not rubber-stamped)
- `javadoc`: **100 warnings, 0 errors** - unchanged baseline
- `shadowJar`/`releaseZip`: both succeed; `unienable.zip` contains exactly `unienable.jar` +
  `data/academic-calendar.txt` (verified with `unzip -l`)
- `bash text-ui-test/runtest.sh`: **`Test passed!`**
- **Clean-extraction smoke test against the real built JAR**, real system clock `2026-08-06
  16:45`-`16:48`: all 8 scenarios (A-H) from the review, reproduced live, not just asserted by
  JUnit - A: 06:30-09:30/45min window, preferred 07:30-21:00 -> placed `07:30-08:15`. B:
  21:15-22:30/45min, preferred 07:30-21:00 -> unscheduled. C: 08:00-12:00/90min, preferred
  09:30-18:30 -> placed `09:30-11:00`. D: 18:00-21:00/60min, preferred 09:30-18:30 -> unscheduled.
  E (today, via direct `activities.txt` injection since `add` itself still correctly refuses an
  already-past same-day window): preferred start 07:30, activity earliest 06:00, run at real time
  `16:47:37` -> placed `16:48` (now correctly wins over both other lower bounds). F: 10-minute
  preferred range (`00:00-00:10`) against a 60-minute-duration activity -> unscheduled, zero
  exceptions in output. G: Tomato OFF vs ON on the same activity -> identical `09:00-10:00`
  placement both times, only the advisory suggestion line differs. H: buffer `20`, fixed activity
  ending `10:00`, flexible window starting `10:00` -> placed `10:20-10:50`, confirming buffer
  enforcement is unchanged.

**Documentation verified consistent** (User Guide §4/§11/§12.6, built-in `guide preference`/
`guide recommend` text, Developer Guide §16) - all state that preferred daily start/end is a hard
scheduling boundary (not advisory), the effective window is the intersection of the activity's own
window and the preferred range, today additionally applies the current-time clamp, an activity is
left unscheduled when the intersection can't fit its duration, a same-day `add`/`edit` start time
must be strictly after now (User Guide §4, previously undocumented), and Tomato remains advisory
only. Developer Guide §16 explicitly records `chooseBestSlot`'s earliest-wins behaviour and the
resulting unreachable buffer/energy/sensory scoring fields as **known technical debt / future
recommender work** - not redesigned in this pass, per instruction.

**Working tree:** clean before and after this HANDOVER.md edit.

**Push status: BLOCKED, not attempted.** `git fetch origin` before starting this verification
showed `origin/main` had moved from `b07e577` (this repo's own last-pushed commit) to `9dca6a9`
("Update AboutUs.md to remove solo project note", same GitHub account, pushed directly outside
this session, unrelated file). `git merge-base --is-ancestor b07e577 9dca6a9` confirms `9dca6a9` is
a normal descendant of `b07e577` (not a rewritten/forced history), but it is **not** an ancestor of
local `HEAD` (`a378643`, built on top of the same `b07e577`) - the two commits are siblings, so a
plain `git push origin main` would be rejected as non-fast-forward. Resolving this needs a merge
commit (safe, no file overlap with `a378643` - `9dca6a9` only touches `docs/AboutUs.md`) or a
rebase (never do this without being asked). Per this session's explicit instruction to stop and
report rather than resolve divergence unilaterally, **no merge/rebase/push was attempted.** The
next action needs the user's explicit choice: merge (creates one merge commit, no conflicts
expected) or something else they prefer.

## 0a. Bug-fix, date-selector, and doc-repair pass (2026-08-06, Claude Code) — read this first

A new session (Claude Code, not Codex — the handoff below never actually happened, or wasn't
recorded here if it did) was driven by three documents supplied outside the repo: a
"bug-fix/hierarchy-refactor master prompt", a "PE regression debug plan", and a prior "PE code
review report" claiming a set of fixes had already landed. **Most of that report's claimed fixes
turned out never to have actually been applied to this HEAD** — re-verifying each one against the
real code and docs (not trusting the report's prose) was most of this session's value. Concretely:

- **Real, previously-unfixed regression found and fixed:** `RecommendationService` never threaded
  `now` past period selection — `recommendDate()` had no `now` parameter at all, and
  `recommendThisWeek()`'s slot enumeration ignored it, so a flexible activity's window on today's
  date could still be proposed at an already-past start time, and `recommend adopt` had no
  staleness check at all. Fixed: candidate starts on today's date are now clamped to `now` rounded
  up to the next whole minute (`RecommendationService.effectiveEarliestStart`); a wholly-elapsed
  window reports the activity as unscheduled instead of backdating it; `recommend adopt` rejects a
  stale proposal (`RecommendationService.hasElapsedPlacement`) before the confirmation prompt.
- **Defect A (date-selector inconsistency) was real**, though narrower than the master prompt
  assumed `list` was already the reference implementation with full support. Closed the actual
  gaps: `dashboard`/`timetable`/`recommend` gained `next week` (`timetable`/`recommend` also
  gained `today`/`tomorrow`), and `find` — the starkest gap, with zero relative-date support before
  this session — gained the full `today`/`tomorrow`/`this week`/`next week` set via the same
  leading-phrase grammar `list` already used. The shared Monday-of-week math (previously
  byte-identical `TemporalAdjusters.previousOrSame(MONDAY)` duplicated three times) is now
  centralised in `logic.RelativeDateResolver`. See
  `docs/planning/DATE_SELECTOR_SUPPORT_MATRIX.md` for the full matrix and the reasoning for why
  `view`/`next`/`order`/`recur` deliberately did **not** get a selector.
- **Defect B (recur needing separate "week 1 to 6"/"week 7 to 13" commands) did not reproduce.**
  `WeekSpecificationParser` already accepted one inclusive range (`1 to 13`) as a single
  `WEEK_SPEC` item, and `RecurrencePlanner` already resolved each requested week against
  `data/academic-calendar.txt` by (year, semester, week number) rather than adding
  `7 * plusWeeks()` to the source date — a recess gap was already skipped correctly in one
  command. Locked in with new regression tests rather than changed. Improved
  `RecurrencePlanner`'s conflict message to name the specific teaching week and calendar date that
  failed (`"Week 9 (2027-03-16): ..."`) — a real, small gap the audit did find.
- **Package hierarchy audit (`docs/planning/PACKAGE_HIERARCHY_REVIEW_AND_PLAN.md`) found the tree
  already feature-oriented, tests already mirrored, no catch-all dumping packages, no circular
  dependencies.** No production classes were moved this session — a candidate 3-way split of
  `command.activity.general` was evaluated and explicitly rejected as cosmetic churn with no
  behavioural or dependency benefit (see that doc's Section 3).
- **Several "previously identified defects" from the master prompt's own re-verification list were
  still live, contradicting the earlier review report:** the built-in guide's main menu is
  correctly 12 items (route=10, timetable=11, Return=12) in code, but the User Guide still said
  "13-item menu" and "item 11 (Route search)" — fixed, and a matching stale `GuideCommand` Javadoc
  comment too. `docs/UserGuide.md` had 71 mojibake character sequences throughout (fixed via exact
  byte-level Windows-1252 round-trip decoding, not guesswork). `README.md` linked to
  `UserGuide.md#14-data-storage` (the real section is `#15`). `docs/DeveloperGuide.md` had no
  Acknowledgements section at all (added Section 26). `docs/AboutUs.md`/`docs/team/lukelouyu.md`
  described only the v1.0 feature set and a stale "657 tests" figure despite v2.0 having shipped
  and the suite having grown well past 1,190 tests. All fixed this session, verified against the
  actual current file contents (not assumed from the old report) before writing each fix.
- **Test count:** 1141 → check `git log`/`grep -rc "@Test" src/test/java` for the exact number as
  of the tip commit; this session added roughly 60 new tests across
  `RelativeDateResolverTest` (new), `RecommendationServiceTest`, `RecommendCommandParserTest`,
  `DashboardServiceTest`/`DashboardCommandParserTest`, `TimetableServiceTest`/
  `TimetableCommandParserTest`, `FindCommandParserTest`, `RecurCommandParserTest`,
  `RecurrencePlannerTest`, and `WeekSpecificationParserTest`. Zero existing tests were weakened;
  a handful were updated in place where the fix's own scope required it (e.g. `timetable today`
  was a deterministic "unknown selector" regression case in `text-ui-test` before `today` became
  valid — replaced with `timetable yesterday` plus a new `timetable today extra` case, not just
  deleted). `bash text-ui-test/runtest.sh` passes end-to-end against the real built JAR.
- **Nothing was pushed, merged, or tagged** — small, focused commits landed directly on `main`
  (per this session's own instruction, not the usual branch-per-feature flow), one per logical
  step, exactly per Section 4's "small commits" convention below. See `git log` for the exact
  sequence starting after `07cf8a7`.
- **Not yet done as of this note:** the full official validation pipeline
  (`checkstyleMain`/`checkstyleTest`/`javadoc`/`releaseZip`/JAR smoke test/PlantUML pairing check
  across every diagram, not just `RecommendationSequence`) — see Section 4 below for the exact
  commands, and re-run them before trusting a release-readiness claim beyond what's recorded here.

## 0b. Final independent verification and push (2026-08-06, Claude Code)

A second pass, in the same session, independently re-ran every gate above from scratch (not
reusing the earlier summary) before pushing. Every command below was actually executed against
commit `1d4144b` (the tip of the 11 commits described in Section 0a, i.e. `ac056ad..1d4144b`), not
assumed from memory.

**Final verification status**
- Date/time: 2026-08-06, local (`MPST`)
- Branch: `main`
- Pre-push tip commit verified: `1d4144b` (this note's own commit lands on top of it as the 12th
  commit, and is what actually gets pushed together with the 11 below it)
- Compared against `origin/main` = `07cf8a7` (re-fetched immediately before verifying; unchanged
  from the original baseline - no remote drift, no rebase needed)
- 11 local commits ahead of `origin/main` at verification time (`ac056ad`..`1d4144b`)
- Java `17.0.18`, Gradle `7.6.2` (both re-confirmed via `java -version`/`./gradlew --version`)
- `git diff --stat origin/main...HEAD`: 39 files changed, 1760 insertions(+), 295 deletions(-) -
  inspected file-by-file; no IDE files, logs, archives, credentials, or unrelated changes present
- `git diff --check origin/main...HEAD`: clean (no whitespace errors)
- `./gradlew clean test`: **1192 passed, 0 failed** (fresh run, `grep -c PASSED`/`FAILED` on the
  raw output, not the earlier session's cached number)
- `./gradlew checkstyleMain checkstyleTest`: clean, both tasks
- `./gradlew javadoc`: **100 warnings, 0 errors** - matches the long-standing pre-existing baseline
  this file has cited every session since `804ce09` (Section 2); zero new warnings introduced
- `./gradlew shadowJar releaseZip`: both succeed; `build/distributions/unienable.zip` contains
  exactly `unienable.jar` and `data/academic-calendar.txt` (verified with `unzip -l`, 3 entries
  including the `data/` directory marker)
- `bash text-ui-test/runtest.sh`: **`Test passed!`** against the real built JAR
- Working tree: clean before this HANDOVER edit (`git status` showed "nothing to commit" both
  before and after every read-only verification step above)

**Clean-extraction smoke test** (fresh temp directory outside the repo, extracted from the
just-built `unienable.zip`, real system clock at the time was `2026-08-06 13:53`):
1. Application starts - welcome banner shown.
2. `add` a Friday `2026-08-14` `ACADEMIC` lecture, then `recur 1 week 1 to 13` - previewed
   `Weeks: 1, 2, ..., 13`, confirmed with `y`, **11 new activities created in one command**.
3. Recurrence dates jump `2026-09-18` (Week 6) -> `2026-10-02` (Week 7), confirming the recess
   gap is skipped correctly by calendar lookup, not generated into.
4. `find next week c/ACADEMIC` - accepted, correctly returned the Week-1-in-next-week occurrence.
5. `dashboard next week` - accepted, `Period: 2026-08-10 to 2026-08-16`.
6. `timetable tomorrow` - accepted, `Period: 2026-08-07` (real tomorrow).
7. `timetable next week` - accepted, `Period: Next week`, showing the Friday occurrence correctly.
8. `recommend today` does not propose elapsed slots - **verified live, not just by unit test**: a
   flexible activity with an already-elapsed `00:00-00:10` window (injected via a direct
   `activities.txt` row, restarted - `add` itself correctly still refuses to create an
   already-past-window activity, confirming that guard is intact) came back
   `Unscheduled activity IDs: [14]`, while a second activity with a genuinely future `23:00-23:59`
   window on the same day was proposed at `23:00 -> 23:30` in the same run - proving the clamp
   rejects only the truly elapsed one, not the whole day.
9. `recommend next week` - accepted, `Period: Next week`.
10. Restarted the JAR after `bye`; `list` showed all 14 activities (12 recurred/source + 2
    flexible), including the manually-injected elapsed-window row, loaded with zero warnings -
    persistence confirmed.

**Documentation and diagram verification**
- Markdown links: the one cross-file anchor in the whole doc set
  (`README.md` -> `docs/UserGuide.md#15-data-storage`) resolves against `docs/UserGuide.md`'s
  actual `## 15. Data Storage` heading. Zero broken links found.
- PlantUML/PNG pairing: 21 `.puml`, 21 `.png` - matched 1:1.
- `RecommendationSequence.png` re-rendered fresh from the committed `.puml` via the local
  `plantuml.jar` and diffed byte-for-byte identical to the committed PNG - not stale.
- Zero mojibake sequences (`â€`/`â”` families) in `UserGuide.md`, `DeveloperGuide.md`, `README.md`,
  `AboutUs.md`, `lukelouyu.md`, or this file.
- No document claims recurrence previously required split ranges - the one relevant sentence
  (User Guide Section 6.12) correctly says a single range "does **not** need to be split," never
  that it used to.
- No stale test-count or v1.0-only claims remain in `AboutUs.md`/`lukelouyu.md`/`README.md`.

**Decision:** every gate passed. Proceeding to push per Step 9 of this verification's own
instructions - see the commit immediately following this one, and `git log`/`git status` after
that push for the final proof this section cannot self-report in advance.

## 0. Handoff to Codex (2026-08-02) — start here

The user is moving the rest of v2.0 development from Claude to **Codex**. This section is a
mechanical, step-by-step starting point. Read it first, then read the rest of this file (it's the
same continuity doc Claude was using — nothing below this section was written differently because
the tool changed).

**0.1 — Read the spec, in-repo now, no external access needed.** The full v2.0 master prompt
(shared rules/sequence for all six features, plus every feature's detailed spec) is at
[`docs/planning/UNIENABLE_V2_CLAUDE_CODEX_MASTER_PROMPT_UPDATED.md`](docs/planning/UNIENABLE_V2_CLAUDE_CODEX_MASTER_PROMPT_UPDATED.md).
It used to live only outside the repo (in the user's Downloads folder); it was copied into the
repo specifically so a tool without access to the user's local filesystem — Codex — can read it.
Treat it as the authoritative v2.0 specification, exactly as Claude did.

**0.2 — Exact current git state.**
- `main` and `origin/main` are at `ada0f5f` — `feature/v2-route`,
  `feature/v2-dashboard`, and `feature/v2-timetable` are merged and pushed. `v1.0` is tagged and
  released on GitHub correctly (zip, not bare jar).
- `feature/v2-preferences` was created from `ada0f5f`. Its approved four-field global profile,
  parser/commands, all-or-default storage, four-file transaction, reset integration, tests,
  Text-UI coverage, guides, and diagrams are complete. It must remain unmerged and unpushed until
  the user separately reviews and approves that action.
- No v2.0 git tag or GitHub Release exists yet. `feature/v2-recommend` has now been created
  locally from `main` and is in progress, but remains unmerged and unpushed pending user review.
- A stale local branch `v2-dashboard` (no `feature/` prefix, tip `9829911`) also exists with
  content unrelated to the current `feature/v2-dashboard` work — confirmed in earlier sessions to
  have zero unique commits worth keeping relative to `main`. Left untouched, not deleted, per
  standing instruction not to take destructive git actions without explicit approval. Don't
  confuse the two branches by name.

**0.3 — What to actually do, in order:**
1. Review the `feature/v2-preferences` completion report and current diff. Merge and push only if the
   user explicitly approves both actions.
2. After Preferences: finish `feature/v2-recommend`, then the final v2.0
   integration/documentation/regression pass — same
   process every time: audit → proposed plan → explicit approval → task-spec folder → implement →
   full quality gate (Section 4) → completion report → **stop and wait**. Never chain straight into
   the next feature branch without a fresh explicit go-ahead, even if the master prompt lists it as
   the obvious next step.

**0.4 — Hard-won lessons from the Claude sessions that produced `route` and `dashboard` — do not
repeat these:**
- **A feature-specific reading of the master prompt can be wrong about the actual codebase; verify
  before implementing.** This bit twice already: `route`'s exact YES-only-filter architecture
  needed a real user decision rather than trusting the prompt's rough sketch (see
  `docs/tasks/v2/route/README.md`'s "Approved design decisions"), and `dashboard`'s guide-menu
  numbering was assumed to need a new menu number (mirroring `route`) when in fact
  `GuideCommand.MENU_NUMBER_TOPICS` already had `"dashboard"` at item 5 since v1.0 — an
  in-progress numbering change had to be fully reverted once this was caught (Section 1). Always
  check the actual current code before assuming a spec's generic description is precise.
- **Release distributables must always be `./gradlew releaseZip`'s zip, never a bare
  `build/libs/*.jar`.** A bare-jar release broke `recur` for a real user (missing
  `data/academic-calendar.txt`, which the app never creates itself) and required deleting a
  short-lived `v1.0.1` tag/release and rebuilding `v1.0`'s release from scratch. Smoke-test the
  *exact uploaded asset* (fresh extraction, run the jar, exercise a feature needing the external
  file) before considering any release done.
- **`text-ui-test/input.txt` insertion position matters.** Activity IDs auto-increment; inserting
  new scenarios mid-file shifts every later ID-dependent assertion. Insert new blocks either at the
  very end, or immediately after a `reset all` full-data-wipe (IDs restart at 1) — that's where
  `dashboard`'s scenarios were placed after an earlier mid-file attempt broke unrelated later
  assertions.
- **Verify pipe-delimited fixture field order against the actual `*Storage.parseLine()` code, not
  a manual read.** `route`'s test scenario initially assumed the wrong field
  (`accessibility` vs. `shelter`) was `NO` for a bundled connection, because of a quick manual
  miscount of `connections.txt`'s columns.
- **Never call `LocalDate.now()`/`LocalDateTime.now()`/`Instant.now()` directly in production
  code.** `now` is captured once at parse time in `CommandDispatcher.dispatch(input, now)` and
  threaded through everywhere — `DashboardCommand` briefly violated this during drafting and was
  fixed before commit (Section 1). Grep for direct `.now()` calls in `src/main` if ever unsure.
- **Standing approval gates are not loosened by convenience.** Never push, merge, tag, delete a
  branch, delete a release, or start the next feature branch without the user explicitly saying so
  in that turn, even if a previous turn approved something that looks similar. This project has had
  exactly one on-record exception (`feature/recur-reset-v2`, where the user asked for test+docs+
  commit+merge+push all in one message) — that doesn't loosen the default; keep asking.
- **A same-session PlantUML permission grant doesn't need re-asking within that session, but a new
  session (or a new tool) should treat file-download permission as unasked.** Diagrams were
  rendered via the public `plantuml.com` server (no local PlantUML/Graphviz toolchain was found
  installed in this environment); if Codex has its own way to render `.puml` → `.png` locally,
  prefer that over a network fetch. **Timetable update:** the PNGs were rendered locally with the
  existing temporary upstream-template helper at
  `C:\Users\lukel\AppData\Local\Temp\unienable-plantuml-tool\tp-master\tools\plantuml.jar`.
  That helper is not part of this repository and was neither downloaded nor committed by the
  Timetable branch; reuse it if it still exists, otherwise obtain explicit permission before any
  download or large binary addition.

**0.5 — Everything else you need is already in this file.** Section 1 has the full implementation
history for `feature/v2-dashboard` and the current `feature/v2-timetable` state. Section 2 is
the condensed project history. Section 4 is the standing working conventions (commit/push
discipline, verification commands, design taste). Section 5 is known live risks. Section 6 is
product/package-layout context. Update Section 1 (and this Section 0) again before/after each
subsequent branch — this file's whole
point is staying current across a tool handoff, not just a session handoff.

## 1. Current state (as of this update)

**Current work: `feature/v2-preferences`, based on merged Timetable commit `ada0f5f`.** Preferences
implements one immutable global everyday profile: preferred start/end (`08:00`/`20:00` defaults),
minimum buffer (`15`, range 0–1440), and advisory Tomato/Pomodoro suggestion (`OFF` default).
Exact grammar is `preference view`, `preference set` followed by one or more of `start/HH:mm`,
`end/HH:mm`, `buffer/MINUTES`, and `tomato/on|off` (in any order, each at most once), and
`preference reset`.

`PreferenceStorage` writes deterministic uppercase four-line `data/preferences.txt`. A missing file
silently loads all defaults; every malformed/incomplete/duplicate/unknown/invalid/inconsistent
profile produces startup warnings and falls back to the complete defaults with no partial-field
retention. Preferences are the fourth member of `Storage.saveAll` and the application snapshot,
so cancelled/failed commands never leave partial memory or disk state. `reset all` option 1 resets
the profile, option 2 retains it, and option 3 cancels. Tomato remains advisory-only data; on
`feature/v2-recommend` it now feeds display-only study suggestions rather than changing slot
generation.

Core commits, oldest first: `a0c6ab2` (task specification), `8cb1d2e` (production and
integration), `252e2af` (JUnit and Text-UI coverage), `688370c` (guides and PlantUML/PNG
diagrams), `2bc282d` (handover), and `121b046` (final edge-coverage audit). Final verification on
2026-08-03: **1,120 JUnit tests** passed with zero
failures/errors/skips; Checkstyle main/test had zero findings; Javadoc succeeded with the unchanged
100-warning baseline and no Preferences warning; the expanded Text-UI harness passed; all 19
PlantUML sources have PNG counterparts and guide image links resolve; `releaseZip` produced only
`unienable.jar` and external `data/academic-calendar.txt`; and a fresh extraction plus restart run
covered view, every approved set shape, Tomato on/off, `guide preference`, `guide 6`, option-2
retention, option-1 reset, explicit preference reset, exact disk lines, and restart persistence.
The branch is complete and awaiting review/explicit merge-and-push approval.

**Timetable history.** Timetable is implemented as a read-only day/week projection with normal,
compact, and detail modes. It keeps flexible activities unscheduled, uses permanent numeric IDs,
marks defensive fixed-activity overlaps, and performs no persistence. Final verification before
merge on 2026-08-02: 1,079 JUnit tests passed with zero failures/errors/skips; Checkstyle main/test
reported zero findings; Javadoc built successfully with the unchanged 100-warning baseline and no
Timetable warning; the Text-UI harness passed; and `releaseZip`/fresh-extraction smoke tests passed.

**Dashboard history.** `feature/v2-dashboard` was merged and pushed to `main` as merge commit
`4b9978e`. It implements `dashboard today|tomorrow|date/YYYY-MM-DD|this week [detail]`: a read-only planning-load
summary computed fresh from `ActivityManager` every time - no persistence, no confirmation, cannot
mutate anything by construction (`DashboardCommand` implements neither `Confirmable`/
`MenuConfirmable` and isn't in `ApplicationRunner.mutatesState`). Metrics: planned workload (fixed
activities clipped to the period; flexible activities count their full requested duration once,
never clipped to window overlap - a stated, tested limitation), "Nominal buffer"/"Overloaded by"
(period capacity - derived generically via `Duration.between`, never hardcoded per period "kind" -
minus workload), energy/sensory totals plus a named `HIGH_RATING_THRESHOLD = 4`, and completion as
a secondary metric where an activity not yet due is excluded from the denominator entirely (never
counted as incomplete). `detail` adds fixed/flexible counts, a deterministic category breakdown,
and 1-5 rating distributions with average (`BigDecimal`/`HALF_UP`) and highest.

**Real audit finding, confirmed and handled exactly as approved, not silently reinterpreted:** the
current v1.0 activity model cannot represent a genuine cross-midnight activity - both
`FixedActivity` and `FlexibleActivity` constrain start/end to one calendar date, constructor-
enforced. The master prompt's cross-midnight requirement was satisfied by implementing the
interval-clipping calculation (`logic.dashboard.DashboardService`'s package-private `clip(...)`)
generically over raw `LocalDateTime` boundaries, and testing it directly with synthetic boundaries
simulating a Monday 23:00 -> Tuesday 01:00 interval (60/60/120-minute contributions against
day/day/week periods) - not by constructing an activity the model's own invariants would reject.
No change was made to `FixedActivity`/`FlexibleActivity`, their parsers, storage format, or
conflict-checking to support cross-date activities - explicitly declined as outside this branch's
scope. Full account in `docs/tasks/v2/dashboard/IMPLEMENTATION_NOTES.md`.

**Second real finding, also confirmed and handled: `dashboard` needed no new guide menu number.**
The originally-assumed plan (mirroring `route`, which genuinely added a new numbered item) was
wrong - `GuideCommand.MENU_NUMBER_TOPICS` already had `"dashboard"` at menu item **5**
("Completion and dashboard") since v1.0, just showing "Coming soon" content. This branch replaces
only that item's topic *text*; no `MAIN_MENU`/`MENU_NUMBER_TOPICS`/`CommandDispatcher` bare-number
change was needed, and nothing was renumbered - a stricter reading of "do not renumber existing
entries" than taking a new number would have been. (This was caught mid-session: an earlier pass
had already added a new item 12 and a corresponding renumbering of Return to 13 before the mistake
was found; those changes were fully reverted before committing anything.)

**Verification (commit `8abcadc`):** `./gradlew clean test checkstyleMain checkstyleTest` all
green, **1031 JUnit tests** (up from 950 after `feature/v2-route` merged - 81 new, zero deleted/
weakened), checkstyle clean (main + test). `bash text-ui-test/runtest.sh` passes -
`dashboard date/...` scenarios added at a safe insertion point (right after `reset all` wipes
state, so no later ID-dependent command shifts); `dashboard today`/`tomorrow`/`this week` are
deliberately **not** covered by `text-ui-test`, matching the existing `list today`/`tomorrow`/
`this week` exclusion (the harness runs the real jar against the real wall clock, no fixed-clock
injection point) - covered instead by `DashboardServiceTest`'s injected-`now` tests.
`./gradlew releaseZip` builds; a clean-extraction smoke test exercised `dashboard`'s default/
detail/empty-period/error paths plus `guide dashboard`/`guide 5` and all matched expectations.

Two diagram PNGs (`DashboardClassDiagram.png`, `DashboardSequence.png`) plus a re-rendered
`ArchitectureDiagram.png` (added a `DashboardService` component in "Business logic", mirroring
`route`'s `AccessibleRouteGraphFactory` addition) were rendered via the same public-PlantUML-server
approach as `route`'s diagrams - no separate permission prompt this session (likely still in
recent memory from the same conversation's earlier approval for `route`'s diagrams).

Everything below this point, through the rest of this section, describes `feature/v2-route`'s
merge and the `v1.0` release-packaging fix - both already landed on `main` before
`feature/v2-dashboard` started, kept as historical record per this file's rolling convention.

**Update (2026-08-02, after `feature/v2-route` below had already been fully committed on its own
branch): a release-packaging mistake was found and fixed on `main` directly, while the route
branch sat unmerged.** `v1.0` was tagged with a GitHub Release whose only asset was a **bare
`unienable.jar`**. That's broken for `recur`, which needs `data/academic-calendar.txt` shipped
alongside the jar - the app never creates that file itself, and the documented distributable has
always been `./gradlew releaseZip`'s zip (jar + calendar file), never a bare jar (see README's
"Distribution" section). A user who downloaded the bare jar hit exactly this:
`[Error] Storage error: academic calendar file not found: data/academic-calendar.txt` on `recur`.
A short-lived `v1.0.1` tag/release was created with the identical mistake (copied from `v1.0`'s
own asset without cross-checking it against this project's own documented rule), then corrected in
place, then - per explicit instruction, rather than keeping two tags pointing at variously-fixed
states of identical code - **deleted entirely** (release and tag, local and remote). `v1.0`'s tag
was moved to the fix commit (`4f040da`, deleted and recreated - git tags aren't normally
force-moved on a published repo, but this was explicitly requested), and `v1.0`'s GitHub Release
was rebuilt with the correct `unienable.zip` as its only asset, verified via a clean-extraction
smoke test that `add` then `recur` both work. **Lesson: when creating a GitHub Release for a
jar-based distributable, always attach `releaseZip`'s output zip, never a bare `build/libs/*.jar`
- and smoke-test the exact uploaded asset (extract fresh, run, exercise the feature that depends
on the external file) before considering a release done, not just the local build.** This fix
landed as a standalone commit directly on `main` (not through a feature branch, since it wasn't
v2.0 work), then `feature/v2-route` was updated with it (this merge) before being merged into
`main` itself.

**`feature/v2-route` is merged into `main`.** It was 4 commits ahead of the `main` it branched
from (`59a9c4a`); `v2-dashboard` remains a stale local branch with zero unique commits (confirmed
again this session) - not touched, not deleted per explicit instruction.

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
aspirational. This paragraph, and everything historical below it, also predates the
release-packaging fix and the `feature/v2-route` merge described earlier in this section.**

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
  redoing it; route and dashboard are now merged, and the current review target is
  `feature/v2-timetable` (Section 1, Section 6).
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
`route from/FACILITY to/FACILITY`, `dashboard today|tomorrow|date/YYYY-MM-DD|this week [detail]`
(both merged into `main`), and `timetable day/YYYY-MM-DD [detail]`,
`timetable week/YYYY-MM-DD [compact|detail]`, and `timetable this week [compact|detail]`
(Section 1, on `feature/v2-timetable`), `guide`, `bye`.
**No git tag or GitHub Release exists yet for v2.0** — `v1.0` itself is tagged and released (see
the historical part of Section 1 below for the release-packaging fix applied to it).

**v2.0 status, per the approved master prompt
(`UNIENABLE_V2_CLAUDE_CODEX_MASTER_PROMPT_UPDATED.md`, supplied 2026-08-02, now the authoritative
v2.0 spec):** the original branch sequence was route -> dashboard -> timetable -> preferences ->
recommend -> export -> final integration pass. Export has since been removed from the approved
scope, so recommend now flows directly into the final v2.0 integration pass. Recurrence and
technical-debt hardening already shipped before this sequence started (Section 2). **Route and
Dashboard are merged into `main`. Timetable is complete on `feature/v2-timetable` and awaiting
review/merge approval.** Preferences are complete on `feature/v2-preferences`, and
`feature/v2-recommend` is now actively in progress. Each remaining feature still needs its own
audit-confirm-implement pass in its own turn. Each feature so far has
also come with its own
feature-specific master prompt (not just the shared v2.0 one) - check
`docs/tasks/v2/<feature>/README.md` for what was actually supplied and approved for that feature
before assuming the shared prompt's rough sketch is authoritative on a point the feature-specific
one addressed more precisely (this has mattered twice already: route's exact-date marker syntax
and `this week` definition, and dashboard's cross-midnight handling and guide-numbering reality).

Package root `seedu.unienable`: `app/` (`ApplicationRunner`, `CommandConfirmationHandler`,
`command.MenuConfirmable`/`MenuOutcome`), `command/` — `activity.crud` (Add/Delete/Edit/View) +
`activity.general` (Find/List/Mark/Next/OrderSet/OrderView/Unmark), `accessibility.facility` +
`accessibility.connection` (4 commands each) + `accessibility.common`
(`AccessibilityDisclaimer`/`ValidationReportFormatter`, both `public`) + `accessibility.route`
(`RouteCommand`) + `dashboard/` (new, `feature/v2-dashboard`: `DashboardCommand` - flat, one
class, not nested under `accessibility` since dashboard reads activities, not accessibility
data), `topic/` and `recur/` left flat (too few classes each to split), `general/`
(Guide/Reset/Bye). `parser/` mostly mirrors `command/` one level up, still flat per domain for
`topic`/`accessibility` (`TopicCommandParser`/`FacilityCommandParser`/`ConnectionCommandParser`/
`RouteCommandParser` all live directly in `parser.accessibility`, none split further), plus new
`dashboard/` (`DashboardCommandParser`), **except `parser.activity`**, split as of the
hardening-plan session (Section 2) into a thin `ActivityCommandParser` router plus
package-private `AddCommandParser`/`EditCommandParser`/`ListCommandParser`/`FindCommandParser` for
the four commands with real grammar — delete/mark/unmark/view/next/order stayed inline in the
router. Plus `common/` for
`FieldParser`/`DateTimeParser`/`RatingParser`/`Parser`/`ArgumentTokenizer`/`ArgumentMarker`, plus
`recur/`. `exception/` (flat, reused as-is by every feature including recur/route/dashboard),
`logic/` (the `*Manager` classes, plus `graph/` [Dijkstra-prep, generic, policy-free - see
Section 1], `recur/`, `route/` [`AccessibleRouteGraphFactory`, the route-specific `YES`-only
filter that deliberately does *not* live in `logic.graph`], and new `dashboard/`
[`DashboardService`, stateless, mirrors `AccessibleRouteGraphFactory`'s shape - owns period
resolution and every metric calculation, including the package-private interval-clipping helper]),
`model/` (`classes`/`enums`/`recur`, and new `dashboard/` [`DashboardPeriod`/`RatingSummary`/
`DashboardSummary`, all immutable - no `model/route`, since route needed no new persistent
type, but dashboard's richer result shape earned one]), `storage/` (plus `recur/` for the
strictly-validated, read-only `AcademicCalendarStorage`; `storage/` imports nothing from
`parser/` as of the hardening-plan session — see Section 2/5; dashboard adds **no** storage
package at all - it is a derived view, never persisted), `ui/` (plus `recur/` for
`RecurrenceFormatter`, `accessibility/` for `RouteFormatter`, and new `dashboard/` for
`DashboardFormatter`), `accessibility/` (the read-only facility/connection domain model — a
different, older package tree from `command.accessibility.*` above, don't conflate the two: this
one is `seedu.unienable.accessibility.classes`/`.enums`, immutable read-only reference data,
unchanged by route or dashboard - both only read it, and dashboard doesn't even do that, reading
`ActivityManager` instead).

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
  anymore - route and dashboard are merged, and the concrete next task is reviewing
  `feature/v2-timetable` (Section 1), per the master prompt's required sequence.
