package seedu.unienable.model.recur;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/**
 * Immutable academic-calendar snapshot loaded from one external text file. It contains no
 * built-in academic year, week limit, semester dates, or holiday data.
 */
public final class AcademicCalendar {
    private final List<AcademicWeek> weeks;
    private final Map<String, AcademicWeek> weeksByKey;
    private final Map<LocalDate, NoClassDate> noClassDates;

    /**
     * Creates and validates a calendar snapshot.
     *
     * @throws IllegalArgumentException if week keys/date ranges or no-class dates are ambiguous
     */
    public AcademicCalendar(List<AcademicWeek> weeks, List<NoClassDate> noClassDates) {
        this.weeks = Collections.unmodifiableList(new ArrayList<>(weeks));
        this.weeksByKey = indexWeeks(this.weeks);
        this.noClassDates = indexNoClassDates(noClassDates);
        validateInstructionalWeekRanges(this.weeks);
    }

    private Map<String, AcademicWeek> indexWeeks(List<AcademicWeek> academicWeeks) {
        Map<String, AcademicWeek> indexed = new HashMap<>();
        for (AcademicWeek week : academicWeeks) {
            String key = keyOf(week.getAcademicYear(), week.getSemester(), week.getWeekNumber());
            if (indexed.putIfAbsent(key, week) != null) {
                throw new IllegalArgumentException("duplicate academic year/semester/week record");
            }
        }
        return indexed;
    }

    private Map<LocalDate, NoClassDate> indexNoClassDates(List<NoClassDate> dates) {
        Map<LocalDate, NoClassDate> indexed = new HashMap<>();
        for (NoClassDate noClassDate : dates) {
            if (indexed.putIfAbsent(noClassDate.getDate(), noClassDate) != null) {
                throw new IllegalArgumentException("duplicate no-class date record");
            }
        }
        return Collections.unmodifiableMap(indexed);
    }

    private void validateInstructionalWeekRanges(List<AcademicWeek> academicWeeks) {
        for (int i = 0; i < academicWeeks.size(); i++) {
            AcademicWeek first = academicWeeks.get(i);
            if (!first.isInstructional()) {
                continue;
            }
            for (int j = i + 1; j < academicWeeks.size(); j++) {
                AcademicWeek second = academicWeeks.get(j);
                if (second.isInstructional() && rangesOverlap(first, second)) {
                    throw new IllegalArgumentException("overlapping instructional week ranges");
                }
            }
        }
    }

    private boolean rangesOverlap(AcademicWeek first, AcademicWeek second) {
        return !first.getEndDate().isBefore(second.getStartDate())
                && !second.getEndDate().isBefore(first.getStartDate());
    }

    /** Returns the instructional week containing the supplied date, if one exists. */
    public Optional<AcademicWeek> findInstructionalWeekContaining(LocalDate date) {
        for (AcademicWeek week : weeks) {
            if (week.isInstructional() && week.contains(date)) {
                return Optional.of(week);
            }
        }
        return Optional.empty();
    }

    /** Finds a week using the file-defined academic-year, semester, and week number. */
    public Optional<AcademicWeek> findWeek(String academicYear, String semester, int weekNumber) {
        return Optional.ofNullable(weeksByKey.get(keyOf(academicYear, semester, weekNumber)));
    }

    /** Returns the no-class reason for a date, if the external file defines one. */
    public Optional<String> findNoClassReason(LocalDate date) {
        NoClassDate noClassDate = noClassDates.get(date);
        return noClassDate == null ? Optional.empty() : Optional.of(noClassDate.getReason());
    }

    /** Returns every loaded week record as an unmodifiable list. */
    public List<AcademicWeek> getWeeks() {
        return weeks;
    }

    private String keyOf(String academicYear, String semester, int weekNumber) {
        return academicYear.toLowerCase(Locale.ROOT) + "\u0000"
                + semester.toLowerCase(Locale.ROOT) + "\u0000" + weekNumber;
    }
}
