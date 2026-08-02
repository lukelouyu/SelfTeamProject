# v2.0 Feature: Accessible Route Search (`route`)

Branch: `feature/v2-route`. Base: `main` @ `59a9c4a`.

## Purpose

Let a user ask for the shortest **confirmed-accessible** path between two known facilities in the
local, read-only facility/connection reference dataset, using distance in metres as the Dijkstra
weight. This is the first v2.0 feature and the first real caller of the `logic.graph`
Dijkstra-prep infrastructure built during v1.0 hardening.

## Command

```text
route from/FACILITY to/FACILITY
```

- `from/` and `to/` may appear in either order (matches `connection find`'s marker parsing).
- Facility names are matched case-insensitively; output always shows the canonical stored name.
- Both markers are required; either missing is a `MissingInputException`.
- Unrecognised leading text is rejected the same way every other marker-based command rejects it.

## Authoritative specification

Per the user's 2026-08-02 explicit approval message (this conversation), the v2.0 master prompt
(`UNIENABLE_V2_CLAUDE_CODEX_MASTER_PROMPT_UPDATED.md`) is authoritative; `docs/planning/` is
supplementary background only where the master prompt is silent. The same message additionally
supplied four binding corrections/decisions to the master prompt's original route section, listed
below — these override the master prompt's own route section wherever they conflict with it.

## Approved design decisions (this conversation, 2026-08-02)

1. **YES-only accessibility filtering lives outside `AccessibilityGraph`.** A new
   `logic/route/AccessibleRouteGraphFactory` reads from the existing `FacilityManager`/
   `ConnectionManager`, filters connections to `AccessibilityStatus.YES` only, and builds the
   existing `AccessibilityGraph` from the filtered list — without constructing a temporary
   `ConnectionManager` purely to perform the filtering. `AccessibilityGraph` itself is not
   modified to encode route policy; it gains only a policy-neutral constructor overload
   (`List<Facility>, List<Connection>`) that the existing `FacilityManager`/`ConnectionManager`
   constructor now delegates to, so the factory can pass a filtered connection list directly. See
   `IMPLEMENTATION_NOTES.md` for why this shape was chosen over the alternatives.
2. **Guide numbering:** "Route Search" becomes numbered menu item **11** (the next available
   number after item 10, "Data files and storage"); item 11 "Return" becomes item **12**. Items
   1–10 keep their existing meanings unchanged. `timetable` stays keyword-only (no number) until
   the timetable feature ships. Only the `route` guide topic's "Coming soon" text is replaced;
   `timetable`/`recommend`/`export` keep theirs.
3. **`route from/X to/X` (same source and destination) is a successful zero-length route**, not an
   error: single-facility chain, `0 m` total distance, explicitly states no travel is required.
   This overrides this session's own earlier (pre-approval) proposal to treat it as an error.
4. **No-route fallback wording** for a disconnected-but-known pair begins exactly with
   `No supported accessible route was found...`, keeps the two guidance lines `Please ask people
   around you.` / `We are still improving our database.`, and must not imply no real-world route
   exists — only that UniEnable's local dataset has none confirmed.

## Package layout

```text
command/accessibility/route/RouteCommand.java
parser/accessibility/RouteCommandParser.java      (flat, like FacilityCommandParser/ConnectionCommandParser)
logic/route/AccessibleRouteGraphFactory.java
ui/accessibility/RouteFormatter.java
```

No `model/route` package: `logic.graph.GraphPath` plus the existing `Connection`/`Facility`
classes are sufficient to build the display; no new persistent or domain data is introduced.
`route` does not add, edit, or delete facility/connection records.

## Out of scope for this branch

Timetable, dashboard, preferences, recommend, export — untouched. No change to any v1.0 command's
syntax, output, or storage format.
