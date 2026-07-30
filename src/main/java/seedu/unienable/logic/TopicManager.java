package seedu.unienable.logic;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import seedu.unienable.exception.DuplicateActivityException;
import seedu.unienable.exception.InvalidIndexException;
import seedu.unienable.model.classes.Activity;
import seedu.unienable.model.enums.ActivityCategory;

/** Manages the in-memory registry of topics, scoped per category. A topic name is unique within its category. */
public class TopicManager {
    private final Map<ActivityCategory, List<String>> topicsByCategory = new LinkedHashMap<>();
    private final ActivityManager activityManager;

    public TopicManager(ActivityManager activityManager) {
        this.activityManager = activityManager;
        for (ActivityCategory category : ActivityCategory.values()) {
            topicsByCategory.put(category, new ArrayList<>());
        }
    }

    /**
     * Creates a topic under the given category.
     *
     * @param category the category to create the topic under
     * @param name the topic name
     * @throws DuplicateActivityException if the name already exists (case-insensitively) under
     *     that category
     */
    public void add(ActivityCategory category, String name) throws DuplicateActivityException {
        if (exists(category, name)) {
            throw new DuplicateActivityException("Topic \"" + name + "\" already exists under " + category + ".");
        }
        topicsByCategory.get(category).add(name);
    }

    /**
     * Returns whether a topic with the given name (case-insensitive) exists under the category.
     *
     * @param category the category to check
     * @param name the topic name
     * @return true if the topic exists under that category
     */
    public boolean exists(ActivityCategory category, String name) {
        for (String existing : topicsByCategory.get(category)) {
            if (existing.equalsIgnoreCase(name)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Returns the topics under the given category, in creation order.
     *
     * @param category the category to list
     * @return an unmodifiable view of that category's topics
     */
    public List<String> list(ActivityCategory category) {
        return Collections.unmodifiableList(topicsByCategory.get(category));
    }

    /**
     * Renames a topic, cascading the change to every activity currently assigned to it under the
     * same category.
     *
     * @param category the category the topic belongs to
     * @param oldName the topic's current name
     * @param newName the topic's new name
     * @return the number of activities updated
     * @throws InvalidIndexException if oldName does not exist under that category
     * @throws DuplicateActivityException if newName already exists (case-insensitively) under
     *     that category as a different topic
     */
    public int rename(ActivityCategory category, String oldName, String newName)
            throws InvalidIndexException, DuplicateActivityException {
        int index = indexOf(category, oldName);
        if (index == -1) {
            throw new InvalidIndexException("Topic \"" + oldName + "\" does not exist under " + category + ".");
        }
        if (!oldName.equalsIgnoreCase(newName) && exists(category, newName)) {
            throw new DuplicateActivityException(
                    "Topic \"" + newName + "\" already exists under " + category + ".");
        }
        topicsByCategory.get(category).set(index, newName);

        int updated = 0;
        for (Activity activity : activityManager.getAll()) {
            if (activity.getCategory() == category && oldName.equalsIgnoreCase(activity.getTopic())) {
                activity.setTopic(newName);
                updated++;
            }
        }
        return updated;
    }

    private int indexOf(ActivityCategory category, String name) {
        List<String> topics = topicsByCategory.get(category);
        for (int i = 0; i < topics.size(); i++) {
            if (topics.get(i).equalsIgnoreCase(name)) {
                return i;
            }
        }
        return -1;
    }
}
