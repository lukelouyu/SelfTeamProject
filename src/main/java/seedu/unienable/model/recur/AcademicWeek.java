package seedu.unienable.model.recur;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.Optional;

/**
 * One calendar-defined academic week. Instances are immutable because they represent read-only
 * reference data loaded from {@code academic-calendar.txt}.
 */
public final class AcademicWeek {
    private final String academicYear;
    private final String semester;
    private final int weekNumber;
    private final String kind;
    private final LocalDate startDate;
    private final LocalDate endDate;

    /** Creates one academic-week record. */
    public AcademicWeek(String academicYear, String semester, int weekNumber, String kind,
            LocalDate startDate, LocalDate endDate) {
        if (academicYear == null || academicYear.isBlank()
                || semester == null || semester.isBlank()
                || kind == null || kind.isBlank()) {
            throw new IllegalArgumentException("calendar text fields must not be blank");
        }
        if (weekNumber < 0) {
            throw new IllegalArgumentException("week number must not be negative");
        }
        if (startDate == null || endDate == null || endDate.isBefore(startDate)) {
            throw new IllegalArgumentException("academic week has an invalid date range");
        }
        this.academicYear = academicYear;
        this.semester = semester;
        this.weekNumber = weekNumber;
        this.kind = kind;
        this.startDate = startDate;
        this.endDate = endDate;
    }

    /** Returns the academic-year label exactly as recorded in the calendar file. */
    public String getAcademicYear() {
        return academicYear;
    }

    /** Returns the semester label exactly as recorded in the calendar file. */
    public String getSemester() {
        return semester;
    }

    /** Returns this record's calendar-defined week number. */
    public int getWeekNumber() {
        return weekNumber;
    }

    /** Returns the generic week kind, such as {@code INSTRUCTIONAL} or {@code ORIENTATION}. */
    public String getKind() {
        return kind;
    }

    /** Returns the first date included in this week. */
    public LocalDate getStartDate() {
        return startDate;
    }

    /** Returns the final date included in this week. */
    public LocalDate getEndDate() {
        return endDate;
    }

    /** Returns whether this is an instructional week eligible for class recurrence. */
    public boolean isInstructional() {
        return "INSTRUCTIONAL".equalsIgnoreCase(kind);
    }

    /** Returns whether the supplied date falls inside this inclusive week range. */
    public boolean contains(LocalDate date) {
        return date != null && !date.isBefore(startDate) && !date.isAfter(endDate);
    }

    /**
     * Finds the date having the requested weekday inside this record's inclusive range.
     * No assumption is made that a calendar week starts on Monday or has seven days.
     */
    public Optional<LocalDate> findDate(DayOfWeek dayOfWeek) {
        LocalDate current = startDate;
        while (!current.isAfter(endDate)) {
            if (current.getDayOfWeek() == dayOfWeek) {
                return Optional.of(current);
            }
            current = current.plusDays(1);
        }
        return Optional.empty();
    }
}
