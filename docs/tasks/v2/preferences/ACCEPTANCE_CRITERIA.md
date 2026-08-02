# Acceptance Criteria: `preference`

## Profile and scope (AC-PREF-PROFILE)

- **AC-PREF-PROFILE-01**: One immutable global profile contains preferred daily start, preferred
  daily end, minimum buffer minutes, and Tomato suggestion state.
- **AC-PREF-PROFILE-02**: Defaults are exactly `08:00`, `20:00`, `15`, and `OFF`, defined by one
  authoritative `PreferenceProfile` value.
- **AC-PREF-PROFILE-03**: Start is strictly earlier than end. Buffer is within `0..1440`.
- **AC-PREF-PROFILE-04**: The model has value equality and hash-code consistency and exposes no
  public setter.
- **AC-PREF-PROFILE-05**: The profile applies identically to every day; weekday/date-specific and
  named profiles are unsupported.

## Grammar and validation (AC-PREF-GRAMMAR)

- **AC-PREF-GRAMMAR-01**: `preference view` accepts no trailing text and is read-only.
- **AC-PREF-GRAMMAR-02**: `preference reset` accepts no trailing text and produces a confirmable
  restoration of all defaults.
- **AC-PREF-GRAMMAR-03**: `preference set` accepts one or more of `start/`, `end/`, `buffer/`, and
  `tomato/`, in any order, each at most once.
- **AC-PREF-GRAMMAR-04**: Command/subcommand/markers and Tomato input values are case-insensitive.
  Stored Tomato values are uppercase `ON`/`OFF`.
- **AC-PREF-GRAMMAR-05**: Time uses strict `HH:mm`; `8:00`, `08:0`, `24:00`, and `25:00` fail.
- **AC-PREF-GRAMMAR-06**: Buffer is an integer from `0` to `1440` inclusive. Negative,
  non-integer, and `1441` fail.
- **AC-PREF-GRAMMAR-07**: Tomato accepts only `on` or `off` in command input.
- **AC-PREF-GRAMMAR-08**: No-field set, empty values, duplicate markers, unknown markers, and
  unexpected trailing tokens fail with helpful errors.
- **AC-PREF-GRAMMAR-09**: Validation constructs and validates the complete proposed profile,
  including unchanged values. For example, `start/21:00` fails when end remains `20:00`.
- **AC-PREF-GRAMMAR-10**: Every validation failure is non-mutating.

## Command transactions (AC-PREF-COMMAND)

- **AC-PREF-COMMAND-01**: `view` shows all four values and the global-everyday scope, with no
  confirmation, mutation, snapshot, or save.
- **AC-PREF-COMMAND-02**: `set` previews every changed old/new value and requests confirmation
  through the existing confirmation mechanism.
- **AC-PREF-COMMAND-03**: A confirmed set mutates once and saves once; a cancelled set changes
  nothing.
- **AC-PREF-COMMAND-04**: A multi-field set is one transaction. One invalid field prevents every
  change.
- **AC-PREF-COMMAND-05**: `reset` previews all values that will return to defaults, confirms, then
  mutates/saves once. Cancellation changes nothing.
- **AC-PREF-COMMAND-06**: A set/reset that would make no change cancels without a storage write.
- **AC-PREF-COMMAND-07**: Any save failure restores the old complete profile in memory and every
  coordinated file on disk.

## Tomato meaning (AC-PREF-TOMATO)

- **AC-PREF-TOMATO-01**: Tomato defaults to `OFF` and can be explicitly set `ON` or `OFF`.
- **AC-PREF-TOMATO-02**: Tomato is advisory only for a future recommender display suggestion.
- **AC-PREF-TOMATO-03**: This branch does not use Tomato to alter stored activities, durations,
  breaks, slots, overlaps, buffers, routes, ratings, ranking, Dashboard, or Timetable output.

## Storage and loading (AC-PREF-STORAGE)

- **AC-PREF-STORAGE-01**: The file is `data/preferences.txt`, written in exactly this order:
  `PREFERRED_START|HH:mm`, `PREFERRED_END|HH:mm`, `MINIMUM_BUFFER|N`,
  `TOMATO_SUGGESTION|ON|OFF`.
- **AC-PREF-STORAGE-02**: A completely missing file returns the full defaults with no warning.
- **AC-PREF-STORAGE-03**: An empty file, malformed delimiter structure, missing/duplicate/unknown
  key, invalid time/buffer/Tomato, lowercase stored Tomato, or inconsistent start/end causes full
  fallback to defaults and a reason-bearing startup warning.
- **AC-PREF-STORAGE-04**: No partial valid-field subset survives an invalid profile.
- **AC-PREF-STORAGE-05**: Saving uses UTF-8, deterministic field order, and stable platform newline
  handling; round trips preserve all four values.
- **AC-PREF-STORAGE-06**: Preference temp/backup files are cleaned after success and failure.

## Integration and reset-all (AC-PREF-INTEGRATION)

- **AC-PREF-INTEGRATION-01**: Startup loads the profile and surfaces malformed-file warnings through
  the existing `preferences.txt` warning channel without crashing.
- **AC-PREF-INTEGRATION-02**: The coordinated atomic transaction covers activities, topics,
  settings, and preferences. A failure at any commit restores all prior destinations.
- **AC-PREF-INTEGRATION-03**: A successful set/reset survives restart.
- **AC-PREF-INTEGRATION-04**: `reset all` option 1 resets all four defaults; option 2 retains the
  complete profile; option 3 changes nothing. Save failure rolls back every affected state.
- **AC-PREF-INTEGRATION-05**: Existing v1.0 directories without `preferences.txt` start normally;
  read-only commands do not create the file.

## Guide and documentation (AC-PREF-DOCS)

- **AC-PREF-DOCS-01**: `guide preference` documents all fields, syntax, defaults, global scope,
  atomic changes/reset, and advisory Tomato meaning without a Coming-soon note.
- **AC-PREF-DOCS-02**: No numbered guide entry is added or renumbered. Item 6 remains the future
  recommender topic and may link to `guide preference` without claiming recommendation exists.
- **AC-PREF-DOCS-03**: User/developer guides, READMEs, handover, architecture/storage diagrams, and
  focused class/sequence diagrams agree with implemented behaviour.
