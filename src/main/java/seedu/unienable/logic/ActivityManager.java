package seedu.unienable.logic;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.OptionalInt;

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
    // Once every int ID up to and including Integer.MAX_VALUE has been consumed, no further int
    // can represent "the next ID" at all - nextId itself is left at Integer.MAX_VALUE (its last
    // valid value, never re-incremented) and this flag is the only source of truth for whether it
    // is still available to hand out.
    private boolean idSpaceExhausted;
    private ActivityOrder defaultOrder = ActivityOrder.CHRONOLOGICAL;

    /**
     * Returns the ID the next added activity will receive.
     *
     * @throws IllegalStateException if every int ID has already been consumed
     */
    public int getNextId() {
        requireIdSpaceAvailable();
        return nextId;
    }

    /**
     * Returns the ID the next added activity would receive, or empty if the ID space is already
     * exhausted - the same information as {@link #getNextId()}, but for a caller that only wants
     * to *report* the value (e.g. in a success message) and must not itself throw when there is
     * none to report.
     *
     * @return the next ID, or empty if exhausted
     */
    public OptionalInt tryGetNextId() {
        return idSpaceExhausted ? OptionalInt.empty() : OptionalInt.of(nextId);
    }

    /**
     * Returns whether any activity ID has ever been allocated - i.e. whether {@link #getNextId()}
     * would return anything other than {@code 1}. Unlike {@link #getNextId()}, this never throws,
     * even once the ID space is exhausted, since exhaustion itself is conclusive proof that IDs
     * have been allocated. Used by callers that only need to know "has anything ever been given
     * out" (e.g. to decide whether there is anything to reset), not to allocate a new ID.
     *
     * @return true if at least one ID has ever been allocated
     */
    public boolean hasAllocatedAnyId() {
        return idSpaceExhausted || nextId != 1;
    }

    /**
     * Captures the exact current next-ID bookkeeping - including whether the ID space is already
     * exhausted - without throwing, unlike {@link #getNextId()}. Used to take an exact
     * pre-command snapshot for possible later restoration (see {@link #restoreState}), which must
     * never itself be blocked by exhaustion the way actually allocating a new ID correctly is.
     *
     * @return the current next-ID allocation state
     */
    public IdAllocationState snapshotIdAllocation() {
        return new IdAllocationState(nextId, idSpaceExhausted);
    }

    /**
     * The exact next-ID bookkeeping state at a point in time: the next ID to hand out, and
     * whether the ID space is already exhausted (in which case {@code nextId} is a frozen,
     * no-longer-meaningful bookkeeping value - see {@link ActivityManager}'s own
     * {@code idSpaceExhausted} field comment).
     */
    public record IdAllocationState(int nextId, boolean idSpaceExhausted) {
    }

    /**
     * Replaces every stored activity with the given previously-saved activities, without
     * re-validating duplicates or overlaps, and syncs the next-ID counter past the highest loaded
     * ID (or marks the ID space exhausted if the highest loaded ID is {@code Integer.MAX_VALUE}).
     * Used to restore trusted data at startup; ordinary mutation should go through add() instead,
     * which does validate.
     *
     * @param loadedActivities the activities loaded from storage
     */
    public void loadAll(List<Activity> loadedActivities) {
        activities.clear();
        activities.addAll(loadedActivities);
        long candidateNextId = 1;
        for (Activity activity : activities) {
            candidateNextId = Math.max(candidateNextId, (long) activity.getId() + 1);
        }
        applyNextId(candidateNextId);
    }

    /**
     * Restores an exact in-memory snapshot after a transactional command (recur, reset) could
     * not be persisted. Unlike {@link #loadAll(List)}, this preserves a next-ID counter that may
     * be higher than the largest current ID, since earlier activities may have been deleted.
     *
     * @param snapshot activities captured immediately before the command ran
     * @param idAllocationState the exact next-ID bookkeeping (see {@link #snapshotIdAllocation()})
     *     captured with the activities, including whether the ID space was already exhausted
     * @param snapshotOrder the exact default order captured with the activities
     * @throws IllegalArgumentException if the supplied next ID would collide with a snapshot ID
     */
    public void restoreState(List<Activity> snapshot, IdAllocationState idAllocationState,
            ActivityOrder snapshotOrder) {
        long minimumNextId = 1;
        for (Activity activity : snapshot) {
            minimumNextId = Math.max(minimumNextId, (long) activity.getId() + 1);
        }
        // An exhausted state is never a collision risk: it represents an ID space that already
        // extends past every representable int, so it cannot fall short of any actual ID.
        if (!idAllocationState.idSpaceExhausted() && idAllocationState.nextId() < minimumNextId) {
            throw new IllegalArgumentException("snapshot next ID would collide with an activity");
        }
        activities.clear();
        activities.addAll(snapshot);
        nextId = idAllocationState.nextId();
        idSpaceExhausted = idAllocationState.idSpaceExhausted();
        defaultOrder = snapshotOrder;
    }

    /**
     * Adds the given activity and consumes its ID from the assignment counter.
     *
     * @param activity the activity to add, constructed using getNextId()'s current value
     * @throws DuplicateActivityException if it exactly duplicates an existing activity, or (for a
     *     FixedActivity) overlaps another fixed activity on the same date
     * @throws IllegalArgumentException if the activity does not carry the current next ID
     * @throws IllegalStateException if every int ID has already been consumed
     */
    public void add(Activity activity) throws DuplicateActivityException {
        int expectedId = getNextId();
        if (activity.getId() != expectedId) {
            throw new IllegalArgumentException("expected activity ID " + expectedId
                    + " but received " + activity.getId());
        }
        conflictChecker.checkNoConflicts(activity, -1, activities);
        activities.add(activity);
        advanceNextId();
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
     * @throws IllegalStateException if every int ID has already been consumed, or the batch would
     *     need an ID beyond {@code Integer.MAX_VALUE}
     */
    public void addAllAtomically(List<? extends Activity> candidates) throws DuplicateActivityException {
        int startId = getNextId();
        long finalNextId = (long) startId + candidates.size();
        if (finalNextId > ((long) Integer.MAX_VALUE) + 1L) {
            throw new IllegalStateException("Activity ID space cannot fit " + candidates.size()
                    + " more activities: only " + (Integer.MAX_VALUE - (long) startId + 1) + " ID(s) remain.");
        }
        List<Activity> validated = new ArrayList<>(activities);
        for (int index = 0; index < candidates.size(); index++) {
            Activity candidate = candidates.get(index);
            int expectedId = startId + index;
            if (candidate.getId() != expectedId) {
                throw new IllegalArgumentException("expected activity ID " + expectedId
                        + " but received " + candidate.getId());
            }
            conflictChecker.checkNoConflicts(candidate, -1, validated);
            validated.add(candidate);
        }
        activities.addAll(candidates);
        applyNextId(finalNextId);
    }

    private void requireIdSpaceAvailable() {
        if (idSpaceExhausted) {
            throw new IllegalStateException("Activity ID space is exhausted: no further activity IDs are "
                    + "available.");
        }
    }

    private void advanceNextId() {
        if (nextId == Integer.MAX_VALUE) {
            idSpaceExhausted = true;
        } else {
            nextId++;
        }
    }

    /**
     * Sets nextId/idSpaceExhausted from a freshly computed candidate next-ID value that may
     * itself already be one past {@code Integer.MAX_VALUE}.
     *
     * @param candidateNextId the next ID to hand out, or {@code Integer.MAX_VALUE + 1} if the ID
     *     space is exhausted
     */
    private void applyNextId(long candidateNextId) {
        if (candidateNextId > Integer.MAX_VALUE) {
            idSpaceExhausted = true;
            nextId = Integer.MAX_VALUE;
        } else {
            idSpaceExhausted = false;
            nextId = (int) candidateNextId;
        }
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
     * @throws IllegalArgumentException if the replacement does not carry the stable ID being
     *     replaced
     */
    public void replace(int id, Activity newActivity) throws InvalidIndexException, DuplicateActivityException {
        int index = indexOfId(id);
        if (index == -1) {
            throw new InvalidIndexException("Activity [" + id + "] does not exist.");
        }
        if (newActivity.getId() != id) {
            throw new IllegalArgumentException("replacement ID " + newActivity.getId()
                    + " does not match stable activity ID " + id);
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
        case CHRONOLOGICAL:
            // TIME used to compare only time-of-day (no date term at all), so two matching
            // activities sharing a start time on different dates fell through to an ID
            // tie-break instead of the earlier date sorting first - list and find both call
            // this same method, so the defect affected either command identically. TIME now
            // shares CHRONOLOGICAL's exact comparator rather than maintaining a second, subtly
            // different one; both marker strings ("order/time"/"order/chronological") remain
            // separately valid input, they just resolve to the same ordering.
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
            FlexibleActivity flexible = (FlexibleActivity) activity;
            return flexible.hasAdoptedPlacement() ? flexible.getAdoptedStartTime() : flexible.getEarliestStart();
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
        Optional<Activity> inProgress = findScheduledInProgress(now);
        if (inProgress.isPresent()) {
            return inProgress;
        }
        Optional<Activity> upcoming = findNearestUpcomingScheduled(now);
        if (upcoming.isPresent()) {
            return upcoming;
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
            return LocalDateTime.of(flexible.getDate(),
                    flexible.hasAdoptedPlacement() ? flexible.getAdoptedEndTime() : flexible.getLatestEnd())
                    .isBefore(now);
        }
        return false;
    }

    private Optional<Activity> findScheduledInProgress(LocalDateTime now) {
        for (Activity activity : activities) {
            if (activity.isComplete()) {
                continue;
            }
            LocalDateTime start = scheduledStart(activity);
            LocalDateTime end = scheduledEnd(activity);
            if (start == null || !start.toLocalDate().equals(now.toLocalDate())) {
                continue;
            }
            if (!now.isBefore(start) && now.isBefore(end)) {
                return Optional.of(activity);
            }
        }
        return Optional.empty();
    }

    private Optional<Activity> findNearestUpcomingScheduled(LocalDateTime now) {
        Activity nearest = null;
        LocalDateTime nearestStart = null;
        for (Activity activity : activities) {
            if (activity.isComplete()) {
                continue;
            }
            LocalDateTime start = scheduledStart(activity);
            if (start == null || !start.isAfter(now)) {
                continue;
            }
            if (nearest == null || start.isBefore(nearestStart)
                    || (start.isEqual(nearestStart) && activity.getId() < nearest.getId())) {
                nearest = activity;
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
            LocalDateTime end = LocalDateTime.of(flexible.getDate(),
                    flexible.hasAdoptedPlacement() ? flexible.getAdoptedEndTime() : flexible.getLatestEnd());
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

    private LocalDateTime scheduledStart(Activity activity) {
        if (activity instanceof FixedActivity) {
            FixedActivity fixed = (FixedActivity) activity;
            return LocalDateTime.of(fixed.getDate(), fixed.getStartTime());
        }
        if (activity instanceof FlexibleActivity) {
            FlexibleActivity flexible = (FlexibleActivity) activity;
            if (flexible.hasAdoptedPlacement()) {
                return LocalDateTime.of(flexible.getDate(), flexible.getAdoptedStartTime());
            }
        }
        return null;
    }

    private LocalDateTime scheduledEnd(Activity activity) {
        if (activity instanceof FixedActivity) {
            FixedActivity fixed = (FixedActivity) activity;
            return LocalDateTime.of(fixed.getDate(), fixed.getEndTime());
        }
        if (activity instanceof FlexibleActivity) {
            FlexibleActivity flexible = (FlexibleActivity) activity;
            if (flexible.hasAdoptedPlacement()) {
                return LocalDateTime.of(flexible.getDate(), flexible.getAdoptedEndTime());
            }
        }
        return null;
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
        idSpaceExhausted = false;
        defaultOrder = ActivityOrder.CHRONOLOGICAL;
    }
}
