# Timetable Test Plan

| Acceptance criteria | Component | Automated coverage | Expected result |
|---|---|---|---|
| AC1-6 | `TimetableCommandParser` | Parser tests for week/day/this-week, leap dates, missing values, invalid dates, modes, casing, and trailing text | Exact accepted grammar; invalid input raises the established exception type |
| AC1-3 | `TimetableService` | Period-resolution tests across Monday, Sunday, month/year boundaries, and leap day | Correct inclusive date range |
| AC7-12 | `TimetableService` | Empty, day, full-week, weekend, input-order, and same-start tests | Every qualifying activity retained and deterministically ordered |
| AC13-17 | `TimetableFormatter` | Default/detail/compact snapshot-style assertions | Exact markers, times, sections, metadata, and no invented placements |
| AC18-19 | `TimetableService` + formatter | Synthetic overlapping, identical-start, nested, and adjacent fixed intervals | All overlapping entries marked; adjacent entries unmarked |
| AC20 | Model compatibility | Existing model tests plus Timetable implementation inspection | No cross-midnight or storage-model change |
| AC21-22 | `TimetableCommand` and `ApplicationRunner` | Manager snapshot and temporary-storage integration tests | Identical in-memory and on-disk state before/after execution |
| AC23 | Formatter | Exact output assertions and scan for terminal-control characters | Deterministic plain text |
| AC24 | `GuideCommand`/dispatcher | Guide tests, bare-number routing tests, Text-UI | `guide timetable`, `guide 12`, and bare `12` resolve; `13` returns |
| Public workflow | `CommandDispatcher` -> command -> service -> formatter | Dispatcher integration tests | End-to-end output for every selector/mode |
| Public CLI | Existing `text-ui-test` harness | Add fixed/flexible data, run week/day/compact/detail/error/guide scenarios after a safe reset point | Transcript matches exactly without shifting unrelated IDs |
| Packaging | `releaseZip` clean extraction | Run packaged JAR with Timetable selectors and restart | Commands work offline; release resources remain intact |

## Determinism and isolation

- Relative-week tests inject a fixed `LocalDateTime`; no test reads the real clock.
- Persistence tests use JUnit temporary directories.
- Overlap fixtures are passed directly to the pure transformation helper because normal manager
  validation correctly prevents constructing overlapping stored state through public mutations.
- No network or real NUS dataset is required.
