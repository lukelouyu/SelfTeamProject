package seedu.unienable.logic;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import seedu.unienable.exception.InvalidIndexException;
import seedu.unienable.model.classes.Activity;

/** Manages the in-memory collection of activities: stable ID assignment plus basic CRUD access. */
public class ActivityManager {
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
     */
    public void add(Activity activity) {
        activities.add(activity);
        nextId++;
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

    /** Returns an unmodifiable view of every stored activity, in input order. */
    public List<Activity> getAll() {
        return Collections.unmodifiableList(activities);
    }

    /** Returns the number of stored activities. */
    public int size() {
        return activities.size();
    }
}
