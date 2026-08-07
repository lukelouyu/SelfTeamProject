# UniEnable presenter guide (5–7 minutes)

## Before the session

1. Download and extract the `v2.0.1` release ZIP.
2. Confirm Java 17 is installed with `java -version`.
3. Open a terminal in the extracted folder and run `java -jar unienable.jar`.
4. Keep the presentation, the app terminal, and the supplied terminal screenshot open.
5. Test both QR codes from the back of the room or at projected size.

## Suggested flow

- **Slide 1 — Hook (40 seconds):** Ask, “What takes more energy in a crowded week: doing the work, or deciding what to do next?” Introduce UniEnable as an offline student-built planning and accessibility tool.
- **Slide 2 — Problem (55 seconds):** Use one scenario: a lecture, a flexible assignment, and an unfamiliar route. Explain that each creates a separate planning decision.
- **Slide 3 — Product (70 seconds):** Show tasks/timetable, preference-aware recommendations, and facilities/routes. Clarify that preference hours are hard scheduling boundaries and route information is local reference data, not live campus conditions.
- **Slide 4 — Demo (2–3 minutes):** Run the four commands below. Let the output speak; do not explain every line.
- **Slide 5 — Audience action (60 seconds):** Pause and ask everyone to scan. Recommend the release QR to first-time users and the repository QR to students interested in code or contribution.
- **Slide 6 — Close (35 seconds):** Ask each person to try one workflow and report the first point where they hesitate.

## Safe demo commands

```text
preference view
recommend next week
recommend adopt
route from/AS7 to/CLB
```

When `recommend adopt` asks for confirmation, answer `n` so prepared demo data is not changed.

## If the live demo fails

Switch to the terminal screenshot after ten seconds. Describe the expected output, then continue to the QR slide. A short, confident fallback is better than debugging in front of the audience.

## Links

- Repository: <https://github.com/lukelouyu/SelfTeamProject>
- Release: <https://github.com/lukelouyu/SelfTeamProject/releases/tag/v2.0.1>
