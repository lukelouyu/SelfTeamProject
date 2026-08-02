# Implementation Notes: `recommend`

## Adopted representation

The smallest compatible representation is to keep flexible activities as flexible and add one
optional adopted scheduled placement to the existing model. This avoids silently converting a
flexible activity into a fixed one, preserves the original flexible window/duration, keeps the
permanent ID stable, and lets timetable/dashboard see one activity instead of fabricating a
second persisted record.

## Proposal state

Preview lives outside persisted activity state in a dedicated in-memory proposal object. Commands
that generate, view, or cancel a proposal mutate only that temporary proposal holder, never the
saved activity model.

## Deterministic scheduling

The branch uses a minute-granularity deterministic slot search inside each flexible window. This
is intentionally simple and explicit: exact-fit and one-minute-too-short behaviour become easy to
test, and equal inputs produce equal candidate lists on every run.

## Preference usage

- Minimum buffer is a hard feasibility rule.
- Preferred start/end guide tie-breaking only.
- Tomato is display-only advisory output.

## Route-aware deferral

The original broader planning material discussed route-aware recommendation, but the live
repository still keeps route planning separate and activities still do not store facility names.
This branch therefore does not claim travel feasibility, inaccessible-edge rejection, or
sheltered-travel ranking. Those behaviours require a separate approved data-model extension first.

## Persistence compatibility

Backward compatibility is preserved by treating the adopted placement as an optional trailing
field on persisted flexible records. Older flexible lines still load; newer lines with no adopted
placement still behave like ordinary unscheduled flexible activities.
