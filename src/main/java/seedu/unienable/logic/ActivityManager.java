package seedu.unienable.logic;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

import seedu.unienable.exception.DuplicateActivityException;
import seedu.unienable.exception.InvalidIndexException;
import seedu.unienable.model.classes.Activity;
import seedu.unienable.model.classes.FixedActivity;
import seedu.unienable.model.classes.FlexibleActivity;
import seedu.unienable.model.enums.ActivityOrder;

/** Manages the in-memory collection of activities: stable ID assignment plus basic CRUD access. */
public class ActivityManager {
    private static final String DUPLICATE_MESSAGE = "An identical activity already exists.";
    private static final String OVERLAP_MESSAGE = "This timing overlaps activity [%d], %s (%s -> %s).";

    private final List<Activity> activities = new ArrayList<>();
    private int nextId = 1;
    private ActivityOrder defaultOrder = ActivityOrder.CHRONOLOGICAL;

    /** Returns the ID the next added activity will receive. */
    public int getNextId() {
        return nextId;
    }

    /**
     * Replaces every stored activity with the given previously-saved activities, without
     * re-validating duplicates or overlaps, and syncs the next-ID counter past the highest loaded
     * ID. Used to restore trusted data at startup; ordinary mutation should go through add()
     * instead, which does validate.
     *
     * @param loadedActivities the activities loaded from storage
     */
    public void loadAll(List<Activity> loadedActivities) {
        activities.clear();
        activities.addAll(loadedActivities);
        nextId = 1;
        for (Activity activity : activities) {
            nextId = Math.max(nextId, activity.getId() + 1);
        }
    }

    /**
     * Adds the given activity and consumes its ID from the assignment counter.
     *
     * @param activity the activity to add, constructed using getNextId()'s current value
     * @throws DuplicateActivityException if it exactly duplicates an existing activity, or (for a
     *     FixedActivity) overlaps another fixed activity on the same date
     */
    public void add(Activity activity) throws DuplicateActivityException {
        validateNoDuplicateOrOverlap(activity, -1);
        activities.add(activity);
        nextId++;
    }

    /**
     * Replaces the activity with the given stable ID with a new activity carrying the same ID.
     * Used when editing changes an activity's scheduling type, since a stored object's runtime
     * class cannot change in place. Duplicate/overlap validation excludes the activity being
     * replaced, so re-submitting its own (possibly partly changed) timing is not rejected as a
     * conflict with itself.
     *
     * @param id the stable ID of the activity being replaced
     * @param newActivity the replacement activity, constructed with the same ID
     * @throws InvalidIndexException if no activity has that ID
     * @throws DuplicateActivityException if the replacement exactly duplicates another activity,
     *     or (for a FixedActivity) overlaps another fixed activity on the same date
     */
    public void replace(int id, Activity newActivity) throws InvalidIndexException, DuplicateActivityException {
        int index = indexOfId(id);
        if (index == -1) {
            throw new InvalidIndexException("Activity [" + id + "] does not exist.");
        }
        validateNoDuplicateOrOverlap(newActivity, id);
        activities.set(index, newActivity);
    }

    /**
     * Checks whether the given candidate would be accepted as a replacement for the activity with
     * the given ID, without mutating any stored state. Used as a side-effect-free preflight check
     * so a doomed edit (an exact duplicate, or a fixed-activity overlap) can be rejected before a
     * confirmation prompt is shown, rather than only failing later when replace() itself runs
     * after the user has already answered "y". replace() re-runs the same check at execution
     * time as defensive protection against state changes between the two calls.
     *
     * @param candidate the proposed replacement activity
     * @param excludeId the ID of the activity being replaced, excluded from the conflict check
     * @throws DuplicateActivityException if candidate exactly duplicates another activity, or
     *     (for a FixedActivity) overlaps another fixed activity on the same date
     */
    public void checkNoConflicts(Activity candidate, int excludeId) throws DuplicateActivityException {
        validateNoDuplicateOrOverlap(candidate, excludeId);
    }

    private void validateNoDuplicateOrOverlap(Activity candidate, int excludeId) throws DuplicateActivityException {
        if (isDuplicate(candidate, excludeId)) {
            throw new DuplicateActivityException(DUPLICATE_MESSAGE);
        }
        if (candidate instanceof FixedActivity) {
            FixedActivity overlapping = findOverlap((FixedActivity) candidate, excludeId);
            if (overlapping != null) {
                throw new DuplicateActivityException(String.format(OVERLAP_MESSAGE, overlapping.getId(),
                        overlapping.getDescription(), overlapping.getStartTime(), overlapping.getEndTime()));
            }
        }
    }

    private int indexOfId(int id) {
        for (int i = 0; i < activities.size(); i++) {
            if (activities.get(i).getId() == id) {
                return i;
            }
        }
        return -1;
    }

    private boolean isDuplicate(Activity candidate, int excludeId) {
        for (Activity existing : activities) {
            if (existing.getId() == excludeId) {
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
            FixedActivity a = (FixedActivity) existing;
            FixedActivity b = (FixedActivity) candidate;
            return a.getStartTime().equals(b.getStartTime()) && a.getEndTime().equals(b.getEndTime());
        }
        if (existing instanceof FlexibleActivity && candidate instanceof FlexibleActivity) {
            FlexibleActivity a = (FlexibleActivity) existing;
            FlexibleActivity b = (FlexibleActivity) candidate;
            return a.getEarliestStart().equals(b.getEarliestStart()) && a.getLatestEnd().equals(b.getLatestEnd())
                    && a.getDurationMinutes() == b.getDurationMinutes();
        }
        return false;
    }

    private FixedActivity findOverlap(FixedActivity candidate, int excludeId) {
        for (Activity existing : activities) {
            if (existing.getId() == excludeId || !(existing instanceof FixedActivity)) {
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

    /**
     * Finds the activity with the given stable ID.
     *
     * @param id the stable activity ID
     * @return the matching activity
     * @throws InvalidIndexException if no activity has that ID
     */
    public Activity getById(int id) throws InvalidIndexException {
        for (Activity activity : activities) {
            if (activity.getId() == id) {
                return activity;
            }
        }
        throw new InvalidIndexException("Activity [" + id + "] does not exist.");
    }

    /**
     * Deletes the activity with the given stable ID. Other activities' IDs are unaffected.
     *
     * @param id the stable activity ID
     * @throws InvalidIndexException if no activity has that ID
     */
    public void delete(int id) throws InvalidIndexException {
        activities.remove(getById(id));
    }

    /**
     * Marks the activity with the given stable ID as complete. Marking an already-complete
     * activity is allowed.
     *
     * @param id the stable activity ID
     * @return the marked activity
     * @throws InvalidIndexException if no activity has that ID
     */
    public Activity mark(int id) throws InvalidIndexException {
        Activity activity = getById(id);
        activity.mark();
        return activity;
    }

    /**
     * Marks the activity with the given stable ID as incomplete. Unmarking an already-incomplete
     * activity is allowed.
     *
     * @param id the stable activity ID
     * @return the unmarked activity
     * @throws InvalidIndexException if no activity has that ID
     */
    public Activity unmark(int id) throws InvalidIndexException {
        Activity activity = getById(id);
        activity.unmark();
        return activity;
    }

    /** Returns an unmodifiable view of every stored activity, in input order. */
    public List<Activity> getAll() {
        return Collections.unmodifiableList(activities);
    }

    /** Returns the saved default order used by list/find when no one-shot override is supplied. */
    public ActivityOrder getDefaultOrder() {
        return defaultOrder;
    }

    /**
     * Sets the saved default order used by list/find when no one-shot override is supplied.
     *
     * @param order the new default order
     */
    public void setDefaultOrder(ActivityOrder order) {
        defaultOrder = order;
    }

    /**
     * Returns activities matching the given filter, ordered as requested.
     *
     * @param filter the filter criteria; null fields mean "no filter" for that field
     * @param order the one-shot order override, or null to use the saved default order
     * @return the matching activities in the requested order
     */
    public List<Activity> list(ActivityFilter filter, ActivityOrder order) {
        List<Activity> result = new ArrayList<>();
        for (Activity activity : activities) {
            if (filter.matches(activity)) {
                result.add(activity);
            }
        }
        sort(result, order == null ? defaultOrder : order);
        return result;
    }

    /**
     * Finds activities matching every given keyword (each keyword matching description, topic, or
     * note, case-insensitively and partially) and the given filter, ordered as requested.
     *
     * @param keywords the keywords that must all match; may be empty to rely on filter alone
     * @param filter the filter criteria; null fields mean "no filter" for that field
     * @param order the one-shot order override, or null to use the saved default order
     * @return the matching activities in the requested order
     */
    public List<Activity> find(List<String> keywords, ActivityFilter filter, ActivityOrder order) {
        List<Activity> result = new ArrayList<>();
        for (Activity activity : activities) {
            if (filter.matches(activity) && matchesAllKeywords(activity, keywords)) {
                result.add(activity);
            }
        }
        sort(result, order == null ? defaultOrder : order);
        return result;
    }

    private boolean matchesAllKeywords(Activity activity, List<String> keywords) {
        for (String keyword : keywords) {
            if (!matchesAnyField(activity, keyword)) {
                return false;
            }
        }
        return true;
    }

    private boolean matchesAnyField(Activity activity, String keyword) {
        String lowerKeyword = keyword.toLowerCase(Locale.ROOT);
        return containsIgnoreCase(activity.getDescription(), lowerKeyword)
                || containsIgnoreCase(activity.getTopic(), lowerKeyword)
                || containsIgnoreCase(activity.getNote(), lowerKeyword);
    }

    private boolean containsIgnoreCase(String field, String lowerKeyword) {
        return field != null && field.toLowerCase(Locale.ROOT).contains(lowerKeyword);
    }

    private void sort(List<Activity> matched, ActivityOrder order) {
        switch (order) {
        case TIME:
            matched.sort(Comparator.comparing(ActivityManager::getSortTime).thenComparingInt(Activity::getId));
            break;
        case CHRONOLOGICAL:
            matched.sort(Comparator.comparing(Activity::getDate)
                    .thenComparing(ActivityManager::getSortTime)
                    .thenComparingInt(Activity::getId));
            break;
        case INPUT:
        default:
            break;
        }
    }

    private static LocalTime getSortTime(Activity activity) {
        if (activity instanceof FixedActivity) {
            return ((FixedActivity) activity).getStartTime();
        }
        if (activity instanceof FlexibleActivity) {
            return ((FlexibleActivity) activity).getEarliestStart();
        }
        throw new IllegalStateException("unknown activity type: " + activity.getClass());
    }

    /**
     * Selects the next relevant activity: an incomplete fixed activity in progress, otherwise the
     * nearest upcoming incomplete fixed activity, otherwise the incomplete flexible activity whose
     * window ends soonest. Completed and overdue activities are never selected.
     *
     * @param now the current date and time
     * @return the next relevant activity, or empty if none qualifies
     */
    public Optional<Activity> next(LocalDateTime now) {
        Optional<FixedActivity> inProgress = findFixedInProgress(now);
        if (inProgress.isPresent()) {
            return Optional.of(inProgress.get());
        }
        Optional<FixedActivity> upcoming = findNearestUpcomingFixed(now);
        if (upcoming.isPresent()) {
            return Optional.of(upcoming.get());
        }
        return findSoonestEndingFlexible(now).map(activity -> activity);
    }

    /**
     * Counts incomplete activities whose allowed time has already fully passed.
     *
     * @param now the current date and time
     * @return the number of overdue incomplete activities
     */
    public int countOverdueIncomplete(LocalDateTime now) {
        int count = 0;
        for (Activity activity : activities) {
            if (!activity.isComplete() && isOverdue(activity, now)) {
                count++;
            }
        }
        return count;
    }

    private boolean isOverdue(Activity activity, LocalDateTime now) {
        if (activity instanceof FixedActivity) {
            FixedActivity fixed = (FixedActivity) activity;
            return LocalDateTime.of(fixed.getDate(), fixed.getEndTime()).isBefore(now);
        }
        if (activity instanceof FlexibleActivity) {
            FlexibleActivity flexible = (FlexibleActivity) activity;
            return LocalDateTime.of(flexible.getDate(), flexible.getLatestEnd()).isBefore(now);
        }
        return false;
    }

    private Optional<FixedActivity> findFixedInProgress(LocalDateTime now) {
        for (Activity activity : activities) {
            if (!(activity instanceof FixedActivity) || activity.isComplete()) {
                continue;
            }
            FixedActivity fixed = (FixedActivity) activity;
            if (!fixed.getDate().equals(now.toLocalDate())) {
                continue;
            }
            LocalTime nowTime = now.toLocalTime();
            if (!nowTime.isBefore(fixed.getStartTime()) && nowTime.isBefore(fixed.getEndTime())) {
                return Optional.of(fixed);
            }
        }
        return Optional.empty();
    }

    private Optional<FixedActivity> findNearestUpcomingFixed(LocalDateTime now) {
        FixedActivity nearest = null;
        LocalDateTime nearestStart = null;
        for (Activity activity : activities) {
            if (!(activity instanceof FixedActivity) || activity.isComplete()) {
                continue;
            }
            FixedActivity fixed = (FixedActivity) activity;
            LocalDateTime start = LocalDateTime.of(fixed.getDate(), fixed.getStartTime());
            if (!start.isAfter(now)) {
                continue;
            }
            if (nearest == null || start.isBefore(nearestStart)
                    || (start.isEqual(nearestStart) && fixed.getId() < nearest.getId())) {
                nearest = fixed;
                nearestStart = start;
            }
        }
        return Optional.ofNullable(nearest);
    }

    private Optional<FlexibleActivity> findSoonestEndingFlexible(LocalDateTime now) {
        FlexibleActivity best = null;
        LocalDateTime bestEnd = null;
        for (Activity activity : activities) {
            if (!(activity instanceof FlexibleActivity) || activity.isComplete()) {
                continue;
            }
            FlexibleActivity flexible = (FlexibleActivity) activity;
            LocalDateTime end = LocalDateTime.of(flexible.getDate(), flexible.getLatestEnd());
            if (!end.isAfter(now)) {
                continue;
            }
            if (best == null || isBetterFlexibleCandidate(flexible, end, best, bestEnd)) {
                best = flexible;
                bestEnd = end;
            }
        }
        return Optional.ofNullable(best);
    }

    private boolean isBetterFlexibleCandidate(FlexibleActivity candidate, LocalDateTime candidateEnd,
            FlexibleActivity current, LocalDateTime currentEnd) {
        if (!candidateEnd.isEqual(currentEnd)) {
            return candidateEnd.isBefore(currentEnd);
        }
        LocalDateTime candidateStart = LocalDateTime.of(candidate.getDate(), candidate.getEarliestStart());
        LocalDateTime currentStart = LocalDateTime.of(current.getDate(), current.getEarliestStart());
        if (!candidateStart.isEqual(currentStart)) {
            return candidateStart.isBefore(currentStart);
        }
        return candidate.getId() < current.getId();
    }

    /** Returns the number of stored activities. */
    public int size() {
        return activities.size();
    }

    /**
     * Clears every stored activity, resets the next-ID counter back to 1, and resets the saved
     * default order to chronological. Used by "reset all"; unlike loadAll(), this always starts
     * from an empty, ID-1 state rather than restoring previously-saved data.
     */
    public void resetAll() {
        activities.clear();
        nextId = 1;
        defaultOrder = ActivityOrder.CHRONOLOGICAL;
    }
}
