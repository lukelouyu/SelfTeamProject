package seedu.unienable.logic.recur;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
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
     * <p>Reuses the same "not in the past" philosophy as normal add/edit (see
     * {@code DateTimeParser.parseNotBeforeDate}/{@code requireNotPastIfToday}): a requested week
     * that would resolve to a new occurrence dated before {@code now}'s date, or dated today with
     * the source's start time not after {@code now}'s time, is rejected atomically - the whole
     * plan is rejected before the confirmation prompt and nothing is created, rather than silently
     * creating the past occurrence or skipping just that one week.
     *
     * @throws InvalidActivityException if the source/calendar/week selection is ineligible, or any
     *     requested week would resolve to a new occurrence dated at or before {@code now}
     * @throws DuplicateActivityException if any new occurrence overlaps a fixed activity
     */
    public RecurrencePlan plan(FixedActivity source, List<Integer> requestedWeeks,
            AcademicCalendar calendar, ActivityManager activityManager, LocalDateTime now)
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

            requireNotPast(weekNumber, targetDate, source.getStartTime(), now);

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

            // nextId comes from ActivityManager.getNextId(), which already guarantees it is a
            // valid available ID, but the batch offset added here could still push the candidate
            // past int range in the (astronomically unlikely) case where nextId is already close
            // to Integer.MAX_VALUE. Computed as long and explicitly rejected with a domain
            // exception - not Math.addExact's raw ArithmeticException, which is an anticipated
            // capacity limit here, not an unexpected internal failure - so it fails predictably
            // instead of silently wrapping into a bogus candidate ID.
            long candidateId = (long) nextId + toCreate.size();
            if (candidateId > Integer.MAX_VALUE) {
                throw new InvalidActivityException(
                        "not enough activity IDs remain to create this many recurring sessions.");
            }
            FixedActivity candidate = copyForDate(source, targetDate, (int) candidateId);
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

    /**
     * Rejects a candidate occurrence dated before {@code now}'s date, or dated today with the
     * source's start time not strictly after {@code now}'s time - the same two-tier "not in the
     * past" check {@code DateTimeParser.parseNotBeforeDate}/{@code requireNotPastIfToday} apply to
     * a freshly typed {@code add}/{@code edit} date.
     */
    private void requireNotPast(int weekNumber, LocalDate targetDate, LocalTime sourceStartTime, LocalDateTime now)
            throws InvalidActivityException {
        LocalDate today = now.toLocalDate();
        if (targetDate.isBefore(today)) {
            throw new InvalidActivityException("Week " + weekNumber + " resolves to " + targetDate
                    + ", which has already passed.\nNo recurring activities were created.");
        }
        if (targetDate.equals(today) && !sourceStartTime.isAfter(now.toLocalTime())) {
            throw new InvalidActivityException("Week " + weekNumber + " resolves to " + targetDate + " "
                    + sourceStartTime + ", which has already passed today.\nNo recurring activities were created.");
        }
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
