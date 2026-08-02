package seedu.unienable.logic;

import java.util.List;

import seedu.unienable.exception.DuplicateActivityException;
import seedu.unienable.model.classes.Activity;
import seedu.unienable.model.classes.FixedActivity;
import seedu.unienable.model.classes.FlexibleActivity;

/** Validates scheduling duplicates and fixed-activity overlaps without mutating activity state. */
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
     * @throws DuplicateActivityException if the candidate is a scheduling duplicate or overlaps
     *     an existing fixed activity
     */
    void checkNoConflicts(Activity candidate, int excludedActivityId,
            List<? extends Activity> activitiesToCheck) throws DuplicateActivityException {
        if (isDuplicate(candidate, excludedActivityId, activitiesToCheck)) {
            throw new DuplicateActivityException(DUPLICATE_MESSAGE);
        }
        if (candidate instanceof FixedActivity) {
            FixedActivity overlapping = findOverlap((FixedActivity) candidate, excludedActivityId,
                    activitiesToCheck);
            if (overlapping != null) {
                throw new DuplicateActivityException(String.format(OVERLAP_MESSAGE, overlapping.getId(),
                        overlapping.getDescription(), overlapping.getStartTime(), overlapping.getEndTime()));
            }
        }
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

    private FixedActivity findOverlap(FixedActivity candidate, int excludedActivityId,
            List<? extends Activity> activitiesToCheck) {
        for (Activity existing : activitiesToCheck) {
            if (existing.getId() == excludedActivityId || !(existing instanceof FixedActivity)) {
                continue;
            }
            FixedActivity fixedExisting = (FixedActivity) existing;
            if (!fixedExisting.getDate().equals(candidate.getDate())) {
                continue;
            }
            boolean overlaps = fixedExisting.getStartTime().isBefore(candidate.getEndTime())
                    && candidate.getStartTime().isBefore(fixedExisting.getEndTime());
            if (overlaps) {
                return fixedExisting;
            }
        }
        return null;
    }
}
