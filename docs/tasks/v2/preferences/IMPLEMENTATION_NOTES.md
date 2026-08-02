# Implementation Notes: `preference`

## Why the profile has four fields

The authoritative recommender rules require a preferred daily range and minimum buffer. The user
approved one additional global advisory Tomato/Pomodoro suggestion switch. Older planning drafts'
peak-period, focus-duration, and break-duration fields remain excluded because the approved
recommender ranking does not consume them. Persisting unused fields would create misleading and
unsupported configuration.

## One authoritative default profile

`PreferenceProfile.defaults()` returns the single immutable default value (`08:00`, `20:00`, `15`,
`OFF`). Parser partial-update fallback, manager reset, missing-file loading, reset-all, formatting,
and tests consume that value rather than duplicating default literals in production layers.

## Whole-profile parsing and validation

`PreferenceCommandParser` tokenizes all set fields before constructing a complete candidate from
the current immutable profile. This makes cross-field validation atomic and catches conflicts with
unchanged fields. Duplicate-marker rejection belongs in the currently production-unused shared
`ArgumentTokenizer`, with regression tests, so Preferences can use its declarative markers without
silently accepting last-value-wins behaviour.

## All-or-default storage

`PreferenceStorage` reads all lines before producing a profile. Exactly one valid instance of every
known key is required. Any error invalidates the whole file and returns the single default profile
with concise reasons. This deliberately differs from independent-record storage: the four lines
form one invariant-bearing aggregate, so partial retention could create a profile the user never
selected.

## Coordinated persistence and rollback

Preferences join the existing `Storage.saveAll` temporary-file, backup, commit, and restoration
transaction. Application state snapshots also capture the immutable profile. Thus command
cancellation never executes, validation never constructs active state, and a save failure restores
both in-memory managers and every coordinated destination.

## Reset semantics

`preference reset` is a confirmable explicit profile reset. Existing `reset all` option 1 also
resets the profile because it promises to delete all user data; option 2 retains the profile
because it keeps the user's ongoing class-schedule planning context; option 3 remains cancellation.

## Future recommender interface

The future recommender can read one immutable `PreferenceProfile` from `PreferenceManager`. Tomato
is exposed as a small `TomatoSuggestion` enum for readable future consumption, but no generic
feature-toggle system or recommender-side behaviour is introduced here.
