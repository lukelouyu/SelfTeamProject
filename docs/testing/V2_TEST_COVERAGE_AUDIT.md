# UniEnable v2.0 Test Coverage Audit

Audit date: August 2, 2026

Tested branch: `feature/v2-recommend`

Tested commit: `efd30bc2b643e2998ccc22800f03f55f41d4b3a0`

## 1. Current git state

- Branch: `feature/v2-recommend`
- HEAD: `efd30bc2b643e2998ccc22800f03f55f41d4b3a0`
- Working tree: dirty; recommend production/tests/docs/diagrams are in progress and uncommitted
- Pre-existing untracked path: `.claude/`

## 2. Scope authority and conflict

The historical v2 master prompt in
`docs/planning/UNIENABLE_V2_CLAUDE_CODEX_MASTER_PROMPT_UPDATED.md` still includes
`feature/v2-export` and CSV Export as Feature 6.

The newer release-readiness plan in
`C:\Users\lukel\Downloads\UNIENABLE_V2_RELEASE_READINESS_TEST_PLAN.md` explicitly removes CSV
Export from approved v2.0 scope.

This audit treats the release-readiness plan as the authoritative final-scope override.

## 3. Feature completeness snapshot

| Feature | State on current branch | Notes |
|---|---|---|
| v1.0 activity/topic/recurrence/reset/storage baseline | Present | Must still be regression-verified after recommend finalization |
| Route Search | Present in repo | Already documented/diagrammed |
| Dashboard | Present in repo | Already documented/diagrammed |
| Weekly Timetable | Present in repo | Already documented/diagrammed |
| Preference Profile | Present in repo | Already documented/diagrammed |
| Schedule Recommender | Present but still being finalized | Code, tests, docs, UML, text-ui present; final audit still needed |
| CSV Export | Removed from approved v2.0 scope | Stale references remain and must be revised surgically |

## 4. Stale Export references

Critical stale export references still remain in live repository materials:

- `src/main/java/seedu/unienable/command/general/GuideCommand.java`
  - menu item `9. CSV export`
  - `export` topic text
  - recommend topic still says export is future work
- `src/test/java/seedu/unienable/command/general/GuideCommandTest.java`
  - export-topic/menu expectations still present
- `text-ui-test/input.txt`
  - `guide export`
- `text-ui-test/EXPECTED.TXT`
  - export menu and export topic transcript
- `docs/DeveloperGuide.md`
  - says CSV export remains future work
- `docs/tasks/v2/recommend/README.md`
  - says CSV export remains future work
- `docs/tasks/v2/recommend/ACCEPTANCE_CRITERIA.md`
  - export still referenced in docs expectations
- `HANDOVER.md`
  - still mentions `feature/v2-export` as the next planned feature

Historical planning/reference files under `docs/planning/` also contain export references, but
those are acceptable if clearly treated as historical context rather than live scope.

## 5. Requirements-to-tests matrix

| Feature | Acceptance criterion | Production component | JUnit unit test | JUnit integration test | Text-UI case | Gap | Required action |
|---|---|---|---|---|---|---|---|
| Recommend grammar | generate/view/cancel/adopt dispatch and parser rejection | `RecommendCommandParser`, `CommandDispatcher` | Partial (`CommandDispatcherTest`) | None direct | Present in official harness for happy-path preview/adopt/cancel | No direct parser test class for `RecommendCommandParser`; invalid forms not covered directly | Add `RecommendCommandParserTest` with valid and invalid branches |
| Recommend preview non-mutation | preview/view must not mutate in-memory state | `RecommendationService`, `RecommendGenerateCommand`, `RecommendViewCommand` | Partial (`RecommendationServiceTest`) | None direct | Indirectly exercised | No command/integration coverage that preview/view leaves managers/files unchanged | Add integration test through `ApplicationRunner` or command-level tests |
| Recommend cancel non-mutation | cancel clears proposal only | `RecommendCancelCommand`, `RecommendationManager` | None direct | None | Covered in official harness | No unit/integration assertions | Add direct command/runner tests |
| Recommend adopt | confirmed adoption saves once, rollback on failure | `RecommendAdoptCommand`, `ApplicationRunner`, `Storage` | None direct | None direct | Covered in official harness happy path | No focused adopt command test; no adoption save-failure rollback coverage yet | Add command/integration rollback tests |
| Adopted recommendation persistence | adopted flexible state survives restart | `FlexibleActivity`, `ActivityStorage`, `ApplicationRunner` | Partial (`ActivityStorageTest`) | Partial (`ApplicationRunnerTest` for preferences only, not adopt) | Indirect via harness same-run only | Restart persistence for adopted recommendation not directly proven | Add restart integration test |
| Timetable adopted entry rendering | adopted flexible shows as `[R]` and unscheduled remains separate | `TimetableService`, `TimetableFormatter` | Present | None | Present in official harness | Good, but later regression batches still needed | Keep |
| Dashboard with adopted recommendations | dashboard reflects adopted state per model | `DashboardService` | Partial | None | Indirect | No end-to-end adopted recommendation dashboard integration test | Add integration test |
| Preference advisory Tomato | Tomato only affects advisory output | `PreferenceProfile`, `RecommendationService`, `RecommendationFormatter` | Partial (`RecommendationServiceTest`) | None | Indirect | No view/generate command output assertion | Add command/formatter test |
| Export removed from scope | no live docs/guide claim export as approved v2.0 feature | `GuideCommand`, `README`, `UserGuide`, `DeveloperGuide`, text-ui transcript | Existing tests currently expect export placeholder | None | Existing harness still includes export | Live docs and harness are stale | Revise docs, guide, tests, transcript to remove export from final v2.0 claims |

## 6. Production classes with no direct tests

The following production classes currently have no same-name direct `*Test` class:

- `AccessibilityDisclaimer`
- `Command`
- `Confirmable`
- `DashboardPeriod`
- `DashboardSummary`
- `DuplicateActivityException`
- `InvalidActivityException`
- `InvalidCommandException`
- `InvalidDateTimeException`
- `InvalidIndexException`
- `MenuConfirmable`
- `MenuOutcome`
- `NoClassDate`
- `PreferenceFormatter`
- `PreferenceResetCommand`
- `PreferenceSetCommand`
- `PreferenceViewCommand`
- `RatingSummary`
- `RecommendAdoptCommand`
- `RecommendationFormatter`
- `RecommendationManager`
- `RecommendationProposal`
- `RecommendCancelCommand`
- `RecommendCommandParser`
- `RecommendedPlacement`
- `RecommendGenerateCommand`
- `RecommendViewCommand`
- `RecurrencePlan`
- `StorageException`
- `TimetableEntry`
- `TimetableEntryType`
- `TimetableMode`
- `TimetablePeriod`
- `TimetableView`
- `TomatoSuggestion`

Not all of these need new tests. The high-priority uncovered v2.0-relevant entries are:

- `RecommendCommandParser`
- `RecommendGenerateCommand`
- `RecommendViewCommand`
- `RecommendCancelCommand`
- `RecommendAdoptCommand`
- `RecommendationFormatter`
- `RecommendationManager`
- `RecommendationProposal`
- `RecommendedPlacement`

## 7. Parser branches lacking clear invalid-input coverage

High-priority likely gaps identified from current file structure:

- no `src/test/java/.../parser/recommend/RecommendCommandParserTest.java`
- therefore no direct invalid-form coverage for:
  - bare unknown subcommand
  - malformed `date/`
  - trailing text after `date/...`
  - `adopt` without active proposal
  - `view` without active proposal at parser/command boundary separation

Lower-priority parser gaps may remain elsewhere, but Recommend is the immediate branch-completion
gap.

## 8. Storage-failure paths lacking rollback tests

Known high-priority gaps:

- recommend adoption save failure rollback:
  - adopted placement must not remain in memory
  - no partial persisted flexible-activity state may leak to disk
- restart after failed adoption should reload pre-command state

## 9. v2.0 commands missing from official Text-UI regression representation

Current official harness now covers representative recommend preview/cancel/adopt flow, but still
contains stale export guide cases.

What is still missing from the official committed regression surface:

- invalid recommend parser forms
- recommend view with no active proposal
- recommend adopt with no active proposal
- recommend exact-fit vs one-minute-too-short boundary

Those do not all belong in the single official transcript; some should live in dedicated batch
files under `text-ui-test/batches/v2/`.

## 10. Production date/time seam audit

Direct real-time call still present in production code:

- `src/main/java/seedu/unienable/app/ApplicationRunner.java`
  - `dispatcher.dispatch(line, LocalDateTime.now())`

This is an existing top-level runtime seam rather than a deep feature-specific clock leak, but it
still means the main text UI loop itself is wall-clock driven. Relative-date text-ui scenarios
must therefore remain excluded from the official deterministic harness unless the runner gains a
test clock injection seam.

No other direct `.now()` call was found under `src/main/java`.

## 11. Deterministic-ordering audit

Current audit findings:

- recommendation placement ordering is explicitly sorted by date, start time, then stable ID
- recommendation unscheduled IDs are explicitly numerically sorted
- timetable ordering has direct test coverage for same-time display ordering
- route deterministic tie handling should be rechecked during Batch 9 and route JUnit audit
- category-order determinism in dashboard should be rechecked in automated coverage audit

One implementation/detail mismatch to re-verify during automated testing:

- `RecommendationService.SlotScore.betterThan(...)` currently compares earlier candidate start
  time before the later soft-score fields. This matches the current DG wording, but the older
  master prompt discussed a richer rule ladder including travel/shelter tie-breakers that are now
  out of scope because activities still lack location bindings.

## 12. Recommended immediate actions

### Critical

1. Remove/revise stale live-scope Export references from:
   - guide
   - tests
   - official text-ui transcript
   - live README/UG/DG wording
   - current recommend task docs
   - handover live-status text
2. Add direct recommend parser/command tests.
3. Add adoption persistence and rollback integration coverage.

### High priority

1. Add `docs/testing/V2_FINAL_REGRESSION_REPORT.md` only after later batches complete.
2. Create `text-ui-test/batches/v2/` batch files for release-readiness regression without
   overloading the official transcript.
3. Re-run full quality gate after stale export cleanup and recommend test additions.

## 13. Stop condition result

Critical/high-priority automated-test gaps are still present.

Manual regression batches should not begin yet.

Required next step: close the critical recommend/export/test gaps above first, then rerun Batch 0
audit status before proceeding into manual regression batches.
