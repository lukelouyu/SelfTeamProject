package seedu.unienable.model.classes;

import java.util.logging.Level;
import java.util.logging.Logger;

import seedu.unienable.model.enums.ActivityCategory;

/** A user-defined topic: an optional one-level grouping of activities within a fixed category. */
public class Topic {
    private static final Logger logger = Logger.getLogger(Topic.class.getName());

    private final ActivityCategory category;
    private String name;

    /**
     * Creates a Topic under the given category.
     *
     * @param category the fixed category this topic belongs to
     * @param name the topic name
     */
    public Topic(ActivityCategory category, String name) {
        this.category = category;
        this.name = name;
    }

    /** Returns the fixed category this topic belongs to. */
    public ActivityCategory getCategory() {
        return category;
    }

    /** Returns the topic name. */
    public String getName() {
        return name;
    }

    /**
     * Renames this topic. The category is fixed and cannot change.
     *
     * @param name the new topic name
     */
    public void setName(String name) {
        logger.log(Level.INFO, "Renaming topic under " + category + " from '" + this.name + "' to '" + name + "'.");
        this.name = name;
    }

    @Override
    public String toString() {
        return category + " / " + name;
    }
}
