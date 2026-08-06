# Relative Date-Selector Support Matrix

Date: 2026-08-06
Scope: every command inspected for `today`/`tomorrow`/`this week`/`next week`/`date/YYYY-MM-DD`
support, per the master prompt's Defect A.

## 1. Shared semantics

All five commands below that support any of these selectors agree on:

```text
today             now.toLocalDate()
tomorrow          now.toLocalDate() + 1 day
this week         Monday 00:00 through Sunday 23:59 of the week containing today
next week         Monday 00:00 through Sunday 23:59 of the week immediately after this week
date/YYYY-MM-DD   the exact specified calendar date
```

`today`/`tomorrow` and the Monday-of-week math are centralised in
[`logic.RelativeDateResolver`](../../src/main/java/seedu/unienable/logic/RelativeDateResolver.java);
every command below resolves through it (directly, or via `DashboardService`/`TimetableService`/
`RecommendationService`, which themselves call it). No command recomputes
`TemporalAdjusters.previousOrSame(MONDAY)` independently any more.

## 2. Matrix

| Command | `today` | `tomorrow` | `this week` | `next week` | `date/YYYY-MM-DD` | Notes |
|---|---|---|---|---|---|---|
| `list` | Yes | Yes | Yes | Yes | Yes | Reference implementation; also has `overdue` (not a date selector - a completion+time condition). |
| `find` | Yes | Yes | Yes | Yes | Yes | Added in this pass (previously had `date/` only). Same leading-phrase grammar as `list`, minus `overdue` (find has no status filter to conflict with). |
| `dashboard` | Yes | Yes | Yes | Yes | Yes | `next week` added in this pass. |
| `timetable` | Yes | Yes | Yes | Yes | Via `day/`/`week/` markers | `today`/`tomorrow`/`next week` added in this pass. `day/`/`week/` markers (pre-existing) accept *any* date, not just today/tomorrow - kept as-is; `today`/`tomorrow` are documented shorthand for `day/<date>`. |
| `recommend` | Yes | Yes | Yes | Yes | Yes (must not be before today) | `today`/`tomorrow`/`next week` added in this pass. `today`/`tomorrow` are shorthand for `date/<date>`, not new service methods. |
| `recur` | N/A | N/A | N/A | N/A | N/A | Operates on **academic teaching weeks** (`week/1`, `week 1 to 13`), a fundamentally different selector concept tied to `data/academic-calendar.txt`, not calendar weeks. Not part of this vocabulary; see the recur-specific work in the final report instead. |
| `view` | N/A | N/A | N/A | N/A | N/A | Views one activity by stable ID (`view ID`). No date range to select - a date selector would have nothing to filter. |
| `next` | N/A | N/A | N/A | N/A | N/A | Always shows the single next relevant activity relative to `now`; that *is* its date semantics; a selector would be redundant with the command's own purpose. |
| `order` | N/A | N/A | N/A | N/A | N/A | Sets/reads the saved default *sort order* for `list`/`find` results (`order set input\|time\|chronological`). Has no date-range concept at all - nothing to select. |

## 3. Why `view`/`next`/`order`/`recur` were left out

The master prompt explicitly warns: "Do not add date parameters to commands for which they are not
meaningful." Each of these four was checked against that bar specifically:

- **`view`** takes a mandatory stable ID and shows exactly one activity's full record. There is no
  set of activities to narrow down, so `today`/`this week` etc. have nothing to act on.
- **`next`** already has an implicit, stronger relationship with `now` than any selector could add:
  it deterministically picks the single next relevant activity and a same-run overdue count,
  driven entirely by the injected `now`. Adding `next today` or `next this week` would just be a
  confusing way to ask a question `next` doesn't answer (a *list* of matches), which `list`/`find`
  already answer.
- **`order`** is a persistent display-ordering preference (`order set`), not a query. It has no
  results of its own to filter by date.
- **`recur`** already has a first-class selector vocabulary of its own - teaching week numbers
  resolved against an external, per-semester calendar file - which is a different concept from a
  calendar-relative `today`/`this week`. Overloading `recur` to additionally accept calendar-week
  selectors would conflate two incompatible ideas of "week" in the same command grammar, which is
  the kind of confusion Defect A is trying to eliminate, not add.

## 4. Deliberate scope boundary: `timetable day/`/`week/` markers unchanged

`timetable`'s existing `day/YYYY-MM-DD` and `week/YYYY-MM-DD` markers already accept an arbitrary
date, a strictly larger capability than `today`/`tomorrow`/`this week`/`next week` (which only ever
resolve to specific relative dates). `today`/`tomorrow`/`next week` were added as additional
keyword selectors *alongside* the markers - exactly mirroring how `dashboard` and `recommend` offer
both `date/` (arbitrary date) and the relative keywords side by side - rather than replacing or
folding the markers into the keyword grammar.

## 5. Text-UI and JUnit coverage added

See the final report's "Tests added" section for the exact test classes. In summary: parser-level
tests for each newly-accepted selector (success, case-insensitivity, combination-rejection,
trailing-text-rejection) were added to `DashboardCommandParserTest`, `TimetableCommandParserTest`,
`RecommendCommandParserTest`, and `FindCommandParserTest`; service-level tests for the new
`resolveNextWeek`/`recommendNextWeek` methods were added to `DashboardServiceTest`,
`TimetableServiceTest`, and `RecommendationServiceTest`; and `RelativeDateResolverTest` covers the
shared resolution math directly, including month/year/week boundary cases. The Text-UI suite's one
now-stale "today is rejected" timetable regression case was replaced with an equivalent
still-deterministic case (`timetable yesterday`) plus a new deterministic trailing-text rejection
case (`timetable today extra`), since a bare `timetable today` success case would make the
transcript depend on the real wall-clock date.
