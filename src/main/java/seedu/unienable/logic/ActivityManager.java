package seedu.unienable.logic;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import seedu.unienable.exception.DuplicateActivityException;
import seedu.unienable.exception.InvalidIndexException;
import seedu.unienable.model.classes.Activity;
import seedu.unienable.model.classes.FixedActivity;
import seedu.unienable.model.classes.FlexibleActivity;

/** Manages the in-memory collection of activities: stable ID assignment plus basic CRUD access. */
public class ActivityManager {
    private static final String DUPLICATE_MESSAGE = "An identical activity already exists.";
    private static final String OVERLAP_MESSAGE = "This timing overlaps activity [%d], %s (%s–%s).";

    private final List<Activity> activities = new ArrayList<>();
    private int nextId = 1;

    /** Returns the ID the next added activity will receive. */
    public int getNextId() {
        return nextId;
    }

    /**
     * Adds the given activity and consumes its ID from the assignment counter.
     *
     * @param activity the activity to add, constructed using getNextId()'s current value
     * @throws DuplicateActivityException if it exactly duplicates an existing activity, or (for a
     *     FixedActivity) overlaps another fixed activity on the same date
     */
    public void add(Activity activity) throws DuplicateActivityException {
        if (isDuplicate(activity)) {
            throw new DuplicateActivityException(DUPLICATE_MESSAGE);
        }
        if (activity instanceof FixedActivity) {
            FixedActivity overlapping = findOverlap((FixedActivity) activity);
            if (overlapping != null) {
                throw new DuplicateActivityException(String.format(OVERLAP_MESSAGE, overlapping.getId(),
                        overlapping.getDescription(), overlapping.getStartTime(), overlapping.getEndTime()));
            }
        }
        activities.add(activity);
        nextId++;
    }

    private boolean isDuplicate(Activity candidate) {
        for (Activity existing : activities) {
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

    private FixedActivity findOverlap(FixedActivity candidate) {
        for (Activity existing : activities) {
            if (!(existing instanceof FixedActivity)) {
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

    /**
     * Returns activities matching the given filter, in current storage (input) order.
     *
     * @param filter the filter criteria; null fields mean "no filter" for that field
     * @return the matching activities
     */
    public List<Activity> list(ActivityFilter filter) {
        List<Activity> result = new ArrayList<>();
        for (Activity activity : activities) {
            if (filter.matches(activity)) {
                result.add(activity);
            }
        }
        return result;
    }

    /** Returns the number of stored activities. */
    public int size() {
        return activities.size();
    }
}
