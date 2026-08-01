package seedu.unienable.testutil.recur;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

import seedu.unienable.exception.InvalidActivityException;
import seedu.unienable.model.classes.EnergyRating;
import seedu.unienable.model.classes.FixedActivity;
import seedu.unienable.model.classes.SensoryRating;
import seedu.unienable.model.enums.ActivityCategory;
import seedu.unienable.model.recur.AcademicCalendar;
import seedu.unienable.model.recur.AcademicWeek;
import seedu.unienable.model.recur.NoClassDate;

/** Shared screenshot-derived calendar and activity fixtures for recurrence tests. */
public final class RecurrenceTestData {
    private RecurrenceTestData() {
    }

    /** Returns the CS2113 Friday lecture shown in the Semester 1 screenshot. */
    public static FixedActivity cs2113Lecture(int id) throws InvalidActivityException {
        return new FixedActivity(id, "CS2113 LEC [1]", ActivityCategory.ACADEMIC,
                LocalDate.of(2026, 8, 14), LocalTime.of(16, 0), LocalTime.of(18, 0),
                EnergyRating.of(2), SensoryRating.of(3), "CS2113", "Bring laptop");
    }

    /** Returns the CG3201 sparse-week lab shown in the Semester 2 screenshot. */
    public static FixedActivity cg3201Lab(int id) throws InvalidActivityException {
        return new FixedActivity(id, "CG3201 LAB [01]", ActivityCategory.ACADEMIC,
                LocalDate.of(2027, 1, 26), LocalTime.of(14, 0), LocalTime.of(17, 0),
                EnergyRating.of(3), SensoryRating.of(3), "CG3201", null);
    }

    /** Returns an in-memory AY2026/27 calendar with both regular semesters. */
    public static AcademicCalendar calendar() {
        List<AcademicWeek> weeks = new ArrayList<>();
        addSemester(weeks, "SEM1", LocalDate.of(2026, 8, 10),
                LocalDate.of(2026, 9, 28));
        addSemester(weeks, "SEM2", LocalDate.of(2027, 1, 11),
                LocalDate.of(2027, 3, 1));
        return new AcademicCalendar(weeks, List.of(
                new NoClassDate("AY2026/2027", LocalDate.of(2026, 10, 9),
                        "NUS Well-Being Day")));
    }

    private static void addSemester(List<AcademicWeek> weeks, String semester,
            LocalDate weekOneStart, LocalDate weekSevenStart) {
        for (int week = 1; week <= 6; week++) {
            LocalDate start = weekOneStart.plusWeeks(week - 1L);
            weeks.add(new AcademicWeek("AY2026/2027", semester, week, "INSTRUCTIONAL",
                    start, start.plusDays(4)));
        }
        for (int week = 7; week <= 13; week++) {
            LocalDate start = weekSevenStart.plusWeeks(week - 7L);
            LocalDate end = week == 7 ? start.plusDays(5) : start.plusDays(4);
            weeks.add(new AcademicWeek("AY2026/2027", semester, week, "INSTRUCTIONAL",
                    start, end));
        }
    }

    /** Writes a minimal valid calendar fixture to the supplied path. */
    public static void writeCalendar(Path path) throws IOException {
        StringBuilder text = new StringBuilder("FORMAT|1\n")
                .append("SOURCE|AY2026/2027|NUS Academic Calendar|2026-07-30\n");
        for (AcademicWeek week : calendar().getWeeks()) {
            text.append("WEEK|").append(week.getAcademicYear()).append('|')
                    .append(week.getSemester()).append('|').append(week.getWeekNumber())
                    .append('|').append(week.getKind()).append('|')
                    .append(week.getStartDate()).append('|').append(week.getEndDate()).append('\n');
        }
        text.append("NO_CLASS|AY2026/2027|2026-10-09|NUS Well-Being Day\n");
        Files.writeString(path, text.toString());
    }
}
