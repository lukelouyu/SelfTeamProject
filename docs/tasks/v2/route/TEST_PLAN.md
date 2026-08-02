# Test Plan: `route`

All new unit/integration tests use synthetic, in-memory or temp-file fixtures — none depend on the
real bundled NUS FASS dataset. The bundled dataset (9 facilities, 10 connections, all
`accessibility == YES` - see `IMPLEMENTATION_NOTES.md`) is used only in `text-ui-test`, which
already runs against the real jar and bundled data for every other facility/connection scenario.

Traceability: AC# refers to `ACCEPTANCE_CRITERIA.md`.

## A. `logic.route.AccessibleRouteGraphFactoryTest` (synthetic fixtures)

| AC | Test method | Expected outcome | Status |
|---|---|---|---|
| AC6 | `build_excludesNoAndUnknownConnections_onlyYesEdgesUsable` | A path that exists only via a `NO` or `UNKNOWN` edge is unreachable through the built graph | Implemented, passing |
| AC5, AC8 | `build_shorterMultiEdgePathBeatsLongerDirectEdge` | Multi-hop YES path wins over a longer direct YES edge | Implemented, passing |
| AC8 | `build_fewerEdgesButLongerDistance_losesToMoreEdgesShorterDistance` | Proves Dijkstra (distance-optimal), not BFS (hop-optimal) | Implemented, passing |
| AC7 | `build_equalDistanceAlternatePaths_deterministicAcrossRepeatedCalls` | Same synthetic graph queried twice returns the identical chain | Implemented, passing |
| AC28 | `build_malformedNegativeDistanceLines_areSkippedNotCrashing` | A synthetic `connections.txt` with a negative distance and a malformed line, loaded through the real `ConnectionStorage`, yields warnings for each and a graph built from the survivors works correctly | Implemented, passing |
| AC28 | `build_duplicateConnectionBetweenSamePair_secondSkippedAtLoadNotGraph` | Two `CONNECTION` lines with the same ID between the same pair — second skipped at storage load, graph construction never sees a duplicate | Implemented, passing |
| — | `build_isolatedSingleFacilityNoConnections_buildsWithoutError` | Zero-connection dataset builds an isolated-node graph without throwing | Implemented, passing |

## B. `command.accessibility.route.RouteCommandTest` (synthetic managers)

| AC | Test method | Expected outcome | Status |
|---|---|---|---|
| AC5, AC12–15,17,18 | `execute_directRoute_showsSingleSegmentWithFullDetail` | One-segment route shows type/shelter/distance/total | Implemented, passing |
| AC8, AC13,14 | `execute_multiEdgeRoute_showsEveryOrderedSegment` | Multi-hop route lists every ordered segment with its own distance | Implemented, passing |
| AC14 | `execute_segmentDirection_matchesTravelDirectionNotStoredConnectionOrder` | A connection stored as `B|A` but travelled `A->B` displays `A -> B` | Implemented, passing |
| AC16 | `execute_multiEdgeRoute_showsEveryOrderedSegment` (barrier+notes case) / `execute_segmentWithoutBarrierOrNotes_omitsBoth` | Optional fields shown only when present | Implemented, passing |
| AC9 | `execute_sameKnownFacility_returnsZeroLengthSuccess` | Single-facility chain, `0 m`, 0 segments, explicit "no travel required" text — not an exception | Implemented, passing |
| AC10 | `execute_unknownSourceFacility_throwsInvalidIndexException` / `execute_unknownDestinationFacility_throwsInvalidIndexException` | Each throws with the unrecognised name in the message | Implemented, passing |
| AC11 | `execute_disconnectedKnownFacilities_showsNoSupportedRouteFallback` | Message starts with the exact required sentence and includes both guidance lines, no real-world-nonexistence claim | Implemented, passing |
| AC6 | `execute_onlyPathUsesNoStatusEdge_treatedAsDisconnected` | A pair connected only by a `NO` edge falls back to AC11's message | Implemented, passing |
| AC4 | `execute_caseInsensitiveFacilityNames_resolvesAndNormalizesOutput` | Lowercase input, canonical-case output | Implemented, passing |
| AC19,20 | `execute_output_neverContainsTimeEstimateOrGuaranteeLanguage` | Negative assertion on forbidden phrases; disclaimer present | Implemented, passing |

## C. `parser.accessibility.RouteCommandParserTest`

| AC | Test method | Expected outcome | Status |
|---|---|---|---|
| AC1 | `parse_markersInReverseOrder_stillParsesCorrectly` | `to/` before `from/` still parses | Implemented, passing |
| AC2 | `parse_missingFrom_throwsMissingInputException` / `parse_missingTo_throwsMissingInputException` / `parse_blankFromValue_throwsMissingInputException` | Each names the missing field | Implemented, passing |
| AC3 | `parse_unrecognisedLeadingText_throwsInvalidCommandException` | e.g. `route bogus from/AS6 to/AS8` rejected | Implemented, passing |
| — | `parse_noArguments_throwsMissingInputException` | Bare `route` rejected | Implemented, passing |

## D. `parser.CommandDispatcherTest` (addition)

| AC | Test method | Expected outcome | Status |
|---|---|---|---|
| AC1–AC6 (integration) | `dispatch_route_returnsRouteCommandWiredToLiveManagers` | `dispatch("route from/AS6 to/AS8", now)` returns a `RouteCommand` wired to the live facility/connection managers | Implemented, passing |
| AC2 | `dispatch_routeMissingTo_throwsMissingInputException` | Dispatcher-level proof that parser errors propagate correctly | Implemented, passing |
| AC22,23 | `dispatch_bareMenuNumberEleven_returnsGuideCommandForRoute` / `dispatch_bareMenuNumberTwelve_returnsGuideCommandForReturn` / `dispatch_bareThirteen_throwsInvalidCommandException` | Bare-number menu shortcuts follow the new numbering | Implemented, passing |

## E. `ui.accessibility.RouteFormatterTest`

| AC | Test method | Expected outcome | Status |
|---|---|---|---|
| AC12,13,18 | `formatRoute_singleSegment_exactText` | Exact string match for a one-segment result | Implemented, passing |
| AC9 | `formatRoute_zeroLengthSameFacility_exactText` | Exact string match for the same-endpoint success case | Implemented, passing |
| AC11 | `formatNoRoute_exactText` | Exact string match for the fallback, including both guidance lines | Implemented, passing |

## F. `GuideCommandTest` (updates to existing tests, per AC21–25)

| AC | Test method | Expected outcome | Status |
|---|---|---|---|
| AC25 | `execute_nullTopic_showsMainMenu` (updated) | 12-item menu text, "1 to 12" | Implemented, passing |
| AC21 | `execute_routeTopic_isAvailableAndHasRouteExamples` (new, replaces the old `execute_v2OnlyTopic_appendsComingSoonNote` route case) | Real syntax/examples, no "Coming soon" | Implemented, passing |
| AC22 | `execute_menuNumberElevenAgreesWithItsOwnKeyword` (new) / `execute_menuNumberEleven_resolvesToRoute` (new) | `guide 11` == `guide route` | Implemented, passing |
| AC23 | `execute_menuNumberTwelve_returnsWithoutShowingATopic` (renamed from the old `...Eleven...` test) | `guide 12` → "Returning to the command prompt." | Implemented, passing |
| AC24 | `execute_everyNumberedMenuMapping_resolvesToItsAdvertisedTopic` (updated, +11) | Adds the route assertion | Implemented, passing |
| — | `execute_menuNumberOutOfRangeAbove_showsFallbackMessage` (updated to use `13`) | Out-of-range boundary moves with the menu | Implemented, passing |

## G. `text-ui-test` (bundled real dataset — see IMPLEMENTATION_NOTES.md for the exact
   facility/connection graph used)

| Scenario | Expected outcome | Status |
|---|---|---|
| `route from/AS8 to/CLB` | Direct 55 m route, one segment | Implemented, passing |
| `route from/CLB to/AS7` | Multi-hop 270 m route via AS6-AS1-AS4 | Implemented, passing |
| `route from/as6 to/as8` | Case-insensitive input, canonical-case output | Implemented, passing |
| `route from/AS6 to/AS6` | Zero-length success | Implemented, passing |
| `route from/AS1 to/AS2` | Normal accessible route (130 m) whose one segment shows both a barrier and shelter-`NO` notes; the bundled dataset has no `NO`/`UNKNOWN` connection at all (see `IMPLEMENTATION_NOTES.md`), so the no-route fallback is covered only by synthetic tests (Sections A/B/E) | Implemented, passing |
| `route from/AS6 to/NonexistentFacility` | Unknown-facility error | Implemented, passing |
| `route`, `route from/AS6`, `route bogus from/AS6 to/AS8` | Malformed-syntax errors | Implemented, passing |
| `guide route`, `guide 11`, `guide 12`, `guide 13`, bare `11`/`12`/`13` | Updated numbering, no stale "Coming soon" for route | Implemented, passing |

Full v1.0 `text-ui-test` coverage (activities, topics, recur, reset, facility/connection
list/view/find/validate) is preserved unchanged except for the guide-numbering ripple above.
