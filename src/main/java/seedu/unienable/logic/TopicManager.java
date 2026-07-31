package seedu.unienable.logic;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import seedu.unienable.exception.DuplicateActivityException;
import seedu.unienable.exception.InvalidIndexException;
import seedu.unienable.model.classes.Activity;
import seedu.unienable.model.classes.Topic;
import seedu.unienable.model.enums.ActivityCategory;

/** Manages the in-memory registry of topics, scoped per category. A topic name is unique within its category. */
public class TopicManager {
    private final Map<ActivityCategory, List<Topic>> topicsByCategory = new LinkedHashMap<>();
    private final ActivityManager activityManager;

    /**
     * Creates a TopicManager with an empty topic list for every fixed category.
     *
     * @param activityManager the manager whose activities are consulted and updated when topics
     *     are renamed or deleted
     */
    public TopicManager(ActivityManager activityManager) {
        this.activityManager = activityManager;
        for (ActivityCategory category : ActivityCategory.values()) {
            topicsByCategory.put(category, new ArrayList<>());
        }
    }

    /**
     * Replaces every stored topic with the given previously-saved topics, without re-validating
     * duplicate names. Used to restore trusted data at startup; ordinary mutation should go
     * through add() instead, which does validate.
     *
     * @param loadedTopics the topics loaded from storage
     */
    public void loadAll(List<Topic> loadedTopics) {
        for (List<Topic> topics : topicsByCategory.values()) {
            topics.clear();
        }
        for (Topic topic : loadedTopics) {
            topicsByCategory.get(topic.getCategory()).add(topic);
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
        topicsByCategory.get(category).add(new Topic(category, name));
    }

    /**
     * Returns whether a topic with the given name (case-insensitive) exists under the category.
     *
     * @param category the category to check
     * @param name the topic name
     * @return true if the topic exists under that category
     */
    public boolean exists(ActivityCategory category, String name) {
        return findTopic(category, name) != null;
    }

    /**
     * Returns the topics under the given category, in creation order.
     *
     * @param category the category to list
     * @return an unmodifiable view of that category's topics
     */
    public List<Topic> list(ActivityCategory category) {
        return Collections.unmodifiableList(topicsByCategory.get(category));
    }

    /**
     * Returns every stored topic across all categories, in a deterministic order: categories in
     * {@link ActivityCategory#values()} order, each category's topics in creation order. Used to
     * persist the full topic registry without exposing the internal per-category map.
     *
     * @return a defensive copy of every topic, safe to iterate or store independently of this
     *     manager's internal state
     */
    public List<Topic> getAll() {
        List<Topic> all = new ArrayList<>();
        for (ActivityCategory category : ActivityCategory.values()) {
            all.addAll(topicsByCategory.get(category));
        }
        return Collections.unmodifiableList(all);
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
        checkCanRename(category, oldName, newName);
        Topic topic = findTopic(category, oldName);
        topic.setName(newName);

        int updated = 0;
        for (Activity activity : activityManager.getAll()) {
            if (activity.getCategory() == category && oldName.equalsIgnoreCase(activity.getTopic())) {
                activity.setTopic(newName);
                updated++;
            }
        }
        return updated;
    }

    /**
     * Checks whether renaming oldName to newName under category would succeed, without
     * performing the rename. Used as a side-effect-free preflight check so a doomed rename can be
     * rejected before a confirmation prompt is shown, rather than only failing later when
     * rename() itself runs after the user has already answered "y".
     *
     * @param category the category the topic belongs to
     * @param oldName the topic's current name
     * @param newName the topic's proposed new name
     * @throws InvalidIndexException if oldName does not exist under that category
     * @throws DuplicateActivityException if newName already exists (case-insensitively) under
     *     that category as a different topic
     */
    public void checkCanRename(ActivityCategory category, String oldName, String newName)
            throws InvalidIndexException, DuplicateActivityException {
        if (findTopic(category, oldName) == null) {
            throw new InvalidIndexException("Topic \"" + oldName + "\" does not exist under " + category + ".");
        }
        if (!oldName.equalsIgnoreCase(newName) && exists(category, newName)) {
            throw new DuplicateActivityException(
                    "Topic \"" + newName + "\" already exists under " + category + ".");
        }
    }

    /**
     * Deletes a topic from the registry. Confirmation prompts, if any, are the caller's
     * responsibility; this performs the deletion outright.
     *
     * @param category the category the topic belongs to
     * @param name the topic name
     * @throws InvalidIndexException if the topic does not exist under that category
     * @throws DuplicateActivityException if any activity under that category is still assigned
     *     to it
     */
    public void delete(ActivityCategory category, String name)
            throws InvalidIndexException, DuplicateActivityException {
        checkCanDelete(category, name);
        topicsByCategory.get(category).remove(findTopic(category, name));
    }

    /**
     * Checks whether deleting a topic would succeed, without performing the deletion. Used as a
     * side-effect-free preflight check so a doomed delete can be rejected before a confirmation
     * prompt is shown, rather than only failing later when delete() itself runs after the user
     * has already answered "y".
     *
     * @param category the category the topic belongs to
     * @param name the topic name
     * @throws InvalidIndexException if the topic does not exist under that category
     * @throws DuplicateActivityException if any activity under that category is still assigned
     *     to it
     */
    public void checkCanDelete(ActivityCategory category, String name)
            throws InvalidIndexException, DuplicateActivityException {
        if (findTopic(category, name) == null) {
            throw new InvalidIndexException("Topic \"" + name + "\" does not exist under " + category + ".");
        }
        int usageCount = countActivitiesUsing(category, name);
        if (usageCount > 0) {
            throw new DuplicateActivityException("Topic " + name + " is used by " + usageCount + " activities.");
        }
    }

    private int countActivitiesUsing(ActivityCategory category, String name) {
        int count = 0;
        for (Activity activity : activityManager.getAll()) {
            if (activity.getCategory() == category && name.equalsIgnoreCase(activity.getTopic())) {
                count++;
            }
        }
        return count;
    }

    /** Clears every stored topic across every category. Used by "reset all". */
    public void resetAll() {
        for (List<Topic> topics : topicsByCategory.values()) {
            topics.clear();
        }
    }

    private Topic findTopic(ActivityCategory category, String name) {
        for (Topic topic : topicsByCategory.get(category)) {
            if (topic.getName().equalsIgnoreCase(name)) {
                return topic;
            }
        }
        return null;
    }
}
