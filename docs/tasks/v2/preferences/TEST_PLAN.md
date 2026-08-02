# Test Plan: `preference`

## Automated coverage

| Area | Required coverage |
|---|---|
| Model | Defaults, custom profile, start before/equal/after end, buffers 0/1440/1441, Tomato OFF/ON, immutability, equality/hash code |
| Parser | View/reset, every one-field update, representative two/three/four-field updates, marker reordering, strict times, buffer bounds/type, Tomato values, empty/unknown/duplicate/trailing input, unchanged-field validation |
| Manager/commands | Default view, one/multi/Tomato/full update, exact previews, confirmation, cancellation, no-op, invalid non-mutation |
| Storage | Missing/valid/round-trip/write order, ON/OFF, empty/malformed/missing/duplicate/unknown fields, invalid time/buffer/Tomato, inconsistent range, all-or-default fallback, UTF-8/newline stability |
| Transaction | Four-file coordinated success, preferences commit failure after earlier commits, another-file failure, temp/backup cleanup, application snapshot rollback |
| Restart | Startup load, malformed warning, persisted set/reset across restart, read-only view performs no write |
| Reset all | Option 1 resets all four, option 2 retains all four, option 3 no-op, persistence failure rollback |
| Guide | `guide preference`, item 6 unchanged and still honest about future recommendation |

## Text-UI scenarios

Exercise successful view, one/two/four-field sets, Tomato ON/OFF, reset, `guide preference`, guide
item 6, cancellation, invalid time/buffer/Tomato, duplicate Tomato, invalid resulting range,
restart persistence, and reset-all options 1 and 2. Insert scenarios only at a safe ID boundary or
after a full reset to avoid unrelated ID drift.

## Complete gate

```text
.\gradlew.bat clean test checkstyleMain checkstyleTest --console=plain
.\gradlew.bat javadoc --console=plain
bash text-ui-test/runtest.sh
.\gradlew.bat releaseZip --console=plain
```

Then extract the newly generated ZIP into a fresh directory and smoke-test every approved command,
confirmation/cancellation, restart persistence, guide topics, and reset-all behaviour. Confirm the
ZIP still contains only the intended JAR and external academic calendar and no user preferences.
