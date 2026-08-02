# Acceptance Criteria: `dashboard`

IDs are referenced from `TEST_PLAN.md`.

## Grammar (AC-DASH-GRAMMAR)

- **AC-DASH-GRAMMAR-01**: `dashboard today`, `dashboard today detail`, `dashboard tomorrow`,
  `dashboard tomorrow detail`, `dashboard date/YYYY-MM-DD`, `dashboard date/YYYY-MM-DD detail`,
  `dashboard this week`, `dashboard this week detail` all parse successfully.
- **AC-DASH-GRAMMAR-02**: `dashboard` (bare, no selector) is rejected (`MissingInputException`).
- **AC-DASH-GRAMMAR-03**: `dashboard date/` (empty value) is rejected.
- **AC-DASH-GRAMMAR-04**: `dashboard date/2026-02-30` (shape-valid, calendar-invalid) is rejected
  with the same "date does not exist" family of message `DateTimeParser` already uses.
- **AC-DASH-GRAMMAR-05**: `dashboard 2026-08-15` (bare date, no `date/` marker) is rejected -
  deliberately deviating from the older planning draft's bare-date sketch, per explicit
  instruction, for consistency with `list date/YYYY-MM-DD`.
- **AC-DASH-GRAMMAR-06**: `dashboard week` (partial/unknown selector) is rejected.
- **AC-DASH-GRAMMAR-07**: `dashboard today detail detail` (duplicate `detail`) is rejected.
- **AC-DASH-GRAMMAR-08**: `dashboard tomorrow extra` (unexpected trailing text) is rejected.
- **AC-DASH-GRAMMAR-09**: `dashboard date/2026-08-15 unexpected` is rejected.
- **AC-DASH-GRAMMAR-10**: `dashboard unknown` is rejected.
- **AC-DASH-GRAMMAR-11**: Every parser failure leaves `ActivityManager` state completely
  unchanged (no read even attempted before validation completes).
- **AC-DASH-GRAMMAR-12**: The command word and `detail` keyword are case-insensitive, matching
  every other command in the app; `date/` marker matching is case-insensitive per
  `FieldParser`'s existing convention.

## Period meaning (AC-DASH-PERIOD)

- **AC-DASH-PERIOD-01**: `today` resolves to `[today 00:00, tomorrow 00:00)` using the injected
  `now`, never `LocalDate.now()`/`LocalDateTime.now()` read directly in production or test code.
- **AC-DASH-PERIOD-02**: `tomorrow` resolves to `[tomorrow 00:00, day-after-tomorrow 00:00)`.
- **AC-DASH-PERIOD-03**: `date/YYYY-MM-DD` resolves to `[that date 00:00, next date 00:00)`.
- **AC-DASH-PERIOD-04**: `this week` resolves to `[Monday 00:00, following Monday 00:00)` of the
  week containing `now` - identical boundary computation to `list this week`'s existing
  `TemporalAdjusters.previousOrSame(MONDAY)`/`nextOrSame(SUNDAY)` logic, not a rolling 7-day
  window.
- **AC-DASH-PERIOD-05**: Period capacity in minutes is derived generically as
  `Duration.between(start, end).toMinutes()`, giving exactly 1440 for a single day and 10080 for a
  week without a hardcoded per-period-type table.
- **AC-DASH-PERIOD-06**: Boundaries are correct across a month transition, a year transition, and
  a leap day (e.g. `date/2028-02-29`), since all period math uses `java.time` date arithmetic, not
  manual day-counting.

## Interval inclusion and clipping (AC-DASH-INTERVAL)

- **AC-DASH-INTERVAL-01**: A fixed activity is included when
  `activityStart < periodEnd AND activityEnd > periodStart` (half-open intersection). An activity
  ending exactly at `periodStart` is excluded; an activity starting exactly at `periodEnd` is
  excluded.
- **AC-DASH-INTERVAL-02**: A flexible activity is included when
  `earliestStart < periodEnd AND latestEnd > periodStart` (same half-open shape over its window,
  not its requested duration).
- **AC-DASH-INTERVAL-03**: An included fixed activity's workload contribution is clipped to the
  selected period: `contribution = durationBetween(max(activityStart, periodStart),
  min(activityEnd, periodEnd))`.
- **AC-DASH-INTERVAL-04**: The clipping calculation must correctly handle a hypothetical interval
  spanning midnight, even though the current v1.0 activity model does not permit users to create
  such an activity (both `FixedActivity` and `FlexibleActivity` constrain start/end to one
  calendar date, constructor-enforced). Verified with synthetic `LocalDateTime` interval
  boundaries directly against the clipping calculation, not against a constructed `FixedActivity`.
  See `IMPLEMENTATION_NOTES.md`.
- **AC-DASH-INTERVAL-05**: An activity fully covering the selected period contributes exactly the
  period's full duration.
- **AC-DASH-INTERVAL-06**: An activity fully outside the selected period contributes `0` and is
  not included at all.
- **AC-DASH-INTERVAL-07**: An included flexible activity's workload contribution is its full
  requested `durationMinutes`, counted once, never clipped to the overlapping portion of its
  window - documented as a stated limitation (AC-DASH-INTERVAL-08), not silently invented data.
- **AC-DASH-INTERVAL-08**: A synthetic Monday-23:00-to-Tuesday-01:00 interval clipped against a
  Monday-only period contributes 60 minutes; against a Tuesday-only period contributes 60 minutes;
  against a Monday-Sunday week period contributes 120 minutes (the two clipped portions, not the
  original span double-counted).

## Planned workload (AC-DASH-WORKLOAD)

- **AC-DASH-WORKLOAD-01**: Planned workload = sum of clipped fixed-activity durations + sum of
  included flexible activities' full requested durations.
- **AC-DASH-WORKLOAD-02**: Overlapping fixed activities are counted individually (their clipped
  durations are simply summed, never merged into a union of occupied time) - intentional, tested,
  and documented as a stated limitation.
- **AC-DASH-WORKLOAD-03**: An empty period (no activities at all) has workload `0`.

## Nominal buffer and overload (AC-DASH-BUFFER)

- **AC-DASH-BUFFER-01**: `rawBufferMinutes = periodCapacityMinutes - plannedWorkloadMinutes`.
- **AC-DASH-BUFFER-02**: `nominalBufferMinutes = max(0, rawBufferMinutes)`.
- **AC-DASH-BUFFER-03**: When `rawBufferMinutes < 0`, `overloadMinutes = abs(rawBufferMinutes)` is
  additionally shown as "Overloaded by: ...", and `nominalBufferMinutes` is `0`.
- **AC-DASH-BUFFER-04**: The metric is always labelled "Nominal buffer", never "Free time".
- **AC-DASH-BUFFER-05**: Exact-full-capacity workload yields `nominalBufferMinutes = 0`,
  `overloadMinutes = 0` (not shown as overloaded). One minute below capacity yields
  `nominalBufferMinutes = 1`. One minute above capacity yields `overloadMinutes = 1`.

## Energy and sensory ratings (AC-DASH-RATING)

- **AC-DASH-RATING-01**: Total energy demand = sum of `energyRating` over every included activity
  (fixed and flexible both contribute). Total sensory load is the same, for `sensoryRating`.
- **AC-DASH-RATING-02**: `HIGH_RATING_THRESHOLD = 4` (a named constant, not an inline magic
  number); ratings `4` and `5` count as high, rating `3` and below does not.
- **AC-DASH-RATING-03**: High-energy and high-sensory activity counts are computed independently
  (an activity can count toward one, both, or neither).
- **AC-DASH-RATING-04**: Detail-mode average rating uses all included activities, rounded to one
  decimal place using half-up rounding, computed identically regardless of platform (not raw
  `double` string formatting whose rounding mode may differ).
- **AC-DASH-RATING-05**: Detail-mode highest rating, for a non-empty period, is the maximum
  included rating. For an empty period (or a period whose activities are all excluded), average
  and highest are shown as unavailable text, never a misleading `0`.

## Completion (AC-DASH-COMPLETION)

- **AC-DASH-COMPLETION-01**: A fixed activity is completion-eligible only when
  `!endTime.isAfter(currentTime)` (i.e. `endTime <= currentTime`), using the injected `now`
  captured once at parse time.
- **AC-DASH-COMPLETION-02**: A flexible activity is completion-eligible only when
  `!latestEnd.isAfter(currentTime)`.
- **AC-DASH-COMPLETION-03**: Future and currently-in-progress activities are excluded from the
  completed count, the incomplete count, and the eligibility denominator entirely - not counted as
  incomplete.
- **AC-DASH-COMPLETION-04**: `eligibleCount = completedEligibleCount + incompleteEligibleCount`.
- **AC-DASH-COMPLETION-05**: If `eligibleCount == 0` but the period has at least one activity,
  show "Completion: No activities are due yet." (no percentage, no division).
- **AC-DASH-COMPLETION-06**: If the period has zero activities at all (not just zero eligible),
  show the distinct message "No activities found for the selected period." - this replaces the
  entire dashboard body, not just the completion line.
- **AC-DASH-COMPLETION-07**: An activity whose eligibility boundary equals `now` exactly
  (`endTime == currentTime` or `latestEnd == currentTime`) is included as eligible (boundary is
  inclusive, per `!isAfter`).

## Completion percentage (AC-DASH-PERCENT)

- **AC-DASH-PERCENT-01**: `percentage = round(completedEligible * 100 / eligibleCount)`, half-up
  to the nearest whole percent, implemented in exactly one place.
- **AC-DASH-PERCENT-02**: `0/10 -> 0%`, `6/10 -> 60%`, `10/10 -> 100%`, `2/3 -> 67%` exactly.

## Detail-mode metrics (AC-DASH-DETAIL)

- **AC-DASH-DETAIL-01**: Detail mode additionally shows fixed activity count and flexible
  activity count among included activities.
- **AC-DASH-DETAIL-02**: Category-grouped counts are shown in the fixed `ActivityCategory` enum
  declaration order (`ACADEMIC, CCA, WORK_INTERNSHIP, OTHERS`), never `HashMap` iteration order.
- **AC-DASH-DETAIL-03**: Energy and sensory rating distributions are always shown in ascending
  rating order `1, 2, 3, 4, 5`, including a `0` count for a rating with no activities.
- **AC-DASH-DETAIL-04**: Distribution bar fill = `round(count * 10 / maxCountInThatDistribution)`
  cells out of a fixed 10-cell bar; if every count in a distribution is `0`, every bar shows all
  10 cells as `-`.

## ASCII output (AC-DASH-OUTPUT)

- **AC-DASH-OUTPUT-01**: Output uses only `#` and `-` for bars, a fixed 10-cell width, no colour,
  no Unicode block characters, no terminal-width detection.
- **AC-DASH-OUTPUT-02**: Every bar is shown beside its exact numeric value(s) - never a bar alone.
- **AC-DASH-OUTPUT-03**: No percentage bar is shown for total energy or total sensory load (there
  is no natural denominator for a point-total), only for completion.
- **AC-DASH-OUTPUT-04**: Output is fully deterministic - the same input state and the same
  injected `now` always produce byte-identical output.

## No-mutation guarantee (AC-DASH-NOMUTATE)

- **AC-DASH-NOMUTATE-01**: Executing any `dashboard` command (valid or invalid) never adds,
  edits, deletes, marks, or unmarks an activity; never changes topic data, the saved default
  order, or any stable ID; never writes any file; never shows a confirmation prompt.
- **AC-DASH-NOMUTATE-02**: `dashboard` is not in `ApplicationRunner.mutatesState`'s recognised
  mutating-command set, so it never triggers a snapshot or a save.
