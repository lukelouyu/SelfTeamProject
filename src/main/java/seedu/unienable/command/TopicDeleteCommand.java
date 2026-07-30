package seedu.unienable.command;

import seedu.unienable.exception.DuplicateActivityException;
import seedu.unienable.exception.InvalidIndexException;
import seedu.unienable.logic.TopicManager;
import seedu.unienable.model.enums.ActivityCategory;

/**
 * Deletes a topic that is not assigned to any activity. Confirmation ("Delete this topic?
 * (y/n)") is a UI-loop concern handled before this command is executed; execute() performs the
 * deletion immediately.
 */
public class TopicDeleteCommand extends Command {
    private final TopicManager topicManager;
    private final ActivityCategory category;
    private final String name;

    public TopicDeleteCommand(TopicManager topicManager, ActivityCategory category, String name) {
        this.topicManager = topicManager;
        this.category = category;
        this.name = name;
    }

    /** Returns the category of the topic this command will delete, for the UI loop's preview. */
    public ActivityCategory getCategory() {
        return category;
    }

    /** Returns the name of the topic this command will delete, for the UI loop's preview. */
    public String getName() {
        return name;
    }

    @Override
    public CommandResult execute() throws InvalidIndexException, DuplicateActivityException {
        topicManager.delete(category, name);
        return new CommandResult("Topic " + name + " has been deleted.");
    }
}
