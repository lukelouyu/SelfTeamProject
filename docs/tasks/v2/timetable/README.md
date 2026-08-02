# v2.0 Feature: Read-only Timetable (`timetable`)

Branch: `feature/v2-timetable`. Base: `main` @ `4b9978e`.

## Purpose

Provide deterministic day and Monday-to-Sunday timetable views without changing the established
chronological `list` command or assigning invented times to flexible activities.

## Command syntax

```text
timetable week/YYYY-MM-DD [compact|detail]
timetable day/YYYY-MM-DD [detail]
timetable this week [compact|detail]
```

- `week/DATE` accepts any valid date and resolves it to the containing Monday-Sunday week.
- `this week` resolves from the single `now` value injected into command dispatch.
- `compact` is the explicit narrow-terminal fallback. UniEnable does not depend on unreliable
  platform-specific terminal-width detection.
- `detail` adds stored activity metadata. `compact` and `detail` are mutually exclusive.

## Supported behaviour

- Fixed activities are grouped by date and ordered by date, start time, then permanent activity
  ID.
- Every weekly view covers Monday through Sunday, including empty days in the normal view.
- Overlapping fixed commitments are all retained and visibly marked with a warning.
- Flexible activities are listed separately as unscheduled, showing their allowed window and
  required duration without pretending they have confirmed start/end times.
- Numeric permanent IDs are used directly. The historical planning example's `A1` alias is not
  introduced because it would create a second identifier system.
- The command is read-only, requires no confirmation, and performs no storage write.

## Dependencies

- `ActivityManager.getAll()` for read-only activity access.
- Existing `FixedActivity` and `FlexibleActivity` fields and permanent IDs.
- Existing injected `LocalDateTime` dispatch seam for `this week`.
- Existing `yyyy-MM-dd` date parser and error conventions.

## Non-goals

- No recommendation, placement, adoption, preference, buffer, or travel-time logic.
- No `[R]` recommended-placement or `[B]` buffer entries until later features create approved
  domain data for them.
- No new persistence file or activity-storage field.
- No stateful `timetable compact`, `timetable details`, or `timetable item/A1` navigation mode.
  `view ID` remains the existing single-activity inspection command.
- No automatic terminal-width detection, ANSI control, colour, mouse input, or screen redrawing.
- No cross-midnight activity-model expansion.

## Known limitations

- The current activity model represents only same-day activities.
- Overlap warnings are defensive: normal add/edit/load validation already rejects overlapping
  fixed activities, but the timetable transformation still handles such input without omission.
- Flexible activities stay unscheduled until a later approved recommender produces adoptable
  placements.
