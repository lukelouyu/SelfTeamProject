# Timetable Acceptance Criteria

## Grammar and period resolution

1. `timetable week/YYYY-MM-DD` renders the Monday-Sunday week containing the supplied date.
2. `timetable this week` uses one injected `now` value and the same Monday-Sunday definition as
   `list this week` and `dashboard this week`.
3. `timetable day/YYYY-MM-DD` renders exactly the supplied calendar day.
4. Weekly selectors accept either no mode, `compact`, or `detail`; day accepts either no mode or
   `detail`.
5. Selectors and mode keywords are case-insensitive; date values retain strict `yyyy-MM-dd` syntax.
6. Missing arguments, missing marker values, invalid calendar dates, unknown selectors, duplicate
   modes, and unexpected trailing text are rejected without touching application state.

## Ordering and inclusion

7. Fixed activities are included when their date lies inside the selected period.
8. Fixed activities are sorted by date, start time, then permanent activity ID.
9. Identical start times never cause an activity to be omitted; permanent ID is the final
   deterministic tie-breaker.
10. Normal weekly output groups all seven days Monday through Sunday, including empty days.
11. Day output contains only the selected day.
12. Empty periods produce helpful deterministic output rather than an exception.

## Fixed and flexible presentation

13. Fixed activities show `[F]`, permanent ID, exact start/end times, and description.
14. Flexible activities are never placed into fixed timetable slots. They appear in a separate
    `UNSCHEDULED FLEXIBLE ACTIVITIES` section with `[U]`, permanent ID, date, allowed window, and
    required duration.
15. Detail mode adds completion status, category, topic when present, energy, sensory, and note
    without changing ordering or inclusion.
16. Compact mode omits empty-day placeholders and nonessential explanatory lines while retaining
    every fixed and unscheduled flexible activity.
17. No `[R]` or `[B]` entry is fabricated before later features create approved recommendation or
    buffer data.

## Overlaps, safety, and compatibility

18. Every fixed activity participating in a same-day half-open interval overlap is marked
    `[OVERLAP]`, and the view contains an explicit warning.
19. Adjacent intervals are not overlaps.
20. Cross-midnight records are not introduced; the existing same-day activity invariant remains
    unchanged.
21. Executing any timetable command performs zero in-memory mutation and zero storage writes.
22. Existing commands, output contracts, permanent IDs, and storage formats remain unchanged.
23. Timetable output is deterministic, colour-independent, ASCII-safe, and uses no terminal
    control sequences.
24. `guide timetable` documents only shipped Timetable behavior. The numbered menu adds Timetable
    at item 12 and moves only Return from 12 to 13.
