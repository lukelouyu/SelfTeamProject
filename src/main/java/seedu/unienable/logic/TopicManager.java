package seedu.unienable.logic;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import seedu.unienable.exception.DuplicateActivityException;
import seedu.unienable.model.enums.ActivityCategory;

/** Manages the in-memory registry of topics, scoped per category. A topic name is unique within its category. */
public class TopicManager {
    private final Map<ActivityCategory, List<String>> topicsByCategory = new LinkedHashMap<>();

    public TopicManager() {
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
}
