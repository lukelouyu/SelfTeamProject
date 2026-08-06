# Implementation Notes: `dashboard`

## Class responsibilities

```text
parser/dashboard/DashboardCommandParser.java
    parse(activityManager, now, args) -> DashboardCommand
    Validates grammar (accept/reject list in ACCEPTANCE_CRITERIA.md), resolves the selected
    period via DashboardService's resolve*(...) methods (using the same `now` captured once,
    matching the existing list/next/edit precedent), and captures the `detail` flag. Never reads
    activity data itself.

command/dashboard/DashboardCommand.java
    execute() -> CommandResult
    Orchestration only: calls DashboardService.summarize(activityManager, period, now), then
    DashboardFormatter.format(summary, detail). Read-only - inherits ReadOnlyCommand and
    implements neither Confirmable nor MenuConfirmable.

logic/dashboard/DashboardService.java
    Stateless (private constructor, all-static), mirroring feature/v2-route's
    AccessibleRouteGraphFactory:
    - resolveToday/resolveTomorrow/resolveDate/resolveThisWeek(...) -> DashboardPeriod
    - summarize(activityManager, period, now) -> DashboardSummary
    - a package-private clip(...) interval-clipping helper (not exposed publicly solely for
      testing, per explicit instruction - covered by same-package unit tests instead)
    Owns period selection and all metric calculation - the only place duration/rating/completion
    arithmetic happens, so it can't drift between the command, parser, and formatter.

model/dashboard/DashboardPeriod.java
    Immutable: label (String, e.g. "Today"/"Tomorrow"/"This week"/the ISO date string for
    date/), start (LocalDateTime, inclusive), end (LocalDateTime, exclusive). Capacity is derived
    (Duration.between(start, end).toMinutes()), not stored, so a day period and a week period
    need no separate "kind" field or hardcoded capacity table.

model/dashboard/RatingSummary.java
    Immutable, reused identically for energy and sensory (same shape: total, highCount, hasData,
    average, highest, distribution[5]) rather than duplicating six fields twice inline in
    DashboardSummary - energy and sensory are structurally identical metrics over different
    ratings.

model/dashboard/DashboardSummary.java
    Immutable: period, totalActivityCount, plannedWorkloadMinutes, nominalBufferMinutes,
    overloadMinutes, energy (RatingSummary), sensory (RatingSummary), eligibleCount,
    completedEligibleCount, completionPercentage (present only if eligibleCount > 0), fixedCount,
    flexibleCount, categoryCounts (Map<ActivityCategory,Integer>, always populated for all four
    categories so the formatter never needs a null-check per category).

ui/dashboard/DashboardFormatter.java
    format(DashboardSummary, boolean detail) -> String. Pure text formatting - period-empty and
    nothing-due-yet special cases, ASCII bars, N/A/unavailable text. No calculation of its own.
```

## Calculation boundaries

- **Period**: half-open `[start, end)` in every case, computed with `java.time` (`LocalDate`,
  `TemporalAdjusters`), never manual day arithmetic - correct across month/year boundaries and
  leap days for free.
- **Capacity**: `Duration.between(period.start, period.end).toMinutes()` - 1440 for any single day,
  10080 for the week, derived rather than hardcoded per period "kind".
- **Fixed-activity inclusion**: `activityStart < periodEnd && activityEnd > periodStart`.
- **Fixed-activity workload**: clipped to
  `[max(activityStart, periodStart), min(activityEnd, periodEnd))`.
- **Flexible-activity inclusion**: same half-open test over `[earliestStart, latestEnd)`.
- **Flexible-activity workload**: the full requested `durationMinutes`, uncllipped, counted once
  per included activity - see "Known limitation" below.
- **Buffer**: `nominalBuffer = max(0, capacity - workload)`;
  `overload = max(0, workload - capacity)`. Never both non-zero at once.
- **High-rating threshold**: `HIGH_RATING_THRESHOLD = 4` (one named constant in
  `DashboardService`, used for both energy and sensory).
- **Average rating**: computed with `BigDecimal`, `RoundingMode.HALF_UP`, scale 1 - not raw
  `double`/`String.format`, so the documented rounding rule can't silently vary by platform or JDK
  version.
- **Completion percentage**: `Math.round(completed * 100.0 / eligible)` - `Math.round` rounds
  half-up for positive values, matching every documented example (`2/3 -> 67%`) exactly; computed
  once in `DashboardService.summarize`, stored on `DashboardSummary`, never recomputed by the
  formatter.

## Completion-eligibility rules

An activity is completion-eligible only once its own time has fully passed, using the same
injected `now` the period itself was resolved against (captured once at parse time, never
re-read):

- Fixed: `!endTime.isAfter(now)`.
- Flexible: `!latestEnd.isAfter(now)`.

Eligibility is independent of period selection - the period decides which activities are *in* the
dashboard; `now` separately decides which of those are old enough to count toward completion.
Future or currently-in-progress activities are excluded from both the completed and incomplete
counts, not folded into "incomplete" (which would misrepresent something not due yet as behind
schedule).

## Cross-midnight handling (approved substitution - read before touching this area)

**The current v1.0 activity model cannot represent a genuine cross-midnight activity.**
`FixedActivity` and `FlexibleActivity` each hold one `LocalDate date` field, and both constructors
assert same-day ordering (`endTime.isAfter(startTime)`, `latestEnd.isAfter(earliestStart)`) -
enforced invariants, not merely convention. A real "23:00 Monday to 01:00 Tuesday" activity simply
cannot be constructed anywhere in this codebase, through the CLI or directly in Java.

This branch does **not** change that. No cross-date activity support was added to
`FixedActivity`/`FlexibleActivity`, their parsers, storage format, or conflict-checking - that
would be a v1.0 data-model expansion, outside `feature/v2-dashboard`'s approved scope, explicitly
declined.

Instead:

- The interval-clipping calculation (`DashboardService`'s package-private `clip(...)` helper) is
  implemented **generically**, over raw `LocalDateTime` boundaries, with no assumption that an
  activity's start and end share a calendar date. It is exactly as correct for a hypothetical
  midnight-spanning interval as for an ordinary same-day one - genericity, not a special case.
- The required cross-midnight behaviour is verified **at the calculation level**:
  `DashboardServiceTest` feeds `clip(...)` synthetic `LocalDateTime` pairs simulating
  Monday 23:00 -> Tuesday 01:00 directly, against a Monday-only period, a Tuesday-only period, and
  the Monday-Sunday week period, asserting 60/60/120-minute contributions respectively (per
  `ACCEPTANCE_CRITERIA.md`'s AC-DASH-INTERVAL-04/08) - not by constructing an invalid
  `FixedActivity`, which the model's own assertions would reject.
- `docs/UserGuide.md` does **not** claim a user can create a cross-midnight activity - this is an
  internal calculation-safety and future-compatibility property, not a user-facing capability.

## Deterministic ordering

- Category-grouped counts iterate `ActivityCategory.values()` (fixed enum declaration order:
  `ACADEMIC, CCA, WORK_INTERNSHIP, OTHERS`), looking up each category's count from a `Map` rather
  than iterating the map itself - sidesteps `HashMap` ordering entirely regardless of the map
  implementation chosen.
- Rating distributions are stored as `int[5]` indexed by `rating - 1`, not a `Map<Integer,Integer>`
  - ascending order is structural, not an iteration-order assumption.

## Rejected alternatives

- **A `DashboardDetail` class separate from `DashboardSummary`.** Rejected: the master prompt's
  own suggested file list has one summary model, not two; detail metrics are cheap to always
  compute (no activity list is large enough for this to matter), so gating by a boolean at
  *format* time rather than a separate *model* type avoids a second object with its own
  emptiness/nullability story.
- **Six separate energy/sensory fields inline on `DashboardSummary`.** Rejected in favour of one
  reusable `RatingSummary`, since energy and sensory are structurally identical metrics; avoids
  duplicating the same average/highest/distribution logic twice.
- **Exposing `clip(...)` as public API for direct testing.** Rejected per explicit instruction;
  it's package-private, tested from a same-package test class instead - the project already uses
  this pattern (e.g. `ActivityConflictChecker`, package-private and tested in-package).
- **`double` average with `String.format("%.1f", ...)`.** Rejected: platform/locale-dependent
  rounding behaviour risk: `BigDecimal` + `RoundingMode.HALF_UP` makes the documented rounding
  rule exact and testable independent of JVM/locale.

## Guide menu numbering - no new number needed

The v2.0 master prompt's Section 21 says to add Dashboard "using the next available number after
Route" (implying a new item 12, renumbering Return to 13, mirroring how `route` got a genuinely
new item 11). That instruction doesn't hold here: `GuideCommand.MENU_NUMBER_TOPICS` already listed
`"dashboard"` at index 4 (menu item **5**, "Completion and dashboard") in v1.0, before this branch
- unlike `route`, `timetable`, `recommend`, and `export`, `dashboard` already had a reserved number
from the original v1.0 menu design, just showing "Coming soon" content. Taking a new number would
have meant renumbering Return (12 -> 13) for no reason, directly working against the master
prompt's own "do not renumber existing entries" rule. This branch instead replaces item 5's topic
*text* only; every menu number keeps its existing meaning, `CommandDispatcher`'s bare-number
switch is unchanged, and no out-of-range boundary moved.

## Deviations from the original planning-document sketches

Both older `docs/planning/` documents disagree with each other and, in places, with the approved
master prompt; the master prompt (and the explicit corrections in this conversation) is
authoritative throughout:

- Exact-date syntax is `date/YYYY-MM-DD` (marker-based, matching `list`), not the bare
  `dashboard 2026-08-15` the older UG draft sketched.
- `this week` reuses `list`'s existing Monday-Sunday definition, not the older UG draft's rolling
  "today + 6 days" window.
- Metrics are totals (energy/sensory) plus a "Nominal buffer" line and high-rating counts, per the
  master prompt's literal Section 9/11/12, not the older UG draft's Average/Highest-only,
  no-buffer-line example.
