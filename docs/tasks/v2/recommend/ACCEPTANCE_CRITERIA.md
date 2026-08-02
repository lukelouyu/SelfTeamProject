# Acceptance Criteria: `recommend`

## Grammar and proposal lifecycle (AC-REC-GRAMMAR)

- **AC-REC-GRAMMAR-01**: Bare `recommend` is accepted and behaves exactly like
  `recommend this week`.
- **AC-REC-GRAMMAR-02**: `recommend this week` and `recommend date/YYYY-MM-DD` generate one new
  proposal and replace any earlier in-memory proposal.
- **AC-REC-GRAMMAR-03**: `recommend view` requires an existing proposal and never recomputes one.
- **AC-REC-GRAMMAR-04**: `recommend adopt` requires an existing proposal, shows a confirmation
  preview, then mutates once and saves once.
- **AC-REC-GRAMMAR-05**: `recommend cancel` clears an existing proposal and performs no save.
- **AC-REC-GRAMMAR-06**: Unknown selectors, malformed dates, and unexpected trailing text fail
  with helpful errors and leave activities, preferences, IDs, and proposal state unchanged.

## Eligibility and hard constraints (AC-REC-CONSTRAINTS)

- **AC-REC-CONSTRAINTS-01**: Only incomplete flexible activities whose date falls in the requested
  period are eligible.
- **AC-REC-CONSTRAINTS-02**: A candidate placement is rejected if its full duration does not fit
  inside the flexible window.
- **AC-REC-CONSTRAINTS-03**: Candidate intervals use half-open interval logic consistently.
- **AC-REC-CONSTRAINTS-04**: An exact-fit slot is accepted; a slot that is exactly one minute too
  short is rejected.
- **AC-REC-CONSTRAINTS-05**: A candidate placement is rejected if it overlaps a fixed activity.
- **AC-REC-CONSTRAINTS-06**: A candidate placement is rejected if it overlaps an already adopted
  flexible placement or an earlier placement in the same proposal.
- **AC-REC-CONSTRAINTS-07**: The global minimum buffer is enforced between sequential scheduled
  commitments.
- **AC-REC-CONSTRAINTS-08**: Fixed activities are never moved, deleted, or rewritten by
  recommendation generation.

## Determinism and ranking (AC-REC-DETERMINISM)

- **AC-REC-DETERMINISM-01**: Recommendation generation is deterministic for equal inputs; no
  randomness, hash-iteration dependence, or wall-clock drift within one command execution is used.
- **AC-REC-DETERMINISM-02**: The command captures the injected `now` once and does not call a new
  time source repeatedly while building one proposal.
- **AC-REC-DETERMINISM-03**: Eligible activities are scheduled in this exact rule order:
  fewest valid slots, earlier slot preference, buffer preservation, high-energy spreading,
  high-sensory spreading, preferred daily start/end, permanent activity ID final tie-break.
- **AC-REC-DETERMINISM-04**: Route-only tie-breakers are not claimed or partially simulated while
  activities lack facility bindings.

## Tomato meaning (AC-REC-TOMATO)

- **AC-REC-TOMATO-01**: With `tomato/off`, recommendation output shows no Tomato suggestion.
- **AC-REC-TOMATO-02**: With `tomato/on`, a suitable long flexible study activity may show a
  short Pomodoro-style study suggestion in the preview/adoption output.
- **AC-REC-TOMATO-03**: Tomato never changes slot generation, ranking, duration, overlap logic,
  buffer checks, persistence shape, timetable entry ordering, or dashboard metrics.

## Preview isolation (AC-REC-PREVIEW)

- **AC-REC-PREVIEW-01**: Generating or viewing a proposal performs no activity save, no
  preference save, no permanent-ID consumption, and no completion-state mutation.
- **AC-REC-PREVIEW-02**: A proposal lives only in in-memory application state until adoption.
- **AC-REC-PREVIEW-03**: Restart before adoption discards the proposal completely.
- **AC-REC-PREVIEW-04**: Cancelling a proposal leaves all persisted files unchanged.

## Adoption and rollback (AC-REC-ADOPT)

- **AC-REC-ADOPT-01**: Adopt preserves each flexible activity's permanent ID, original date,
  original window, and original duration while adding one adopted scheduled placement.
- **AC-REC-ADOPT-02**: Adopted placement survives restart through the existing coordinated save
  transaction.
- **AC-REC-ADOPT-03**: Adoption integrates with timetable and dashboard views without duplicating
  activity records.
- **AC-REC-ADOPT-04**: Any coordinated persistence failure restores the entire in-memory activity,
  topic, settings, and preference state and leaves no partial adopted placements on disk.

## Documentation honesty (AC-REC-DOCS)

- **AC-REC-DOCS-01**: `guide recommend`, UG, DG, README, docs README, and handover describe only
  the implemented recommend feature set.
- **AC-REC-DOCS-02**: Documentation does not present removed backlog items as active or promised
  v2.0 functionality.
- **AC-REC-DOCS-03**: Route-aware recommendation remains explicitly deferred because activities do
  not store facility/location bindings.
