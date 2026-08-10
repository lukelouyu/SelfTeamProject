package seedu.unienable.logic.recur;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import seedu.unienable.exception.DuplicateActivityException;
import seedu.unienable.exception.InvalidActivityException;
import seedu.unienable.logic.ActivityManager;
import seedu.unienable.model.classes.Activity;
import seedu.unienable.model.classes.FixedActivity;
import seedu.unienable.model.recur.AcademicCalendar;
import seedu.unienable.model.recur.AcademicWeek;
import seedu.unienable.model.recur.RecurrencePlan;
import seedu.unienable.model.recur.RecurrencePlan.PlannedOccurrence;
import seedu.unienable.model.recur.RecurrencePlan.SkippedOccurrence;

/** Builds a complete, side-effect-free recurrence plan from calendar reference data. */
public class RecurrencePlanner {
    /**
     * Plans all requested occurrences and preflights every conflict before confirmation.
     *
     * @throws InvalidActivityException if the source/calendar/week selection is ineligible
     * @throws DuplicateActivityException if any new occurrence overlaps a fixed activity
     */
    public RecurrencePlan plan(FixedActivity source, List<Integer> requestedWeeks,
            AcademicCalendar calendar, ActivityManager activityManager)
            throws InvalidActivityException, DuplicateActivityException {
        AcademicWeek sourceWeek = calendar.findInstructionalWeekContaining(source.getDate())
                .orElseThrow(() -> new InvalidActivityException(
                        "activity date is not inside an instructional week in academic-calendar.txt."));
        if (!requestedWeeks.contains(sourceWeek.getWeekNumber())) {
            throw new InvalidActivityException("week specification must include source Week "
                    + sourceWeek.getWeekNumber() + ".");
        }

        List<PlannedOccurrence> toCreate = new ArrayList<>();
        List<SkippedOccurrence> skipped = new ArrayList<>();
        int nextId = activityManager.getNextId();

        for (int weekNumber : requestedWeeks) {
            AcademicWeek targetWeek = findTargetWeek(calendar, sourceWeek, weekNumber);
            LocalDate targetDate = targetWeek.findDate(source.getDate().getDayOfWeek())
                    .orElseThrow(() -> new InvalidActivityException("Week " + weekNumber
                            + " does not contain " + source.getDate().getDayOfWeek() + "."));

            if (targetDate.equals(source.getDate())) {
                skipped.add(new SkippedOccurrence(weekNumber, targetDate,
                        "source activity [" + source.getId() + "]"));
                continue;
            }

            Optional<String> noClassReason = calendar.findNoClassReason(targetDate);
            if (noClassReason.isPresent()) {
                skipped.add(new SkippedOccurrence(weekNumber, targetDate,
                        noClassReason.get() + " - no classes"));
                continue;
            }

            Activity existing = findIdenticalOccurrence(activityManager, source, targetDate);
            if (existing != null) {
                skipped.add(new SkippedOccurrence(weekNumber, targetDate,
                        "already exists as activity [" + existing.getId() + "]"));
                continue;
            }

            // Checked arithmetic: nextId comes from ActivityManager.getNextId(), which already
            // guarantees it is a valid available ID, but the batch offset added here could still
            // overflow int range in the (astronomically unlikely) case where nextId is already
            // close to Integer.MAX_VALUE - fail predictably instead of silently wrapping into a
            // bogus candidate ID.
            FixedActivity candidate = copyForDate(source, targetDate, Math.addExact(nextId, toCreate.size()));
            try {
                activityManager.checkNoConflicts(candidate, -1);
            } catch (DuplicateActivityException e) {
                throw new DuplicateActivityException(
                        "Week " + weekNumber + " (" + targetDate + "): " + e.getMessage());
            }
            toCreate.add(new PlannedOccurrence(weekNumber, candidate));
        }

        return new RecurrencePlan(source, sourceWeek, requestedWeeks, toCreate, skipped);
    }

    private AcademicWeek findTargetWeek(AcademicCalendar calendar, AcademicWeek sourceWeek,
            int weekNumber) throws InvalidActivityException {
        AcademicWeek target = calendar.findWeek(sourceWeek.getAcademicYear(),
                sourceWeek.getSemester(), weekNumber).orElseThrow(() ->
                        new InvalidActivityException("Week " + weekNumber + " is not defined for "
                                + sourceWeek.getAcademicYear() + " " + sourceWeek.getSemester()
                                + " in academic-calendar.txt."));
        if (!target.isInstructional()) {
            throw new InvalidActivityException("Week " + weekNumber
                    + " is not an instructional week in academic-calendar.txt.");
        }
        return target;
    }

    private Activity findIdenticalOccurrence(ActivityManager activityManager,
            FixedActivity source, LocalDate date) {
        for (Activity activity : activityManager.getAll()) {
            if (!(activity instanceof FixedActivity)) {
                continue;
            }
            FixedActivity fixed = (FixedActivity) activity;
            if (fixed.getDescription().equals(source.getDescription())
                    && fixed.getDate().equals(date)
                    && fixed.getStartTime().equals(source.getStartTime())
                    && fixed.getEndTime().equals(source.getEndTime())) {
                return fixed;
            }
        }
        return null;
    }

    private FixedActivity copyForDate(FixedActivity source, LocalDate date, int id) {
        return new FixedActivity(id, source.getDescription(), source.getCategory(), date,
                source.getStartTime(), source.getEndTime(), source.getEnergyRating(),
                source.getSensoryRating(), source.getTopic(), source.getNote());
    }
}
