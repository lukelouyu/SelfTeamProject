# Implementation Notes: `route`

## Why the YES-only filter lives in `logic.route.AccessibleRouteGraphFactory`, not in `AccessibilityGraph`

`AccessibilityGraph` was built during v1.0 hardening explicitly as **generic** Dijkstra-prep
infrastructure over *any* facility/connection dataset, with no accessibility-status opinion baked
in (see its class Javadoc). Baking "only `YES` connections are usable" into it would collapse a
reusable graph algorithm and a route-specific business rule into one class, and would silently
change the meaning of `AccessibilityGraph` for any future caller that might legitimately want an
unfiltered graph (e.g. a future admin/debug view over the full connection set).

**Alternatives considered:**

- **Modify `AccessibilityGraph`'s constructor to filter by status internally.** Rejected: couples a
  general-purpose class to one command's policy, and would silently change
  `AccessibilityGraphTest`'s existing "every facility reaches every other facility" assertion
  against the real bundled dataset (which does include the one `NO` connection at `AS1-AS2`) for
  no reason connected to that test's own purpose.
- **Build a temporary `ConnectionManager` from the filtered list, then call the existing
  `AccessibilityGraph(FacilityManager, ConnectionManager)` constructor.** Rejected per explicit
  instruction: `ConnectionManager` exists to own a *loaded, trusted* dataset; manufacturing one
  solely to smuggle a filtered list through an existing constructor signature is a workaround, not
  a design.
- **Chosen: add a policy-neutral `AccessibilityGraph(List<Facility>, List<Connection>)`
  constructor**, with the existing `AccessibilityGraph(FacilityManager, ConnectionManager)`
  constructor delegating to it (`this(facilityManager.list(), connectionManager.list())`). This is
  the smallest possible change to `AccessibilityGraph` — a constructor overload, not new behaviour
  — and every existing `AccessibilityGraphTest` case is unaffected since the manager-based
  constructor's behaviour is unchanged. `AccessibleRouteGraphFactory.build(FacilityManager,
  ConnectionManager)` then filters `connectionManager.list()` to `YES`-only and calls the new
  list-based constructor directly, with no `ConnectionManager` involved in the filtered path at
  all.

## Why malformed/duplicate/negative-distance data isn't re-validated inside route code

`ConnectionStorage`/`FacilityStorage` already reject non-positive/duplicate IDs, non-positive
distances, blank/self-referencing endpoints, and unknown-facility endpoints at load time (v1.0
RC01-RC06 hardening), and `ApplicationRunner` only ever constructs `FacilityManager`/
`ConnectionManager` from an already-validated `LoadResult.getRecords()`. By the time any command
(including `route`) runs, the managers can only contain trusted, self-consistent records. Adding a
second validation pass inside `AccessibleRouteGraphFactory` would duplicate an invariant the
storage layer already guarantees, contradicting the project's standing "don't validate what can't
happen" rule. `AccessibleRouteGraphFactoryTest` proves this pipeline end-to-end (a synthetic
malformed file loaded through the real `ConnectionStorage`, then fed to the factory) rather than
re-implementing the check.

## Segment reconstruction (path nodes -> displayed connection details)

`GraphPath` carries only the ordered facility-name chain and total distance (it is a generic
shortest-path result type, reused as-is). `route` needs each segment's own distance, traversal
type, shelter, barrier, and notes for display. `RouteCommand` resolves each consecutive
`(pathNode[i], pathNode[i+1])` pair against the same YES-only connection list the factory built the
graph from (obtained via `AccessibleRouteGraphFactory.filterConfirmedAccessible`, so the two never
disagree about which connections are eligible), matching either stored direction (`from`/`to` are
symmetric — every connection is two-way). This stays in `RouteCommand` rather than becoming a
separate class: it is orchestration glue directly tied to what one command needs to hand its
formatter, not a reusable policy or algorithm in its own right.

**Displayed segment direction always follows actual travel direction**
(`pathNode[i] -> pathNode[i+1]`), not the connection record's own stored `from`/`to` order, since a
connection stored as `B|A` is still validly travelled `A -> B`.

## Guide numbering ripple

Adding item 11 ("Route Search") and shifting "Return" from 11 to 12 touches:
`GuideCommand.MENU_NUMBER_TOPICS`, `GuideCommand.MAIN_MENU` text, the `topic.equals("11")` special
case (becomes `"12"`), and `CommandDispatcher`'s bare-number `switch` (adds `case "12"`). Bare `11`
now shows the route topic instead of returning; bare `12` is the new return shortcut. Confirmed
via `GuideCommandTest` updates and a `text-ui-test` re-run (Section G of `TEST_PLAN.md`).

## Bundled dataset detail used by `text-ui-test` (real data, not synthetic)

`src/main/resources/connections.txt` has 9 facilities and 10 connections; every one of them has
`accessibility == YES` (connection 7, `AS1-AS2`, has `shelter == NO` — its *shelter* status, not
its accessibility - a field-position mix-up caught only once during implementation, when
`route from/AS1 to/AS2` returned a normal 130 m route instead of the no-route fallback an earlier
draft of this note incorrectly predicted). Consequently the bundled dataset's YES-only route graph
is identical in connectivity to the unfiltered graph `AccessibilityGraphTest`'s
`bundledDataset_everyFacilityReachesEveryOtherFacility` already proves is fully connected - there
is no naturally-occurring disconnected pair to exercise the no-route fallback against the real jar.
That fallback is covered exhaustively instead by synthetic fixtures in
`AccessibleRouteGraphFactoryTest`, `RouteCommandTest`, and `RouteFormatterTest` (see
`TEST_PLAN.md`), consistent with the approved constraint that algorithmic edge cases live in
synthetic unit/integration tests, not `text-ui-test`. `text-ui-test`'s real-dataset route scenarios
instead prove: a direct edge (`AS8-CLB`, 55 m), a real 4-edge Dijkstra result
(`CLB-AS6-AS1-AS4-AS7` = 270 m), case-insensitive input, the zero-length same-facility case, a
route whose segment carries both a barrier and shelter-`NO` notes (`AS1-AS2`), an unknown
facility, and three malformed-syntax shapes.
