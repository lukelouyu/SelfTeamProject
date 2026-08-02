# v2.0 Feature: Deterministic Schedule Recommendation (`recommend`)

Branch: `feature/v2-recommend`. Base: `main` @ `efd30bc`.

## Purpose

Generate one deterministic recommendation preview for incomplete flexible activities using the
existing activity model, global planning preferences, and already merged timetable/dashboard
foundations. Preview is read-only; adoption is explicit, atomic, and persisted through the
existing rollback path.

## Command syntax

```text
recommend
recommend this week
recommend date/YYYY-MM-DD
recommend view
recommend adopt
recommend cancel
```

- Bare `recommend` is an alias for `recommend this week`.
- `recommend this week` resolves from the single injected `now` value already used by command
  dispatch.
- `recommend date/YYYY-MM-DD` generates a one-day recommendation for that date only.
- `recommend view` re-displays the current in-memory proposal without recomputing it.
- `recommend adopt` confirms and commits the current proposal once.
- `recommend cancel` discards the current proposal without touching persisted state.

## Supported behaviour

- Only incomplete flexible activities in the requested period are eligible.
- Fixed activities remain unchanged and remain the authoritative timed commitments.
- Each recommended placement stays within the flexible activity's original date, window, and
  required duration.
- Minimum buffer from the global preference profile is enforced as a hard constraint between
  sequential scheduled commitments.
- Preferred daily start/end are used as deterministic tie-break guidance, not as a separate
  stored override or a second activity constraint system.
- Preview stores proposal state only in memory for the current application run.
- Adoption preserves permanent IDs and the original flexible window/duration while recording one
  adopted scheduled placement for restart persistence and downstream timetable/dashboard views.
- Restart with no adoption loses the proposal. Restart after adoption preserves the adopted
  schedule.

## Tomato boundary

Tomato remains advisory-only. When the global preference profile is `tomato/on`, recommendation
output may show a short Pomodoro-style study suggestion for suitable long flexible study
activities. Tomato does not alter ranking, slot generation, duration, storage transaction shape,
buffer rules, dashboard calculations, or timetable ordering.

## Explicit scope reduction

- Activities still do not store facility/location bindings, so route-aware recommendation,
  travel-time feasibility, inaccessible-edge rejection, and sheltered-travel tie-breaks are
  deferred rather than half-implemented.

## Non-goals

- No automatic activity completion changes.
- No conversion of flexible activities into fixed activities.
- No generated break activities or split-study blocks.
- No new saved recommendation-history file.
- No route/facility inference from free-text notes, topics, or activity names.
