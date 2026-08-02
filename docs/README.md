# UniEnable

UniEnable is a single-user, offline, CLI-based Java 17 application that helps tertiary students
with ASD or ADHD, and tertiary students who use wheelchairs, prepare for unfamiliar university,
internship, or entry-level work routines. It combines fixed and flexible activity planning
(with completion tracking, energy-demand and sensory-load ratings, and category/topic
organisation), a deterministic "next relevant activity" lookup, read-only local
facility/connection accessibility reference data, and — new in v2.0 — Dijkstra-based accessible
route search (`route`), a read-only planning-load dashboard (`dashboard`), deterministic
day/week timetable views (`timetable`), one persisted global everyday planning-preference
profile (`preference`), and deterministic schedule recommendation (`recommend`) — all in a fast,
offline CLI.

The documents in this folder describe the finished release scope. In particular, `recommend` is a
complete deterministic preview-and-adopt workflow rather than a placeholder, and no further
command-scope expansion is planned beyond what the guides already describe.

It is explicitly not a general calendar, a medical tool, a NUSMods clone, a live GPS/navigation
tool, or a multi-user system.

Useful links:
* [User Guide](UserGuide.md)
* [Developer Guide](DeveloperGuide.md)
* [About Us](AboutUs.md)
