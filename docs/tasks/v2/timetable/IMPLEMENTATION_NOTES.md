# Timetable Implementation Notes

## Responsibilities

- `TimetableCommandParser`: validates the one-shot grammar and resolves the requested period.
- `TimetableCommand`: coordinates a read-only calculation and formatting pass.
- `TimetableService`: selects, copies into immutable display entries, orders, and marks overlaps.
- `TimetablePeriod`: immutable day/week boundary and label.
- `TimetableEntry`: immutable display projection of one fixed or flexible activity.
- `TimetableView`: immutable calculated result containing fixed and unscheduled entries.
- `TimetableFormatter`: owns normal, detail, and compact plain-text presentation.

## Approved decisions

1. Use strict `yyyy-MM-dd`; the historical `dd-MM-yyyy` example is superseded by the repository's
   established convention.
2. Use permanent numeric IDs directly rather than introducing historical `A1` aliases.
3. Use modifiers on one-shot commands rather than stateful `compact`/`details` commands.
4. Provide explicit `compact` fallback rather than nondeterministic terminal-width detection.
5. Keep flexible activities explicitly unscheduled. Recommendation adoption is a later feature.
6. Add Timetable as guide item 12 and move only Return to 13.

## Overlap handling

The service compares fixed intervals only when their dates match. Intervals use half-open
semantics: `[start, end)`, so `09:00-10:00` and `10:00-11:00` are adjacent, not overlapping. Both
members of every overlap pair are marked. Detection is independent of list ordering and does not
discard duplicates or identical starts.

Normal `ActivityManager` and storage validation already reject overlaps. The pure service accepts
an activity list so synthetic tests can prove defensive rendering without weakening those existing
rules.

## Rejected alternatives

- A wide hour-cell grid was rejected for this branch because arbitrary-minute activities,
  overlapping entries, and long descriptions would either be truncated or require terminal-width
  assumptions. Day-grouped chronological sections preserve every record and remain readable.
- Automatic terminal-width detection was rejected because Java 17 exposes no reliable portable
  terminal width API and environment-variable fallbacks are nondeterministic.
- Reusing mutable `Activity` objects in the calculated view was rejected. Immutable projections
  prevent accidental mutation and give the later recommender a stable display boundary.
- A new persistence file was rejected because Timetable is a derived read-only view.

## Future extension boundary

A later recommender may extend the display-entry kind with adopted `[R]` placements and explicit
buffer records after their persistence semantics are approved. Timetable does not pre-implement
or infer either concept.
