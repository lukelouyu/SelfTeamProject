package seedu.unienable.model.recur;

import java.time.LocalDate;

/** One read-only no-class date loaded from the external academic-calendar file. */
public final class NoClassDate {
    private final String academicYear;
    private final LocalDate date;
    private final String reason;

    /** Creates one no-class date record. */
    public NoClassDate(String academicYear, LocalDate date, String reason) {
        if (academicYear == null || academicYear.isBlank()
                || date == null || reason == null || reason.isBlank()) {
            throw new IllegalArgumentException("no-class record contains a blank field");
        }
        this.academicYear = academicYear;
        this.date = date;
        this.reason = reason;
    }

    /** Returns the academic-year label exactly as recorded in the calendar file. */
    public String getAcademicYear() {
        return academicYear;
    }

    /** Returns the date on which regular classes are not held. */
    public LocalDate getDate() {
        return date;
    }

    /** Returns the human-readable reason from the calendar file. */
    public String getReason() {
        return reason;
    }
}
