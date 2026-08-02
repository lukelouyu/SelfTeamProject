# Test Plan: `dashboard`

All tests use an injected fixed `now`/synthetic fixtures - no test reads the real system clock.
Traceability: AC# refers to `ACCEPTANCE_CRITERIA.md`.

## A. `logic.dashboard.DashboardServiceTest`

| AC | Test method | Expected outcome | Status |
|---|---|---|---|
| AC-DASH-PERIOD-01/02 | `resolveToday_resolveTomorrow_matchExpectedHalfOpenBounds` | Each resolver returns the documented `[00:00, next-day 00:00)` bounds | Implemented, passing |
| AC-DASH-PERIOD-03 | `resolveDate_matchesHalfOpenBounds` | Same half-open shape for an exact date | Implemented, passing |
| AC-DASH-PERIOD-04 | `resolveThisWeek_matchesListThisWeekMondayToSundayBoundary` | Same Monday/Sunday bounds as `ListCommandParser`'s own logic | Implemented, passing |
| AC-DASH-PERIOD-05 | `getCapacityMinutes_dayAndWeekPeriods_returnExpectedMinutes` | 1440 for a day, 10080 for a week, derived not hardcoded | Implemented, passing |
| AC-DASH-PERIOD-06 | `resolveDate_monthYearAndLeapDayTransitions_boundariesCorrect` | `2026-01-31->02-01`, `2026-12-31->2027-01-01`, `2028-02-29` (leap) all resolve correctly | Implemented, passing |
| AC-DASH-INTERVAL-03/05/06 | `clip_fullyInsidePeriod_contributesFullDuration` / `clip_fullyOutsidePeriod_contributesZero` / `clip_activityFullyCoveringPeriod_contributesExactPeriodDuration` | Fully-inside = full duration; fully-outside = 0; fully-covering = exactly period duration | Implemented, passing |
| AC-DASH-INTERVAL-01 | `clip_endsExactlyAtPeriodStart_contributesZero` / `clip_startsExactlyAtPeriodEnd_contributesZero` | Boundary-touching contributes 0 at the calculation level | Implemented, passing |
| AC-DASH-INTERVAL-01 | `summarize_fixedActivityStartingExactlyAtPeriodEnd_excludedFromEarlierPeriod` / `summarize_flexibleWindowStartingExactlyAtPeriodEnd_excludedFromEarlierPeriod` | Boundary-touching excluded at the inclusion level, using a real next-day activity (the only realistic way to hit this boundary given same-day-only activities) | Implemented, passing |
| AC-DASH-INTERVAL-04/08 | `clip_syntheticCrossMidnightInterval_matchesDayAndWeekContributions` | Monday 23:00->Tuesday 01:00 synthetic bounds clipped against day/day/week periods give 60/60/120 minutes - see IMPLEMENTATION_NOTES.md | Implemented, passing |
| AC-DASH-INTERVAL-07 | `summarize_flexibleWorkload_usesFullRequestedDurationNotWindowSpan` | A flexible activity's contribution is its requested duration, not its (wider) window span | Implemented, passing |
| AC-DASH-WORKLOAD-01 | `summarize_mixedFixedAndFlexible_workloadIsSumOfBothContributions` | Matches AC's formula exactly | Implemented, passing |
| AC-DASH-WORKLOAD-02 | `summarize_overlappingFixedActivities_countedIndividuallyNotMerged` | Two overlapping 2h fixed activities (10-12, 11-13) sum to 4h, not 3h | Implemented, passing |
| AC-DASH-WORKLOAD-03 | `summarize_emptyPeriod_workloadIsZeroAndBufferIsFullCapacity` | 0 workload, 0 activities, buffer = full capacity | Implemented, passing |
| AC-DASH-BUFFER-01/02/05 | `summarize_exactCapacity_bufferAndOverloadAreBothZero` / `summarize_oneMinuteUnderCapacity_bufferIsOneMinute` | Exact capacity (via two flexible activities summing to 1440, since a single same-day activity maxes at 1439) -> 0/0; one under -> buffer 1 | Implemented, passing |
| AC-DASH-BUFFER-03/05 | `summarize_oneMinuteOverCapacity_overloadIsOneMinute` | One minute over capacity -> overload 1, buffer 0 | Implemented, passing |
| AC-DASH-BUFFER-04 | (covered by `DashboardFormatterTest`, label text) | "Nominal buffer" label, never "Free time" | Implemented, passing |
| AC-DASH-RATING-01/03 | `summarize_totalsAndHighCounts_matchIndependentThresholdCheck` | Sums correct; an activity can count toward both/either/neither high-count | Implemented, passing |
| AC-DASH-RATING-02 | `highRatingThreshold_ratingThreeNotHigh_ratingFourAndFiveAreHigh` | Boundary check at the threshold itself | Implemented, passing |
| AC-DASH-RATING-04 | `averageRating_halfUpRoundingToOneDecimal_deterministic` | Mean 3.333... -> 3.3 (half-up), verified via `BigDecimal` result, not raw double comparison | Implemented, passing |
| AC-DASH-RATING-05 | `averageAndHighest_emptyIncludedSet_reportedAsHasDataFalse` | `RatingSummary.hasData() == false`; formatter-level text asserted separately in Section D | Implemented, passing |
| AC-DASH-COMPLETION-01/07 | `eligibility_fixedEndExactlyNow_includedAsEligible` | Boundary-equals-now included (inclusive `!isAfter`) | Implemented, passing |
| AC-DASH-COMPLETION-02/07 | `eligibility_flexibleLatestEndExactlyNow_includedAsEligible` | Same, flexible | Implemented, passing |
| AC-DASH-COMPLETION-01 | `eligibility_futureAndOngoingFixed_excludedFromEligibility` | Neither counted eligible | Implemented, passing |
| AC-DASH-COMPLETION-02 | `eligibility_futureAndOngoingFlexible_excludedFromEligibility` | Neither counted eligible | Implemented, passing |
| AC-DASH-COMPLETION-03/04 | `eligibleCount_equalsCompletedPlusIncompleteEligible_futureExcludedEntirely` | Denominator matches; future activities not folded into incomplete | Implemented, passing |
| AC-DASH-PERCENT-01/02 | `completionPercentage_zeroPercent` / `completionPercentage_sixtyPercent` / `completionPercentage_hundredPercent` / `completionPercentage_twoThirds_roundsToSixtySeven` | `0/10->0`, `6/10->60`, `10/10->100`, `2/3->67` | Implemented, passing |
| — | `completionPercentage_noEligibleActivities_isEmpty` | `OptionalInt.empty()` when nothing is eligible yet | Implemented, passing |
| AC-DASH-DETAIL-01 | `summarize_fixedAndFlexibleCounts_matchIncludedActivityTypes` | Counts split correctly | Implemented, passing |
| AC-DASH-DETAIL-02 | `categoryCounts_allFourCategoriesPresent_inEnumDeclarationOrder` | Map always has all 4 keys; formatter iterates `values()` | Implemented, passing |
| AC-DASH-DETAIL-03 | `distribution_ascendingOrderWithZeroCounts` | `1..5` order including zero counts (bar-scaling formula itself is verified in Section D) | Implemented, passing |
| AC-DASH-NOMUTATE-01 | `summarize_doesNotMutateActivityManagerOrActivities` | Manager size, activity completion flags, and default order all unchanged after `summarize` | Implemented, passing |
| — | `ratingSummary_getDistribution_returnsDefensiveCopy` | Mutating the returned array does not affect the summary's own state | Implemented, passing |

## B. `parser.dashboard.DashboardCommandParserTest`

| AC | Test method | Expected outcome | Status |
|---|---|---|---|
| AC-DASH-GRAMMAR-01 | `parse_today_succeeds` / `parse_todayDetail_succeeds` / `parse_tomorrow_succeeds` / `parse_tomorrowDetail_succeeds` / `parse_dateWithValue_succeeds` / `parse_dateWithValueDetail_succeeds` / `parse_thisWeek_succeeds` / `parse_thisWeekDetail_succeeds` | All 8 documented forms parse (one test method each) | Implemented, passing |
| AC-DASH-GRAMMAR-02 | `parse_bareDashboard_throwsMissingInputException` | | Implemented, passing |
| AC-DASH-GRAMMAR-03 | `parse_emptyDateValue_throwsMissingInputException` | | Implemented, passing |
| AC-DASH-GRAMMAR-04 | `parse_calendarInvalidDate_throwsInvalidDateTimeException` | `date/2026-02-30` | Implemented, passing |
| AC-DASH-GRAMMAR-05 | `parse_bareDateNoMarker_throwsInvalidCommandException` | `dashboard 2026-08-15` | Implemented, passing |
| AC-DASH-GRAMMAR-06 | `parse_unknownSelectorWeek_throwsInvalidCommandException` / `parse_thisWithoutWeek_throwsInvalidCommandException` | `dashboard week`, `dashboard this` (missing "week") | Implemented, passing |
| AC-DASH-GRAMMAR-10 | `parse_unknownSelector_throwsInvalidCommandException` | `dashboard unknown` | Implemented, passing |
| AC-DASH-GRAMMAR-07 | `parse_duplicateDetail_throwsInvalidCommandException` | `dashboard today detail detail` | Implemented, passing |
| AC-DASH-GRAMMAR-08 | `parse_tomorrowExtraTrailingText_throwsInvalidCommandException` | `dashboard tomorrow extra` | Implemented, passing |
| AC-DASH-GRAMMAR-09 | `parse_dateWithUnexpectedTrailingText_throwsInvalidCommandException` | `dashboard date/2026-08-15 unexpected` | Implemented, passing |
| AC-DASH-GRAMMAR-11 | `parse_everyRejectCase_neverTouchesActivityManager` | `ActivityManager.size() == 0` after every reject case in one sweep | Implemented, passing |
| AC-DASH-GRAMMAR-12 | `parse_caseInsensitiveSelectorAndDetailKeyword_succeeds` | `TODAY DETAIL`, `This Week Detail`, `DATE/2026-08-20` | Implemented, passing |

## C. `command.dashboard.DashboardCommandTest`

| AC | Test method | Expected outcome | Status |
|---|---|---|---|
| AC-DASH-COMPLETION-06 | `execute_emptyPeriod_showsNoActivitiesFoundMessage` | Exact message, no other body | Implemented, passing |
| AC-DASH-COMPLETION-05 | `execute_nonEmptyPeriodNothingEligible_showsNoActivitiesDueYetMessage` | | Implemented, passing |
| AC-DASH-NOMUTATE-01 | `execute_neverMutatesActivityManagerState` | `getAll()` before/after identical (same objects, same completion flags) | Implemented, passing |
| — | `execute_detailFlagTrue_includesDetailSectionInOutput` | Delegates detail correctly to formatter | Implemented, passing |

## D. `ui.dashboard.DashboardFormatterTest`

| AC | Test method | Expected outcome | Status |
|---|---|---|---|
| AC-DASH-OUTPUT-01/02 | `format_defaultOutput_exactText` | Exact string match, `#`/`-` only, values beside every bar | Implemented, passing |
| AC-DASH-BUFFER-04 | `format_overloadedPeriod_showsOverloadedByLineExactText` | Exact text incl. "Nominal buffer"/"Overloaded by" | Implemented, passing |
| AC-DASH-COMPLETION-05 | `format_nothingDueYet_exactMessage` | | Implemented, passing |
| AC-DASH-COMPLETION-06 | `format_emptyPeriod_exactMessage` | | Implemented, passing |
| AC-DASH-DETAIL-04 | `format_allZeroDistribution_showsAllHyphenBars` | All-zero distribution bar-scaling exception | Implemented, passing |
| AC-DASH-DETAIL-* | `format_detailMode_exactSectionText` | Category breakdown, both distributions, averages/highest | Implemented, passing |
| AC-DASH-RATING-05 | `format_emptyRatingSummary_showsUnavailableNotZero` | | Implemented, passing |
| AC-DASH-OUTPUT-03 | `format_neverShowsPercentageBarForEnergyOrSensoryTotals` | Negative assertion | Implemented, passing |

## E. `parser.CommandDispatcherTest` (addition)

| AC | Test method | Expected outcome | Status |
|---|---|---|---|
| AC-DASH-GRAMMAR-01 | `dispatch_dashboard_returnsDashboardCommandWiredToLiveManager` | | Implemented, passing |
| AC-DASH-GRAMMAR-02 | `dispatch_dashboardMissingSelector_throwsMissingInputException` | | Implemented, passing |

## F. `logic.dashboard.DashboardIntegrationTest`

| Scenario | Expected outcome | Status |
|---|---|---|
| ActivityManager integration | `dashboard` reads through `ActivityManager.getAll()`; fixed+flexible combine correctly; result is deterministic given the same state | Implemented, passing |
| Restart consistency | Save synthetic activities -> fresh `ActivityManager`/`Storage` load (same temp dir) with the same fixed clock -> identical `DashboardSummary` before and after "restart" | Implemented, passing |
| Malformed storage line | A temp `activities.txt` with one malformed line loads via the existing `ActivityStorage`, producing a warning and skipping only that line; the dashboard computed from the valid survivors is correct; no crash, no mutation of the valid records | Implemented, passing |
| No-write verification | Record `activities.txt`/`topics.txt`/`settings.txt` byte content and mtime before running `dashboard today`/`dashboard today detail`/`dashboard this week`; assert byte-identical and (where the filesystem clock resolution allows) unmodified after | Implemented, passing |

## G. `text-ui-test`

Per README.md's already-documented convention (and this branch's own precedent from `route`),
`dashboard today`/`tomorrow`/`this week` are **not** covered by `text-ui-test`, since the harness
runs the real built jar against the real wall clock with no fixed-clock injection point - identical
to why `list today`/`tomorrow`/`this week` are excluded already. Covered instead exhaustively by
`DashboardServiceTest`'s and `DashboardCommandParserTest`'s injected-clock tests above.

`dashboard date/YYYY-MM-DD` *is* deterministic regardless of wall-clock date (the period itself
doesn't depend on `now`), so it's used for all `text-ui-test` scenarios, with an intentionally
far-future date (matching the existing `2099-01-01`-style far-future fixtures already used
elsewhere in `input.txt`) so completion is deterministically "not yet due" regardless of when the
suite runs - true completion-percentage scenarios (0%/60%/100%) are exhaustively covered by
`DashboardServiceTest`'s injected-`now` tests instead, for the same reason.

| Scenario | Expected outcome | Status |
|---|---|---|
| `dashboard date/2099-06-01` | Default output, activities added on that date, all "not yet due" | Implemented, passing |
| `dashboard date/2099-06-01 detail` | Detail section: fixed/flexible counts, category breakdown, both distributions | Implemented, passing |
| `dashboard date/2099-06-02` (no activities added) | "No activities found for the selected period." | Implemented, passing |
| `dashboard` | Missing-selector error | Implemented, passing |
| `dashboard date/` | Missing-value error | Implemented, passing |
| `dashboard date/2026-02-30` | Calendar-invalid error | Implemented, passing |
| `dashboard 2099-06-01` | Bare-date rejection | Implemented, passing |
| `dashboard week` | Unknown-selector error | Implemented, passing |
| `dashboard date/2099-06-01 detail detail` | Duplicate-`detail` error | Implemented, passing |
| `dashboard tomorrow extra` | Unexpected-argument error | Implemented, passing |
| `guide dashboard`, `guide 5` | Dashboard's Coming-soon text replaced; no numbering change at all - item 5 ("Completion and dashboard") was already reserved for this topic in v1.0's menu, unlike `route`, which had no number before v2.0. See `IMPLEMENTATION_NOTES.md`. | Implemented, passing |

## H. `command.general.GuideCommandTest` (updates)

| AC | Test method | Expected outcome | Status |
|---|---|---|---|
| — | `execute_menuNumberFive_resolvesToDashboard` (updated) | Starts with "Dashboard", not "Completion and daily load" | Implemented, passing |
| — | `execute_menuNumberFiveAgreesWithItsOwnKeyword` (new) | `guide 5` == `guide dashboard` | Implemented, passing |
| — | `execute_dashboardTopic_isAvailableAndHasDashboardExamples` (new, replaces the Coming-soon dashboard case) | Real syntax, no "Coming soon" | Implemented, passing |
| — | `execute_dashboardTopicIsCaseInsensitive` (new) | `guide DASHBOARD` resolves | Implemented, passing |
| — | `execute_everyNumberedMenuMapping_resolvesToItsAdvertisedTopic` (updated, item 5's expected text only) | No count/range change - menu stays 1-12 | Implemented, passing |

**No `MAIN_MENU`, `MENU_NUMBER_TOPICS`, `CommandDispatcher` bare-number-switch, or out-of-range-boundary
change is needed for this branch** - only item 5's topic *text* changes; every number keeps its
existing meaning, unlike `route`'s branch which genuinely added a new item 11.
