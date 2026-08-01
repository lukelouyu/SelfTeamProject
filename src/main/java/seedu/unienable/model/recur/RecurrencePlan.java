package seedu.unienable.model.recur;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import seedu.unienable.model.classes.FixedActivity;

/**
 * Side-effect-free result of planning one recurrence command. It records all new, existing, and
 * skipped occurrences so the UI can show a complete confirmation preview before mutation.
 */
public final class RecurrencePlan {
    private final FixedActivity source;
    private final AcademicWeek sourceWeek;
    private final List<Integer> requestedWeeks;
    private final List<PlannedOccurrence> occurrencesToCreate;
    private final List<SkippedOccurrence> skippedOccurrences;

    /** Creates one immutable recurrence plan. */
    public RecurrencePlan(FixedActivity source, AcademicWeek sourceWeek,
            List<Integer> requestedWeeks, List<PlannedOccurrence> occurrencesToCreate,
            List<SkippedOccurrence> skippedOccurrences) {
        this.source = source;
        this.sourceWeek = sourceWeek;
        this.requestedWeeks = immutableCopy(requestedWeeks);
        this.occurrencesToCreate = immutableCopy(occurrencesToCreate);
        this.skippedOccurrences = immutableCopy(skippedOccurrences);
    }

    private static <T> List<T> immutableCopy(List<T> source) {
        return Collections.unmodifiableList(new ArrayList<>(source));
    }

    /** Returns the fixed activity from which this plan was derived. */
    public FixedActivity getSource() {
        return source;
    }

    /** Returns the calendar-defined week containing the source activity. */
    public AcademicWeek getSourceWeek() {
        return sourceWeek;
    }

    /** Returns the normalized requested week numbers. */
    public List<Integer> getRequestedWeeks() {
        return requestedWeeks;
    }

    /** Returns the new occurrences to add in chronological order. */
    public List<PlannedOccurrence> getOccurrencesToCreate() {
        return occurrencesToCreate;
    }

    /** Returns source/existing/no-class occurrences that will not be created. */
    public List<SkippedOccurrence> getSkippedOccurrences() {
        return skippedOccurrences;
    }

    /** Returns whether this plan would add at least one new activity. */
    public boolean hasOccurrencesToCreate() {
        return !occurrencesToCreate.isEmpty();
    }

    /** One new occurrence that will become an independently manageable fixed activity. */
    public static final class PlannedOccurrence {
        private final int weekNumber;
        private final FixedActivity activity;

        /** Creates one planned occurrence. */
        public PlannedOccurrence(int weekNumber, FixedActivity activity) {
            this.weekNumber = weekNumber;
            this.activity = activity;
        }

        /** Returns the calendar-defined teaching week. */
        public int getWeekNumber() {
            return weekNumber;
        }

        /** Returns the fully constructed activity that will be added. */
        public FixedActivity getActivity() {
            return activity;
        }
    }

    /** One requested occurrence deliberately not created, together with the reason. */
    public static final class SkippedOccurrence {
        private final int weekNumber;
        private final LocalDate date;
        private final String reason;

        /** Creates one skipped-occurrence record. */
        public SkippedOccurrence(int weekNumber, LocalDate date, String reason) {
            this.weekNumber = weekNumber;
            this.date = date;
            this.reason = reason;
        }

        /** Returns the calendar-defined teaching week. */
        public int getWeekNumber() {
            return weekNumber;
        }

        /** Returns the date that was considered. */
        public LocalDate getDate() {
            return date;
        }

        /** Returns why no new activity will be created. */
        public String getReason() {
            return reason;
        }
    }
}
