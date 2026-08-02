# v2.0 Feature: Accessible Planning Dashboard (`dashboard`)

Branch: `feature/v2-dashboard`. Base: `main` @ `ce71765`.

## Purpose

Give a user a single-command summary of their planning load for a period (today, tomorrow, a
specific date, or the current week): how much is planned, how much room is left, how demanding it
is (energy/sensory), and how much of what's already due has been completed. This is v2.0's second
feature, following `route`.

## User benefit

Today, seeing "how full is my day/week" requires manually running `list` and adding things up by
eye. `dashboard` answers that in one command, using only data the user already entered (ratings,
durations, completion) — no new data entry, no inference beyond what's stored.

## Command syntax

```text
dashboard today [detail]
dashboard tomorrow [detail]
dashboard date/YYYY-MM-DD [detail]
dashboard this week [detail]
```

`detail` is a single optional trailing keyword. See `ACCEPTANCE_CRITERIA.md` for the full
accept/reject list.

## Supported periods

| Selector | Meaning |
|---|---|
| `today` | The calendar day containing the injected current time: `[00:00 today, 00:00 tomorrow)`. |
| `tomorrow` | The calendar day after today: `[00:00 tomorrow, 00:00 day-after-tomorrow)`. |
| `date/YYYY-MM-DD` | That specific calendar day, same half-open shape. |
| `this week` | Reuses `list this week`'s existing Monday-Sunday definition: `[Monday 00:00, following Monday 00:00)`. **Not** a rolling 7-day window from today - the two planning documents in `docs/planning/` disagreed with each other and with `list`'s own shipped behaviour; this branch follows `list`'s precedent, per explicit instruction. |

## Default output

Period header, total activity count, planned workload time, nominal buffer (or overload) time,
total energy demand + high-energy count, total sensory load + high-sensory count, and completion
as a secondary metric (bar + fraction + percentage, or "No activities are due yet." if nothing in
the period has reached its end time yet, or "No activities found for the selected period." if the
period is empty).

## Detail output

Adds: fixed vs flexible activity counts, category-grouped counts (deterministic order, the
existing `ActivityCategory` enum order), and per-rating (energy and sensory) breakdowns: a 1-5
distribution with ASCII bars, average (one decimal place, half-up), and highest rating.

## Non-goals

- No new persistence file. Dashboard is a read-only derived view computed fresh every time from
  `ActivityManager`'s in-memory state - it never mutates activities, topics, settings, or any
  file, and needs no confirmation.
- No recommendation, scheduling, or feasibility claim. "Nominal buffer" is arithmetic capacity
  minus planned workload, not a promise the remaining time is actually usable (overlapping fixed
  activities are counted individually toward workload, not merged; travel time and route
  accessibility are not considered).
- No medical or performance judgement. Energy/sensory numbers are shown as-is, labelled as
  self-reported planning data.
- Does not require, imply, or introduce activities that span midnight - see
  `IMPLEMENTATION_NOTES.md`'s cross-midnight section for exactly why and what's tested instead.

## Dependencies on existing components

`ActivityManager.getAll()` (read-only iteration; no new query method needed - dashboard's
inclusion rule differs from every existing `ActivityFilter` predicate, so it filters the full list
itself rather than forcing a new `ActivityFilter` shape), `FixedActivity`/`FlexibleActivity`,
`EnergyRating`/`SensoryRating`, `CompletionStatus`, `ActivityCategory`, and the existing
`now: LocalDateTime`-threading convention already used by `list today`/`tomorrow`/`this week` and
`next` (`CommandDispatcher.dispatch(input, now)` -> parser captures `now` once -> stored on the
command -> used at `execute()`, never re-read from the system clock).

## Known limitations (restated in the final branch report)

- Nominal buffer is not guaranteed free time.
- Overlapping fixed activities are counted individually toward workload (intentional, not a bug).
- Flexible activities contribute their requested `durationMinutes` in full once included; this is
  not clipped to however much of their window overlaps the period, and does not prove the
  activity is actually schedulable inside that portion.
- Travel time and route accessibility are not considered.
- No genuine cross-midnight activity can exist in this codebase's data model (see
  `IMPLEMENTATION_NOTES.md`); the clipping calculation is nonetheless implemented generically and
  correctly, verified at the calculation level with synthetic interval boundaries.
- Timetable, preferences, recommend, and export remain unimplemented v2.0 backlog.
