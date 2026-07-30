package seedu.unienable.command;

import java.util.List;

import seedu.unienable.logic.TopicManager;
import seedu.unienable.model.classes.Topic;
import seedu.unienable.model.enums.ActivityCategory;

/**
 * Lists topics. With no category filter, shows every category's topics on one line each. With a
 * category filter, shows only that category's topics as a numbered list.
 */
public class TopicListCommand extends Command {
    private static final String CATEGORY_LINE_FORMAT = "%-15s: %s";

    private final TopicManager topicManager;
    private final ActivityCategory category;

    public TopicListCommand(TopicManager topicManager, ActivityCategory category) {
        this.topicManager = topicManager;
        this.category = category;
    }

    @Override
    public CommandResult execute() {
        return new CommandResult(category == null ? formatAllCategories() : formatOneCategory(category));
    }

    private String formatAllCategories() {
        StringBuilder result = new StringBuilder("Here are your topics:");
        for (ActivityCategory eachCategory : ActivityCategory.values()) {
            List<Topic> topics = topicManager.list(eachCategory);
            String names = topics.isEmpty() ? "No topics" : joinNames(topics);
            result.append('\n').append(String.format(CATEGORY_LINE_FORMAT, eachCategory, names));
        }
        return result.toString();
    }

    private String formatOneCategory(ActivityCategory selectedCategory) {
        List<Topic> topics = topicManager.list(selectedCategory);
        StringBuilder result = new StringBuilder(selectedCategory + " topics:");
        if (topics.isEmpty()) {
            return result.append("\nNo topics.").toString();
        }
        for (int i = 0; i < topics.size(); i++) {
            result.append('\n').append(i + 1).append(". ").append(topics.get(i).getName());
        }
        return result.toString();
    }

    private String joinNames(List<Topic> topics) {
        StringBuilder names = new StringBuilder();
        for (int i = 0; i < topics.size(); i++) {
            if (i > 0) {
                names.append(", ");
            }
            names.append(topics.get(i).getName());
        }
        return names.toString();
    }
}
