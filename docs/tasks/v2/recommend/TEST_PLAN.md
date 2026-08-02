# Test Plan: `recommend`

## Unit coverage

- Parser:
  `recommend`, `recommend this week`, `recommend date/YYYY-MM-DD`, `recommend view`,
  `recommend adopt`, `recommend cancel`, malformed selectors, malformed dates, trailing-text
  rejection.
- Recommendation engine:
  zero eligible flexible activities, exact-fit slot, one-minute-too-short slot, fixed overlap
  rejection, adopted/proposed overlap rejection, buffer rejection, deterministic activity order,
  deterministic slot order, preferred-range tie-break, energy-spread tie-break,
  sensory-spread tie-break, permanent-ID final tie-break.
- Proposal state:
  generate replaces older proposal, `view` requires proposal, `cancel` clears proposal,
  preview does not mutate activity state.
- Tomato:
  advisory suggestion shown only when `tomato/on` and the activity satisfies the documented
  suitability rule; omitted otherwise.

## Integration coverage

- Scenario A: fixed lectures plus multiple flexible study activities, set preferences, generate
  deterministic recommendation, adopt, verify timetable output, verify dashboard output, restart,
  verify persistence.
- Scenario B: recommendation preview then cancel, assert no in-memory activity mutation and no
  storage write.
- Scenario C: adoption save failure through injectable storage, assert complete rollback of
  activities and no persisted partial schedule.
- Scenario D: editing/reloading adopted flexible activities preserves the original flexible window
  while restoring the adopted placement accurately.

## Text-UI coverage

- `recommend`
- `recommend this week`
- `recommend date/YYYY-MM-DD`
- `recommend view`
- `recommend cancel`
- `recommend adopt`
- no eligible activities
- Tomato advisory output on
- restart persistence after adoption
- storage failure rollback where harness/integration supports it

## Deferred coverage

- No route-aware recommendation tests on this branch because activities still have no approved
  facility/location binding in the live model.
- No CSV export tests on this branch because export was explicitly removed from scope.
