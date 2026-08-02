# v2.0 Feature: Global Planning Preferences (`preference`)

Branch: `feature/v2-preferences`. Base: `main` @ `ada0f5f`.

## Purpose

Persist one global, everyday preference profile for the future deterministic recommender. The
profile is user-entered planning data, not a medical assessment and not an inferred disability
profile.

## Commands

```text
preference view
preference set start/HH:mm [end/HH:mm] [buffer/MINUTES] [tomato/on|off]
preference reset
```

For `set`, one or more markers may appear in any order and each may appear at most once.

## Profile and defaults

```text
Preferred daily start: 08:00
Preferred daily end: 20:00
Minimum buffer: 15 minutes
Tomato suggestion: OFF
```

The defaults live in `PreferenceProfile` as the single authoritative default profile. The profile
is global: it applies to every date considered by the future recommender. Weekday-specific,
date-specific, and multiple named profiles are out of scope.

## Tomato boundary

Tomato is an advisory display preference. When enabled, the future recommender may display a
Pomodoro-style study suggestion for suitable flexible study activities. It does not change stored
activities, create breaks, split duration, affect slot generation, overlap/buffer checks,
energy/sensory scoring, route feasibility, recommendation ranking, Dashboard metrics, or
Timetable ordering. Recommender-side display behaviour is not implemented on this branch.

## Persistence and failure policy

`data/preferences.txt` stores the complete profile in deterministic four-line order. A missing
file loads defaults silently. Any existing malformed, incomplete, duplicate, unknown, invalid, or
internally inconsistent profile produces a startup warning and falls back to the whole default
profile; no valid subset is retained.

Preferences join activities, topics, and order settings in the existing coordinated save,
backup, commit, and rollback transaction. Command cancellation and any persistence failure restore
the complete in-memory profile, and a failed file commit restores every coordinated file.

## Reset integration

- `preference reset` confirms and restores all four defaults atomically.
- `reset all` option 1 restores all four defaults.
- `reset all` option 2 retains the complete preference profile.
- `reset all` option 3 remains a no-change cancellation.

## Non-goals

- No recommendation generation, preview, adoption, or ranking.
- No per-day overrides or multiple profiles.
- No peak-period, focus-duration, or break-duration fields.
- No generic feature-toggle framework.
- No changes to activity, Dashboard, Timetable, or route semantics.
