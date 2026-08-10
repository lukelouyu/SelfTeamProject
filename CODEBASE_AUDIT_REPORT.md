# UniEnable Codebase Audit Report

**Audit date:** 2026-08-10
**Scope:** Full verification and remediation pass over eight findings from a prior audit, covering
correctness (P1-P3), documentation drift (P3), and maintainability (P4).
**Change range:** `a84146d..6952cda` (see [Final Git State](#9-validation-results) for the exact
range this report covers)

---

## 1. Executive Summary

Of the eight findings carried into this audit, **five were Confirmed** defects requiring a code
fix, **one was Partially Confirmed** (already half-fixed by an earlier commit), **one was
Confirmed stale documentation** (only partially - the other half of the same section was already
accurate), and **one was Confirmed as real but low-severity**, with a documented decision not to
change it. No finding was Not Reproducible or Already Fixed in full.

| Priority | Finding | Classification | Final Status |
|---|---|---|---|
| P1 | AccessibilityGraph cumulative-distance overflow | Confirmed | Fixed |
| P1 | Rollback after unchecked persistence failure | Partially Confirmed | Fixed |
| P2 | Duplicate Add/Edit prefixes | Confirmed | Fixed |
| P2 | Time-dependent E2E tests | Confirmed | Fixed |
| P3 | Activity ID overflow | Confirmed | Fixed |
| P3 | Recommender DG drift | Partially Confirmed | Fixed |
| P3 | Duplicate date/time parsing logic | Confirmed (low severity) | Declined, documented |
| P4 | Large classes and maintainability | Partially justified | Documented, deferred |

**Release risk:** All five confirmed correctness defects (two P1, two P2, one P3) are release
risks in the sense that each has a concrete, demonstrable failure scenario (an integer overflow
producing a wrong shortest-route or an infinite reconstruction loop; a mutated-but-unpersisted
model after an unanticipated storage bug; a silently-mismerged field on a common typo; a suite that
starts failing once the wall clock passes 2026-08-15; an ID counter that resets to 1 after loading
a maximum-value ID). None were reachable via the bundled sample dataset or realistic v2.0 usage
without either a malformed/adversarial data file or billions of prior operations, so none were
observed in the wild - but all five are now fixed, tested, and documented.

**Strongest areas:** the existing `CommandTransactionExecutor` snapshot/rollback architecture
(only needed to be widened to cover one more exception type, not redesigned); the storage layer's
existing atomic multi-file commit engine; the parser layer's existing `ArgumentTokenizer`, which
already had the correct duplicate-rejection semantics for the fix to converge toward; and dense,
example-driven Javadoc throughout that made intended behaviour easy to verify against actual
behaviour.

**Highest-risk issue found:** the `ActivityManager` ID-overflow bug was the most subtly dangerous
of the five, because `Math.max(1, id + 1)` on an overflowed value didn't merely produce a wrong
number - it silently reset `nextId` back to `1`, which would have let a subsequent `add()` reuse an
ID already in use by a loaded activity with no error at any point.

---

## 2. Initial Repository State

```text
Branch:        main
Starting HEAD: a84146df51af19e6cc209ab0575bf98a42b21c79
Working tree:  clean (three unrelated untracked presentation files, not part of this audit's
               scope: .codex-presentations/, UniEnable_NUS_Enablers_Presentation.pptx.inspect
               .ndjson, ~$UniEnable_NUS_Enablers_Presentation.pptx)
Remote:        origin/main (up to date at start)
```

Environment: Windows 11, Git Bash (bash 3-compatible per `dca0675`), Java 17.0.18, Gradle wrapper
7.6.2. A `plantuml.jar` was available at `/tmp/plantuml.jar` for diagram regeneration; `dos2unix`
was available for the text-ui-test harness.

Notably, the starting HEAD already included one prior remediation commit
(`cdb2a24 fix: address codebase audit findings`) that had extracted `CommandTransactionExecutor`
from `ApplicationRunner` and added checked-`StorageException` rollback coverage - this meant the
P1 persistence-rollback finding needed to be re-verified against the *current* code rather than
assumed still fully open, which is exactly what happened (see Finding 2 below: the checked path was
already fixed; only the unchecked path remained open).

---

## 3. Audit Methodology

For each finding: the relevant production source was read in full (not excerpted), the relevant
test files were read in full, and where practical the defect was reproduced either by tracing the
exact code path by hand against a constructed input, or by writing a failing test first and
confirming it failed before the fix and passed after. Eight parallel research passes (one per
finding) were run first, each independently reading source + tests + git history for its area and
reporting back file:line evidence, before any code was changed - this is what allowed the P1
persistence-rollback finding's already-fixed half to be identified before touching it, and the DG
recommender-drift finding's already-accurate half to be identified before rewriting the whole
section.

Reviewed: production code across `logic`, `app`, `parser`, `storage`, and `command` packages; the
full JUnit suite (1236 tests before this audit's own additions, 1250 after); parser grammar and
tokenizer implementations; the storage transaction/backup engine; `CommandTransactionExecutor` and
`ApplicationRunner`'s command loop; `AccessibilityGraph`'s Dijkstra implementation; the
recommendation algorithm and its test suite; `docs/DeveloperGuide.md`, `docs/UserGuide.md`, and
every `.puml`/`.png` pair under `docs/diagrams/`; and `build.gradle`/`config/checkstyle` (read for
context, not modified, since no finding required a build-configuration change).

---

## 4. Detailed Findings

### Finding 1 - AccessibilityGraph cumulative-distance overflow

```text
Priority: P1
Classification: Confirmed
Verification status: Reproduced (regression test added; failed against the pre-fix code path
    when hand-traced, confirmed via a synthetic dataset test after the fix)
Affected components: logic.graph.AccessibilityGraph, logic.graph.GraphPath
Impact: A multi-hop accessible route whose true total distance exceeds Integer.MAX_VALUE (or a
    two-hop path that happens to sum past it while a shorter direct edge exists) could be silently
    mis-ranked by Dijkstra due to int wraparound, returning a worse or wrong route as "shortest".
    Additionally, no cycle guard existed in path reconstruction, so any future corruption of the
    predecessor map (from this bug or otherwise) could hang the command loop.
```

#### Description

`AccessibilityGraph.getShortestPath` accumulated Dijkstra's running distance as a plain `int`
(`bestDistance` map values, `QueueEntry.distanceInMetres`, the `candidate` local, and
`GraphPath.totalDistanceInMetres`). Individual connection distances are validated as positive
`int` by `ConnectionStorage` at load time, but nothing bounded their *sum* across a multi-hop
route.

#### Root Cause

`int candidate = current.distanceInMetres + edge.distanceInMetres;` (the pre-fix line) performs
plain 32-bit addition with no overflow check. Once the running total exceeds
`Integer.MAX_VALUE` (2,147,483,647), it wraps to a large negative number, which then compares as
"less than" every other candidate distance - Dijkstra would greedily prefer the wrapped
(corrupted) path over a genuinely shorter one.

#### Reproduction / Evidence

Hand-traced: two edges of 1,200,000,000 m each (both individually valid `int`s, well under
`ConnectionStorage`'s only bound - `distanceInMetres <= 0` is rejected, there was no upper bound)
sum to 2,400,000,000, which overflows to `-1,894,967,296` as a 32-bit `int`. A regression test
constructing exactly this scenario against the pre-fix arithmetic would have selected the
(illusorily "cheaper", actually longer) two-hop path over a valid 2,000,000,000 m direct edge.

#### Expected Behaviour

The shortest path by true total distance is returned regardless of how large individual or
cumulative distances get, up to what a `long` can represent.

#### Actual Behaviour (pre-fix)

Cumulative distances above `Integer.MAX_VALUE` could wrap to negative, corrupting comparisons.

#### Original Suggested Fix

Retain individual edge distances as `int`; widen cumulative/queue/`GraphPath` totals to `long`.
Add reconstruction-cycle detection via a visited set.

#### Final Implementation

Exactly as suggested, with one addition: `reconstructPath` was changed from `private` to
package-private specifically so `AccessibilityGraphTest` can exercise the cycle guard directly
with a deliberately malformed `previous` map, rather than needing to first corrupt a live Dijkstra
run to reach it (which the fix itself makes effectively unreachable in practice).

- `Connection.distanceInMetres`, `AccessibilityGraph.Edge.distanceInMetres`: unchanged, `int`.
- `AccessibilityGraph.QueueEntry.distanceInMetres`, `bestDistance` map values, the `candidate`
  local: now `long`.
- `GraphPath.totalDistanceInMetres` and `getTotalDistanceInMetres()`: now `long`.
- `reconstructPath` now tracks a `Set<String> visited` and throws `IllegalStateException` if a
  facility is revisited while walking the predecessor chain.

#### Why This Implementation Was Chosen

Individual persisted edge values are already bounded by `ConnectionStorage`'s load-time
validation (rejected if `<= 0`; no realistic campus distance approaches `Integer.MAX_VALUE`), so
only the *cumulative* addition needed widening - matching the original recommendation exactly and
minimizing model churn. Existing test assertions like `assertEquals(45, path
.getTotalDistanceInMetres())` continue to compile unchanged, since Java widens an `int` literal to
`long` for `assertEquals(long, long)` overload resolution.

#### Regression Tests

- `AccessibilityGraphTest.getShortestPath_cumulativeDistanceExceedsIntegerMaxValue_choosesTrueShortestPath`
  - proves the fix picks the true-shortest (direct) edge over an overflow-corrupted two-hop sum.
- `AccessibilityGraphTest.getShortestPath_multiHopAboveIntegerMaxValue_reportsExactUnwrappedTotal`
  - proves a 3,000,000,000 m total is reported exactly, not wrapped.
- `AccessibilityGraphTest.reconstructPath_cyclicPredecessorMap_throwsInsteadOfLoopingIndefinitely`
  - proves the cycle guard fires instead of hanging.
- `GraphPathTest.getTotalDistanceInMetres_valueAboveIntegerMaxValue_preservedExactly`.

#### Commit

`3b036d6 fix: prevent overflow in accessibility route cumulative distance`

---

### Finding 2 - Rollback after unchecked persistence failure

```text
Priority: P1
Classification: Partially Confirmed
Verification status: Reproduced (the checked-exception path was independently confirmed already
    fixed by inspection of cdb2a24; the unchecked-exception gap was reproduced via a new test
    double that failed before the fix)
Affected components: app.ApplicationRunner, app.CommandTransactionExecutor
Impact: A RuntimeException from Storage.saveAll() (e.g. an unanticipated bug in a serializer, not
    the documented StorageException failure mode) left the in-memory model mutated while disk
    still reflected the pre-command state, and was reported as a generic "unexpected internal
    error" rather than a storage failure - with no rollback performed.
```

#### Description

`CommandTransactionExecutor` (added by an earlier commit, `cdb2a24`) already provided complete
snapshot/rollback coverage for (a) a command's own execution throwing, and (b) an explicit
caller-invoked rollback after `ApplicationRunner.trySave()` catches a checked `StorageException`.
What remained open was: if `storage.saveAll()` itself throws an *unchecked* `RuntimeException`
(not documented/declared, e.g. an `NullPointerException` inside a future serializer bug), that
exception propagated straight past the `if (!trySave()) { execution.rollback(); ... }` call site
(since `trySave()` never returned `false` for it) and was caught only by
`ApplicationRunner.processCommand`'s outer `catch (RuntimeException e)`, which logs and shows a
generic error but has no reference to the `Execution` object needed to roll back (it is
block-scoped to the `try`, per ordinary Java scoping rules).

#### Root Cause

`trySave()` had a single `catch (StorageException e)` clause; any other exception type from
`storage.saveAll()` was simply not caught there.

#### Reproduction / Evidence

A new test double, `RuntimeExceptionAfterStorage`/`RuntimeExceptionOnceThenSucceedStorage`
(mirroring the existing `FailAfterStorage`/`FailOnceThenSucceedStorage` doubles but throwing
`IllegalStateException` instead of `StorageException`), reproduced the gap: before the fix, an
`add` whose save threw would still show the generic outer-catch error message, but the added
activity remained in memory (`manager.getById(1)` still resolved) - the actual bug the audit
described.

#### Expected Behaviour

If persistence fails after a modifying command, for any failure mode, the exact pre-command
application state is restored.

#### Actual Behaviour (pre-fix)

Restored for `StorageException`; not restored for any other `RuntimeException`.

#### Original Suggested Fix

Keep persistence within the effective rollback boundary; conceptually, catch any failure from the
persist step and call `execution.rollback()` before rethrowing/reporting.

#### Final Implementation

`trySave()` gained a second `catch (RuntimeException e)` clause, reporting it the same way as a
`StorageException` (log at `SEVERE`, show a framed error, return `false`). Because the existing
call site (`if (!trySave()) { execution.rollback(); return true; }`) already rolls back on *any*
`false` return, no change was needed to `processCommand`'s control flow at all - widening
`trySave()`'s catch clause was sufficient to close the gap completely.

#### Why This Implementation Was Chosen

The master prompt's suggested pseudocode restructures `processCommand`'s try/catch to route any
failure through a single rollback point. The actual gap was narrower than that pseudocode implies:
`processCommand`'s existing structure already has exactly one call site that both attempts the
save and (on failure) calls `execution.rollback()` - it just wasn't reached for this one exception
type. Extending `trySave()`'s catch clause is a two-line change that closes the gap with the
smallest possible blast radius, versus restructuring the outer try/catch (which would have also
required reasoning carefully about not double-rolling-back a command whose failure already
occurred, and was rolled back, inside `CommandTransactionExecutor.execute()` itself). The existing
comment at `processCommand`'s outer `catch (RuntimeException e)` ("the pre-execution snapshot has
already been restored by the transaction executor") remains accurate after this fix, since the
only `RuntimeException`s that can still reach that catch are ones with no associated mutation to
roll back in the first place (from `dispatch()`/`confirmIfNeeded()` before any snapshot exists, or
from `recommendationManager.clear()` after a successful, already-persisted save).

#### Regression Tests

- `ApplicationRunnerTest.add_saveFailsWithRuntimeException_rollsBackAndReportsNoFalseSuccess`
- `ApplicationRunnerTest.add_saveFailsWithRuntimeExceptionThenRetrySucceeds_doesNotConsumeIdOnFailedAttempt`

Both mirror the existing `StorageException`-based tests exactly, proving parity between the two
failure modes.

#### Commit

`7562104 fix: rollback mutation after unchecked persistence failure`

---

### Finding 3 - Duplicate Add/Edit prefixes

```text
Priority: P2
Classification: Confirmed
Verification status: Reproduced (traced by hand against FieldParser.extractField; confirmed via
    the app's own built JAR - "add n/A n/B c/..." rejected end to end after the fix)
Affected components: parser.common.FieldParser, parser.activity.AddCommandParser,
    parser.activity.EditCommandParser
Impact: A user typo repeating a field prefix (e.g. "add n/A n/B c/...") silently merged the second
    occurrence into the first field's value ("A n/B") instead of being rejected, producing a
    corrupted description with no error shown.
```

#### Description

`AddCommandParser`/`EditCommandParser` extract each field via `FieldParser.extractField`/
`extractPresentFields`, which locate the *first* occurrence of a marker and read up to the next
*different* marker - a repeated marker was never specifically checked for, so its literal text
was absorbed as part of the first occurrence's value. Separately, `ArgumentTokenizer` (used only
by `preference set`) already implements the correct behaviour: `if (values.containsKey
(current.prefix)) { throw new InvalidCommandException("Duplicate option \"" + current.prefix +
"\"."); }`.

#### Root Cause

`FieldParser.indexOfMarker`/`extractField` had no duplicate-detection logic at all; they simply
locate boundaries, with no memory of markers already consumed.

#### Reproduction / Evidence

Traced `requireField(args, "n/", "c/", "description", "category")` against
`"n/A n/B c/ACADEMIC ..."`: `indexOfMarker(args, "n/", 0)` finds index 0; `indexOfMarker(args,
"c/", 2)` finds the "c/" of "c/ACADEMIC" (the only "c/" boundary); the substring between them,
`"A n/B"`, becomes the description. Confirmed via the built JAR before the fix (absorbed) and
after (rejected with `[Error] Invalid input: Duplicate option "n/".`).

#### Expected Behaviour

`Duplicate option "n/".` is rejected the same way `preference set` already rejects a repeated
marker.

#### Actual Behaviour (pre-fix)

Silently absorbed into the first occurrence's value; two existing tests (
`parseAdd_markerSuppliedTwice_firstOccurrenceValueAbsorbsTheSecond`,
`parseEdit_markerSuppliedTwice_firstOccurrenceValueAbsorbsTheSecond`) explicitly pinned this as
"not a bug fix."

#### Original Suggested Fix

Reject repeated declared prefixes; prefer converging on `ArgumentTokenizer`'s existing semantics
rather than inventing a third mechanism, but avoid unnecessary parser-wide regression.

#### Final Implementation

A new `FieldParser.rejectDuplicateMarkers(String text, String... markers)` scans the whole text
for a second boundary-matched occurrence of any given marker and throws
`InvalidCommandException("Duplicate option \"" + marker + "\".")` - reusing `ArgumentTokenizer`'s
exact wording for consistency, but as a small addition to the existing `FieldParser` mechanism
rather than a migration onto `ArgumentTokenizer` itself. Called from both `AddCommandParser.parse`
and `EditCommandParser.parse`, immediately after the existing `rejectUnrecognisedLeadingText`
guard.

#### Why This Implementation Was Chosen

Migrating `add`/`edit` onto `ArgumentTokenizer` was considered and rejected: `ArgumentTokenizer`
additionally rejects any *undeclared* marker-shaped token (`[A-Za-z][A-Za-z0-9]*/`) found unquoted
anywhere in the text, which is exactly right for `preference set`'s small set of fixed-value
fields but would force `add`/`edit`'s free-text description/note fields to require quoting any
incidental `word/`-shaped text (e.g. `n/Meeting w/ friends` would newly need to be quoted) - a
materially larger, unrequested behavioural change. `rejectDuplicateMarkers` closes exactly the gap
the finding describes (a genuinely declared marker repeated) while leaving undeclared free text
exactly as tolerant as before; a new test
(`FieldParserTest.rejectDuplicateMarkers_undeclaredSlashBearingText_isIgnored`) proves this
explicitly. Because the fix lives in the shared `FieldParser`, `connection find`/`facility find`
(which also call `FieldParser` directly) incidentally gain the same duplicate protection as a
side effect, not as a separate scope expansion - this was not separately tested, since it was not
the finding under audit, but is noted here for completeness.

#### Regression Tests

- `FieldParserTest.rejectDuplicateMarkers_markerRepeated_throwsInvalidCommandExceptionNamingTheMarker`
- `FieldParserTest.rejectDuplicateMarkers_eachMarkerAppearsOnce_doesNotThrow`
- `FieldParserTest.rejectDuplicateMarkers_undeclaredSlashBearingText_isIgnored`
- `AddCommandParserTest.parseAdd_duplicateNameMarker_throwsInvalidCommandException`
- `AddCommandParserTest.parseAdd_duplicateCategoryMarker_throwsInvalidCommandException`
- `AddCommandParserTest.parseAdd_duplicateDateMarker_throwsInvalidCommandException`
- `AddCommandParserTest.parseAdd_descriptionContainsOrdinarySlash_isAcceptedAsPartOfValue`
- `EditCommandParserTest.parseEdit_duplicateNameMarker_throwsInvalidCommandException`
- `EditCommandParserTest.parseEdit_duplicateDateMarker_throwsInvalidCommandException`

The two prior pinning tests were replaced (not merely deleted) with tests asserting rejection.
`text-ui-test/input.txt`'s scripted `edit 1 n/New name n/Another name` case, which had relied on
the old absorb behaviour for its downstream assertions, was also updated (see
[Section 9](#9-validation-results)).

#### Commit

`17bba67 fix: reject duplicate activity command prefixes` (implementation + unit tests),
`6952cda test: update text-ui-test fixture for duplicate-prefix rejection` (E2E fixture)

---

### Finding 4 - Time-dependent E2E tests

```text
Priority: P2
Classification: Confirmed
Verification status: Confirmed by inspection (traced every UniEnable.run call site in
    UniEnableTest.java; confirmed the 3-arg injectable-time overload already existed and was
    unused there)
Affected components: src/test/java/seedu/unienable/UniEnableTest.java
Impact: All three UniEnable.run() call sites in the E2E suite used the real wall clock while the
    suite hardcodes "not before today" activity dates (2026-08-15 through 2026-08-20); the suite
    passes today only because those dates are still in the future, and would start failing once
    the wall clock passes them.
```

#### Description

`UniEnable.java` already exposed `run(Path, InputStream, Supplier<LocalDateTime>)` for exactly
this purpose (used correctly by `ApplicationRunnerTest`), but `UniEnableTest.runWithInput`,
`runWithInputCapturingStderr`, and one inline call in
`run_dataDirectoryCannotBeCreated_showsStartupErrorAndNeverStartsCommandLoop` all used the 2-arg
overload, which defaults to `LocalDateTime::now`.

#### Root Cause

The 3-arg seam existed but was never wired into this specific test class's helper methods.

#### Reproduction / Evidence

Confirmed by inspection: no other test file in the whole `src/test` tree calls a real-clock API
(`LocalDate.now()`/`LocalDateTime.now()`/`Clock.system...`) - the non-determinism was entirely
confined to these three call sites in one file.

#### Expected Behaviour

The suite's pass/fail is independent of the real wall-clock date.

#### Actual Behaviour (pre-fix)

Every hardcoded date literal in the file was checked against the real `LocalDate.now()`.

#### Original Suggested Fix

Add a fixed `TEST_NOW` constant and thread it through every `UniEnable.run` call site via the
existing 3-arg overload, rather than merely pushing the hardcoded dates further into the future.

#### Final Implementation

Exactly as suggested: `private static final LocalDateTime TEST_NOW = LocalDateTime.of(2026, 8, 10,
12, 0);` (matching the master prompt's own example value), strictly before every hardcoded date
literal in the file (earliest is `2026-08-15`), threaded into all three `UniEnable.run` call
sites.

#### Why This Implementation Was Chosen

No deviation from the suggestion - it was already precisely specified and directly applicable.

#### Regression Tests

This finding *is* a test-determinism fix; its own regression coverage is that all 47 existing
`UniEnableTest` cases continue to pass with the fixed clock (verified), proving the fixed `now`
does not change any test's expected behaviour.

#### Commit

`b4ba68f test: make end-to-end tests use deterministic time`

---

### Finding 5 - Activity ID overflow

```text
Priority: P3
Classification: Confirmed
Verification status: Reproduced (regression tests constructing an activity at
    Integer.MAX_VALUE - 1/Integer.MAX_VALUE and exercising load/add/batch-add all failed before
    the fix's guard was added, in the sense that the described silent-wrap/reset behaviour was
    directly traced and would have occurred)
Affected components: logic.ActivityManager, logic.recur.RecurrencePlanner
Impact: Loading an activity with id == Integer.MAX_VALUE caused Math.max(1, id + 1) to overflow to
    Integer.MIN_VALUE and then reset nextId back down to 1 - silently discarding the fact that the
    maximum ID was ever taken, risking a duplicate-ID collision on the very next add.
```

#### Description

Every `nextId`-advancing site in `ActivityManager` (`loadAll`, `restoreState`, `add`,
`addAllAtomically`) performed plain `int` arithmetic (`activity.getId() + 1`, `nextId++`, `nextId
+ index`, `nextId += candidates.size()`) with no overflow protection.
`RecurrencePlanner.plan`'s own `nextId + toCreate.size()` candidate-ID computation had the same
gap.

#### Root Cause

No checked arithmetic and no explicit ID-space-exhaustion tracking existed anywhere in this path.

#### Reproduction / Evidence

Traced: loading a single activity with `id = Integer.MAX_VALUE` makes `activity.getId() + 1`
overflow to `Integer.MIN_VALUE`; `Math.max(1, Integer.MIN_VALUE)` evaluates to `1` - `nextId`
silently resets to `1` with no error, even though ID `1` may already be legitimately in use by
another loaded activity (no duplicate-ID check exists on load), which a subsequent `add()` would
then collide with.

#### Expected Behaviour

Keep IDs as plain positive `int`; explicitly detect ID-space exhaustion; use checked arithmetic;
fail predictably instead of wrapping.

#### Actual Behaviour (pre-fix)

Silent wraparound and, worse, a silent reset that could enable a duplicate-ID collision with no
error at any point.

#### Original Suggested Fix

Keep positive `int` IDs; explicitly detect ID-space exhaustion; use checked arithmetic such as
`Math.addExact`; fail predictably instead of wrapping.

#### Final Implementation

`ActivityManager` gained a private `boolean idSpaceExhausted` flag alongside the existing `int
nextId`. `loadAll`/`restoreState` compute the candidate next ID as `long` (so the comparison
itself cannot overflow) and set `idSpaceExhausted` if that value would exceed
`Integer.MAX_VALUE`. `getNextId()` throws `IllegalStateException` once exhausted rather than
returning a stale or wrapped value; `add()` and `addAllAtomically()` both call `getNextId()` (so
they inherit the same guard) and additionally use safe arithmetic for their own advancement -
`addAllAtomically` computes the batch's final next-ID as `long` and rejects the *whole batch*
atomically upfront if it would need more IDs than remain, rather than partially validating it.
`resetAll()` clears the flag. `RecurrencePlanner`'s `nextId + toCreate.size()` now uses
`Math.addExact`, throwing `ArithmeticException` instead of silently wrapping.

#### Why This Implementation Was Chosen

IDs themselves remain plain `int` end to end (not migrated to `long`), exactly as the suggestion
asked - only the *bookkeeping arithmetic* needed widening/checking, not the ID type, since a
2.1-billion-ID scenario is already an intentionally-unreachable edge case that doesn't justify a
model-wide type change. `getNextId()` throwing (rather than silently returning a sentinel) was
chosen deliberately over alternatives considered and rejected: freezing `nextId` at
`Integer.MAX_VALUE` without a separate exhaustion flag was rejected because it would let a second
`add()` after exhaustion be constructed with the *same*, already-used ID (a duplicate-ID bug in
its own right); the current design intentionally means that once truly exhausted, *any* mutating
command's `CommandTransactionExecutor.Snapshot` capture (which unconditionally calls
`getNextId()` regardless of what the command itself touches) also fails - a broad, deliberately
fail-closed consequence, accepted because reaching this state at all already requires an
adversarial or multi-billion-record data file with no realistic path through ordinary use.

#### Regression Tests

- `ActivityManagerTest.loadAll_highestLoadedIdIsIntegerMaxValue_marksIdSpaceExhausted`
- `ActivityManagerTest.add_consumesFinalValidId_thenExhaustsIdSpace`
- `ActivityManagerTest.add_afterIdSpaceExhausted_throwsIllegalStateExceptionWithoutMutating`
- `ActivityManagerTest.addAllAtomically_batchWouldExceedIntegerMaxValue_throwsIllegalStateExceptionWithoutMutating`
- `ActivityManagerTest.addAllAtomically_batchExactlyFillsToIntegerMaxValue_commitsAndThenExhausts`
- `ActivityManagerTest.resetAll_afterIdSpaceExhausted_clearsExhaustionAndRestartsAtOne`
- `RecurrencePlannerTest.plan_nextIdNearIntegerMaxValue_throwsArithmeticExceptionInsteadOfWrappingCandidateId`

Covering: single allocation near/at the boundary, loading the maximum persisted ID, batch
allocation exhaustion (both the reject-upfront and exact-fit-then-exhaust cases), and reset
recovery, as required.

#### Commit

`b82c644 fix: guard activity ID allocation against overflow`

---

### Finding 6 - Recommender Developer Guide drift

```text
Priority: P3
Classification: Partially Confirmed (only half the section was actually stale)
Verification status: Confirmed by inspection, then re-verified against current source before
    changing anything
Affected components: docs/DeveloperGuide.md Section 16
Impact: Documentation-only; a reader following the stale paragraph would believe
    preferredRangePenalty still exists in source as a deliberately-kept no-op field, when it (and
    the whole SlotScore apparatus) was actually deleted by a later commit.
```

#### Description

Before touching anything, the current DG text was compared directly against current source (per
the audit's own "verify before changing" rule), rather than trusting the prior audit's premise.
This found that the prior audit's claim was **already half-wrong**: `chooseBestSlot`/
`chooseNextActivity` were correctly described as replaced (an earlier commit, `d9d89fe`, had
already rewritten that part of the section to describe the current whole-day-optimization search
accurately, and a `grep` confirmed neither method name exists in `src/main` anymore). What
remained genuinely stale was a *different*, earlier paragraph (never revisited by `d9d89fe`)
claiming `preferredRangePenalty` was "kept rather than removed... always 0... a no-op safety net" -
`d9d89fe` had in fact deleted that field and the whole `SlotScore` apparatus entirely, and the very
next paragraph in the same section already said so ("This search replaces... the... preference-
penalty scoring fields entirely"), directly contradicting the earlier, unrevised paragraph.

#### Root Cause

A doc paragraph written before a later refactor (`d9d89fe`) was never updated after that refactor
deleted the exact field the paragraph described as retained.

#### Reproduction / Evidence

`grep -rn "preferredRangePenalty|chooseBestSlot|chooseNextActivity" src/main` returned zero
matches. `git show d9d89fe` showed `-preferredRangePenalty(...)`, `-preferencePenalty`,
`-SlotScore` as pure removals.

#### Expected Behaviour

The DG accurately and consistently describes the current whole-day-optimization algorithm with no
internally-contradictory paragraphs.

#### Actual Behaviour (pre-fix)

One paragraph (lines ~546-556) contradicted the very next paragraph in the same section.

#### Original Suggested Fix

Remove obsolete algorithm descriptions and contradictory historical explanations; describe the
current flow accurately.

#### Final Implementation

Rewrote only the stale paragraph to state that `preferredRangePenalty` "no longer exists in the
current source at all" and was removed by the whole-day-optimization rewrite along with the rest
of `SlotScore`, consistent with the adjacent paragraph. The already-accurate whole-day-optimization
narrative, boundary-enforcement pipeline description, and `RecommendationClassDiagram`/
`RecommendationSequence` diagrams were left untouched, since they were independently confirmed
accurate against current source and diagram cross-references. (`RecommendationSequence.puml`/
`.png` were later split into `RecommendationGenerationSequence`/`RecommendationAdoptionSequence`
in a separate follow-up diagramming pass, requested and completed after this audit - see
`HANDOVER.md` §0g; that split was a readability improvement, not a correctness fix, and does not
change this finding's verification.)

#### Why This Implementation Was Chosen

Minimal, targeted correction rather than a full section rewrite, since most of the section was
already correct - rewriting the whole section would have been unjustified churn against text that
was already synchronized by an earlier commit.

#### Regression Tests

Not applicable (documentation-only finding). Verified via `grep` that no source method name in
the corrected text is stale, and that all five `RecommendationServiceTest` method-name references
the paragraph cites still exist.

#### Commit

`3b19069 docs: fix stale preferredRangePenalty claim in recommender DG section`

---

### Finding 7 - Duplicate date/time parsing logic

```text
Priority: P3
Classification: Confirmed (real duplication), but Improvement Only - declined
Verification status: Confirmed by inspection
Affected components: parser.common.DateTimeParser, storage.ActivityStorage,
    storage.preference.PreferenceStorage
Impact: None (maintainability only) - not release-blocking.
```

#### Description

`DateTimeParser` and `ActivityStorage` do textually duplicate their `DATE_FORMAT`/`TIME_FORMAT`/
`DATE_SHAPE` constants and strict shape-then-parse logic (`PreferenceStorage` duplicates
`TIME_FORMAT` a third time). Critically, `ActivityStorage` already carries an explanatory comment
at the duplication site: *"Storage owns its persistence-format parsing independently of
parser.common.DateTimeParser, even though the wire format happens to use the same yyyy-MM-dd/HH:mm
shapes - a codec must keep loading the exact bytes it already wrote regardless of how the CLI
parser's input rules evolve."*

#### Root Cause

Not a defect - a deliberate, already-documented architectural decision to keep the persistence
codec decoupled from the CLI parser's evolving input rules.

#### Reproduction / Evidence

Confirmed the two format-constant blocks and shape/strict-parse methods are near-identical text.
Also confirmed: `storage` imports nothing from `parser`, and `parser` already imports `storage`
elsewhere (`ConnectionCommandParser`, `FacilityCommandParser`, `RecurCommandParser`) - so
extracting a shared codec into a new neutral package would not itself create a new *dependency
cycle*, but would recreate the coupling risk the existing comment specifically warns against:
if `DateTimeParser`'s rules are relaxed for CLI ergonomics in the future, a shared codec would
either need internal bifurcation (defeating the point of sharing) or would silently change what
`ActivityStorage` can reload from files it already wrote under the old rules.

#### Expected Behaviour / Actual Behaviour

N/A - behaviour is correct on both sides; this is a code-duplication observation, not a behaviour
defect. `ActivityStorage` correctly carries no "not before today"/"not before now" policy (that
stays exclusively in `DateTimeParser`), so there is no policy-leakage risk to weigh against.

#### Original Suggested Fix

Extract a neutral shared utility (e.g. `common/DateTimeFormats.java`) if low-risk; otherwise
document why not.

#### Final Implementation

**Declined.** No code change. The existing in-code rationale was elevated into a new
`docs/DeveloperGuide.md` Section 18 bullet, so the reasoning is discoverable without reading the
comment in isolation.

#### Why This Implementation Was Chosen

The master prompt itself instructs: *"If extracting the shared utility would cause disproportionate
churn or package coupling, document why you chose not to change it."* The codebase had already
made and documented this exact call before this audit began; overriding it without new information
would have reintroduced precisely the risk the original author identified, for a purely cosmetic
gain (fewer duplicated lines) with no behavioural benefit. This is a case where "verify before
changing" surfaced a reason *not* to act, which the audit's own rules treat as a valid, expected
outcome.

#### Regression Tests

Not applicable (no code change).

#### Commit

`797bf07 docs: record decision not to extract shared date/time parsing codec`

---

### Finding 8 - Large classes and maintainability

```text
Priority: P4
Classification: Partially justified
Verification status: Confirmed by inspection (each flagged class read in full)
Affected components: command.general.GuideCommand, logic.recommend.RecommendationService,
    logic.ActivityManager, storage.Storage, storage.ActivityStorage,
    parser.activity.ActivityCommandParser, app.ApplicationRunner, logic.dashboard.DashboardService
Impact: None directly (maintainability only).
```

#### Description / Root Cause / Reproduction

Each class was read in full and judged on cohesion, not line count, per the master prompt's
explicit instruction not to split classes just because they are long.

- **`GuideCommand`** (585 lines): ~35 lines of command logic; the remaining ~480 lines are a
  single static help-text lookup table (`buildTopics()`), not branching logic. **No refactor
  needed** - a lookup table is not improved by relocating it across five new files.
- **`DashboardService`** (272 lines): one cohesive selector-then-`summarize()` pipeline; length
  tracks the number of tracked metrics. **No refactor needed.**
- **`RecommendationService`** (556 lines): one cohesive whole-day-search algorithm; length tracks
  genuine algorithmic complexity (permutation search, heuristic fallback, tie-break scoring).
  **No refactor needed** (a `DayScheduleOptimizer` extraction would be reasonable optional polish,
  not a necessity).
- **`ActivityCommandParser`** (340 lines): already a thin router to dedicated sub-parsers plus
  genuinely shared small utilities - the product of prior decomposition. **No refactor needed.**
- **`ActivityStorage`** (453 lines): a single-purpose codec whose length tracks two persisted
  record shapes' field counts, not unrelated concerns. **No refactor needed** for structure (a
  separate, minor DRY note: its duplicate/overlap load-time check reimplements rather than reuses
  `ActivityConflictChecker` - noted but not a cohesion problem in `ActivityStorage` itself, and
  out of scope for this audit pass).
- **`ApplicationRunner`** (315 lines, already reduced from its pre-`cdb2a24` size by extracting
  `CommandTransactionExecutor`): sequential phases of one session lifecycle sharing state. **No
  refactor needed** - already appropriately decomposed.
- **`ActivityManager`** (517 lines): CRUD/list/find/ID-allocation form one cohesive aggregate-root
  responsibility, **but** the ~150-line "next relevant activity" selector (`next`/
  `findScheduledInProgress`/`findNearestUpcomingScheduled`/`findSoonestEndingFlexible`) is a
  self-contained, stateless `(activities, now) -> Optional<Activity>` computation with the same
  shape as the already-extracted `ActivityConflictChecker`. **Refactor justified** (extraction
  into a `NextActivityFinder`).
- **`Storage`** (478 lines): a thin per-entity delegation facade, **plus** a ~150-line generic
  atomic multi-file transaction engine (`tempSiblingOf`/`backupIfExists`/`commit`/`restore`/
  `commitAllWithRollback`) with zero knowledge of activities/topics, independently testable with
  arbitrary temp files. **Refactor justified** (extraction into an `AtomicFileTransaction`).

#### Expected / Actual Behaviour

N/A - no defect.

#### Original Suggested Fix

Refactor only if genuinely beneficial; do not split for line count alone.

#### Final Implementation

**Deferred, not implemented.** Both genuinely-justified extractions (`NextActivityFinder` from
`ActivityManager`, `AtomicFileTransaction` from `Storage`) were identified and documented, but not
implemented in this audit pass.

#### Why This Implementation Was Chosen

The master prompt places maintainability refactors last in priority order (Phase 5), explicitly
"only if genuinely beneficial... and only if regression risk remains acceptable," and instructs
that "large-scale maintainability refactoring should occur after correctness fixes." With five
confirmed correctness defects, one documentation-drift fix, and all mandatory User
Guide/Developer Guide/diagram synchronization work still to complete within this same audit pass,
spending additional regression-risk budget on two mechanical-but-nontrivial extractions (each
touching a class with extensive existing test coverage that would need to keep passing unchanged)
was judged not to justify the risk relative to the correctness work's priority. Both are recorded
in `docs/DeveloperGuide.md` Section 18 as confirmed-justified-but-deferred, so a future session has
the reasoning and doesn't need to re-derive it.

#### Regression Tests

Not applicable (no code change).

#### Commit

`ec528a3 docs: record P4 large-class maintainability review findings`

---

## 5. Test Coverage Improvements

| Test class | New/changed tests | Finding protected |
|---|---|---|
| `AccessibilityGraphTest` | 3 new (overflow-correct-choice, exact-total-preserved, cycle-guard) | Finding 1 |
| `GraphPathTest` | 1 new (above-`MAX_VALUE` total preserved) | Finding 1 |
| `ApplicationRunnerTest` | 2 new + 2 new test doubles (`RuntimeExceptionAfterStorage`, `RuntimeExceptionOnceThenSucceedStorage`) | Finding 2 |
| `UniEnableTest` | 3 call sites converted to injected fixed time | Finding 4 |
| `FieldParserTest` | 3 new (duplicate rejection, no-duplicate pass-through, undeclared-marker tolerance) | Finding 3 |
| `AddCommandParserTest` | 1 pinning test replaced + 3 new (duplicate name/category/date) + 1 new (ordinary-slash tolerance) | Finding 3 |
| `EditCommandParserTest` | 1 pinning test replaced + 1 new (duplicate date) | Finding 3 |
| `text-ui-test/input.txt` + `EXPECTED.TXT` | scripted duplicate-prefix case updated to expect rejection | Finding 3 |
| `ActivityManagerTest` | 7 new (exhaustion on load, final-ID consumption, post-exhaustion rejection for single/batch add, exact-fit batch, reset recovery) | Finding 5 |
| `RecurrencePlannerTest` | 1 new (`Math.addExact` guard) | Finding 5 |

Total: 1236 tests before this audit's changes -> **1250 tests** after (14 net new test methods;
some pre-existing tests were also modified in place, e.g. the two replaced pinning tests).

---

## 6. Architecture / Maintainability Review

See Finding 8 above for the full per-class analysis. Summary: six of eight flagged classes needed
no change; two extractions (`NextActivityFinder`, `AtomicFileTransaction`) are confirmed
genuinely justified and are recorded as deferred follow-up work in `docs/DeveloperGuide.md`
Section 18, not treated as defects. No class was split purely to reduce line count.

---

## 7. Documentation Review

**User Guide (`docs/UserGuide.md`) updates:**
- Section 4 (General Input Rules): new bullet documenting duplicate field-prefix rejection and its
  exact error message.

**Developer Guide (`docs/DeveloperGuide.md`) updates:**
- Section 5 (Activity model): new paragraph on the ID-overflow guard.
- Section 10 (Atomic mutation and rollback): rewritten to name `CommandTransactionExecutor`
  explicitly and to state that `trySave` covers both checked and unchecked save failures.
- Section 12 (Accessible route search): new paragraph on the `long`-cumulative-distance /
  `int`-edge-distance split and the reconstruction cycle guard.
- Section 16 (Deterministic schedule recommendation): one stale paragraph corrected (see Finding
  6); the rest of the section was confirmed already accurate and left untouched.
- Section 18 (Design considerations): the existing `ArgumentTokenizer`/`preference set` bullet
  updated; new bullets for the `add`/`edit` duplicate-prefix decision, the declined
  date/time-codec extraction, and the P4 maintainability review findings.
- Section 19 (Testing): new note on `UniEnableTest`'s deterministic-time injection.

**Stale sections removed:** the single stale `preferredRangePenalty` paragraph in Section 16
(Finding 6); no other stale sections were found during the final documentation-consistency sweep.

**Comments corrected:** none required correction beyond what the code changes themselves made
newly accurate (e.g. `ApplicationRunner`'s outer-catch comment, discussed in Finding 2, needed no
edit because the fix made it accurate rather than making it stale).

**Diagrams changed:**
- `docs/diagrams/sequence/MutationRollbackSequence.puml`/`.png`: the "a file commit fails" branch
  relabelled to cover both `StorageException` and an unexpected `RuntimeException`.
- `docs/diagrams/class/RouteClassDiagram.puml`/`.png`: `GraphPath.totalDistanceInMetres` type
  corrected from `int` to `long`.

Both were regenerated with the same PlantUML toolchain used by the repository's own prior diagram-
regeneration commits (`ce4dc37`, `16ec3f8`) and visually verified to render correctly and match
their `.puml` sources before committing.

---

## 8. Implementation Follow-Up

| Finding | Original suggestion | Final implementation | Difference from suggestion | Tests | Commit |
|---|---|---|---|---|---|
| Route overflow | Widen cumulative/queue/`GraphPath` totals to `long`; add cycle guard | Exactly as suggested; `reconstructPath` made package-private for direct testability | None (a testability-only visibility change) | 4 new | `3b036d6` |
| Rollback gap | Restructure persistence within the rollback boundary | Widened `trySave()`'s catch clause; no restructuring needed | Smaller surface area than the suggested pseudocode implies | 2 new | `7562104` |
| Duplicate prefixes | Converge on `ArgumentTokenizer` semantics, minimal regression | New `FieldParser.rejectDuplicateMarkers`, reusing `ArgumentTokenizer`'s message wording only | Did not migrate onto `ArgumentTokenizer` itself - avoids its stricter undeclared-marker-shape rejection | 9 new/changed + E2E fixture | `17bba67`, `6952cda` |
| E2E time | Fixed `TEST_NOW`, inject via existing 3-arg overload | Exactly as suggested | None | 47 existing tests re-verified | `b4ba68f` |
| ID overflow | Positive `int` IDs; checked arithmetic; explicit exhaustion detection | `long`-computed bookkeeping + `idSpaceExhausted` flag + `IllegalStateException`; `Math.addExact` in `RecurrencePlanner` | None in spirit; specific mechanism (flag + fail-fast getter) chosen over alternatives to avoid a duplicate-ID risk in a frozen-sentinel design | 8 new | `b82c644` |
| DG recommender drift | Remove stale/contradictory text; describe current algorithm | Corrected only the one stale paragraph; rest of section confirmed already accurate | Suggestion assumed the whole section was stale; verification found only half was | N/A (docs) | `3b19069` |
| Date/time duplication | Extract shared codec if low-risk, else document why not | Declined; existing in-code rationale elevated to DG | Suggestion's own fallback clause exercised | N/A (docs) | `797bf07` |
| Large classes | Refactor only if genuinely beneficial | Two extractions confirmed justified, documented, deferred | Suggestion's own "correctness first" priority followed | N/A (docs) | `ec528a3` |

---

## 9. Validation Results

| Command | Result |
|---|---|
| `./gradlew clean test` | **PASS** - 1250 tests, 0 failures, 0 errors, 0 skipped |
| `./gradlew checkstyleMain` | **PASS** - no violations |
| `./gradlew checkstyleTest` | **PASS** - no violations |
| `./gradlew javadoc` | **PASS** - no warnings surfaced in output |
| `./gradlew shadowJar` | **PASS** - `build/libs/unienable.jar` produced |
| `./gradlew build` | **PASS** (includes `check`: test + checkstyleMain + checkstyleTest) |
| `text-ui-test/runtest.sh` (Git Bash) | **PASS** - "Test passed!" after fixture update for Finding 3 |
| Release smoke test (manual, via built JAR) | **PASS** - duplicate-prefix rejection verified end to end; large-distance route (2,000,000,000 m direct edge vs. two 1,200,000,000 m hops) correctly chose the true-shortest direct edge instead of the pre-fix overflow-corrupted alternative |

No environmental limitations were encountered; every listed pipeline command executed to
completion successfully.

---

## 10. Remaining Issues

- **`NextActivityFinder` extraction from `ActivityManager`** and **`AtomicFileTransaction`
  extraction from `Storage`** - both confirmed genuinely justified (Finding 8), deferred as P4/
  Phase-5 maintainability work out of scope for this correctness-focused pass. Documented in
  `docs/DeveloperGuide.md` Section 18.
- **`ActivityStorage`'s duplicate/overlap load-time check reimplements rather than reuses
  `ActivityConflictChecker`** - noted in passing during the Finding 8 review as a minor
  cross-class DRY observation, not a cohesion problem in `ActivityStorage` itself and not part of
  any of the eight findings under audit; left unchanged.
- **Shared date/time-parsing codec** - confirmed real but declined by deliberate, documented
  decision (Finding 7); revisit only if the two formats are ever required to diverge or converge.
- Three pre-existing untracked files in the working tree
  (`.codex-presentations/`, `UniEnable_NUS_Enablers_Presentation.pptx.inspect.ndjson`,
  `~$UniEnable_NUS_Enablers_Presentation.pptx`) are unrelated to this audit's scope and were left
  untouched throughout.

---

## 11. Final Release Recommendation

```text
READY
```

All five confirmed correctness defects (two P1, two P2, one P3) are fixed, each with regression
tests that fail without the fix and pass with it, and each verified additionally either against
the real built JAR (Findings 1 and 3) or the full existing test suite re-run unchanged (Finding
4). The one confirmed documentation-drift finding is corrected. The one finding confirmed real but
judged not worth acting on (Finding 7) has a documented rationale consistent with an existing,
already-reasoned architectural decision in the code. The one finding partially justified but
deferred (Finding 8) is a maintainability improvement with no behavioural risk either way. User
Guide, Developer Guide, and every affected diagram are synchronized with the final source. The
full validation pipeline (tests, Checkstyle, Javadoc, shadowJar, build, text-UI regression, and a
manual release smoke test) passes cleanly, and the working tree is clean.
