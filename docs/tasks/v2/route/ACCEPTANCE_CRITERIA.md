# Acceptance Criteria: `route`

## Grammar

```text
route from/FACILITY to/FACILITY
```

- AC1. `from/` and `to/` are both required; each may appear in any order relative to the other.
- AC2. A missing `from/` or `to/` value (absent marker, or blank value) is rejected with a
  `MissingInputException` naming which one is missing.
- AC3. Text before the first recognised marker is rejected as unrecognised leading text
  (`InvalidCommandException`), matching `connection find`'s convention.
- AC4. Facility names are matched case-insensitively against the loaded facility dataset.

## Routing behaviour

- AC5. The route uses Dijkstra's algorithm with each connection's stored `distanceInMetres` as the
  edge weight.
- AC6. Only connections with `accessibility == YES` are eligible edges. `NO` and `UNKNOWN` are
  excluded entirely — never selected even as a last resort.
- AC7. Among ties (equal total distance via different paths), the result is deterministic — same
  input always produces the same chain (inherited from `AccessibilityGraph`'s lazy-deletion
  Dijkstra plus stable adjacency-list iteration order).
- AC8. A shorter multi-edge path beats a longer direct edge, and vice versa (proves true Dijkstra
  behaviour, not hop-count/BFS).
- AC9. `from` and `to` naming the same known facility succeeds: single-facility chain, `0 m` total
  distance, zero segments, output states no travel is required. **Not** an error.
- AC10. `from` or `to` naming a facility not in the loaded dataset is rejected with an
  `InvalidIndexException` ("Not found") naming the unrecognised facility — checked before pathing.
- AC11. `from`/`to` both known but no YES-only path connects them: output begins exactly with
  `No supported accessible route was found...`, includes `Please ask people around you.` and
  `We are still improving our database.`, and does not claim no real-world route exists.

## Output content (successful route)

- AC12. Shows the normalized (canonical-case) source and destination names.
- AC13. Shows the ordered facility chain, source to destination inclusive.
- AC14. Shows each segment's own distance in metres, in the direction actually travelled (not
  necessarily the stored connection's own `from`/`to` order, since every connection is two-way).
- AC15. Shows each segment's traversal type (`RAMP`/`SHELTERED_RAMP`/`LIFT`/`PATH`/`OTHER`).
- AC16. Shows each segment's known barrier and notes when recorded; omitted when absent (matches
  `connection view`'s "omit when unset" convention).
- AC17. Shows each segment's shelter status (`YES`/`NO`/`UNKNOWN`).
- AC18. Shows the total distance in metres.
- AC19. Does **not** show a travel-time estimate.
- AC20. Does **not** claim real-time verification, a guarantee of accessibility, or current
  usability — carries the same `AccessibilityDisclaimer.TEXT` every facility/connection command
  already ends with.

## Guide / discoverability

- AC21. `guide route` shows real syntax, purpose, constraints, 2–4 examples, related commands, and
  the accessibility disclaimer — no "Coming soon" text remains for `route`.
- AC22. `guide 11` and `guide route` produce identical text (numbered-item/keyword agreement,
  matching the existing pattern for items 7/8).
- AC23. `guide 12` (and bare `12` typed as a command) now performs the "Return" action previously
  at 11; bare `12` is recognised as a menu-number shortcut by `CommandDispatcher`, matching bare
  `1`–`11` today.
- AC24. Items 1–10 are textually unchanged. `timetable`'s guide topic is unchanged (still
  keyword-only, still "Coming soon").
- AC25. The main menu text lists exactly 12 items, "Enter a number from 1 to 12."

## Non-functional / compatibility

- AC26. No v1.0 command's syntax, output wording, or storage format changes.
- AC27. `facility`/`connection` records remain read-only; `route` adds, edits, or deletes nothing.
- AC28. Malformed/duplicate/negative-distance connection or facility lines already rejected at
  `Storage` load time never reach the route graph — proven, not merely assumed.
