# Luke Louyu - Project Portfolio Page

## Overview

**UniEnable** is a single-user, offline, CLI-based Java 17 application that helps tertiary
students with ASD or ADHD, and tertiary students who use wheelchairs, prepare for unfamiliar
university, internship, or entry-level work routines. It combines fixed/flexible activity
planning, energy-demand and sensory-load ratings, category/topic organisation, a deterministic
"next relevant activity" lookup, and read-only local facility/accessible-route reference data.

This is a CS2113 team project (tP) built solo, simulating the workflow of a small team: all
requirements, design, implementation, and documentation below are my own work.

### Summary of Contributions

- **Product design**: defined the target personas (Sam, a student with ASD/ADHD; Jordan, a
  wheelchair user), the problem statement, and the v1.0/v2.0 feature scope, working from a
  prioritised user-story backlog.
- **Architecture**: an AB3-inspired layered design (`command` / `parser` / `logic` / `model` /
  `storage` / `ui`, plus a separate read-only `accessibility` domain for facility/connection
  reference data), with command objects separating parsing from execution.
- **Features implemented (v1.0)**:
  - Activity `add`/`list`/`view`/`find`/`edit`/`delete`/`mark`/`unmark`/`next`/`order`, covering
    both fixed-time and flexible-window activities with energy-demand and sensory-load ratings.
  - Category and one-level topic organisation (`topic add`/`list`/`rename`/`delete`), including
    cascading topic renames across affected activities and delete protection while a topic is
    still in use.
  - Read-only facility and connection accessibility reference data (`facility`/`connection`
    `list`/`view`/`find`) backed by a real digitised NUS FASS campus map, loaded from
    human-editable `facilities.txt`/`connections.txt` files with per-line malformed-record
    reporting.
  - A built-in `guide` command with a numbered, number-or-keyword-selectable menu and copyable
    command examples.
  - Local, human-editable text-file storage for activities and topics.
  - Academic-calendar-driven recurrence (`recur`), resolving each requested teaching week
    independently against an external, human-maintained calendar file rather than assuming a
    fixed number of days between weeks - so a single command can span a semester recess without
    generating an activity during it.
- **Features implemented (v2.0)**:
  - Accessible route search (`route`) over the same facility/connection reference data, returning
    a confirmed-accessible shortest path with per-segment distance and barrier notes.
  - A read-only accessible-planning dashboard (`dashboard`) summarising planned workload, nominal
    buffer, and energy/sensory demand for a day or week.
  - A read-only text timetable (`timetable`) showing fixed and flexible activities by day or week.
  - A persisted global planning-preference profile (`preference`) feeding a deterministic
    schedule-recommendation engine (`recommend`) that proposes and, on confirmation, adopts
    conflict-free start times for incomplete flexible activities - never before the current
    moment, and never adoptable once a proposal has gone stale.
  - A shared relative date-selector vocabulary (`today`/`tomorrow`/`this week`/`next week`)
    consistently supported across `list`, `find`, `dashboard`, `timetable`, and `recommend`,
    resolved through one centralised date-resolution helper rather than duplicated per command.
- **Testing**: grew the JUnit suite past 1,190 tests and the `text-ui-test` end-to-end regression
  script to a multi-thousand-line scripted run covering both v1.0 and v2.0 commands, boundary
  cases, and error paths (date-dependent cases like `list today` are covered by JUnit with a
  fixed injected clock instead, since they can't be scripted deterministically), catching several
  real bugs - including a v2.0 recommender regression that could propose or adopt a schedule
  already in the past - before release.
- **Documentation**: wrote the [User Guide](../UserGuide.md) and
  [Developer Guide](../DeveloperGuide.md), including the architecture, design rationale, and
  manual testing instructions.
