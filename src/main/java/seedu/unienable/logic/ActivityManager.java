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
    private final List<Activity> activities = new ArrayList<>();
    private final ActivityConflictChecker conflictChecker = new ActivityConflictChecker();
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
     * Restores an exact in-memory snapshot after a transactional command (recur, reset) could
     * not be persisted. Unlike {@link #loadAll(List)}, this preserves a next-ID counter that may
     * be higher than the largest current ID, since earlier activities may have been deleted.
     *
     * @param snapshot activities captured immediately before the command ran
     * @param snapshotNextId the exact next-ID counter captured with the activities
     * @param snapshotOrder the exact default order captured with the activities
     * @throws IllegalArgumentException if the supplied next ID would collide with a snapshot ID
     */
    public void restoreState(List<Activity> snapshot, int snapshotNextId, ActivityOrder snapshotOrder) {
        int minimumNextId = 1;
        for (Activity activity : snapshot) {
            minimumNextId = Math.max(minimumNextId, activity.getId() + 1);
        }
        if (snapshotNextId < minimumNextId) {
            throw new IllegalArgumentException("snapshot next ID would collide with an activity");
        }
        activities.clear();
        activities.addAll(snapshot);
        nextId = snapshotNextId;
        defaultOrder = snapshotOrder;
    }

    /**
     * Adds the given activity and consumes its ID from the assignment counter.
     *
     * @param activity the activity to add, constructed using getNextId()'s current value
     * @throws DuplicateActivityException if it exactly duplicates an existing activity, or (for a
     *     FixedActivity) overlaps another fixed activity on the same date
     */
    public void add(Activity activity) throws DuplicateActivityException {
        conflictChecker.checkNoConflicts(activity, -1, activities);
        activities.add(activity);
        nextId++;
    }

    /**
     * Adds a preconstructed batch of activities (e.g. a recurrence plan's occurrences) as one
     * in-memory operation. Every candidate - including its stable ID and any conflict with
     * existing activities or with an earlier candidate in the same batch - is validated before
     * the stored list or next-ID counter changes, so a rejection partway through never leaves a
     * partial series added.
     *
     * @param candidates activities whose IDs must start at {@link #getNextId()} and increase by
     *     exactly one each
     * @throws DuplicateActivityException if any candidate exactly duplicates, or (for a
     *     FixedActivity) overlaps, existing or same-batch data
     * @throws IllegalArgumentException if a candidate's ID is not the expected consecutive value
     */
    public void addAllAtomically(List<? extends Activity> candidates) throws DuplicateActivityException {
        List<Activity> validated = new ArrayList<>(activities);
        for (int index = 0; index < candidates.size(); index++) {
            Activity candidate = candidates.get(index);
            int expectedId = nextId + index;
            if (candidate.getId() != expectedId) {
                throw new IllegalArgumentException("expected activity ID " + expectedId
                        + " but received " + candidate.getId());
            }
            conflictChecker.checkNoConflicts(candidate, -1, validated);
            validated.add(candidate);
        }
        activities.addAll(candidates);
        nextId += candidates.size();
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
        conflictChecker.checkNoConflicts(newActivity, id, activities);
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
        conflictChecker.checkNoConflicts(candidate, excludeId, activities);
    }

    private int indexOfId(int id) {
        for (int i = 0; i < activities.size(); i++) {
            if (activities.get(i).getId() == id) {
                return i;
            }
        }
        return -1;
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
     * Returns overdue incomplete activities (see {@link #isOverdue}) additionally matching the
     * given filter, ordered as requested. Used by "list overdue" - a separate, additive view;
     * the plain {@link #list(ActivityFilter, ActivityOrder)} continues to return every activity
     * regardless of overdue status, unchanged.
     *
     * @param now the current date and time
     * @param filter the filter criteria; null fields mean "no filter" for that field
     * @param order the one-shot order override, or null to use the saved default order
     * @return the matching overdue incomplete activities in the requested order
     */
    public List<Activity> listOverdue(LocalDateTime now, ActivityFilter filter, ActivityOrder order) {
        List<Activity> result = new ArrayList<>();
        for (Activity activity : activities) {
            if (!activity.isComplete() && isOverdue(activity, now) && filter.matches(activity)) {
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
