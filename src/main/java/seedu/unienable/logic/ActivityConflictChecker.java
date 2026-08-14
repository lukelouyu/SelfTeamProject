package seedu.unienable.logic;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import seedu.unienable.exception.DuplicateActivityException;
import seedu.unienable.model.classes.Activity;
import seedu.unienable.model.classes.FixedActivity;
import seedu.unienable.model.classes.FlexibleActivity;

/**
 * Validates scheduling duplicates and occupied-interval overlaps without mutating activity state.
 *
 * <p>An activity "occupies" time - and can therefore conflict with another occupied interval -
 * only when it has an actual committed schedule: a {@link FixedActivity} always does, while a
 * {@link FlexibleActivity} only does once it has an adopted placement (see
 * {@link FlexibleActivity#hasAdoptedPlacement()}). An unadopted flexible activity represents just
 * a scheduling window, never an occupied interval, and can never conflict with anything.
 */
final class ActivityConflictChecker {
    private static final String DUPLICATE_MESSAGE = "An identical activity already exists.";
    private static final String OVERLAP_MESSAGE = "This timing overlaps activity [%d], %s (%s -> %s).";

    ActivityConflictChecker() {
    }

    /**
     * Checks one candidate against the supplied activities while ignoring every activity with the
     * excluded numeric ID.
     *
     * @param candidate activity whose scheduling details are being checked
     * @param excludedActivityId numeric ID to exclude from duplicate and overlap checks
     * @param activitiesToCheck existing activities to check in list order
     * @throws DuplicateActivityException if the candidate is a scheduling duplicate, or its
     *     effective occupied interval (see class Javadoc) overlaps another activity's
     */
    void checkNoConflicts(Activity candidate, int excludedActivityId,
            List<? extends Activity> activitiesToCheck) throws DuplicateActivityException {
        if (isDuplicate(candidate, excludedActivityId, activitiesToCheck)) {
            throw new DuplicateActivityException(DUPLICATE_MESSAGE);
        }
        Optional<ScheduledInterval> candidateInterval = effectiveInterval(candidate);
        if (candidateInterval.isEmpty()) {
            return;
        }
        Activity overlapping = findOverlap(candidateInterval.get(), excludedActivityId, activitiesToCheck);
        if (overlapping != null) {
            ScheduledInterval overlappingInterval = effectiveInterval(overlapping).orElseThrow();
            throw new DuplicateActivityException(String.format(OVERLAP_MESSAGE, overlapping.getId(),
                    overlapping.getDescription(), overlappingInterval.start(), overlappingInterval.end()));
        }
    }

    /**
     * Returns the given activity's effective occupied interval, or empty if it does not currently
     * occupy any time (an unadopted flexible activity).
     */
    private static Optional<ScheduledInterval> effectiveInterval(Activity activity) {
        if (activity instanceof FixedActivity) {
            FixedActivity fixed = (FixedActivity) activity;
            return Optional.of(new ScheduledInterval(fixed.getDate(), fixed.getStartTime(), fixed.getEndTime()));
        }
        if (activity instanceof FlexibleActivity) {
            FlexibleActivity flexible = (FlexibleActivity) activity;
            if (flexible.hasAdoptedPlacement()) {
                return Optional.of(new ScheduledInterval(flexible.getDate(), flexible.getAdoptedStartTime(),
                        flexible.getAdoptedEndTime()));
            }
        }
        return Optional.empty();
    }

    /** One activity's occupied date/time span, used only to compare candidates for overlap. */
    private record ScheduledInterval(LocalDate date, LocalTime start, LocalTime end) {
    }

    private boolean isDuplicate(Activity candidate, int excludedActivityId,
            List<? extends Activity> activitiesToCheck) {
        for (Activity existing : activitiesToCheck) {
            if (existing.getId() == excludedActivityId) {
                continue;
            }
            if (hasSameSchedulingDetails(existing, candidate)) {
                return true;
            }
        }
        return false;
    }

    private boolean hasSameSchedulingDetails(Activity existing, Activity candidate) {
        if (!existing.getDescription().equals(candidate.getDescription())
                || !existing.getDate().equals(candidate.getDate())) {
            return false;
        }
        if (existing instanceof FixedActivity && candidate instanceof FixedActivity) {
            FixedActivity fixedExisting = (FixedActivity) existing;
            FixedActivity fixedCandidate = (FixedActivity) candidate;
            return fixedExisting.getStartTime().equals(fixedCandidate.getStartTime())
                    && fixedExisting.getEndTime().equals(fixedCandidate.getEndTime());
        }
        if (existing instanceof FlexibleActivity && candidate instanceof FlexibleActivity) {
            FlexibleActivity flexibleExisting = (FlexibleActivity) existing;
            FlexibleActivity flexibleCandidate = (FlexibleActivity) candidate;
            return flexibleExisting.getEarliestStart().equals(flexibleCandidate.getEarliestStart())
                    && flexibleExisting.getLatestEnd().equals(flexibleCandidate.getLatestEnd())
                    && flexibleExisting.getDurationMinutes() == flexibleCandidate.getDurationMinutes();
        }
        return false;
    }

    private Activity findOverlap(ScheduledInterval candidateInterval, int excludedActivityId,
            List<? extends Activity> activitiesToCheck) {
        for (Activity existing : activitiesToCheck) {
            if (existing.getId() == excludedActivityId) {
                continue;
            }
            Optional<ScheduledInterval> existingInterval = effectiveInterval(existing);
            if (existingInterval.isPresent() && overlaps(candidateInterval, existingInterval.get())) {
                return existing;
            }
        }
        return null;
    }

    private static boolean overlaps(ScheduledInterval first, ScheduledInterval second) {
        return first.date().equals(second.date())
                && first.start().isBefore(second.end())
                && second.start().isBefore(first.end());
    }
}
